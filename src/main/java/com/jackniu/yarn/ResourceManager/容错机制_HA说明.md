## Yarn 容错机制
YARN作为一个分布式系统，需要考虑ApplicationMaster，NodeManager，Container和ResourceManager等服务或组件的容错性，
这些服务或组件的容错机制如下:

ApplicationMaster容错

    不同类型的应用程序有不同的ApplicationMaster，而ResourceManager负责监控ApplicationMaster的运行状态，一旦发现它运行失败或者超时
    会为其重新分配资源并启动他。 至于启动之后ApplicationMaster内部的状态如何恢复需要由自己保证。
  
NodeManager容错

    如果NodeManager 在一定时间内未向ResourceManager汇报心跳信息(可能是网络原因或者自身原因)，则Resourcemanager
    认为它已经死掉了，会将它上麦年所有正在运行的Container状态设置为失败，并告诉对应的ApplicationMaster，以决定如何处理这些
    Container中运行的任务。

Container容错

ResourceMansger容错

    非常重要，自身的容错性决定了Yarn的可用性和可靠性。
    
###Hadoop HA基本框架
在MS架构中，为了解决Master的单点故障问题，通常采用热备方案，即集群中存在一个对外服务的ActiveMAster和若干个处于
就绪状态的Standby Master，一旦Active Master出现故障，立即采用一定的策略选择某个Standby Master转换为Active Master
以正常对外提供服务。

总体来说，Hadoop 2.0 中的HDFS和YARN均采用了基于共享存储的HA解决方案，即Active Master不断将信息写入一个共享存储系统，
而Standby Master 则不断读取这些信息，以与Active Master的内存信息保持同步，当需要主备切换时，选中的Standby Master
需先保证信息完全同步后，再将自己的角色切换至Active  Master。目前而言，常用的共享存储系统有以下几个:

    Zookeeper
    NFS
    HDFS:
    BookKeeper

Hadoop2.0 中，YARN HA采用了基于Zookeeper的解决方案，