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
    
    DataJoinMapperBase
    三个方法：
    generateInputTag(String inputFile): 在map任务开始时被调用，指定一个全局的标签，Text类型； 这个变量会被设置: inputType 的值
    generateGroupKey(TaggedMapOutput taggedMapOutput)：取得被标记的记录(TaggedOutput类型)，返回组键，数据是TaggedOutput 数据
    enerateTaggedMapOutput(Object o): 返回一个待用任何Text标签的TaggedWritable，Text由上一个方法产生。
                                                      返回的数据: groupKey: Tag, Writable内容
                                                      
    所以，执行顺序是:
    map 方法组合执行顺序， 一般显示map任务开始时，执行了generateInputTag，后在map方法中，aRecord， groupKey， 然后collect
    generateInputTag(inputFile)----> generateTaggedOutputMapOutput(Object o)(即为每一行记录)--> generateGroupKey(上一个输出的带有标签的Writable)-->
    output.collect(Text,Writable)
    
    父类总的map方法：
    public void map(Object key, Object value, OutputCollector output, Reporter reporter) throws IOException {
            if(this.reporter == null) {
                this.reporter = reporter;
            }
    
            this.addLongValue("totalCount", 1L);
            TaggedMapOutput aRecord = this.generateTaggedMapOutput(value);
            if(aRecord == null) {
                this.addLongValue("discardedCount", 1L);
            } else {
                Text groupKey = this.generateGroupKey(aRecord);
                if(groupKey == null) {
                    this.addLongValue("nullGroupKeyCount", 1L);
                } else {
                    output.collect(groupKey, aRecord);
                    this.addLongValue("collectedCount", 1L);
                }
            }
        }
        
    
    
    
    reduce操作:combine操作，其返回reduce的输出结果，就是Writable类型值
    protected TaggedMapOutput combine(Object[] tags, Object[] values) {
    从map传递过来的数据， 第一组是 tags[0]和values[0],第二组tags[1],values[1]......
    tags={"Coutomers","Orders"}
    values={"3,Jose Madriz,281-330-8004","3,D,25.02,22-Jan-2009"}
    
     TaggedWritable tw = (TaggedWritable)values[i];
                     String line = ((Text) tw.getData()).toString();
                     String[] tokens =line.split(",",2);
                     joinedStr += tokens[1];
     上面的输出是将所有的结果都联结成一个单个记录输出
     line.split(char,int): int为分隔次数，1 次代表不分隔
     
     现在已经获取了combine的值，也就是reduce的value值，但是reduce中的key,value 不用设置吗？
     在父类中： output.collect(key, aRecord.getData());
     因此，不用实现reduce，只是combine
     
     运行不成功: java.lang.ClassNotFoundException: org.apache.hadoop.contrib.utils.join.DataJoinMapperBase
     
### 基于DistributedCache的复制联结
Reduce侧的联结技术直到Reduce阶段才会进行联结。 如果在map结果就去除不必要的数据，联结就会更有效率

在map阶段执行联结的主要难点是一个mapper正在处理的记录可能会与另一个不易访问的记录进行联结。如果在map一条记录时
可以访问所有需要的数据，在map侧的联结就可以工作。

    如果两个数据源被划分到相同数量的分区，分区按照键排序的，且这个键又是所需的联结键，则每个mapper
    就可以精确的查找和检索到执行联结所需的所有数据。
    org.apache.hadoop.mapred.join软件含有helper类，可以很容易的实现map侧的联结。
    
     
### 5.3 创建一个Bloom filter
事务性处理的需求。在实时数据处理中，Bloom filter是一个相对而言较少为人知的的工具，是一个数据集的招摇，它的使用让其他的数据处理技术更为有效。
当数据集很大时，Hadoop经常被用来生成一个Bloom filter的数据集表示。Bloom filter有时还被用作Hadoop内部的数据联结。

Bloom filter对象支持两个方法，add()和contains(),contains() 会有小概率的误报。误报的概率依赖于集合中的元素个数，以及一些Bloom filter自身的配置参数。

Bloom filter的主要又是在于他的大小为常数且在初始化时被设置。增加更多的元素到一个Bloom filter中不会增加它的大小，仅增加误报的概率。

    误报率: (1-exp(-kn/m))k ，k为散列函数的个数，m是用于存储bloom filter的比特个数，而n是被添加到Bloom filter的元素个数。 
    k=ln(2)*(m/n)= 0.7*(m/n)
    m/n 即为每个元素的比特个数。
    
Bloom filter 类的签名

    class BloomFilter<E>{
        public BloomFilter(int m,int k){...}
        public void add(E obj){...}
        public boolean contains(E obj){....}
     }
     
mapper: 一般意义上的mapper是处理整个分片中的每条记录，一般在map中输出每一个记录的处理，而在这里是将整个分片的记录处理完成之后再统一输出到reducer，
在close()方法中输出整个分片的BloomFilter，确保所有的就都被读取了。

一个用于生成Bloom filter的MapReduce程序：

    .....
    
这个程序在很多地方可以修改: 主要是获取哈希索引的地方
    

    
    

