## Hadoop RPC参数调优
* Reader线程数目: 由参数ipc.server.read.threadpool.size配置，默认是1，也就是说，默认情况下，一个RPC Server只包含一个reader线程
    reader就是将新的RPC请求封装成call对象，放到共享队列callQueue中
 
* 每个Handler线程对应的最大Call数目。 由参数ipc.server.handler.queue.size 指定，默认是100，也就是说，默认情况下，
    每个Handlee线程对应10，则整个Call队列最大长度为100*10 = 1000

* Handler线程数目。handler数目 yarn.resourcemanager.resource-tracker.client.thread-count 
                    dfs.namenode.service.handler.count 指定，默认分别是50和10 ， 当集群规模较大时，会大大影响系统性能。
   
* 客户端最大尝试重连次数， ipc.client.connect.max.retries指定。
    public static final String IPC_CLIENT_CONNECT_MAX_RETRIES_KEY = "ipc.client.connect.max.retries";
    
