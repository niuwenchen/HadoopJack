## ResourceManager剖析
按照前面的理解，ResourceManager负责集群中所有资源的统一管理和分配，接受来自各个节点(NodeManager)的资源汇报信息，
并将这些信息按照一定的策略分配给各个应用程序(实际上是ApplicationMaster)。

ResourceManager基本职能

    两个RPC协议与NodeManager和ApplicationMaster交互
    ResourceTracker: NodeManager --> ResourceManager
    ApplicationMasterProtocol: AppMaster --> ResourceManager
    ApplicationClientProtocol: Client--> ResourceManager: 
    
    Client--> Master--> ResourceManager
     ResourceManager 主要完成以下几个功能
        与客户端交互，处理来自客户端的请求
        启动和管理ApplicationMaster，并在它运行失败时重新启动它。
        管理NodeManager，接受来自NodeManager的资源汇报信息，并向NodeManager下达管理命令
        资源管理与调度，接受来自ApplicaionMaster的资源申请请求，并为之分配资源。

##ResourceManager内部架构

用户交互模块

YarnClient是基础程序，上层的程序Client会借助YarnClient提供的方法实现具体的功能。

    普通用户，管理员和Web提供了三种对外服务。使用协议 ApplicationClientProtocol
    ClientRMService，AdminService，WebApp
    ClientRMService: 处理来自客户端的RPC请求，作为一个Server，比如处理提交应用程序，终止应用程序。
        Configuration conf = getConfig();
        YarnRPC rpc = YarnRPC.create(conf);
        this.server = rpc.getServer(ApplicationClientProtocol.class, this, this.clientBindAddress, conf, this.rmDTSecretManager, conf.getInt("yarn.resourcemanager.client.thread-count", 50));
        this.server.start();
        
        区别就是端口:
            return conf.getSocketAddr("yarn.resourcemanager.address", "0.0.0.0:8032", 8032);
    
    AdminService: 为管理员提供了一套独立的服务接口，以防止大量的普通用户请求使管理员发送的命令阻塞。
        this.masterServiceAddress = conf.getSocketAddr("yarn.resourcemanager.admin.address", "0.0.0.0:8033", 8033);
        Configuration conf = getConfig();
            YarnRPC rpc = YarnRPC.create(conf);
            this.server = rpc.getServer(ResourceManagerAdministrationProtocol.class, this, this.masterServiceAddress, conf, null, conf.getInt("yarn.resourcemanager.admin.client.thread-count", 1));
        this.server.start();
        
    
    WebApp: 集群资源使用情况和应用程序运行状态等信息，仿照Haml 开发的一个轻量级Web框架

NM管理模块

    NMLivelinessMonitor: 监控NodeManager是否存活
    NodesListManager: 维护节点列表
    ResourceTrackerService: 处理来自NodeManager的请求。 注册和心跳两种。
    
AM管理模块

    AMLivelinessMonitor:监控AM是否存活。
    ApplicationMasterLauncher: 与NodeManager进行通信，要求为某个应用程序启动ApplicationMaster
    ApplicationMasterService(AMS): 处理来自ApplictionMatser的请求，注册和心跳两种请求、
        
        (1) 用户-->YARN ResourceManager--> 资源 --> ApplicationMasterLauncher与NodeManager通信--> 启动ApplicationMaster
        (2) ApplicationMaster启动完成--> ApplicationMasterLauncher 注册AM 到AMLivelinessMonitor --> 心跳监控
        (3) Am启动后，自己注册自己到AMS，所有的信息均汇报
        (4) AM运行，汇报信息到AMS
        (5) AMS收到信息，通知AMlivelinessMontior更新时间
        (6) AM完成，通知AMS 注销自己
        (7) AMS收到注销请求，标记引用程序运行状态为完成，同时通知AMLivelinessMonitor移除对它的心跳监控
        
    

Application管理模块

Application 是指应用程序，可能启动多个运行实例，每个运行实例由一个ApplicationMaster与一组该ApplicationMaster启动的任务组成，拥有名称、队列名
优先级等属性，可以是一个MR作业，一个DAG应用程序，甚至可以是一个Storm集群实例。

    ApplicationACLsManager: 访问权限
    RMAppManager: 管理应用程序的启动和关闭
    ContainerAllocationExpier: 当AM收到RM新分配的一个Container后，必须在一定时间内在对应的NM上启动该Container
 
状态管理机模块

    RMApp: 维护一个应用程序的整个运行周期，启动到结束，一个Application可能会启动多个Application运行实例(Application Attempt)
    RMAppAttempt: 每次启动称为一次运行尝试
    RMContainer: 维护一个Container的运行周期
    RMNode: NodeManager的生命周期
    
    Yarn中的Application生命周期由状态机RMAppImpl维护，每一次尝试运行由 RMAppAttemptImpl维护。
    
安全管理模块

    权限管理机制
资源分配模块

    ResourceScheduler 资源调度器，按照一定条件将资源分配给各个应用程序，主要考虑内存和CPU资源

