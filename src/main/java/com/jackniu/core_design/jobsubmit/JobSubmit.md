## 作业提交与初始化过程分析
在本章中，将深入分析一个MR作业的提交与初始化过程，即从用户输入提交作业命令到作业初始化的整个过程。
该过程涉及JobClient,JobTracker和TaskScheduer三个组件，功能分别是准备运行环境、接收作业以及初始化作业。

### 5.1 作业提交与初始化概述
作业提交过程比较简单，为后续作业执行准备环境，主要涉及创建目录、上传文件等操作；而一旦用户提交作业后，JobTracker端
便会对作业进行初始化。

    JobTracker对作业进行初始化: 根据输入数据量和作业配置参数将作业分解成若干个Map Task和Reduce Task，并添加
    到相关数据结构中，以等待后续被调度执行。四个步骤
    1: 用户使用Hadoop提供的shell命令提交作业
    2: JobClient按照作业配置信息(JobConf)将作业运行需要的全部文件上传到JobTracker文件系统(通过为HDFS)的某个目录下
    3: JobClient调用RPC接口向JobTracker提交作业
    4: JobTracker接收到作业后，将其告知TaskScheduler，由TaskScheduler对作业进行初始化。
    
    将作业相关文件(应用程序Jar包，xml文件及其依赖的文件等)上传到HDFS，主要是:
        HDFS 是一个分布式文件系统，hadoop集群上任何一个节点可以直接从该系统下载文件。也就是说，HDFS上所有
            文件都是节点间共享的
        作业文件是运行Task所必须的，一旦被上传到HDFS后，任何一个Task只需知道存放路径便可以下载到自己的工作目录中使用，因而可看到一种非常简单的文件共享方式。

### 5.2 作业提交过程

    提交作业
    bin/hadoop jar xxx.jar \
    -D mapred.job.name="xxx" \
    -D mapred.reduce.tasks=2 \
    -files=blacklist.txt,whitelist.txt \
    -archives=dicationary.zip
    -input /test/input
    -output /test/output
    
    看具体的shell脚本
    elif [ "$COMMAND" = "jar" ] ; then
          CLASS=org.apache.hadoop.util.RunJar
    
    import org.apache.hadoop.util.RunJar;
    
    检查jarfile是否存在，解压该jar，获取元信息，MainClass，如果在运行参数中不写启动主类
    if(mainClassName == null) {
                if(args.length < 2) {
                    System.err.println(usage);
                    System.exit(-1);
                }
    
                mainClassName = args[var19++];
            }
    
    这个反射机制
    Class mainClass = Class.forName(mainClassName, true, var20);
    Method main = mainClass.getMethod("main", new Class[]{Array.newInstance(String.class, 0).getClass()});
    main.invoke((Object)null, new Object[]{newArgs}); // //静态方法可省略对象，直接用null替代，这里的main是静态类，用null代替
    

已经从运行的jar包中提取出了主类以及运行参数，现在开始main 类的运行：
    
    JobClient client = new JobClient();
            JobConf conf = new JobConf(WordCount.class);
    各种配置信息
    最后  JobClient.runJob(job)
    
    JobClient 类
    public static RunningJob runJob(JobConf job) throws IOException {
            JobClient jc = new JobClient(job);
            RunningJob rj = jc.submitJob(job);
    
    public RunningJob submitJob(JobConf job) throws FileNotFoundException, IOException {
            try {
                return this.submitJobInternal(job);
    
     public RunningJob submitJobInternal(final JobConf job) throws FileNotFoundException, ClassNotFoundException, InterruptedException, IOException {
             return (RunningJob)this.ugi.doAs(new PrivilegedExceptionAction() {
                 public RunningJob run() throws FileNotFoundException, ClassNotFoundException, InterruptedException, IOException {
                     JobConf jobCopy = job;
                     Path jobStagingArea = JobSubmissionFiles.getStagingDir(JobClient.this, jobCopy);
                     JobID jobId = JobClient.this.jobSubmitClient.getNewJobId();
                     Path submitJobDir = new Path(jobStagingArea, jobId.toString());
                     jobCopy.set("mapreduce.job.dir", submitJobDir.toString());
                     JobStatus status = null;
     
                     JobClient.NetworkedJob var16;
                     try {
                         JobClient.this.populateTokenCache(jobCopy, jobCopy.getCredentials());
                         JobClient.this.copyAndConfigureFiles(jobCopy, submitJobDir);
                         TokenCache.obtainTokensForNamenodes(jobCopy.getCredentials(), new Path[]{submitJobDir}, jobCopy);
                         Path submitJobFile = JobSubmissionFiles.getJobConfPath(submitJobDir);
                         int reduces = jobCopy.getNumReduceTasks();
                         InetAddress ip = InetAddress.getLocalHost();
                         if(ip != null) {
                             job.setJobSubmitHostAddress(ip.getHostAddress());
                             job.setJobSubmitHostName(ip.getHostName());
                         }
     
                         JobContext context;
                         label167: {
                             label166: {
                                 context = new JobContext(jobCopy, jobId);
                                 if(reduces == 0) {
                                     if(jobCopy.getUseNewMapper()) {
                                         break label166;
                                     }
                                 } else if(jobCopy.getUseNewReducer()) {
                                     break label166;
                                 }
     
                                 jobCopy.getOutputFormat().checkOutputSpecs(JobClient.this.fs, jobCopy);
                                 break label167;
                             }
     
                             OutputFormat fs = (OutputFormat)ReflectionUtils.newInstance(context.getOutputFormatClass(), jobCopy);
                             fs.checkOutputSpecs(context);
                         }
     
                         jobCopy = (JobConf)context.getConfiguration();
                         FileSystem fs1 = submitJobDir.getFileSystem(jobCopy);
                         JobClient.LOG.debug("Creating splits at " + fs1.makeQualified(submitJobDir));
                         int maps = JobClient.this.writeSplits(context, submitJobDir);
                         jobCopy.setNumMapTasks(maps);
                         String queue = jobCopy.getQueueName();
                         AccessControlList acl = JobClient.this.jobSubmitClient.getQueueAdmins(queue);
                         jobCopy.set(QueueManager.toFullPropertyName(queue, QueueACL.ADMINISTER_JOBS.getAclName()), acl.getACLString());
                         FSDataOutputStream out = FileSystem.create(fs1, submitJobFile, new FsPermission(JobSubmissionFiles.JOB_FILE_PERMISSION));
                         TokenCache.cleanUpTokenReferral(jobCopy);
     
                         try {
                             jobCopy.writeXml(out);
                         } finally {
                             out.close();
                         }
     
                         JobClient.this.printTokens(jobId, jobCopy.getCredentials());
                         status = JobClient.this.jobSubmitClient.submitJob(jobId, submitJobDir.toString(), jobCopy.getCredentials());
                         JobProfile prof = JobClient.this.jobSubmitClient.getJobProfile(jobId);
                         if(status == null || prof == null) {
                             throw new IOException("Could not launch job");
                         }
     
                         var16 = new JobClient.NetworkedJob(status, prof, JobClient.this.jobSubmitClient);
                     } finally {
                         if(status == null) {
                             JobClient.LOG.info("Cleaning up the staging area " + submitJobDir);
                             if(JobClient.this.fs != null && submitJobDir != null) {
                                 JobClient.this.fs.delete(submitJobDir, true);
                             }
                         }
     
                     }
     
                     return var16;
                 }
             });
         }
       
    
     String tracker = conf.get("mapred.job.tracker", "local");
            tasklogtimeout = conf.getInt("mapreduce.client.tasklog.timeout", '\uea60');
            this.ugi = UserGroupInformation.getCurrentUser();
            if("local".equals(tracker)) {
                conf.setNumMapTasks(1);
                this.jobSubmitClient = new LocalJobRunner(conf);
            } else {
                this.rpcJobSubmitClient = createRPCProxy(JobTracker.getAddress(conf), conf);
                this.jobSubmitClient = createProxy(this.rpcJobSubmitClient, conf);
            }
     
    和属性  mapred.job.tracker 属性有关，
    


### 5.2.2 作业文件上传
JobClient将作业提交到JobTracker之前，需要进行一些初始化工作，包括:获取作业ID,创建HDFS目录，上传作业文件以及生成Split文件等
由函数JobClient.submitJobInternal(job)实现。

MR作业文件的上传与下载是由DistributedCache工具完成的。是hadoop为方便用户进行应用程序开发而设计的数据分发工具。
用户只需要在提交作业时指定文件位置，至于这些文件的分发(需广播到各个TaskTracker上以运行Task)，完全由DistrubutedCche工具完成，
不需要用户参与。

    对一个典型的Java MR作业，可能包含以下资源:
    * 程序Jar包: 用户用java 编写的MR程序的jar包
    * 作业配置文件: 描述Mr程序的配置信息
    * 依赖的第三方Jar包，提交时用参数-libjars 指定
    * 依赖的归档文件，压缩文件， -archives
    * 依赖的普通文件：应用程序中可能用到普通文件，比如文件格式的字典文件，提交作业时用-files指定。
    
    
### 5.2.3 产生InputSplit文件
org.apache.hadoop.mapred.InputSplit[] splits = job.getInputFormat().getSplits(job, job.getNumMapTasks());

JobSplitWriter.createSplitFiles(jobSubmitDir, job, jobSubmitDir.getFileSystem(job), splits);

生成的InputSplits 数据数目是由numMapTasks决定的。该 信息生成两部分数据，InpusSplit元数据和原始InputSplit信息。
第一部分被JobTracker使用，用以生成task本地性相关的数据结构；第二部分则被Map Task初始化时使用，用以获取自己要处理的数据。
这两部分信息分别被保存在目录${mapreduce.jobtracker.root.dir}/${user}/.staging/${JobId}下的文件job.split和job.splitmetinfo

    public static class SplitMetaInfo implements Writable {
            private long startOffset;   该InputSplit元信息在job.split文件中的偏移量
            private long inputDataLength; 该InputSplit的数据长度
            private String[] locations; 该InputSplit所在的host列表
    SplitMetaInfo: 描述一个InputSplit的元数据信息
    TaskSplitMetaInfo: 用于保存InputSplit元信息的数据结构
        
         public static class TaskSplitMetaInfo {
                private JobSplit.TaskSplitIndex splitIndex; Split元信息在job.split文件中的位置
                private long inputDataLength;   InputSplit的数据长度
                private String[] locations;     InputSplit所在的host列表
    
    
    TaskTracker从JobTracker端收到该信息后，便可以从job.split文件中读取InputSplit信息，便可以运行一个新任务。
    
### 5.4.2 作业提交到JobTracker
JobClient最终调用RPC方法submitJob将作业提交到JobTracker端，在JobTracker.submitJob中，会依次进行如下操作。

     this.jobSubmitClient = createProxy(this.rpcJobSubmitClient, conf);
    JobClient.this.printTokens(jobId, jobCopy.getCredentials());
                        status = JobClient.this.jobSubmitClient.submitJob(jobId, submitJobDir.toString(), jobCopy.getCredentials());
                        JobProfile prof = JobClient.this.jobSubmitClient.getJobProfile(jobId);
    
    JobClient最终调用RPC方法submitJob将作业提交到JobTracker端，在JobTracker.submitJob中，会依次进行以下操作。
    在JobTracker中进行的动作是:
    (1) 为作业创建JobInProgress对象
    JobTracker会为每个作业创建一个JobInProgess对象，该对象维护了作业的运行时信息。在作业运行过程中一直存在，主要用于跟踪正在运行作业的
    运行状态和进度
    
    (2)检查用户是否具有指定队列的作业权限
    Hadoop以队列为单位管理作业和资源，每个队列分配一定量的资源，每个用户属于一个或多个队列且只能使用所属队列中的资源。
    管理员可以为每个队列指定哪些用户具有作业提交权限和管理权限
    
    (3) 检查作业配置的内存的使用量是否合理
    用户提交作业时，可分别用参数mapred.job.map.memory.mb和mapred.job.reduce.memory.mb 指定Map task和Reduce Task占用的内存量；
    而管理员可通过参数mapred.cluster.max.map.memory.mb和mapred.cluster.max.reduce.memory.mb限制用户配置的任务最大内存
    使用量，一旦用户配置的内存使用量超过系统限制，则作业提交失败。
    
    (3) 通知TaskScheduler初始化作业
    JobTracker收到作业后，并不会马上对其初始化，而是交给调度器，由他按照一定的策略对作业进行初始化。
    之所以不选择jobTracker而让调度器初始化，主要考虑以下:
        作业初始化便会占用大量的内存资源
        任务调度器的职责是根据每个节点的资源使用情况对其分配最合适的任务
        
         

        
    
    
             
    
        
    
