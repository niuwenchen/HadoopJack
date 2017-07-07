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

### 通过脚本使用Streaming
注意： 写一个python程序，

    import sys,random
        if (random.randint(1,100) <= int(sys.argv[1])):
 
警告： 数据采样所带来的精度损失重要是否取决于要计算的是什么数据以及数据集如何分布。例如，通过一个采样数据集
来计算平均值通常是有效的，但是如果数据偏斜的很严重，会使平均值取决于少数几个值，这样采样就会带来问题。
类似的，如果仅仅为了得到数据的概貌，对采样数据做聚类通常是有效的。但是如果希望找到一个较小且不规则的聚类，
采样会使得它们被排除在外。另外，最大值和最小值等功能也不适合对数据进行采样。

Streaming可以运行Unix命令和Python程序，但是unix命令已经可以在集群上所有节点运行，而python不行。
Streaming支持-file将python程序提交为作业的一部分。

    bin/hadoop jar  hadoop-xx-streaming.jar
        -input   input/cite75_99.txt
        -output   output
        -mapper     'RandomSample.py 10'
        -file   RandomSample.py
        -D  mapred.reduce.tasks=1  默认使用IdentityReducer，直接将输入转入输出
        
     hadoop jar /opt/hadoop/share/hadoop/tools/lib/hadoop-streaming-2.6.0.jar  -input /jackniu/cite75_99.txt -output /jackniu/output -mapper 'python RandomSample.py 10'  -file ./RandomSample.py
     
    将file提交上去作为mapper程序。那有没有将file提交上去作为reducer程序？
 Streaming模式和标准Java模式，Streaming中每个mapper都会看到完整的数据流，mapper处理数据。标准Java模式，是由框架自身将输入数据分割为记录。
 Streaming中可以维护一个分片的状态信息，则可以多这个分片的总信息进行一个统计。
 
 http://blog.csdn.net/kirayuan/article/details/8651603 hadoop的mapper数目不是能控制的

注意脚本文件的具体内容：

    import sys
    
    index= int(sys.argv[1])
    max=0
    for line in sys.stdin:
        fileds= line.strip().split(",")
        if fileds[index].isdigit():
            val = int(fileds[index])
            if (val > max):
                max = val
    
    print(max)
    这里的print就是传递到reduce的值，和java中的collect的作用是一样的。
    
### 通过Aggregete包使用Streaming
Hadoop通过提供一个Aggregate的软件包，让数据集的汇总更加简单。尤其在做Streaming时，可以简化Java的编写
只需提供一个mapper来记录并以特定格式输出。function:key\tvalue

    import sys
    index = int(sys.argv[1])
    
    for line in sys.stdin:
        fields = line.split(",")
        print("LongValueSum:"+fields[index]+"\t"+"1")
    
    
    '''
    hadoop jar /opt/hadoop/share/hadoop/tools/lib/hadoop-streaming-2.6.0.jar  -input /jackniu/apat63_99.txt -output /jackniu/output -mapper 'python AttributeCount.py 1' -reducer aggregate  -file AttributeCount.py
    
    '''
hadoop 的mapper构建function:word\t1, reducer用 aggreagte标识，去解析functionname，按照解析后的函数去执行reduce，按键求和的function

    UniqValueCount: 求单一值的个数
    ValueHistogram: 求每个值的个数，最小值个数，中值个数，最大个数，平均个数，标准方差
    
主要是-reducer  aggregate  mapper输出: functionname: key\tvalue
### 4.6 使用combiner提升性能
AverageByAttributeMapper.py: 读取每个记录，并用键值对输出该记录的属性与计数，网络上洗牌键值对后，传给reducer
AverageByAttributeReducer.py: 计算每个键的平均值

效率瓶颈
*   如果10亿条输入，mapper就会生成10亿个键值对在网络上洗牌，但是我们可以在mapper阶段进行一个设计，使洗牌数目减小
*   数据倾斜，大多数的键值对会进入单一的reducer中，负载不均衡。

combiner作为erducer的助手，致力于减少mapper的输出以及降低网络和reducer压力。如果我们指定了一个combiner，在数据格式上必须与reducer等价，
去掉combiner，reducer的输出也应该保持不变。

在上述描述中，加一个combiner，用于mapper的求和。

         
        
    