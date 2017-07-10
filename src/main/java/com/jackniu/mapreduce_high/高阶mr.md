## 高阶MapReduce
在高阶数据处理中，经常会发现无法把整个流程写在单个MapReduce作业中。因此Hadoop支持将多个MapReduce程序链接成更大的作业。
高阶数据处理涉及多个数据集。Hadoop中用于同时处理多个数据集的各种联结技术。当一次处理一组记录时，我们就可以
写出一些更高效的数据处理任务的代码。Streaming天生具有一次处理整个分片的能力，最大值函数的Streaming实现充分运用了这种能力。

Java程序也可以做同样的事情，还会剖析Bloom filter,并把她的实现放在一个保持着记录信息的mapper中。

### 5.1.1 顺序链接MapReduce作业
将MR作业按照顺序链接在一起，用一个MR作业作为下一个的输入，MR作业的连接类似于Unix的管道

    mapreduce-1 | mapreduce-2|mapreduce-3

driver为MR作业创建一个JobConf对象，带有很多参数，JobClient.runJob(job)启动这个MR作业，
JobClient.runJob()运行到作业结尾处被阻止时，MR作业的链接会在一个MR作业之后调用另一个作业的driver。每个作业的
driver都必须创建一个新的JobConf对象，并将其输入路径设置为前一个作业的输出路径。可以在最后阶段删除在链上的每个阶段生成的中间数据。

### 5.1.2 具有复杂依赖的MR链接
子任务并不是按照顺序运行，则MR作业不能按照线性方式链接。MR1处理A，MR2处理B，MR3对1和2的输出结果做内部联结。

Hadoop: 通过Job和JobControl类来管理这种(非线性)作业之间依赖，Job对象是MR作业的表现形式。除了必要的配置信息外，JOb对象还可以
通过设定addDependdingJob维护作业的依赖关系

    x.addDependingJob(y)
    JobControl.addJob(job)
    JobControl.run()方法，生成一个线程来提交作业并监督执行。
    JobControl.getFailedJobs() JobControl.getFInished() 监控Job任务的执行。
    
### 5.1.3 预处理和后处理阶段的链接

    使用ChainMapper和ChainReducer所生产的作业表达式如下
    MAP+ | REDUCE | MAP+
    预处理： 多个Mapper，reduce之后，执行多个Mapper 执行后处理
    全部预处理和后处理步骤在单一的作业中运行，不会生成中间文件。
    
    ChainMapper.addMapper(job,Map4.class,Text.class,Text.class,Text.class,Text.class,true,map4conf);
    
说明：

    （1）全局JobConf对象， 作业名，输入输出路径。
    （2） 本地JobConf对象采用一个新的JobConf对象，且在初始化时不设默认值——new JobConf(false)
    （3） byValue: 标准的Mapper模型中，键值对的输出在序列化之后写入磁盘，等待被洗牌到一个可能完全不同的节点上，形式上认为这个过程采用的是值传递，发送的是键值对的副本。
            如果Map不改变kv值，则buvalue为false获得一定的性能，最后设置为byValue=true，采用值传递。
            
## 5.2 连接不同来源的数据
cite75_99.txt: 专利的引用数据
apat63_99.txt: 专利数据中的国家信息

在Mysql中联结和简单，

### 5.2.1 Reduce侧的联结
Hadoop 有一个名为datajoin的contrib软件包，数据连接的通用框架。 datajoin/hadoop-*-datajoin.jar

    /opt/hadoop/share/hadoop/tools/lib
    reduce侧联结: repartitioned join（重分区联结）， repartitioned sort-merge join(重分区排序-合并联结)
   
MapReduce范式每次以无状态的方式处理一个记录。如果想保持一些状态信息，就得保持记录的状态
groupkey的作用就是数据库中的join key(联结键)。

1 数据流
    
    1,Stephanie Leung,555-555-5555
    group key, Coutomers 为Tag
    
    重分区联结中，mapper首先用一个组键和一个标签封装每个记录。组键为联结属性，标签记录数据源
    
2 DataJoin软件包实现联结

    特殊的类型: TaggedMapOutput: 组键为Text，值为TaggedMapOutput类型
    子类实现TaggedMapOutput， 并为Writable类型
    

