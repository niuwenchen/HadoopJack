## 编程读写HDFS

项目: 分析来自Web服务器的Apache日志，将多个小文件合并为大文件。在存储到HDFS之前合并小文件。
Hadoop有一个getmerge 命令，用于将一组HDFS文件在复制到本地计算机以前进行合并。

    Hadoop文件API的起点是FileSystem类。 这是一个与文件系统交互的抽象类，存在不同的具体实现子类用来处理HDFS
    和本地文件系统。可以调用factory的FileSystem.get(Configuration conf)来得到FileSystem 实例。configutation类
    适用于保存参数的特殊类，默认实例化方法是以HDFS系统的资源配置为基础的。
    Configuration  conf = new Configuration();
    FileSystem hdfs = FileSystem.get(conf)
    
    FileSystem local = FileSystem.getLocal(conf)
    
    Hadoop文件API使用Path对象来编制文件和目录名，使用FileStatus对象类存储文件和目录的元数据。
    
    注意： 具体的文件形式无需关注，和Java 中的一样。
    http://hadoop.apache.org/docs/stable/api/  中的FileSystem 均有讲。
    


## 分析MapReduce
MapReduce 一般通过操作键/值对来处理数据，一般形式为：
map:(K1,V1) -> list(K2,V2)
reduce:(K2,list(V2))-> list(K2,V3)

![](file:///C:\Users\loneve\Pictures\hadoop\0.png)

    输入数据--> 分布在两个节点上--> 每个map任务处理一个数据分片--> Mapper输出中间数据--> 节点间的数据交换在“洗牌”阶段完成
    --> 相同key的中间数据进入相同的reducer--> 存储reduce的输出
### Hadoop数据类型
Hadoop中的数据为了能够在集群上移动，需要序列化的数据，只有支持序列化的数据才能在框架中充当键/值

实现Writable接口的类可以是值，而实现WritableComparable<T>接口的类既可以是键也可以是值。注意WritableComparable<T>接口是Writable
和Comparable<T>接口的组合。对于键而言，如果需要排序比较，则需要而实现WritableComparable

Hadoop预定义的： BooleanWritable, ByteWritable, DoubleWritable, FloatWritable,LongWritable,Text,NullWritable
    

### Mapper
一个类作为Mapper，需继承MapReduceBase基类并实现Mapper接口，

    void configure(JobConf job): 该函数读取XML配置文件或者应用程序主类中的参数，在数据处理之前调用该函数
    void close(): 作为map任务结束前的最后一个操作，该函数完成所有的结尾工作，如关闭数据库连接，文件等

    Mapper<K1,V1,K2,V2>
    void map(K1 key,V1 value,OutputCollector<K2,V2> output,Reporter reporter) throws Exception
    OutputCollector 接受这个映射过程的输出，Reporter可提供对mapper相关附加信息的记录，形成任务进度。
    
Hadoop的一些Mapper实现

    IdentityMapper<K,V>:  实现Mapper<K,V,K,V> 将输入直接映射到输出。
    InverseMapper<K,V>:   实现Mapper<K,V,K,V>反转键值对
    RegexMapper<K,V>:     实现Mapper<K,Text,Text,LongWritable>,为每个常规表达式的匹配项生成一个(match,1)对
    TokenCountMapper<K>:  实现Mapper<K,Text,Text,LongWritable>，当输入的值为分词时，生成一个(token,1)对

### Reducer

    void  reduce(K2 key,Iterator<V2> values,OutputCollector<K3,V3> output,Reporter reporter) thrws Exception
    接受来自各个mapper的输出时，按照键值对中的键对输入数据进行排序，并将相同键的值归并。
    然后调用reduce函数，通过迭代处理哪些指定键相关联的值，生成一个可能为空的(K3,V3).
    
    IdentityReducer<K,V>  实现Reducer<K,V,K,V>，将输入直接映射为输出
    LongSumReducer<K>       实现Reducer<K,LongWritable,K,LongWritable>，计算与给定键相对应的所有值的和
    
### Partitioner:重定向Mapper输出
在map和reduce阶段还有一个非常重要的步骤: 将mapper的结果输出给不同的reducer，就是partitioner的工作

当使用多个reduer时，我们需要采取一些方法来确定mapper应该把键值对输出给谁。默认的做法是对键进行散列开确定
reducer。Hadoop通过HashPartitioner类强制执行这个策略。

http://www.cnblogs.com/luoliang/p/4184938.html

![](file:///C:\Users\loneve\Pictures\hadoop\1.png)

在map和reduce阶段之间，一个MapReduce应用必然从mapper任务得到输出结果，并把这些结果发布给reduce任务。
该过程通常称为洗牌，因为在单节点上的mapper输出可能会被送往分布在集群多个节点上的reducer

### 3.2.5 Combiner: 本地reduce
在分发mapper结果之前做一下本地reduce， 即("the",574）键值对 比("the",1) 更为高效

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(LongWritable.class);
    conf.setMapperClass(TokenCountMapper.class);
    conf.setCombinerClass(LongSumReducer.class);
    conf.setReducerClass(LongSumReducer.class);
   
JobClient 和JobConf 的意思


### 读和写
MapReduce处理的基本原则之一是将输入数据分隔成块。这种块可以在多台计算机上并行处理。这些块被称为输入分片(Input Split),
每个分片应该足够小以实现更细粒度的并行。(如果所有的输入数据都在一个分片中，那就没有并行了),另一方面，每个分片也不能太小，
否则启动与停止各个分片处理所需的开销将占去很大一部分执行时间。

并行系统切分输入数据(输入数据通常为单一的大文件)揭示了其背后通用文件系统(HDFS)的一些设计决策。例如，Hadoop的文件系统
提供了FSDataInputStream类用于读取文件，而未采用Java中的java.io.DataInputStream.
如果每个分片/块都有他所驻留的机器进行处理，就自动实现了并行。

Hadoop默认的将输入文件中的每一行视为一个记录，而键值对分别为该行的字节偏移key 和内容 value。

### 3.3.1 InputFormat
InputFormat:TextInputFormat 是默认实现，从TextInputFormat返回的键为每行的字节偏移量，而尚未有程序使用这个键用于数据处理。

常用的InputFormat类

    TextInputFormat: 一行一个记录，key为一行的字节偏移，value为内容
    KeyValueTextInputFormat: 每一行为一个记录，分隔符为界，前一个为key，后一个为value，分隔在
                    key.value.separator.in.input.line 中设定，默认为\t
    SequenceFileInputFormat<K,V>:   用于读取序列文件的InputFormat,键和值由用户定义。序列文件为Hadoop专用的压缩二进制文件格式
                专用于一个MapReduce作业和其他MapReduce作业之间传送数据。
                Key: 用户定义， Value: 用户定义
    NLineInputFormat:   每个分片一定有N行，N在mapred.line.input.format.linespermap中设置，默认1
                    Key: LongWritable
                    value: Text
    
一般最常用的是 KeyValueTextInputFormat  

    conf.setInputFormat(KeyValueTextInputFormat.class)
当链接多个MapReduce作业时，首选序列文件格式。均有用户定义，输出和输入类型必须匹配.

2 自定义InputFormat

    public interface InputFormat<K,V>{
        InputSplit[] getSplits(JobConf job,int numSplits) throws IOException;
        RecordReader<K,V> getRecordReader(InputSplit split,JobConf job, Reporter reporter)
    确定所有用于输入数据的文件，将之分隔为输入分片，每个map任务分配一个分片
    提供一个对象，循环提取给定分片中的记录，并解析每个记录为预定义类型的键与值
    
    所有大于一个分块的文件都要分片。isSplitable(FileSystem fs,Path filename)方法，检查是否可以将
    给定文件分片，默认实现总是返回true。
    一些压缩文件并不支持分隔（并不能从文件的中间读取数据）。一些数据处理操作，如文件转换，需要把每个文件视为一个原子
    记录，也不能将之分片。
    
    RecordReader： 将一个输入分片解析为记录，把每个记录解析为一个K/V对。
    LineRecordReader 实现了RecordReader，具体的行为和前面描述的一样。
    KeyValueLineRedordReader 被用于KeyValueTextInputFormat
    
### OutputFormat
RecordWriter 对象将输出结果进行格式化，而RecordReader对输入格式进行解析

    TextOutputFormat<K,V>
    SequenceFileOutputFormat<K,V>
    NullOutputFormat
    


## 小结
Mapper
Reducer
Partitioner: 重定向Mapper输出，并行操作
combiner: map输出之前reduce

