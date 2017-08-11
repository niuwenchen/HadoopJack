package com.jackniu.yarn.application;

/**
 * Created by JackNiu on 2017/8/9.
 */

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync.CallbackHandler;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

public class MyApplicationMaster {
    private static final Log LOG = LogFactory.getLog(MyApplicationMaster.class);
    private Configuration conf;
    private AMRMClientAsync amRMClient;
    private NMClientAsync nmClientAsync;
    private NMCallbackHandler containerListener;
    private ApplicationAttemptId appAttemptID;
    private String appMasterHostname = "";

    private int appMasterRpcPort = -1;

    private String appMasterTrackingUrl = "";

    private int numTotalContainers = 1;

    private int containerMemory = 10;
    private int requestPriority;
    private AtomicInteger numCompletedContainers = new AtomicInteger();

    private AtomicInteger numAllocatedContainers = new AtomicInteger();

    private AtomicInteger numFailedContainers = new AtomicInteger();

    private AtomicInteger numRequestedContainers = new AtomicInteger();

    private String shellCommand = "";

    private String shellArgs = "";

    private Map<String, String> shellEnv = new HashMap();

    private String shellScriptPath = "";

    private long shellScriptPathTimestamp = 0L;

    private long shellScriptPathLen = 0L;

    private final String ExecShellStringPath = "ExecShellScript.sh";
    private volatile boolean done;
    private volatile boolean success;
    private ByteBuffer allTokens;
    private List<Thread> launchThreads = new ArrayList();

    private void dumpOutDebugInfo()
    {
        LOG.info("Dump debug output");
        Map envs = System.getenv();
        for (Object xx: envs.entrySet()) {
            Map.Entry env = (Map.Entry)xx;
            LOG.info("System env: key=" + (String)env.getKey() + ", val=" + (String)env.getValue());
            System.out.println("System env: key=" + (String)env.getKey() + ", val=" + (String)env.getValue());
        }

        String cmd = "ls -al";
        Runtime run = Runtime.getRuntime();
        Process pr = null;
        try {
            pr = run.exec(cmd);
            pr.waitFor();

            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line = "";
            while ((line = buf.readLine()) != null) {
                LOG.info("System CWD content: " + line);
                System.out.println("System CWD content: " + line);
            }
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MyApplicationMaster()
    {
        this.conf = new YarnConfiguration();
    }

    public boolean init(String[] args)
            throws ParseException, IOException
    {
        Options opts = new Options();
        opts.addOption("app_attempt_id", true, "App Attempt ID. Not to be used unless for testing purposes");

        opts.addOption("shell_command", true, "Shell command to be executed by the Application Master");

        opts.addOption("shell_script", true, "Location of the shell script to be executed");

        opts.addOption("shell_args", true, "Command line args for the shell script");
        opts.addOption("shell_env", true, "Environment for shell script. Specified as env_key=env_val pairs");

        opts.addOption("container_memory", true, "Amount of memory in MB to be requested to run the shell command");

        opts.addOption("num_containers", true, "No. of containers on which the shell command needs to be executed");

        opts.addOption("priority", true, "Application Priority. Default 0");
        opts.addOption("debug", false, "Dump out debug information");

        opts.addOption("help", false, "Print usage");
        CommandLine cliParser = new GnuParser().parse(opts, args);

        if (args.length == 0) {
            printUsage(opts);
            throw new IllegalArgumentException("No args specified for application master to initialize");
        }

        if (cliParser.hasOption("help")) {
            printUsage(opts);
            return false;
        }

        if (cliParser.hasOption("debug")) {
            dumpOutDebugInfo();
        }

        Map envs = System.getenv();

        if (!envs.containsKey(ApplicationConstants.Environment.CONTAINER_ID.name())) {
            if (cliParser.hasOption("app_attempt_id")) {
                String appIdStr = cliParser.getOptionValue("app_attempt_id", "");
                this.appAttemptID = ConverterUtils.toApplicationAttemptId(appIdStr);
            } else {
                throw new IllegalArgumentException("Application Attempt Id not set in the environment");
            }
        }
        else {
            ContainerId containerId = ConverterUtils.toContainerId((String)envs.get(ApplicationConstants.Environment.CONTAINER_ID.name()));

            this.appAttemptID = containerId.getApplicationAttemptId();
        }

        if (!envs.containsKey("APP_SUBMIT_TIME_ENV")) {
            throw new RuntimeException("APP_SUBMIT_TIME_ENV not set in the environment");
        }

        if (!envs.containsKey(ApplicationConstants.Environment.NM_HOST.name())) {
            throw new RuntimeException(ApplicationConstants.Environment.NM_HOST.name() + " not set in the environment");
        }

        if (!envs.containsKey(ApplicationConstants.Environment.NM_HTTP_PORT.name())) {
            throw new RuntimeException(ApplicationConstants.Environment.NM_HTTP_PORT + " not set in the environment");
        }

        if (!envs.containsKey(ApplicationConstants.Environment.NM_PORT.name())) {
            throw new RuntimeException(ApplicationConstants.Environment.NM_PORT.name() + " not set in the environment");
        }

        LOG.info("Application master for app, appId=" + this.appAttemptID.getApplicationId().getId() + ", clustertimestamp=" + this.appAttemptID.getApplicationId().getClusterTimestamp() + ", attemptId=" + this.appAttemptID.getAttemptId());

        if (!cliParser.hasOption("shell_command")) {
            throw new IllegalArgumentException("No shell command specified to be executed by application master");
        }

        this.shellCommand = cliParser.getOptionValue("shell_command");

        if (cliParser.hasOption("shell_args")) {
            this.shellArgs = cliParser.getOptionValue("shell_args");
        }
        if (cliParser.hasOption("shell_env")) {
            String[] shellEnvs = cliParser.getOptionValues("shell_env");
            for (String env : shellEnvs) {
                env = env.trim();
                int index = env.indexOf(61);
                if (index == -1) {
                    this.shellEnv.put(env, "");
                }
                else {
                    String key = env.substring(0, index);
                    String val = "";
                    if (index < env.length() - 1) {
                        val = env.substring(index + 1);
                    }
                    this.shellEnv.put(key, val);
                }
            }
        }
        if (envs.containsKey("DISTRIBUTEDSHELLSCRIPTLOCATION")) {
            this.shellScriptPath = ((String)envs.get("DISTRIBUTEDSHELLSCRIPTLOCATION"));

            if (envs.containsKey("DISTRIBUTEDSHELLSCRIPTTIMESTAMP")) {
                this.shellScriptPathTimestamp = Long.valueOf((String)envs.get("DISTRIBUTEDSHELLSCRIPTTIMESTAMP")).longValue();
            }

            if (envs.containsKey("DISTRIBUTEDSHELLSCRIPTLEN")) {
                this.shellScriptPathLen = Long.valueOf((String)envs.get("DISTRIBUTEDSHELLSCRIPTLEN")).longValue();
            }

            if ((!(this.shellScriptPath.length()==0)) && ((this.shellScriptPathTimestamp <= 0L) || (this.shellScriptPathLen <= 0L)))
            {
                LOG.error("Illegal values in env for shell script path, path=" + this.shellScriptPath + ", len=" + this.shellScriptPathLen + ", timestamp=" + this.shellScriptPathTimestamp);

                throw new IllegalArgumentException("Illegal values in env for shell script path");
            }

        }

        this.containerMemory = Integer.parseInt(cliParser.getOptionValue("container_memory", "10"));

        this.numTotalContainers = Integer.parseInt(cliParser.getOptionValue("num_containers", "1"));

        if (this.numTotalContainers == 0) {
            throw new IllegalArgumentException("Cannot run distributed shell with no containers");
        }

        this.requestPriority = Integer.parseInt(cliParser.getOptionValue("priority", "0"));

        return true;
    }

    private void printUsage(Options opts)
    {
        new HelpFormatter().printHelp("ApplicationMaster", opts);
    }

    public boolean run()
            throws YarnException, IOException
    {
        LOG.info("Starting ApplicationMaster");

        Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();

        DataOutputBuffer dob = new DataOutputBuffer();
        credentials.writeTokenStorageToStream(dob);

        Iterator iter = credentials.getAllTokens().iterator();
        while (iter.hasNext()) {
            Token token = (Token)iter.next();
            if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
                iter.remove();
            }
        }
        this.allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());

        //对象负责与Resource Manager 交互
        AMRMClientAsync.CallbackHandler allocListener = new RMCallbackHandler();
        this.amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, allocListener);
        this.amRMClient.init(this.conf);
        this.amRMClient.start();

        // 上一个是RMCallback， 这一个是NMCallback
        this.containerListener = createNMCallbackHandler();
        this.nmClientAsync = new NMClientAsyncImpl(this.containerListener);
        this.nmClientAsync.init(this.conf);
        this.nmClientAsync.start();

        this.appMasterHostname = NetUtils.getHostname();
        RegisterApplicationMasterResponse response = this.amRMClient.registerApplicationMaster(this.appMasterHostname, this.appMasterRpcPort, this.appMasterTrackingUrl);

        int maxMem = response.getMaximumResourceCapability().getMemory();
        LOG.info("Max mem capabililty of resources in this cluster " + maxMem);

        if (this.containerMemory > maxMem) {
            LOG.info("Container memory specified above max threshold of cluster. Using max value., specified=" + this.containerMemory + ", max=" + maxMem);

            this.containerMemory = maxMem;
        }

        // 暂时到这里
        for (int i = 0; i < this.numTotalContainers; i++) {
            AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM();
            // 这里就是将所需的所有资源一次性发送给ResourceManager
            this.amRMClient.addContainerRequest(containerAsk);
        }
        this.numRequestedContainers.set(this.numTotalContainers);

        while ((!this.done) && (this.numCompletedContainers.get() != this.numTotalContainers))
            try {
                Thread.sleep(200L);
            } catch (InterruptedException ex) {
            }
        finish();

        return this.success;
    }

    @VisibleForTesting
    NMCallbackHandler createNMCallbackHandler() {
        return new NMCallbackHandler(this);
    }

    private void finish()
    {
        for (Thread launchThread : this.launchThreads) {
            try {
                launchThread.join(10000L);
            } catch (InterruptedException e) {
                LOG.info("Exception thrown in thread join: " + e.getMessage());
                e.printStackTrace();
            }

        }

        LOG.info("Application completed. Stopping running containers");
        this.nmClientAsync.stop();

        LOG.info("Application completed. Signalling finish to RM");

        String appMessage = null;
        this.success = true;
        FinalApplicationStatus appStatus;
        if ((this.numFailedContainers.get() == 0) && (this.numCompletedContainers.get() == this.numTotalContainers))
        {
            appStatus = FinalApplicationStatus.SUCCEEDED;
        } else {
            appStatus = FinalApplicationStatus.FAILED;
            appMessage = "Diagnostics., total=" + this.numTotalContainers + ", completed=" + this.numCompletedContainers.get() + ", allocated=" + this.numAllocatedContainers.get() + ", failed=" + this.numFailedContainers.get();

            this.success = false;
        }
        try {
            this.amRMClient.unregisterApplicationMaster(appStatus, appMessage, null);
        } catch (YarnException ex) {
            LOG.error("Failed to unregister application", ex);
        } catch (IOException e) {
            LOG.error("Failed to unregister application", e);
        }

        this.amRMClient.stop();
    }

    private AMRMClient.ContainerRequest setupContainerAskForRM()
    {
        Priority pri = (Priority)Records.newRecord(Priority.class);

        pri.setPriority(this.requestPriority);

        Resource capability = (Resource)Records.newRecord(Resource.class);
        capability.setMemory(this.containerMemory);

        AMRMClient.ContainerRequest request = new AMRMClient.ContainerRequest(capability, null, null, pri);

        LOG.info("Requested container ask: " + request.toString());
        return request;
    }
    private class LaunchContainerRunnable implements Runnable
    {
        Container container;
        MyApplicationMaster.NMCallbackHandler containerListener;

        public LaunchContainerRunnable(Container lcontainer, MyApplicationMaster.NMCallbackHandler containerListener)
        {
            this.container = lcontainer;
            this.containerListener = containerListener;
        }

        public void run()
        {
            MyApplicationMaster.LOG.info(new StringBuilder().append("Setting up container launch container for containerid=").append(this.container.getId()).toString());

            ContainerLaunchContext ctx = (ContainerLaunchContext)Records.newRecord(ContainerLaunchContext.class);

            ctx.setEnvironment(MyApplicationMaster.this.shellEnv);

            Map localResources = new HashMap();

            if (!(MyApplicationMaster.this.shellScriptPath.length()==0)) {
                LocalResource shellRsrc = (LocalResource)Records.newRecord(LocalResource.class);
                shellRsrc.setType(LocalResourceType.FILE);
                shellRsrc.setVisibility(LocalResourceVisibility.APPLICATION);
                try {
                    shellRsrc.setResource(ConverterUtils.getYarnUrlFromURI(new URI(MyApplicationMaster.this.shellScriptPath)));
                }
                catch (URISyntaxException e) {
                    MyApplicationMaster.LOG.error(new StringBuilder().append("Error when trying to use shell script path specified in env, path=").append(MyApplicationMaster.this.shellScriptPath).toString());

                    e.printStackTrace();

                    MyApplicationMaster.this.numCompletedContainers.incrementAndGet();
                    MyApplicationMaster.this.numFailedContainers.incrementAndGet();
                    return;
                }
                shellRsrc.setTimestamp(MyApplicationMaster.this.shellScriptPathTimestamp);
                shellRsrc.setSize(MyApplicationMaster.this.shellScriptPathLen);
                localResources.put("ExecShellScript.sh", shellRsrc);
            }
            ctx.setLocalResources(localResources);

            Vector vargs = new Vector(5);

            vargs.add(MyApplicationMaster.this.shellCommand);

            if (!(MyApplicationMaster.this.shellScriptPath.length()==0)) {
                vargs.add("ExecShellScript.sh");
            }

            vargs.add(MyApplicationMaster.this.shellArgs);

            vargs.add("1><LOG_DIR>/stdout");
            vargs.add("2><LOG_DIR>/stderr");

            StringBuilder command = new StringBuilder();
            for (Object str : vargs) {
                command.append(str).append(" ");
            }

            List commands = new ArrayList();
            commands.add(command.toString());
            ctx.setCommands(commands);

            ctx.setTokens(MyApplicationMaster.this.allTokens.duplicate());

            this.containerListener.addContainer(this.container.getId(), this.container);
            MyApplicationMaster.this.nmClientAsync.startContainerAsync(this.container, ctx);
        }
    }

    @VisibleForTesting
    static class NMCallbackHandler
            implements NMClientAsync.CallbackHandler
    {
        private ConcurrentMap<ContainerId, Container> containers = new ConcurrentHashMap();
        private final MyApplicationMaster applicationMaster;

        public NMCallbackHandler(MyApplicationMaster applicationMaster)
        {
            this.applicationMaster = applicationMaster;
        }

        public void addContainer(ContainerId containerId, Container container) {
            this.containers.putIfAbsent(containerId, container);
        }

        public void onContainerStopped(ContainerId containerId)
        {
            if (MyApplicationMaster.LOG.isDebugEnabled()) {
                MyApplicationMaster.LOG.debug("Succeeded to stop Container " + containerId);
            }
            this.containers.remove(containerId);
        }

        public void onContainerStatusReceived(ContainerId containerId, ContainerStatus containerStatus)
        {
            if (MyApplicationMaster.LOG.isDebugEnabled())
                MyApplicationMaster.LOG.debug("Container Status: id=" + containerId + ", status=" + containerStatus);
        }

        public void onContainerStarted(ContainerId containerId, Map<String, ByteBuffer> allServiceResponse)
        {
            if (MyApplicationMaster.LOG.isDebugEnabled()) {
                MyApplicationMaster.LOG.debug("Succeeded to start Container " + containerId);
            }
            Container container = (Container)this.containers.get(containerId);
            if (container != null)
                this.applicationMaster.nmClientAsync.getContainerStatusAsync(containerId, container.getNodeId());
        }

        public void onStartContainerError(ContainerId containerId, Throwable t)
        {
            MyApplicationMaster.LOG.error("Failed to start Container " + containerId);
            this.containers.remove(containerId);
            this.applicationMaster.numCompletedContainers.incrementAndGet();
            this.applicationMaster.numFailedContainers.incrementAndGet();
        }

        public void onGetContainerStatusError(ContainerId containerId, Throwable t)
        {
            MyApplicationMaster.LOG.error("Failed to query the status of Container " + containerId);
        }

        public void onStopContainerError(ContainerId containerId, Throwable t)
        {
            MyApplicationMaster.LOG.error("Failed to stop Container " + containerId);
            this.containers.remove(containerId);
        }
    }

    private class RMCallbackHandler implements AMRMClientAsync.CallbackHandler
    {
        private RMCallbackHandler()
        {
        }

        public void onContainersCompleted(List<ContainerStatus> completedContainers)
        {
            MyApplicationMaster.LOG.info("Got response from RM for container ask, completedCnt=" + completedContainers.size());

            for (ContainerStatus containerStatus : completedContainers) {
                MyApplicationMaster.LOG.info("Got container status for containerID=" + containerStatus.getContainerId() + ", state=" + containerStatus.getState() + ", exitStatus=" + containerStatus.getExitStatus() + ", diagnostics=" + containerStatus.getDiagnostics());

                assert (containerStatus.getState() == ContainerState.COMPLETE);

                int exitStatus = containerStatus.getExitStatus();
                if (0 != exitStatus)
                {
                    if (-100 != exitStatus)
                    {
                        MyApplicationMaster.this.numCompletedContainers.incrementAndGet();
                        MyApplicationMaster.this.numFailedContainers.incrementAndGet();
                    }
                    else
                    {
                        MyApplicationMaster.this.numAllocatedContainers.decrementAndGet();
                        MyApplicationMaster.this.numRequestedContainers.decrementAndGet();
                    }

                }
                else
                {
                    MyApplicationMaster.this.numCompletedContainers.incrementAndGet();
                    MyApplicationMaster.LOG.info("Container completed successfully., containerId=" + containerStatus.getContainerId());
                }

            }

            int askCount = MyApplicationMaster.this.numTotalContainers - MyApplicationMaster.this.numRequestedContainers.get();
            MyApplicationMaster.this.numRequestedContainers.addAndGet(askCount);

            if (askCount > 0) {
                for (int i = 0; i < askCount; i++) {
                    AMRMClient.ContainerRequest containerAsk = MyApplicationMaster.this.setupContainerAskForRM();
                    MyApplicationMaster.this.amRMClient.addContainerRequest(containerAsk);
                }
            }

            if (MyApplicationMaster.this.numCompletedContainers.get() == MyApplicationMaster.this.numTotalContainers)
                MyApplicationMaster.this.done = true;
        }

        // 在分配NodeManager之后进行回调
        public void onContainersAllocated(List<Container> allocatedContainers)
        {
            MyApplicationMaster.LOG.info("Got response from RM for container ask, allocatedCnt=" + allocatedContainers.size());

            MyApplicationMaster.this.numAllocatedContainers.addAndGet(allocatedContainers.size());
            for (Container allocatedContainer : allocatedContainers) {
                MyApplicationMaster.LOG.info("Launching shell command on a new container., containerId=" + allocatedContainer.getId() + ", containerNode=" + allocatedContainer.getNodeId().getHost() + ":" + allocatedContainer.getNodeId().getPort() + ", containerNodeURI=" + allocatedContainer.getNodeHttpAddress() + ", containerResourceMemory" + allocatedContainer.getResource().getMemory());

                MyApplicationMaster.LaunchContainerRunnable runnableLaunchContainer =
                        new MyApplicationMaster.LaunchContainerRunnable(allocatedContainer, MyApplicationMaster.this.containerListener);

                Thread launchThread = new Thread(runnableLaunchContainer);

                MyApplicationMaster.this.launchThreads.add(launchThread);
                launchThread.start();
            }
        }

        public void onShutdownRequest()
        {
            MyApplicationMaster.this.done = true;
        }

        public void onNodesUpdated(List<NodeReport> updatedNodes)
        {
        }

        public float getProgress()
        {
            float progress = MyApplicationMaster.this.numCompletedContainers.get() / MyApplicationMaster.this.numTotalContainers;

            return progress;
        }

        public void onError(Throwable e)
        {
            MyApplicationMaster.this.done = true;
            MyApplicationMaster.this.amRMClient.stop();
        }
    }

    public static void main(String[] args) {
        boolean result = false;
        try {
            MyApplicationMaster appMaster = new MyApplicationMaster();
            LOG.info("Initializing ApplicationMaster");
            boolean doRun = appMaster.init(args);
            if (!doRun) {
                System.exit(0);
            }
            result = appMaster.run();
        } catch (Throwable t) {
            LOG.fatal("Error running ApplicationMaster", t);
            System.exit(1);
        }
        if (result) {
            LOG.info("Application Master completed successfully. exiting");
            System.exit(0);
        } else {
            LOG.info("Application Master failed. exiting");
            System.exit(2);
        }
    }



}
