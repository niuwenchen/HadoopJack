package com.jackniu.yarn.application;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.*;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.ClientRMProxy;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Created by JackNiu on 2017/8/8.
 */
public class MyYarnClientImpl extends  YarnClient{

    private static final Log LOG = LogFactory.getLog(MyYarnClientImpl.class);
    protected ApplicationClientProtocol rmClient;
    protected InetSocketAddress rmAddress;
    protected long statePollIntervalMillis;
//    MRClientProtocol.MRClientProtocolService.Interface.

    private static final String ROOT = "root";

    public MyYarnClientImpl(){
        super(MyYarnClientImpl.class.getName());
    }

    private static InetSocketAddress getRmAddress(Configuration conf){
        return conf.getSocketAddr(YarnConfiguration.RM_ADDRESS,YarnConfiguration.DEFAULT_RM_ADDRESS, YarnConfiguration.DEFAULT_RM_PORT);
    }

    @Override
    protected void serviceInit(Configuration conf) throws Exception {
        this.rmAddress = getRmAddress(conf);
        statePollIntervalMillis = conf.getLong(
                YarnConfiguration.YARN_CLIENT_APP_SUBMISSION_POLL_INTERVAL_MS,
                YarnConfiguration.DEFAULT_YARN_CLIENT_APP_SUBMISSION_POLL_INTERVAL_MS);
        super.serviceInit(conf);
    }

    @Override
    protected void serviceStart() throws Exception {
        try {
            // Client 创建一个ApplicationClientProtocol协议的RPC Client，并通过该Client与ResourceManager 进行通信。
            this.rmClient = ClientRMProxy.createRMProxy(getConfig(), ApplicationClientProtocol.class);
        }catch(IOException e){
            throw  new YarnException(e);
        }
        super.serviceStart();
    }

    @Override
    protected void serviceStop() throws Exception {
        if (this.rmClient != null) {
            RPC.stopProxy(this.rmClient);
        }
        super.serviceStop();
    }

    private GetNewApplicationResponse getNewApplication() throws YarnException, IOException {
        // 请求
        // 构造一个可序列化的对象，具体采用的序列化工厂默认指定
        // 序列化工厂: org.apache.hadoop.yarn.factories.impl.pb.RecordFactoryPBImpl
        GetNewApplicationRequest request = Records.newRecord(GetNewApplicationRequest.class);
        return rmClient.getNewApplication(request);
    }

    public YarnClientApplication createApplication() throws YarnException, IOException {
        ApplicationSubmissionContext context=Records.newRecord(ApplicationSubmissionContext.class);
        // getNewApplication 获取唯一的 applicationID
        GetNewApplicationResponse newApp = getNewApplication();
        ApplicationId appId = newApp.getApplicationId();
        context.setApplicationId(appId);
        return new YarnClientApplication(newApp, context);
    }

    // 提交代码
    public ApplicationId submitApplication(ApplicationSubmissionContext appContext) throws YarnException, IOException {
        ApplicationId applicationId = appContext.getApplicationId();
        appContext.setApplicationId(applicationId);
        SubmitApplicationRequest request =
                Records.newRecord(SubmitApplicationRequest.class);
        request.setApplicationSubmissionContext(appContext);
        //将应用程序提交到ResourceManager 上
        rmClient.submitApplication(request);

        int pollCount = 0;
        while (true) {
            YarnApplicationState state =
                    getApplicationReport(applicationId).getYarnApplicationState();
            if (!state.equals(YarnApplicationState.NEW) &&
                    !state.equals(YarnApplicationState.NEW_SAVING)) {
                break;
            }
            // Notify the client through the log every 10 poll, in case the client
            // is blocked here too long.
            if (++pollCount % 10 == 0) {
                LOG.info("Application submission is not finished, " +
                        "submitted application " + applicationId +
                        " is still in " + state);
            }
            try {
                Thread.sleep(statePollIntervalMillis);
            } catch (InterruptedException ie) {
            }
        }

            LOG.info("Submitted application " + applicationId + " to ResourceManager"
                    + " at " + rmAddress);
            return applicationId;
    }

    public void killApplication(ApplicationId applicationId) throws YarnException, IOException {
        LOG.info("Killing application " + applicationId);
        KillApplicationRequest request =
                Records.newRecord(KillApplicationRequest.class);
        request.setApplicationId(applicationId);
        rmClient.forceKillApplication(request);
    }

    public ApplicationReport getApplicationReport(ApplicationId appId) throws YarnException, IOException {
        GetApplicationReportRequest request = Records.newRecord(GetApplicationReportRequest.class);
        request.setApplicationId(appId);
        GetApplicationReportResponse response = rmClient.getApplicationReport(request);

        return response.getApplicationReport();
    }

    public Token<AMRMTokenIdentifier> getAMRMToken(ApplicationId appId) throws YarnException, IOException {
        org.apache.hadoop.yarn.api.records.Token token = getApplicationReport(appId).getAMRMToken();
        org.apache.hadoop.security.token.Token<AMRMTokenIdentifier> amrmToken =
                null;
        if (token != null) {
            amrmToken = ConverterUtils.convertFromYarn(token, null);
        }
        return amrmToken;
    }

    public List<ApplicationReport> getApplications() throws YarnException, IOException {
        return getApplications(null, null);
    }

    public List<ApplicationReport> getApplications(Set<String> applicationTypes) throws YarnException, IOException {
        return getApplications(applicationTypes, null);
    }

    public List<ApplicationReport> getApplications(EnumSet<YarnApplicationState> applicationStates) throws YarnException, IOException {
        return getApplications(null, applicationStates);
    }

    public List<ApplicationReport> getApplications(Set<String> applicationTypes, EnumSet<YarnApplicationState> applicationStates) throws YarnException, IOException {
        GetApplicationsRequest request =
                GetApplicationsRequest.newInstance(applicationTypes, applicationStates);
        GetApplicationsResponse response = rmClient.getApplications(request);
        return response.getApplicationList();
    }

    public YarnClusterMetrics getYarnClusterMetrics() throws YarnException, IOException {
        GetClusterMetricsRequest request =
                Records.newRecord(GetClusterMetricsRequest.class);
        GetClusterMetricsResponse response = rmClient.getClusterMetrics(request);
        return response.getClusterMetrics();
    }

    public List<NodeReport> getNodeReports(NodeState... states) throws YarnException, IOException {
        EnumSet<NodeState> statesSet = (states.length == 0) ?
                EnumSet.allOf(NodeState.class) : EnumSet.noneOf(NodeState.class);
        for (NodeState state : states) {
            statesSet.add(state);
        }
        GetClusterNodesRequest request = GetClusterNodesRequest
                .newInstance(statesSet);
        GetClusterNodesResponse response = rmClient.getClusterNodes(request);
        return response.getNodeReports();
    }

    public org.apache.hadoop.yarn.api.records.Token getRMDelegationToken(Text renewer) throws YarnException, IOException {
        GetDelegationTokenRequest rmDTRequest =
                Records.newRecord(GetDelegationTokenRequest.class);
        rmDTRequest.setRenewer(renewer.toString());
        GetDelegationTokenResponse response =
                rmClient.getDelegationToken(rmDTRequest);
        return response.getRMDelegationToken();
    }

    private GetQueueInfoRequest
    getQueueInfoRequest(String queueName, boolean includeApplications,
                        boolean includeChildQueues, boolean recursive) {
        GetQueueInfoRequest request = Records.newRecord(GetQueueInfoRequest.class);
        request.setQueueName(queueName);
        request.setIncludeApplications(includeApplications);
        request.setIncludeChildQueues(includeChildQueues);
        request.setRecursive(recursive);
        return request;
    }

    public QueueInfo getQueueInfo(String queueName) throws YarnException, IOException {
        GetQueueInfoRequest request =
                getQueueInfoRequest(queueName, true, false, false);
        Records.newRecord(GetQueueInfoRequest.class);
        return rmClient.getQueueInfo(request).getQueueInfo();
    }

    public List<QueueInfo> getAllQueues() throws YarnException, IOException {
        List<QueueInfo> queues = new ArrayList<QueueInfo>();
        QueueInfo rootQueue =
                rmClient.getQueueInfo(getQueueInfoRequest(ROOT, false, true, true))
                        .getQueueInfo();
        getChildQueues(rootQueue, queues, true);
        return queues;
    }

    public List<QueueInfo> getRootQueueInfos() throws YarnException, IOException {
        List<QueueInfo> queues = new ArrayList<QueueInfo>();

        QueueInfo rootQueue =
                rmClient.getQueueInfo(getQueueInfoRequest(ROOT, false, true, true))
                        .getQueueInfo();
        getChildQueues(rootQueue, queues, false);
        return queues;
    }

    private void getChildQueues(QueueInfo parent, List<QueueInfo> queues,
                                boolean recursive) {
        List<QueueInfo> childQueues = parent.getChildQueues();

        for (QueueInfo child : childQueues) {
            queues.add(child);
            if (recursive) {
                getChildQueues(child, queues, recursive);
            }
        }
    }
    public List<QueueInfo> getChildQueueInfos(String parent) throws YarnException, IOException {
        List<QueueInfo> queues = new ArrayList<QueueInfo>();

        QueueInfo parentQueue =
                rmClient.getQueueInfo(getQueueInfoRequest(parent, false, true, false))
                        .getQueueInfo();
        getChildQueues(parentQueue, queues, true);
        return queues;
    }

    public List<QueueUserACLInfo> getQueueAclsInfo() throws YarnException, IOException {
        GetQueueUserAclsInfoRequest request =
                Records.newRecord(GetQueueUserAclsInfoRequest.class);
        return rmClient.getQueueUserAcls(request).getUserAclsInfoList();
    }

    @InterfaceAudience.Private
    @VisibleForTesting
    public void setRMClient(ApplicationClientProtocol rmClient) {
        this.rmClient = rmClient;
    }


}
