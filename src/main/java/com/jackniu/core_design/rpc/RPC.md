## Hadoop 的RPC框架
RPC就是网络通信，是分布式系统中最底层的模块，直接支撑了上层部分是环境下复杂的进程间通信逻辑，是所有
分布式系统的基础。RPC是一种常用的分布式网络通信协议，运行运行于一台计算机的程序调用另一台计算机的子程序，同时将
网络的通信细节隐藏起来，使得用户无需额外的为这个交互作用编程。

RPC实际上是Client/Server模型的一个应用实例。

* Java反射机制

        interface CalulatorProtocol{// 定义一个接口协议
            public int add(int a,int b);
            public int subtract(int a.int b);
            }
        
        class Server implements CalculatorProtocol{
            public int add(int a,int b){
                return a+b;
               }
            ..
            }
        
        // 实现调用处理器接口
        class CalulatorHandler implements InvocationHandler{
            private Object objOriginal;
            public CalculatorHandler(Object obj){
                this.objOriginal = obj;
            }
            // 用来处理动态代理类对象上的方法调用
            public Object invoke(Oject proxy, Method method,Object[] args){
            throws Throwable{
                Object result = method.incoke(this.objOriginal,args);
                return result;
                }
         }
         public class DynamicProxyExample{
            public static void main(String[] args}{
               CalulatorProtocol server = new Server();
               InvocationHandler handler = new CalulatorHandler();
               CalculatorProtocol client = Proxy.newProxyInstance(server.getClass().getClassLoader(),server.getCass().getInterfaces(),handler);
               // client调用远程服务的方法实现。
               int r = client.add(5,3);
               ....
             }
             
* Java网络编程
    
        网络通信，Nio，OIO，但是还是采用Netty更加简单。
        
* Hadoop RPC框架分析

    一个典型的RPC程序通常包含以下几个模块
        
        1.通信模块: 两个相互写作的通信模块实现请求-应答协议，在client和server之间传递请求和接受消息
        2. Stub程序: 客户端和服务器均包含Stub程序，可将之代理程序。使得远程调用函数和在本地的表现一样。
            经过在客户端的调用和本地一样，但是不是本地直接执行的，通过网络模块将请求发送给服务器。
        3. 调度程序: 调度程序接受来自通知模块的请求消息，并根据其中的标识选择一个Stub程序处理。
        
            客户端                                     服务器端
            客户程序  stub程序   通信模块 通信模块，调度程序，stub程序，服务过程
            
        
        Hadoop RPC 使用
            1. 定义RPC协议，通信协议，服务器和客户端，继承VersionedProtocol  getProtocolVersion(String protocol,   
                                                                                        long clientVersion)
                                                                                        用以检查服务器和客户端是否一致
            2. 实现RPC协议
                实现上面的接口
            3. 构造RPC Server 并启动
                使用静态方法getServer构造一个server
            4. 构造RPC cleint 并发送请求。
            
  
Hadoop ipc.RPC
  
        RPC类是对底层client/server网络模型的封装，提供简洁的上层接口。
        public static class Server extends org.apache.hadoop.ipc.Server 
        
        主要方法 public Writable call(Class<?> protocol, Writable param, long receivedTime) throws IOEx
        
        CLientCatch 缓存对象，达到Client对象重用的目的。
         private static class ClientCache {
                private Map<SocketFactory, Client> clients;
                
        Hadoop RPC 调用由客户端发出，在服务端执行并返回，因此不能在invoke中调用相关函数
        网络传输，invoke参数必须可序列化，发送到服务器。
        private static class Invocation implements Writable, Configurable
        
  
Hadoop ipc.Client
    
        主要是发送远程调用并接受执行结果。
        内部call方法
         public Writable call(Writable param, InetSocketAddress addr, Class<?> protocol, UserGroupInformation ticket, 
                    int rpcTimeout, Configuration conf) throws InterruptedException, IOException {
         public Writable call(Writable param, Client.ConnectionId remoteId) throws InterruptedException, IOException 
            
         内部Call类，封装一个RPC请求。
         private class Call {
                 int id;  // 通过Id识别不同的函数调用
                 Writable param;
                 Writable value;   //返回值
                 IOException error;
                 boolean done;
              
         Connection client和server之间通信连接 ，netty中叫channel，链接相关信息被封装到Connection中。
         
         private class Connection extends Thread 
            addCall  将一个Call对象添加到哈希表中
            sendParam   向服务器发送RPC请求
                d.writeInt(call.id); call.param.write(d);                             
            receiveResponse    接受已经处理完的请求
            run     
          
         所以呢 整个过程就比较明晰
            1. 创建一个Connections对象，并将远程方法调用信息封装成Call对象，放到Connection对象中的哈希表calls中。
            2. 调用Connection类中的sendParam()方法将当前Call对象发送给Server
            3. Server端处理完RPC请求后，将结果通知通过网络返回给Client端，Client端通过receiveResponse() 函数取得结果
            4. Client端检查结果处理状态（成功还是失败），并将对应的Call对象从calls中删除。

ipc.Server 

    Hadoop  M/S 结构，Master是整个系统的单点，如NameNode或JobTracker。
    Master通过ipc.Server接受并处理所有slave发送过来的请求，ipc.Server 将高并发和可扩展性作为设计目标。
    
    Reactor是并发编程中的一种基于事件驱动的设计模式
        Reactor: IO事件的派发者
        Acceptor: 接受来自Client的连接，并建立与Client对应的Handler，并向Reactor注册此Handler
        Handler: 处理对象，并包含业务逻辑。 
        Render/Sender: 存放数据处理线程的线程池。
        
    (1) 接受请求Listener： 监听类，用以监听客户端发来的请求。同时Listener下面还有一个静态类，Listener.Reader，当监听器监听到用户请求，便用让Reader读取用户请求
    Listener主要负责Socket的监听以及Connection的建立，同时监控ClientSocket的数据可读事件，通知Connection进行processData，收到完成请求包以后，
    封装为一个Call对象（包含Connection对象，从网络流中读取的参数信息,调用方法信息），将其放入队列
    
    (2) 处理请求： Handler实现，并家结果返回给对应的俄客户端，由Responder线程实现
        Server.this.responder.doRespond(e);
    (3) 返回结果
         private class Responder extends Thread {
                 private Selector writeSelector;
                 采用异步方式发送给客户端。
                 if(ie.isValid() && ie.isWritable()) {
                       this.doAsyncWrite(ie);
                 }
                 
Hadoop 参数调优

    Render数目: ipc.server.read.threadpool.size 配置，默认1，一个RPC Server只包含一个Reader线程
    Handler线程对应的最大call数目，默认100，
    Handler线程数目，JobTracker和NameNode分别是HDFS两个子系统中的RPC Server，对应的Handler数目分别是
        mapred.job.tracker.handler.count 和dfs.namenode.service.handler.count 指定，默认是10
    客户端最大重连次数: 客户端最大尝试重连次数: IPC.CLIENT.CONNECT.MAX.RETRIES.DEFAULT 
    
## MR 通信协议
在Hadoop MR中，不同组件之间的通信协议都是基于RPC的，支撑整个MR系统

*   JobSubmissionProtocol: Client(普通用户)与JobTracker之间的通信协议，用户通过该协议提交作用，查看作业运行情况等。
*   RefreshUserMappingsProtocol: Client(管理员)通过该协议更新用户-用户组映射关系。
*   RefreshAuthorizedPolicyProtocol: Client(管理员)通过协议更新MR服务级别访问控制列表。
*   AdminOperationsProtocol: Client(管理员)通过该协议更新队列访问控制列表和节点列表。
*   InterTrackProtocol: TT和JT的通信协议，TT通过该协议汇报本节点的资源和任务运行状态等信息，并执行JobTracker发送的命令
*   TaskUmbilicalProtocol: Tash和TaskTracker之间的通信协议，每个Task实际上是其同节点TaskTracker的子进程，通过协议汇报Task运行状态

### JobSubmissionProtocol通信协议

    （1）作业提交
        public JobStatus submitJob(JobID jobName,String jobSubmitDir,Credentials ts) throws IOException
        ts 为该作业分配到的密钥或安全令牌
     (2) 作业控制
        修改作业优先级，杀死一个作业 killJob, 杀死一个任务 killTask
     (3) 查看系统状态和作业运行状态
        ClusterStatus getClusterStatus()
        JobStatus getJobStatus
        ...
### InterTrackerProtocol通信协议
该协议一个最重要的参数就是beartbeat,周期性的被调用，进而形成了TaskTracker与JobTracker之间的心跳。

    HeartbeatResponse heartbeat(TastTrackerStatus status,boolean restarted,
                                boolean restrated....)
    status 封装了所有节点的资源使用情况和任务运行情况。
    完成TT和JT的通信
    
### TaskUmbilicalProtocol通信协议
Task和TaskTracker之间的通信协议。

    按调用频率，一种是周期性的被调用的方法，一种是按需调用的方法
    boolean statusUpdate(TaskAttemptID taskID,TaskStatus taskStatus,JvmContext jvmContext)
    boolean ping(TaskAttemptID  tsakId,JvmContext jvmContext) throws IOException
    完成Task的汇报情况。Task每 3秒内调用statusUpdate向TT汇报最新进度，如果没有任何数据，则主动调用ping方法
    
    第二类方法: Task的不同阶段被调用。
    
### 其他通信协议
(1)RefreshUserMappingsProtocol 更新用户-用户组映射关系和更新超级用户代理列表
   
    Hadoop可以直接使用相应的shell命令即可完成更新操作
    默认情况下，Hadoop使用Unix/linux系统自带的用户-用户组映射关系，且对其进行了缓存，如果管理员修改了某些映射关系
    可以使用Shell命令更新hadoop MR缓存
    bin/hadoop mradmin -refershUserToGroupMappings
    还可以在core-site.xml中配置 
   
(2) RefreshAuthorizedPolicyProtocol更新MR服务级别访问控制列表，hadoop-policy.xml中配置。
    
        <name>security.job.submission.protocol.acl</name>
        <value> user1,user2 group1,groups</value>
        
        bin/hadoop mradmin -refreshServiceAcl

......

小结
    
    下层是一个C/S模型，反射，动态代理，网络编程，nio等
    RPC: RPC,Client,Server,对外编程接口，客户端实现和服务器端实现
    6个通信协议。
    
    
非常有意思，加油加油！！！
    