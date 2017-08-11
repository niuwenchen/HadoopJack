## YARN RPC自己分析
客户端

    IClientNamenodeProtocol proxy= RPC.getProxy(IClientNamenodeProtocol.class, 1, new InetSocketAddress("localhost", 8888), new Configuration());
    public static <T> T getProxy(Class<T> protocol,
                                     long clientVersion,
                                     InetSocketAddress addr, Configuration conf)
         throws IOException {
    
         return getProtocolProxy(protocol, clientVersion, addr, conf).getProxy();
       }
    。。。。
    一直到这里
    
    public static <T> ProtocolProxy<T> getProtocolProxy(Class<T> protocol,
                                    long clientVersion,
                                    InetSocketAddress addr,
                                    UserGroupInformation ticket,
                                    Configuration conf,
                                    SocketFactory factory,
                                    int rpcTimeout,
                                    RetryPolicy connectionRetryPolicy) throws IOException {    
        if (UserGroupInformation.isSecurityEnabled()) {
          SaslRpcServer.init(conf);
        }
        return getProtocolEngine(protocol,conf).getProxy(protocol, clientVersion,
            addr, ticket, conf, factory, rpcTimeout, connectionRetryPolicy);
      }
    返回ProtocolEngine
    
    // // return the RpcEngine configured to handle a protocol
    static synchronized RpcEngine getProtocolEngine(Class<?> protocol,
          Configuration conf) {
        RpcEngine engine = PROTOCOL_ENGINES.get(protocol);
        if (engine == null) {
          Class<?> impl = conf.getClass(ENGINE_PROP+"."+protocol.getName(),
                                        WritableRpcEngine.class);
          engine = (RpcEngine)ReflectionUtils.newInstance(impl, conf);
          PROTOCOL_ENGINES.put(protocol, engine);
        }
        return engine;
      }
    默认是采用WritableRpcEngine.class 实现的
    获取engine之后，engine.getProxy()
    具体的实现有三种
    看ProtocolRpcEngine
    
    public class ProtobufRpcEngine implements RpcEngine 
    
    内部类:
        Invoker
        RpcWrapper
        RpcMessageWithHeader
        RpcRequestWrapper
        RpcRequestMessageWrapper
        RpcResponseWrapper
        RpcResponseWrapper
        Server
        
        public <T> ProtocolProxy<T> getProxy(Class<T> protocol, long clientVersion,
              InetSocketAddress addr, UserGroupInformation ticket, Configuration conf,
              SocketFactory factory, int rpcTimeout, RetryPolicy connectionRetryPolicy
              ) throws IOException {
             // 创建Invoker对象，具体实现处理代码
            final Invoker invoker = new Invoker(protocol, addr, ticket, conf, factory,
                rpcTimeout, connectionRetryPolicy);
            return new ProtocolProxy<T>(protocol, (T) Proxy.newProxyInstance(
                protocol.getClassLoader(), new Class[]{protocol}, invoker), false);
          }
       
        private static class Invoker implements RpcInvocationHandler {
        
        返回 ProtocolProxy的对象， 代理对象。返回客户端的RPC代理对象。
    
    如何返回信息的？
    
    具体的方法调用是在Invoker的invoke中
        public Object invoke(Object proxy, Method method, Object[] args)
            RequestHeaderProto rpcRequestHeader = constructRpcRequestHeader(method);
             Message theRequest = (Message) args[1];
             
             final RpcResponseWrapper val;
                   try {
                     val = (RpcResponseWrapper) client.call(RPC.RpcKind.RPC_PROTOCOL_BUFFER,
                         new RpcRequestWrapper(rpcRequestHeader, theRequest), remoteId);
            
            Client的call方法：
            public Writable call(RPC.RpcKind rpcKind, Writable rpcRequest,
                  ConnectionId remoteId, int serviceClass) throws IOException {
                final Call call = createCall(rpcKind, rpcRequest);
                // connection.setupIOstreams();  才是真正建立连接的过程
                Connection connection = getConnection(remoteId, call, serviceClass); 
                
            connection.sendRpcRequest(call);                 // send the rpc request
            //也是在这里获取并返回结果

Server分析

            RPC.Builder builder =new RPC.Builder(new Configuration());
            builder.setBindAddress("localhost")
                    .setPort(8888).setProtocol(IClientNamenodeProtocol.class)
                    .setInstance(new MyNameNode());
            RPC.Server server=builder.build();
            server.start();
     用RPC提供的Builde构造服务器
      /**
        * Class to construct instances of RPC server with specific options.
        */
       public static class Builder 
       
       build方法
       public Server build() throws IOException, HadoopIllegalArgumentException {
             ...
             return getProtocolEngine(this.protocol, this.conf).getServer(
                 this.protocol, this.instance, this.bindAddress, this.port,
                 this.numHandlers, this.numReaders, this.queueSizePerHandler,
                 this.verbose, this.conf, this.secretManager, this.portRangeConfig);
           }
       
       也有一个获取对应RpcEngine的过程。
       
       @Override
         public RPC.Server getServer(Class<?> protocol, Object protocolImpl,
             String bindAddress, int port, int numHandlers, int numReaders,
             int queueSizePerHandler, boolean verbose, Configuration conf,
             SecretManager<? extends TokenIdentifier> secretManager,
             String portRangeConfig)
             throws IOException {
           return new Server(protocol, protocolImpl, conf, bindAddress, port,
               numHandlers, numReaders, queueSizePerHandler, verbose, secretManager,
               portRangeConfig);
         }
       new Server() 中的代码
       
       // Register  protocol and its impl for rpc calls
          void registerProtocolAndImpl(RpcKind rpcKind, Class<?> protocolClass, 
              Object protocolImpl) 
       
       getProtocolImplMap(rpcKind).put(new ProtoNameVer(protocolName, version),
                new ProtoClassProtoImpl(protocolClass, protocolImpl)); 
       
       这个继承关系:
            org.apache.hadoop.ipc.ProtobufRpcEngine.Server extends  org.apache.hadoop.ipc.RPC.Server  extends org.apache.hadoop.ipc.Server
            这样的分析就和前面的第一代能够对应上了
       
       主要就是Server的构造方式发生变化， Builder来实现。
       
       但这里全部都是Hadoop RPC的实现
       
YARN RPC的变动在哪里？

和前面分析的一样，这里的RPC只是变成了一个工具类，并没有提供具体的实现，全部用那个get方式
getProtocolEngine(),getProxy(),等

真正的提供RPC实现的是Rpcengin吗？RPCEngine只是提供了三个方法

    getProxy
    getServer
    getProxyMetaInfo，真正的实现是其子类
    
    三个:
    WritanleRpcEngine, AvroRpcEngine， ProtocolBufEngine
    前面我们使用的是WritableRpcEngine
    Class<?> impl = conf.getClass(ENGINE_PROP+"."+protocol.getName(),
                                        WritableRpcEngine.class);
          engine = (RpcEngine)ReflectionUtils.newInstance(impl, conf);
    
    
YarnRPC和Hadoop  RPC到底是什么关系？

    YarnRpc是一个抽象类
        getProxy()
        stopProxy()
        getServer()
        create(in conf:Configuration): YarnRPC
    
    Create方法，创建RPC实例
    public static YarnRPC create(Configuration conf) {
        
        String clazzName = conf.get(YarnConfiguration.IPC_RPC_IMPL);
        if (clazzName == null) {
          clazzName = YarnConfiguration.DEFAULT_IPC_RPC_IMPL;
        }
        
          return (YarnRPC) Class.forName(clazzName).newInstance();
            //public static final String DEFAULT_IPC_RPC_IMPL = 
                  "org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC";
      }
      

HadoopYarnProtoRPC通过RPC工厂生成器(工厂设计模式)RpcFactoryProvider生成客户端工厂。而RpcFactoryProvider是主要的实现方式

 
    看一个Demo:
    rpc.getProxy()
         @Override
          public Object getProxy(Class protocol, InetSocketAddress addr,
              Configuration conf) {
            
            return RpcFactoryProvider.getClientFactory(conf).getClient(protocol, 1,
                addr, conf);
          }
          
          public static RpcClientFactory getClientFactory(Configuration conf) {
              String clientFactoryClassName = conf.get(
                  YarnConfiguration.IPC_CLIENT_FACTORY_CLASS,
                  YarnConfiguration.DEFAULT_IPC_CLIENT_FACTORY_CLASS);
              return (RpcClientFactory) getFactoryClassInstance(clientFactoryClassName);
            }
          
          常量: YarnConfiguration.IPC_CLIENT_FACTORY_CLASS   ipc.client.factory.class
          默认是下面的这个实现类
          YarnConfiguration.DEFAULT_IPC_CLIENT_FACTORY_CLASS org.apache.hadoop.yarn.factories.impl.pb.RpcClientFactoryPBImpl
          根据这些提供的工厂和工厂类生成对应的实例对象。
          
          会生成哪些对象？
          RPCServerFactory接口: getServer
          RpcServerFactoryPBImpl  org.apache.hadoop.yarn.factories.impl.pb.RpcClientFactoryPBImpl
          RpcClientFactoryPBImpl  org.apache.hadoop.yarn.factories.impl.pb.RpcClientFactoryPBImpl
          RpcClientFacotry
          
    先看Client
    public class RpcClientFactoryPBImpl implements RpcClientFactory
    RpcClientFactoryPBImpl根据通信协议接口(实际上就是一个Java Interface)和Protobuf Buffers定义构造RPC客户端句柄,但是对通信
    协议的存放位置和类命名有一定的要求。假设通信协议接口XXX 所在Java 包为XXXPackage，则客户端实现代码必须位于Java包
    XxxPackage.impl.pb.client中，且实现类名为PBClientImplXxx
    
    
    对Protobuf的实现类有一个类别的封装
    private String getPBImplClassName(Class<?> clazz) {
        String srcPackagePart = getPackageName(clazz);
        String srcClassName = getClassName(clazz);
        String destPackagePart = srcPackagePart + "." + PB_IMPL_PACKAGE_SUFFIX;  //impl.pb.client
        String destClassPart = srcClassName + PB_IMPL_CLASS_SUFFIX;       //PBClientImpl
        return destPackagePart + "." + destClassPart;
      }
    
    获得代理对象的proxy
    public Object getClient(Class<?> protocol, long clientVersion,
          InetSocketAddress addr, Configuration conf) {
       
        Constructor<?> constructor = cache.get(protocol);
        
            pbClazz = localConf.getClassByName(getPBImplClassName(protocol));
          
          try {
            constructor = pbClazz.getConstructor(Long.TYPE, InetSocketAddress.class, Configuration.class);
            constructor.setAccessible(true);
            ....
           Object retObject = constructor.newInstance(clientVersion, addr, conf);
           return retObject;
                 
    Server和上面的一样。但是一个很重要的一点就和前面结合起来了
    
    private Server createServer(Class<?> pbProtocol, InetSocketAddress addr, Configuration conf, 
          SecretManager<? extends TokenIdentifier> secretManager, int numHandlers, 
          BlockingService blockingService, String portRangeConfig) throws IOException {
        RPC.setProtocolEngine(conf, pbProtocol, ProtobufRpcEngine.class);
        RPC.Server server = new RPC.Builder(conf).setProtocol(pbProtocol)
            .setInstance(blockingService).setBindAddress(addr.getHostName())
            .setPort(addr.getPort()).setNumHandlers(numHandlers).setVerbose(false)
            .setSecretManager(secretManager).setPortRangeConfig(portRangeConfig)
            .build();
        LOG.info("Adding protocol "+pbProtocol.getCanonicalName()+" to the server");
        server.addProtocol(RPC.RpcKind.RPC_PROTOCOL_BUFFER, pbProtocol, blockingService);
        return server;
      }
      
        (1) 设置ProtocolBuf为自己的RpcEngine
            RPC.setProtocolEngine(conf, pbProtocol, ProtobufRpcEngine.class);
        (2) 建立Server
            RPC.Server.Builder()方法就和前面使用的一样了。
            
         自己建立的时候实现需要在impl.pb.service 名字加 ProtocolPBServiceImpl
         启动失败  org.apache.hadoop.yarn.exceptions.YarnRuntimeException: Could not find constructor with params: long, class java.net.InetSocketAddress, class org.apache.hadoop.conf.Configuration

             Server server=rpc.getServer(IClientNamenodeProtocol.class, PBServiceImplIClientNamenodeProtocol.class, new InetSocketAddress(7777), new Configuration(), null,100)
    
        (3) YARN RPC已经将Protobuf BUffers作为默认的序列化机制，不是Writable

### YARN RPC应用实例
    
    (1) ResourceTrackerPB
            public interface ResourceTrackerPB extends ResourceTrackerService.BlockingInterface {

    NodeManager 中的main方法以及执行过程
        private void initAndStartNodeManager(Configuration conf, boolean hasToReboot
                        nodeManagerShutdownHook = new CompositeServiceShutdownHook(this);
                        ShutdownHookManager.get().addShutdownHook(nodeManagerShutdownHook, 30);
                        this.init(conf);
                        this.start();
        
        init函数
            
    
        
