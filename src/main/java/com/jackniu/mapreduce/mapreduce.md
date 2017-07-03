## MapReduce基础程序
http://www.nber.org/patents/

寻找这些数据 中的关系，引用关系等。

何为倒排？ 对于每一个专利，希望找到那些引用它的专利并进行合并。

和wordcount的区别就是多了一个  

    job.setInputFormat(KeyValueTextInputFormat.class); 默认是TextInputFormat，需要额外指定
    job.setOutputFormat(TextOutputFormat.class);
    job.set("key.value.separator.in.input.line",",");
    
class MyJob extends Configured implements Tool 

    run()方法，driver，实例化，配置并传递一个JobConf对象命名的作业给JobClient.runJob()以启动MapReduce作业。
    反过来，JobClient类与JobTracker类通信让该作业在集群上启动。
    JobConf对象将保持作业运行所需的全部配置参数。
    Driver需要为每个作业定制基本参数，包括输入路径，输出路径，Mapper类和Reducer类，另外可以充值默认的作业属性，
    例如InputFormat，OutputFormat
    
        -conf       指定一个配置文件
        -D          给JobConf属性赋值
        -fd         指定一个NameNode，可以是local
        -jt         指定一个JobTracker
        -files      指定一个以逗号分隔的文件列表，用于MapReduce作业
    
     一般Reduce方法都会循环遍历 Iterator中的值
     setOutputKeyClass和setOutputValueClass：设置Reducer的输出的key-value对的格式
     

### 4.3 计数
上一步是计算 不同专利号的被引用专利号
这一步是统计 一个专利号下面有多少次被引用
下一步是计算被引用次数的分布。比如被引用3次的有多少个，5次？10次？


### 4.4 新版本的API变化

    JobConf job  ==>  Job job  = new Job()
    没有Collector 而是Context 
        Context.write()
    不知道有没有KeyValueInputFormat,如果使用TextInputFormat ,就需要按照行来分隔成key，value
        
### 4.5 Hadoop的Strraming
Hadoop Streaming使用unix中的流与程序进行交互，从STDIN输入数据，而输出到SDTOUT。数据必须为每行被视为一个记录。
Hadoop运行使用这些Unix命令作为mapper和reducer，
    
    Hadoop 的类unix写法
    cat [input_file] [mapper] [sort] [reducer] >[output_file]

### 4.5.1 通过Unix命令使用Streaming
cite75_99.txt 中读取一列被引用的专利

    hadoop jar  streaming/hadoop-xx-streaming.jar
        -input  input/cite75_99.txt
        -output output
        -mapper  'cut -f 2 -d ,'   // 第二列数据
        -reducer 'uniq'  // 排序去重
    
    hadoop jar  streaming/hadoop-xx-streaming.jar
         -input  output
         -output    output_a
         -mapper    'wc -l'  //统计每个分片中的记录数
         -D mapred.reduce.tasks=0
         

### 4.6 使用combiner提升性能

         
        
    