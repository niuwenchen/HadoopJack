## YARN RPC实现
和Hadoop RPC一样，均由两部分组成： 对象序列化和远程过程调用(protocol 提供了序列化实现，并未提供远程调用相关实现)。

* 跨语言特性
    由于Hadoop采用Java编写，因而其RPC客户端和服务器仅支持Java语言；但对于更通用的RPC框架，如Thrift或者Protocol等
    其客户端和服务端均可采用任何语言编写，Java,C++,python 等

* 引入IDL
    开源RPC框架均提供了一套接口描述语言(Interface Description Language,IDL)，提供一套通用的
    数据类型，并以这些数据类型类定义更为复杂的数据类型和对外服务接口。一旦用户按照IDL定义的语法
    编写完接口文档之后，可根据实际应用需要生成特定编程语言的客户端和服务端代码。

YARN RPC
    
    Hadoop YARN将RPC中的序列化部分剥离开，以便现有的开源RPC框架集成进来，RPC类变成了一个工厂，将具体的RPC实现
    授权给RpcEngine实现类，而现有的开源RPC只要实现RpcEngine接口，可以集成到Hadoop RPC中。
    
Hadoop 2 RPC  主要的实现是RPCEngine接口。

    内部类:
    RPCKind: RPC 数据类型
    RpcInvoker
    VersionMismatch
    Builder
    Server
    
    RPCKind
    public enum RpcKind {
        RPC_BUILTIN ((short) 1),         // Used for built in calls by tests
        RPC_WRITABLE ((short) 2),        // Use WritableRpcEngine 
        RPC_PROTOCOL_BUFFER ((short) 3); // Use ProtobufRpcEngine
        final static short MAX_INDEX = RPC_PROTOCOL_BUFFER.value; // used for array size
        public final short value; //TODO make it private
    
        RpcKind(short val) {
          this.value = val;
        } 
      }
      
    Yarn内部的全程调用都使用了RPC_PROTOCOL_BUFFER
    
    Hadoop中，每种RPC调用的处理类都有一个Engine。基于RPC_PROTOCOL_BUFFER的RPC的engine叫做ProtobufPrcEngine，即ProtobufRpcEngine是专门
    处理基于google的protobuf消息格式的RPC协议。根据Hadoop的协议管理类ipc.Server的实际规定，所有的Engine在工作开始前，都必须向ipc.Server
    注册自己，
    
    public class ProtobufRpcEngine implements RpcEngine {
      public static final Log LOG = LogFactory.getLog(ProtobufRpcEngine.class);
      
      static { // Register the rpcRequest deserializer for WritableRpcEngine 
        org.apache.hadoop.ipc.Server.registerProtocolEngine(
            RPC.RpcKind.RPC_PROTOCOL_BUFFER, RpcRequestWrapper.class,
            new Server.ProtoBufRpcInvoker());
      }
    注册的目的，是将自己纳入中央处理器ipc.Server 的调度管理之下，这样，在收到某个消息，就可以根据消息的RpcKind就可以
    知道这个消息的数据格式，因此ipc.Server会将消息直接交给对应的已经注册成为该RpcKind的engine进行处理
    
    public static void registerProtocolEngine(RPC.RpcKind rpcKind, 
              Class<? extends Writable> rpcRequestWrapperClass,
              RpcInvoker rpcInvoker) {
        RpcKindMapValue  old = 
            rpcKindMap.put(rpcKind, new RpcKindMapValue(rpcRequestWrapperClass, rpcInvoker));
        if (old != null) {
          rpcKindMap.put(rpcKind, old);
          throw new IllegalArgumentException("ReRegistration of rpcKind: " +
              rpcKind);      
        }
        LOG.debug("rpcKind=" + rpcKind + 
            ", rpcRequestWrapperClass=" + rpcRequestWrapperClass + 
            ", rpcInvoker=" + rpcInvoker);
      }
    注册的过程就是将每一个Engine的Invoker和RpcKind对应起来，Invoker就是真正处理这个请求的处理者，每一个engine都必须包含
    这样一个Invoker，这样ipc.Server拿到请求的RPCkind，实际上去交给对应Engine的Invker进行处理。
    那Engine拿到了请求以后，是如何知道这个请求具体该怎么去运行和处理呢？这就涉及到不同的protobuf协议定义。
    
    每一个协议，在服务端都有一个Service与之对应，这个service在启动的时候，会向ipc.Service 注册自己，这个注册是指某个具体的
    protobuf协议的注册，要与上面的engine注册区分开。 
    

YarnRpc
   
public abstract class YarnRPC  抽象类

    public static YarnRPC create(Configuration conf) {
            LOG.debug("Creating YarnRPC for " + conf.get("yarn.ipc.rpc.class"));
            String clazzName = conf.get("yarn.ipc.rpc.class");
            if(clazzName == null) {
                clazzName = "org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC";
            }
    
            try {
                return (YarnRPC)Class.forName(clazzName).newInstance();
            } catch (Exception var3) {
                throw new YarnRuntimeException(var3);
            }
        }
    默认实现是 "org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC"; 
    public class HadoopYarnProtoRPC extends YarnRPC {
        // getProxy 获取客户端的代理对象
        public Object getProxy(Class protocol, InetSocketAddress addr, Configuration conf) {
                LOG.debug("Creating a HadoopYarnProtoRpc proxy for protocol " + protocol);
                return RpcFactoryProvider.getClientFactory(conf).getClient(protocol, 1L, addr, conf);
            }
            // 
            public void stopProxy(Object proxy, Configuration conf) {
                RpcFactoryProvider.getClientFactory(conf).stopClient(proxy);
            }
            // 获取服务器的实现对性爱那个
            public Server getServer(Class protocol, Object instance, InetSocketAddress addr, Configuration conf, SecretManager<? extends TokenIdentifier> secretManager, int numHandlers, String portRangeConfig) {
                LOG.debug("Creating a HadoopYarnProtoRpc server for protocol " + protocol + " with " + numHandlers + " handlers");
                return RpcFactoryProvider.getServerFactory(conf).getServer(protocol, instance, addr, conf, secretManager, numHandlers, portRangeConfig);
            }
        
    
        RPC工厂模式下的RpcFactoryProvider 
        class RpcFactoryProvider
        
            public static RpcServerFactory getServerFactory(Configuration conf) {
                  String serverFactoryClassName = conf.get("yarn.ipc.server.factory.class", "org.apache.hadoop.yarn.factories.impl.pb.RpcServerFactoryPBImpl");
                  return (RpcServerFactory)getFactoryClassInstance(serverFactoryClassName);
            
            public static RpcClientFactory getClientFactory(Configuration conf) {
                    String clientFactoryClassName = conf.get("yarn.ipc.client.factory.class", "org.apache.hadoop.yarn.factories.impl.pb.RpcClientFactoryPBImpl");
                    return (RpcClientFactory)getFactoryClassInstance(clientFactoryClassName);
                }
            
            getFactoryInstance(classname)
                private static Object getFactoryClassInstance(String factoryClassName) {
                        try {
                            Class e = Class.forName(factoryClassName);
                            Method method = e.getMethod("get", (Class[])null);
                            method.setAccessible(true);
                            // 调用的是空对象，和空方法
                            return method.invoke((Object)null, (Object[])null);
        
        
        RpcServerFactoryPBImpl 和 RpcClientFactoryPBImpl
        
        RpcClientFactoryPBImpl: 根据通信协议接口（实际上就是一个Javainterrface）及Protocol Buffers定义构造RPC客户端句柄，但它对
        通信协议的存放位置和类命名有一定要求。
        public Object getClient(Class<?> protocol, long clientVersion, InetSocketAddress addr, Configuration conf) {
                Constructor constructor = (Constructor)this.cache.get(protocol);
                
                try {
                            Object e1 = constructor.newInstance(new Object[]{Long.valueOf(clientVersion), addr, conf});
                            return e1;
                .....
               }
   
   
一个YarnRPC的应用实例

    public interface ResourceTracker {
        @Idempotent
        RegisterNodeManagerResponse registerNodeManager(RegisterNodeManagerRequest var1) throws YarnException, IOException;
    
        @AtMostOnce
        NodeHeartbeatResponse nodeHeartbeat(NodeHeartbeatRequest var1) throws YarnException, IOException;
    }
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    