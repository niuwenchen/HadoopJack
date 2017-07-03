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
    
    