## ResourceManager剖析
按照前面的理解，ResourceManager负责集群中所有资源的统一管理和分配，接受来自各个节点(NodeManager)的资源汇报信息，
并将这些信息按照一定的策略分配给各个应用程序(实际上是ApplicationMaster)。

    ResourceManager 主要完成一下几个功能
    与客户端交互，处理来自客户端的请求
    启动和管理ApplicationMaster，并在它运行失败时重新启动它。
    管理NodeManager，接受来自NodeManager的资源汇报信息，并向NodeManager下达管理命令
    资源管理与调度，接受来自ApplicaionMaster的资源申请请求，并为之分配资源。
    