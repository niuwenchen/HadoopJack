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
    
### 5.1.3 预处理和后处理阶段的链接
