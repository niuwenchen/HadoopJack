## maperduce 计算框架
MapReduce On Yarn在编程模型和数据处理引擎方面的实现一样的，唯一的不同是运行时环境。 不同于MRV1中由JobTracker
和TaskTracker构成的运行时环境，Mapreduce on Yarn的运行时环境由Yarn与ApplicationMaster构成，这种新颖的设计
使得Mapreduce可以与其他计算框架运行在一个集群中，从而达到共享集群资源、提高资源利用率的目的。

MRAppMaster是Mapreduce的ApplicationMaster实现，使得Mapreduce应用程序可以直接运行于Yarn之上，在Yarn中，
MRAppMaster负责管理MapReduce作业的生命周期，包括作业管理、资源申请与再分配、Container启动与释放、作业恢复等。

###基本构成

    ContainerAllocator: 与RM通信，为MR申请资源。作业的每个任务资源需求可描述为5元组<Priority,hostname,capability,containers,relax_locality>
    分别表示作业优先级，期望资源所在的host，资源量(内存和CPU两种资源)，Container数目，是否松弛本地性。
    ContainerAllocator周期性通过RPC与RM进行通信，而RM则通过心跳应答的方式为之返回已经分配的Container列表，
    完成的Container列表等信息。
    
    ClientService: 接口，MRClientService实现，实现了MRClientProtocol协议，客户端可通过该协议获取作业的执行状态和控制作业
    Job: 表示一个MapReduce作业，与MRV1的JobInProgress功能一样，负责监控作业的运行状态。维护一个作业状态机，实现异步执行各种作业相关的操作
    Task: 表示一个Mapreduce作业中的各个任务，与MRV1 的TaskInProgress功能类似。
    TaskAttempt: 任务尝试实例。
    TaskCleaner: 负责清理失败任务或者被杀死任务使用的目录和产生的临时结果，维护了一个线程池和一个共享队列。
    Speculator: 
    ContainerLauncher: 启动container,和NodeManager通信。
    TaskAttemptListener: 负责管理各个任务的心跳信息。
    JobHistoryEventHandler: 对作业的各个事件记录日志，比如创建作业，作业开始运行，一个任务开始运行。
    

### Mapreduce客户端
MapReduce涉及两个RPC通信协议
    
    ApplicationClientProtocol: 
    MRClientProtocol: 当作业的ApplicationMaster启动成功后，后启动MRClientService服务，运行客户端直接通过该协议与ApplicationMaster通信以控制作业等任务。
    
    YarnClientProtocolProvider: 该类会创建一个YARNRunner对象作为真正的客户端。 YARNRunner实现了MRv1中的ClientProtocol接口，
    并将ApplicationClientProtocol协议的RPC客户端句柄作为它的内部成员。用户的作业实际是通过YARNRunner
    类的submitJob函数提交的，在该函数内部，会进一步调用ApplicationClientProtocol的submitApplication函数，最终将
    作业提交到ResourceManager上。当ApplicationMaster成功启动后，客户端可以通过ClientServiceDelegate直接与ApplicationMaster交互
    以查询作业运行状态和控制作业。
    
    
### 8.3 MRAppMaster工作流程
本地模式(通常用于作业调试)，Uber模式，和Non-Uber模式。

(1) Uber运行模式

    小作业运行时，为了降低延时，使用Uber模式，
    Map Task 数目不超过mapreduce.job.ubertask.maxmaps   9
    Reduce Task数目不超过mapreduce.job.ubertask.maxmaps   1
    输入文件大小不超过mapreduce.job.ubertask.maxbytes(默认是一个Block大小)
    Map Task和Reduce Task需要的资源量不超过MRAppMaster可使用的资源量
    链式作业不允许运行在Uber模式下
    
    
