## Yarn

    1-2章是基础篇:简单的介绍Hadoop YARN的环境搭建和基本设计架构
    3-7章是核心设计: 讲解YARN基本库，应用程序设计方法和运行时环境的实现，ResourceManager，NodeManager和资源调度器等关键组件的内部实现细节
    8-10章是框架，计算比较流行的可在YARN上运行的计算框架，包括MR，DAG计算框架Tez，Storm和内存计算框架Spark
    11-13章开源资源管理系统，Corona，Mesos等，总结了资源管理系统的特点以及发展趋势。
    
Hadoop YARN分为5部分: API、 Common、Applications、Client 和Server

    YARN API(hadoop-yarn-api目录):给出了YARN内部涉及的4个主要RPC协议的Java声明和Protocol Buffers定义，
        这4个RPC协议分别是ApplicationClientProtocol、ApplicationMasterProtocol、ContainerManagementProtocol
        和ResourceManagerAdministrationProtocol，第2章对这部分内容进行解释
    
    YARN Common(hadoop-yarn-common目录):该部分包含了YARN底层库实现，包括事件库、服务库、状态机库
        Web界面库等，第三章进行详细介绍
    
    YARN Applications(hadoop-yarn-applications目录): 该部分包含了两个Application编程实例分别是distributedshell
        和Unmanaged AM，将在第四部分进行介绍
    
    YARN Client(hadoop-yarn-client目录): 该部分封装了几个与YARN RPC协议交互相关的库，方便用户开发应用程序，将在
        第四部分进行介绍
    
    YARN Server(hadoop-yarn-server目录): 该部分给出了YARN的核心实现，包括ResourceManager、NodeManager、
        资源管理器等核心组件的实现、
    