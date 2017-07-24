## YARN设计理念与基本架构
MR1在扩展性，可靠性，资源利用率和多框架等方面明显存在不足，Apache开始尝试对MR进行升级改造，于是诞生了更加先进
的下一代MR计算框架MR2，由于MR2将资源管理模块购建成了一个独立的通用系统YARN，这就使得MRv2的核心从计算框架
MR转移为资源管理系统YARN。

### 2.1 产生背景
* 扩展性   MRV1中，JobTracker同时兼备资源管理和作业控制两个功能，系统的最大瓶颈，严重制约了Hadoop集群拓展性
* 可靠性   MRV1采用Master/slave结构，master存在单点故障问题，一旦出现故障将导致整个集群不可用。
* 资源利用率低    槽位策略
* 无法支持多种计算框架    内存计算型框架，流式计算框架和迭代计算框架等，MRv不能支持多种计算框架并存

以MapReduce为核心和以Yarn为核心的软件对比发现，以Mr为核心的架构，如Yarn，Mesos都在MR上进行运算，而以YARN为核心
的都在YARN上，如MR和Spark等都在YARN上。

### 2.1.2 轻量级弹性计算平台
YARN实际上是一个弹性计算平台，他的目的已经不在局限于支持MR的一种计算框架，而是朝着对多种框架进行统一管理的方向发展
相比于"一种计算框架一个集群"的模式，共享集群的模式存在多种好处：
* 资源利用率高。 
* 运维成本低
* 数据共享

Hadoop2.0即第二代Hadoop，为克服Hadoop1.0中HDFS和MR存在的各种问题而提出的。针对HDFS中的单NameNode制约HDFS的扩展性问题，提出了HDFS Federation，
让多个NameNode分管不同的目录而实现访问访问隔离和横向扩展，同时解决了NameNode单点故障问题；针对Hadoop 1.0
中的MR在扩展性和多框架支持等方面的不足，将JobTracker中的资源管理和作业控制功能分开，分别由组件ResourceManager和ApplicationMaster实现，
其中，ResourceManager负责所有应用程序的资源分配，而ApplicationMaster仅负责管理一个应用程序，进而诞生了
全新的通用资源管理框架YARN。基于YARN，用户可以运行各种类型的应用程序，从离线计算MR到在线计算流式的Storm等

变动:
HDFS模块: 主要增加的新特性包括支持追加操作与建立符号连接、SecondaryNameNode改进(secondary namenode被剔除，取而代之的是checkpoint node同时添加一个
backup node的角色，作为NameNode的冷备)，运行用户自定义block放置算法等
MR模块: 在作业API方面，开始启动新的MR API，但仍然兼容老版API。


2 YARN基本组成结构
YARN总体上仍然是Master/Slaver结构，RM为Master，NodeManager为Slave，RM负责对各个NodeManager上的资源进行统一管理和调度。
当用户提交一个应用程序时，需要提供一个用以跟踪和管理这个程序的ApplicationMaster，负责像RM申请资源，并要求NM启动
可以占用一定资源的任务。由于不同的AM被分布到不同的节点上，因此它们之间不会相互影响。

* 1 ResourceManager(RM)
RM是一个全局的资源管理器，负责整个系统的资源管理和分配。主要由两个组件构成:调度器(Schedular)和应用程序管理器(Application Master, AM)

(1) 调度器
调度器根据容量，队列等限制条件(如每个队列分配一定的资源，最多执行一定数量的作业等)，将系统中的资源分配给各个正在运行的应用程序。
该调度器是一个“纯调度器”，不再从事任何与具体应用程序相关的工作，比如不负责监控或者跟踪应用的执行状态等，
也不负责重新启动因应用执行失败或者硬件故障导致而产生的失败任务，这些均交由应用程序相关的AM完成。调度器仅
根据各个应用程序的资源需求进行资源分配，而资源分配单位用一个抽象概念"资源容器"(Resource Container,简称Container)表示
Container是一个动态资源分配单位，将内存，CPU，磁盘，网络等资源封装在一起，从而限定每个任务使用的资源量。
此外，该调度器是一个可插拔的组件，用户可根据自己的需求设计新的调度器，如FairScheduler和Capacity Scheduler等。






















