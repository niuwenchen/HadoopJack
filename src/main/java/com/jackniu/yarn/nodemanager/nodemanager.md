## NodeManager 
NodeManager是运行在单个节点上的代理，需要与应用程序的AM和集群管理者RM进行交互； 从AM上接受有关Container的命令并执行；
向RM汇报各个container的运行状态和节点健康状况，并领取有关Container的命令。

### NodeManager基本职能。
整体来说，NM需要两个RPC协议与rm服务和各个应用程序的AM进行交互。

ResourceTrackerProtocol协议

    NodeManager 通过该协议将自己注册到RM，还有一系列动作。
    NodeManager总是周期性的主动向RM发起请求，并领取下达给自己的命令。以下两个RPC协议:
    (1) registerNodeManager
    
ContainerManagerProtocol

    应用程序的AM通过该RPC协议向NM发起针对Container的操作，包括启动Container，杀死Container，获取Container执行状态等。
  
NodeManager内部架构

NodeStatusUpdater

    NM和RM通信的唯一通道。当NM启动时，该组件负责向RM注册，并汇报节点上总的可用资源，之后，该组件周期性的与RM进行通信，
    汇报各个Container的状态更新，包括节点上正在运行的OCntainer，已完成的Container等信息，同时RM会为之返回待清理的
    Container列表，待清理应用程序列表，诊断信息，各种Token等信息。
ContainerManager

    Container是NM中最核心的组件之一，由多个子组件构成，每个子组件负责一部分功能，写作共同管理运行在该节点上的所有Container。
    
ContainerExecutor:

    ContainerExecutor可与底层操作系统交互，安全存放Container需要的文件和目录，进而以一种安全的方式启动和清除Container对应的进程。
    YARN提供了DefaultContainerExecutor和LinuxContainerExecutor两种实现。 Default是默认实现，未提供任何安全措施，以NodeManager
    启动者的身份启动和停止Container； 而LinuxContainerexecutor则以应用程序拥有者的身份启动和停止Container，因此更加安全。
    
NodeHealthCheckerService

    NodeHealthCheckerService周期性的运行一个自定义脚本(由组件NodeHealthScriptRunner完成)和向磁盘写文件(由服务LocalDirsHandlerService完成)
    检查节点的健康状况，并将之通过NodeStatusUpdater传递给RM。一旦RM发现一个节点处于不健康状态，则会将它加入黑名单，此后
    不再为他/她分配任务，知道再次转换为健康状态。需要注意的是，节点被加入黑名单后，正在运行的Container仍会正常运行，不会被杀死。
    
DeletionService

    NodeManager将文件删除功能服务化，即提供一个专门的文件删除服务异步删除失败文件，这样可避免同步删除文件带来的性能开销。
    
Security
    
    安全模块

WebServer
    
    Web界面展示信息。
    

### 7.2 节点健康状况检测
节电健康状况检测是 NM自带的健康状况诊断机制，通过该机制，NM可时刻掌握自己的健康状况，并及时汇报给RM。而RM则根据每个NM的健康
状况适当的调整分配的任务数目。

自定义Shell脚本

    NM上专门有一个服务判断所在节点的健康状况，该服务通过两种策略判断节点健康状态，第一种通过管理员自定义的shell脚本
    另一种是判断磁=磁盘好坏。
 
    
### 7.3 分布式缓存机制
分布式缓存是一种分布式分发与缓存机制，主要作用是将用户应用程序执行时所需的外部文件资源自动透明的下载并缓存到各个节点上，从而省去了
手动部署这些文件的麻烦。

YARN分布式缓存工作流程如下:

    步骤1: 客户端竞应用程序所需的文件资源(外部字典，JAR包，二进制文件等)提交到HDFS上
    步骤2: 客户端将应用程序提交到RM上
    步骤3: RM与某个NM进行通信，启动ApplicationMaster，NM收到命令后，首先从HDFS下载文件，然后启动AM
    步骤4: AM与RM通信，以请求和获取计算资源
    步骤5: AM收到新分配的计算资源后，与对应的NM通信，以启动任务。
    步骤6: 如果该应用程序第一次在该节点上启动任务，则NM首先从HDFS上下载文件缓存到本地，然后启动任务
    步骤7: NM后续收到启动任务请求后，如果文件已在本地缓存，则直接运行任务，否则等待文件缓存完成后再启动。
    

###资源可见性
按照可见性，NM将资源分为三类:
    
    public  所有用户共享该资源
    private  同一用户的所有应用程序共享该资源
    application  节点上同一应用程序的所有container共享，
    
YARN Mapreduce是采用目录权限方式判断资源可见性的，如果一个HDFS文件的父目录的用户执行权限、组执行权限和其他都是打开的，
则认为是public， 否则是private。如果你想将一个文件可见性设置为public，必须在运行MapReduce应用程序之前将它上传到HDFS上，
并修改所在目录的权限。 

### 7.4 目录结构管理
YARN运行NodeManager配置多个挂在不同磁盘的目录作为中间结果存放目录、 对于任意一个应用程序，YARN会在每个中创建相同的目录结构。，
然后采用轮询策略使用这些目录。

数据目录 yarn.nodemanager.local-dirs 
日志目录 yarn.nodemanager.log-dirs
