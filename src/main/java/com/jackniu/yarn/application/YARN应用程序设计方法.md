## YARN应用程序设计方法

http://www.datastart.cn/tech/2015/05/05/yarn-dist-shell.html

http://cd-bigdata01:8088

应用程序是用户编写的处理数据的程序的统称，从YARN中申请资源以完成自己的计算任务。YARN自身对应程序类型没有任何限制，可以使
处理短类型任务的MapReduce作业，也可以是部署长时间运行的服务的应用程序(Storm On Yarn)。应用程序可以向YARN申请资源
完成各类计算任务。必须MR应用程序向YARN申请资源，用于运行Map Task和Reduce Task两类任务。

由于YARN应用程序编写比较复杂，且需对YARN自身架构有一定了解，因此通常由专业人员开发，通过回调的形式供其他普通用户使用。
比如，专业人员可以实现直接运行在yarn上的MapReduce框架库(假设打包后为yarn-mapreduce.jar，主要完成数据切分、资源申请
任务调度和容错，网络通信等功能)，而普通用户只需要编写map和reduce两个函数完成mapreduce程序设计(假设打包后为my-app.jar，主要完成
自己计算所需的逻辑)。这样用户提交应用程序时，YARN会自动将yarn-mapreduce.jar和my-app.jar两个JAR包同时提交到YARN之上，以完成
一个分布式应用的计算。 重点介绍如何编写一个可以直接运行在YARN之上的框架。

### 4.1 概述
YARN是一个资源管理系统，负责集群资源的管理和调度。如果想要将一个新的应用程序运行在YARN之上，通常需要编写两个组件Client客户端
和ApplicationMaster。这两个组件编写非常复杂，尤其是ApplicationMaster，需要考虑RPC调用、任务容错等细节。
如果大量应用程序可抽象成一种通用框架，只需实现一个客户端和一个ApplicationMaster，然后让所有应用程序重用这两个组件即可。
比如MapReduce是一种通用的计算框架，YARN已经为其实现了一个直接可以使用的客户端(JobClient)和ApplicationMaster
(MRAppMaster).

    Two Parts
        Client
        ApplicationMaster
    Client
        Submit application to RM,tell app-related configurations & properties
        Connect to AppMaster to fetch information if necessary (Always)
    ApplicationMaster
        Negotiate with RM for resources (containers)
        Assign containers to internal "tasks"
        Communicate with RM to lauch containers
        Fault tolerate of itself and managed containers
        
    APIS Needed
    Only three protocols
        Client to ResourceManager - Application submission
        ApplicationMaster to ResourceManager - container allocation
        ApplicationMaster to NodeManager - container launch
            (NodeManager 管理container，即和NodeManager通信)
    org.apache.hadoop.yarn.client.api
    
        hadoop jar share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.6.0.jar org.apache.hadoop.yarn.applications.distributedshell.Client      -jar share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.6.0.jar        -shell_command 'ls'  -num_containers  5
        但是并没有输出ls的内容。
        

运行在YARN上的应用程序主要分为短应用程序和长应用程序两类，其中，短应用程序是短时间内可运行完成的程序，比如MapReduce作业、Tez作业等。
长应用程序是永不终止运行的服务，比如Storm Service,HBase Service等。尽管这两种应用程序不同，但在YARN上的工作流程和编写方式是相同的。

    客户端Client需要向ResourceManager提交ApplicationMaster，并查询应用程序运行状态；
    ApplicationMaster负责向ResourceManager申请资源(Container形式表示)，并与NodeManager通信以启动各个Container，此外，
    ApplicationMaster还负责监控各个任务运行状态，并在失败时为其重新申请资源。
    
    主要方法：
    import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
    protected ApplicationClientProtocol rmClient;
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
     
     public ApplicationId submitApplication(ApplicationSubmissionContext appContext) throws YarnException, IOException {
             ApplicationId applicationId = appContext.getApplicationId();
             appContext.setApplicationId(applicationId);
             SubmitApplicationRequest request =
                     Records.newRecord(SubmitApplicationRequest.class);
             request.setApplicationSubmissionContext(appContext);
             //将应用程序提交到ResourceManager 上
             rmClient.submitApplication(request);
     
     。。。。
     除了提交Application接口外，客户端还需提供以下几种接口的实现.
     
     但是以上的实现是通用的，不仅仅是ApplicationMasterProtocol,还可以是别的协议。
     不同类型应用程序与ResourceManager交互逻辑是类似的，YarnClient，对常用函数进行了封装，并提供了重试、
     容错等机制，用户使用该库可以快速开发一个包含应用程序提交、状态查询和控制等逻辑的Yarn客户端。
     @Public
       public static YarnClient createYarnClient() {
         YarnClient client = new YarnClientImpl();
         return client;
       }
     
     createApplication
     submitApplication
     killApplication
     getApplicationReport
     getAMRMToken
     。。。。。 一些常用的方法或者实现。
     
     所以以后就只需要继承YarnClient 就可以实现自己的Client

    上面的只是最底层的操作，想要在较高层操作还需要 调用底层的方法并完成具体的功能。
    
    
### ApplicationMaster设计
ApplicationMaster 需要与RM和NM两个服务器交互，通过与RM交互可以获得任务计算所需的资源； 通过NM，可启动计算任务容器
并监控它直到运行完成。

    （1） ApplicationMaster通过ApplicationMasterProtocol#registerApplicationMaster 向ResourceManager注册。
        ApplicationMaster注册成功后，将收到一个RegisterApplicationMasterResponse 类型的返回值，主要包含以下信息
            maximumCapablity: 最大可申请的单个Container占用的资源量。用户可以通过函数
                    RegisterApplicationMasterResponse#getMaximumResourceCapability获取该值
         client_to_am_token_master_key: ClientToAmToken 通行证
         application_acls: 应用程序访问控制列表，可以访问的目录或文件，
    
        
    // 向ResourceManager注册
    RegisterApplicationMasterResponse response = this.amRMClient.registerApplicationMaster(this.appMasterHostname, this.appMasterRpcPort, this.appMasterTrackingUrl);
    int maxMem = response.getMaximumResourceCapability().getMemory();
    LOG.info("Max mem capabililty of resources in this cluster " + maxMem);
    
    ApplicationMaster向ResourceManager申请资源
    
        private AMRMClient.ContainerRequest setupContainerAskForRM()
        {
            // 设置continers的请求
            Priority pri = (Priority)Records.newRecord(Priority.class);
    
            pri.setPriority(this.requestPriority);
    
            Resource capability = (Resource)Records.newRecord(Resource.class);
            capability.setMemory(this.containerMemory);
            
            AMRMClient.ContainerRequest request = new AMRMClient.ContainerRequest(capability, null, null, pri);
    
            LOG.info("Requested container ask: " + request.toString());
            return request;
        }
        
    RM回调函数
    
        // 在分配NodeManager之后进行回调
                public void onContainersAllocated(List<Container> allocatedContainers)
                MyApplicationMaster.LaunchContainerRunnable runnableLaunchContainer =
                                        new MyApplicationMaster.LaunchContainerRunnable(allocatedContainer, MyApplicationMaster.this.containerListener);
    
     
     for (int i = 0; i < this.numTotalContainers; i++) {
                 AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM();
                 // 这里就是将所需的所有资源一次性发送给ResourceManager
                 this.amRMClient.addContainerRequest(containerAsk);
             }
     
      this.containerListener.addContainer(this.container.getId(), this.container);
                 MyApplicationMaster.this.nmClientAsync.startContainerAsync(this.container, ctx);
                 