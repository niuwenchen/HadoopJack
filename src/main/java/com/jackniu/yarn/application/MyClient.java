package com.jackniu.yarn.application;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.apache.hadoop.security.token.Token;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by JackNiu on 2017/8/9.
 */
public class MyClient {
    private static final Log LOG = LogFactory.getLog(MyClient.class);
    private Configuration conf;
    private YarnClient yarnClient;
    private String appName = "";

    private int amPriority = 0;

    private String amQueue = "";

    private int amMemory = 10;

    private String appMasterJar = "";
    private final String appMasterMainClass;
    private String shellCommand = "";

    private String shellScriptPath = "";

    private String shellArgs = "";

    private Map<String, String> shellEnv = new HashMap();

    private int shellCmdPriority = 0;

    private int containerMemory = 10;

    private int numContainers = 1;

    private String log4jPropFile = "";

    private final long clientStartTime = System.currentTimeMillis();

    private long clientTimeout = 600000L;

    boolean debugFlag = false;
    private Options opts;

    public MyClient(Configuration conf) throws Exception
    {
        this("org.apache.hadoop.yarn.applications.distributedshell.ApplicationMaster", conf);
    }
    public MyClient(String appMasterMainClass,Configuration conf){
        this.appMasterMainClass=appMasterMainClass;
        this.yarnClient = YarnClient.createYarnClient();
        /*
        * serviceInit(config);
        *  if (isInState(STATE.INITED)) {
        *    //if the service ended up here during init,
        *    //notify the listeners
        *    notifyListeners();
        *  }
        *  应该是将这个客户端发布为服务吗？ 还是初始化这个client
        * */
        this.yarnClient.init(conf);
        this.opts = new Options();
        this.opts.addOption("appname", true, "Application Name. Default value - DistributedShell");
        this.opts.addOption("priority", true, "Application Priority. Default 0");
        this.opts.addOption("queue", true, "RM Queue in which this application is to be submitted");
        this.opts.addOption("timeout", true, "Application timeout in milliseconds");
        this.opts.addOption("master_memory", true, "Amount of memory in MB to be requested to run the application master");
        this.opts.addOption("jar", true, "Jar file containing the application master");
        this.opts.addOption("shell_command", true, "Shell command to be executed by the Application Master");
        this.opts.addOption("shell_script", true, "Location of the shell script to be executed");
        this.opts.addOption("shell_args", true, "Command line args for the shell script");
        this.opts.addOption("shell_env", true, "Environment for shell script. Specified as env_key=env_val pairs");
        this.opts.addOption("shell_cmd_priority", true, "Priority for the shell command containers");
        this.opts.addOption("container_memory", true, "Amount of memory in MB to be requested to run the shell command");
        this.opts.addOption("num_containers", true, "No. of containers on which the shell command needs to be executed");
        this.opts.addOption("log_properties", true, "log4j.properties file");
        this.opts.addOption("debug", false, "Dump out debug information");
        this.opts.addOption("help", false, "Print usage");

    }
    public MyClient()  throws Exception
    {
        this(new YarnConfiguration());
    }
    private void printUsage()
    {
        new HelpFormatter().printHelp("Client", this.opts);
    }
    public boolean init(String[] args) throws ParseException {
        CommandLine cliParser = new GnuParser().parse(this.opts, args);

        if (args.length == 0) {
            throw new IllegalArgumentException("No args specified for client to initialize");
        }

        if (cliParser.hasOption("help")) {
            printUsage();
            return false;
        }
        if (cliParser.hasOption("debug")) {
            this.debugFlag = true;
        }

        this.appName = cliParser.getOptionValue("appname", "DistributedShell");
        this.amPriority = Integer.parseInt(cliParser.getOptionValue("priority", "0"));
        this.amQueue = cliParser.getOptionValue("queue", "default");
        this.amMemory = Integer.parseInt(cliParser.getOptionValue("master_memory", "10"));
        if (this.amMemory < 0) {
            throw new IllegalArgumentException(new StringBuilder().append("Invalid memory specified for application master, exiting. Specified memory=").append(this.amMemory).toString());
        }

        if (!cliParser.hasOption("jar")) {
            throw new IllegalArgumentException("No jar file specified for application master");
        }

        this.appMasterJar = cliParser.getOptionValue("jar");

        if (!cliParser.hasOption("shell_command")) {
            throw new IllegalArgumentException("No shell command specified to be executed by application master");
        }
        this.shellCommand = cliParser.getOptionValue("shell_command");

        if (cliParser.hasOption("shell_script")) {
            this.shellScriptPath = cliParser.getOptionValue("shell_script");
        }
        if (cliParser.hasOption("shell_args")) {
            this.shellArgs = cliParser.getOptionValue("shell_args");
        }
        if (cliParser.hasOption("shell_env")) {
            String[] envs = cliParser.getOptionValues("shell_env");
            for (String env : envs) {
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
        this.shellCmdPriority = Integer.parseInt(cliParser.getOptionValue("shell_cmd_priority", "0"));

        this.containerMemory = Integer.parseInt(cliParser.getOptionValue("container_memory", "10"));
        this.numContainers = Integer.parseInt(cliParser.getOptionValue("num_containers", "1"));

        if ((this.containerMemory < 0) || (this.numContainers < 1)) {
            throw new IllegalArgumentException(new StringBuilder().append("Invalid no. of containers or container memory specified, exiting. Specified containerMemory=").append(this.containerMemory).append(", numContainer=").append(this.numContainers).toString());
        }

        this.clientTimeout = Integer.parseInt(cliParser.getOptionValue("timeout", "600000"));

        this.log4jPropFile = cliParser.getOptionValue("log_properties", "");

        return true;
    }

    public boolean run() throws IOException, YarnException {
        LOG.info("Running Client");
        this.yarnClient.start();

        YarnClusterMetrics yarnClusterMetrics = this.yarnClient.getYarnClusterMetrics();
        LOG.info(new StringBuilder().append("Got Cluster metric info from ASM, numNodeManagers=").append(yarnClusterMetrics.getNumNodeManagers()).toString());

        List<NodeReport> clusterNodeReports = this.yarnClient.getNodeReports(new NodeState[]{NodeState.RUNNING});

        LOG.info("Got Cluster node info from ASM");
        for (NodeReport node : clusterNodeReports) {
            LOG.info(new StringBuilder().append("Got node report from ASM for, nodeId=").append(node.getNodeId()).append(", nodeAddress").append(node.getHttpAddress()).append(", nodeRackName").append(node.getRackName()).append(", nodeNumContainers").append(node.getNumContainers()).toString());
        }

        QueueInfo queueInfo = this.yarnClient.getQueueInfo(this.amQueue);
        LOG.info(new StringBuilder().append("Queue info, queueName=").append(queueInfo.getQueueName()).append(", queueCurrentCapacity=").append(queueInfo.getCurrentCapacity()).append(", queueMaxCapacity=").append(queueInfo.getMaximumCapacity()).append(", queueApplicationCount=").append(queueInfo.getApplications().size()).append(", queueChildQueueCount=").append(queueInfo.getChildQueues().size()).toString());

        QueueUserACLInfo aclInfo;
        List<QueueUserACLInfo> listAclInfo = this.yarnClient.getQueueAclsInfo();
        for (Iterator i$ = listAclInfo.iterator(); i$.hasNext(); ) {
            aclInfo = (QueueUserACLInfo) i$.next();
            for (QueueACL userAcl : aclInfo.getUserAcls())
                LOG.info(new StringBuilder().append("User ACL Info for Queue, queueName=").append(aclInfo.getQueueName()).append(", userAcl=").append(userAcl.name()).toString());
        }

        YarnClientApplication app = this.yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
        int maxMem = appResponse.getMaximumResourceCapability().getMemory();
        LOG.info(new StringBuilder().append("Max mem capabililty of resources in this cluster ").append(maxMem).toString());

        if (this.amMemory > maxMem) {
            LOG.info(new StringBuilder().append("AM memory specified above max threshold of cluster. Using max value., specified=").append(this.amMemory).append(", max=").append(maxMem).toString());

            this.amMemory = maxMem;
        }
        // 提交客户端
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();
        appContext.setApplicationName(this.appName);

        // set up the container launch context for application master
        ContainerLaunchContext amContainer = (ContainerLaunchContext) Records.newRecord(ContainerLaunchContext.class);

        Map localResources = new HashMap();

        LOG.info("Copy App Master jar from local filesystem and add to local environment");
        FileSystem fs = FileSystem.get(this.conf);
        Path src = new Path(this.appMasterJar);
        String pathSuffix = new StringBuilder().append(this.appName).append("/").append(appId.getId()).append("/AppMaster.jar").toString();
        Path dst = new Path(fs.getHomeDirectory(), pathSuffix);
        fs.copyFromLocalFile(false, true, src, dst);
        FileStatus destStatus = fs.getFileStatus(dst);
        LocalResource amJarRsrc = (LocalResource) Records.newRecord(LocalResource.class);

        amJarRsrc.setType(LocalResourceType.FILE);

        amJarRsrc.setVisibility(LocalResourceVisibility.APPLICATION);

        amJarRsrc.setResource(ConverterUtils.getYarnUrlFromPath(dst));

        amJarRsrc.setTimestamp(destStatus.getModificationTime());
        amJarRsrc.setSize(destStatus.getLen());
        localResources.put("AppMaster.jar", amJarRsrc);

        if (!(this.log4jPropFile.length() == 0)) {
            Path log4jSrc = new Path(this.log4jPropFile);
            Path log4jDst = new Path(fs.getHomeDirectory(), "log4j.props");
            fs.copyFromLocalFile(false, true, log4jSrc, log4jDst);
            FileStatus log4jFileStatus = fs.getFileStatus(log4jDst);
            LocalResource log4jRsrc = (LocalResource) Records.newRecord(LocalResource.class);
            log4jRsrc.setType(LocalResourceType.FILE);
            log4jRsrc.setVisibility(LocalResourceVisibility.APPLICATION);
            log4jRsrc.setResource(ConverterUtils.getYarnUrlFromURI(log4jDst.toUri()));
            log4jRsrc.setTimestamp(log4jFileStatus.getModificationTime());
            log4jRsrc.setSize(log4jFileStatus.getLen());
            localResources.put("log4j.properties", log4jRsrc);
        }

        String hdfsShellScriptLocation = "";
        long hdfsShellScriptLen = 0L;
        long hdfsShellScriptTimestamp = 0L;
        if (!(this.shellScriptPath.length() == 0)) {
            Path shellSrc = new Path(this.shellScriptPath);
            String shellPathSuffix = new StringBuilder().append(this.appName).append("/").append(appId.getId()).append("/ExecShellScript.sh").toString();
            Path shellDst = new Path(fs.getHomeDirectory(), shellPathSuffix);
            fs.copyFromLocalFile(false, true, shellSrc, shellDst);
            hdfsShellScriptLocation = shellDst.toUri().toString();
            FileStatus shellFileStatus = fs.getFileStatus(shellDst);
            hdfsShellScriptLen = shellFileStatus.getLen();
            hdfsShellScriptTimestamp = shellFileStatus.getModificationTime();
        }

        amContainer.setLocalResources(localResources);
        LOG.info("Set the environment for the application master");
        Map env = new HashMap();

        env.put("DISTRIBUTEDSHELLSCRIPTLOCATION", hdfsShellScriptLocation);
        env.put("DISTRIBUTEDSHELLSCRIPTTIMESTAMP", Long.toString(hdfsShellScriptTimestamp));
        env.put("DISTRIBUTEDSHELLSCRIPTLEN", Long.toString(hdfsShellScriptLen));

        StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$()).append(File.pathSeparatorChar).append("./*");

        for (String c : this.conf.getStrings("yarn.application.classpath", YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            classPathEnv.append(File.pathSeparatorChar);
            classPathEnv.append(c.trim());
        }
        classPathEnv.append(File.pathSeparatorChar).append("./log4j.properties");

        if (this.conf.getBoolean("yarn.is.minicluster", false)) {
            classPathEnv.append(':');
            classPathEnv.append(System.getProperty("java.class.path"));
        }

        // 设置classPathEnv
        env.put("CLASSPATH", classPathEnv.toString());
        // 这是ApplicationMaster的环境
        amContainer.setEnvironment(env);

        Vector vargs = new Vector(30);

        LOG.info("Setting up app master command");
        vargs.add(new StringBuilder().append(ApplicationConstants.Environment.JAVA_HOME.$()).append("/bin/java").toString());

        vargs.add(new StringBuilder().append("-Xmx").append(this.amMemory).append("m").toString());

        vargs.add(this.appMasterMainClass);

        vargs.add(new StringBuilder().append("--container_memory ").append(String.valueOf(this.containerMemory)).toString());
        vargs.add(new StringBuilder().append("--num_containers ").append(String.valueOf(this.numContainers)).toString());
        vargs.add(new StringBuilder().append("--priority ").append(String.valueOf(this.shellCmdPriority)).toString());
        if (!(this.shellCommand.length() == 0)) {
            vargs.add(new StringBuilder().append("--shell_command ").append(this.shellCommand).append("").toString());
        }
        if (!(this.shellArgs.length() == 0)) {
            vargs.add(new StringBuilder().append("--shell_args ").append(this.shellArgs).append("").toString());
        }
        for (Map.Entry entry : this.shellEnv.entrySet()) {
            vargs.add(new StringBuilder().append("--shell_env ").append((String) entry.getKey()).append("=").append((String) entry.getValue()).toString());
        }
        if (this.debugFlag) {
            vargs.add("--debug");
        }

        vargs.add("1><LOG_DIR>/AppMaster.stdout");
        vargs.add("2><LOG_DIR>/AppMaster.stderr");

        StringBuilder command = new StringBuilder();
        for (Object str : vargs) {
            command.append(str).append(" ");
        }

        LOG.info(new StringBuilder().append("Completed setting up app master command ").append(command.toString()).toString());
        List commands = new ArrayList();
        commands.add(command.toString());
        amContainer.setCommands(commands);

        Resource capability = (Resource) Records.newRecord(Resource.class);
        capability.setMemory(this.amMemory);
        appContext.setResource(capability);

        if (UserGroupInformation.isSecurityEnabled()) {
            Credentials credentials = new Credentials();
            String tokenRenewer = this.conf.get("yarn.resourcemanager.principal");
            if ((tokenRenewer == null) || (tokenRenewer.length() == 0)) {
                throw new IOException("Can't get Master Kerberos principal for the RM to use as renewer");
            }

            Token[] tokens = fs.addDelegationTokens(tokenRenewer, credentials);

            if (tokens != null) {
                for (Token token : tokens) {
                    LOG.info(new StringBuilder().append("Got dt for ").append(fs.getUri()).append("; ").append(token).toString());
                }
            }
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            ByteBuffer fsTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            amContainer.setTokens(fsTokens);
        }
            appContext.setAMContainerSpec(amContainer);

            Priority pri = (Priority) Records.newRecord(Priority.class);

            pri.setPriority(this.amPriority);
            appContext.setPriority(pri);

            appContext.setQueue(this.amQueue);
            //权限验证完成，具体提交到ResourceManager 进行操作。
            LOG.info("Submitting application to ASM");

            this.yarnClient.submitApplication(appContext);

        return monitorApplication(appId);
    }


    private boolean monitorApplication(ApplicationId appId)  throws YarnException, IOException
    {
        while(true){
            try
            {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOG.debug("Thread sleep in monitoring loop interrupted");
            }
            ApplicationReport report = this.yarnClient.getApplicationReport(appId);

            LOG.info(new StringBuilder().append("Got application report from ASM for, appId=").append(appId.getId()).append(", clientToAMToken=").append(report.getClientToAMToken()).append(", appDiagnostics=").append(report.getDiagnostics()).append(", appMasterHost=").append(report.getHost()).append(", appQueue=").append(report.getQueue()).append(", appMasterRpcPort=").append(report.getRpcPort()).append(", appStartTime=").append(report.getStartTime()).append(", yarnAppState=").append(report.getYarnApplicationState().toString()).append(", distributedFinalState=").append(report.getFinalApplicationStatus().toString()).append(", appTrackingUrl=").append(report.getTrackingUrl()).append(", appUser=").append(report.getUser()).toString());

            // 监控Application
            YarnApplicationState state = report.getYarnApplicationState();
            FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
            if (YarnApplicationState.FINISHED == state) {
                if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
                    LOG.info("Application has completed successfully. Breaking monitoring loop");
                    return true;
                }

                LOG.info(new StringBuilder().append("Application did finished unsuccessfully. YarnState=").append(state.toString()).append(", DSFinalStatus=").append(dsStatus.toString()).append(". Breaking monitoring loop").toString());

                return false;
            }

            if ((YarnApplicationState.KILLED == state) || (YarnApplicationState.FAILED == state))
            {
                LOG.info(new StringBuilder().append("Application did not finish. YarnState=").append(state.toString()).append(", DSFinalStatus=").append(dsStatus.toString()).append(". Breaking monitoring loop").toString());

                return false;
            }

            if (System.currentTimeMillis() > this.clientStartTime + this.clientTimeout) {
                LOG.info("Reached client specified timeout for application. Killing application");
                forceKillApplication(appId);
                return false;
            }
        }
    }
    private void forceKillApplication(ApplicationId appId)
            throws YarnException, IOException
    {
        this.yarnClient.killApplication(appId);
    }

    public static void main(String[] args)
    {
        boolean result = false;
        try {
            MyClient client = new MyClient();
            LOG.info("Initializing Client");
            try {
                boolean doRun = client.init(args);
                if (!doRun)
                    System.exit(0);
            }
            catch (IllegalArgumentException e) {
                System.err.println(e.getLocalizedMessage());
                client.printUsage();
                System.exit(-1);
            }
            result = client.run();
        } catch (Throwable t) {
            LOG.fatal("Error running CLient", t);
            System.exit(1);
        }
        if (result) {
            LOG.info("Application completed successfully");
            System.exit(0);
        }
        LOG.error("Application failed to complete successfully");
        System.exit(2);
    }


}
