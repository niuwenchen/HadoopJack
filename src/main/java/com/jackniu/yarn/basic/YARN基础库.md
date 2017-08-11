## YARN基础库
### 3.1 概述
YARN基础库是其他一起模块的基础，他的设计直接决定了YARN的稳定性和拓展性，概括起来，基础库主要有以下几个。

    Protocol Buffers: YARN将Protocol Buffers用到了RPC通信中，默认情况下，YARN RPC中所有参数采用Protocol Buffers
                    进行序列化和反序列化，相比于MRv1中基于自定义Writable框架的方式，YARN在向后兼容性、扩展性等方面提高了很多
                    
    Apache avro: avro是Hadoop生态系统中RPC框架，具有平台无关性、支持动态模式(无需编译)等有点，Avro的最初设计动机是解决
                YARN RPC兼容性和拓展性差等问题，目前，YARN采用Avro记录MapReduce应用程序日志(用于故障后应用程序恢复)，今后可能代替
                Protocol Buffers作为RPC辅助库(至少会作为一个可选方案)
    
    RPC库: YARN采用MRV1中的RPC库，但其中采用的默认序列化方法被替换成了Protocol Buffers。
    服务库和事务库: YARN将所有的对象服务化，以便统一管理(比如创建、销毁等)。而服务之间则采用事件机制进行通信。
        不再使用类似MRv1中基于函数调用的方式
    状态机库: 在YARN中，很多对象都是由若干个状态组成的，且当有事件发生时，状态之间会发生转移，比如作业、任务、Container等，
            而YARN正是采用有限状态之间的转移。引入状态机模型后，相比MRv1，YARN的代码结构更加清晰了。
    
    
Protobuf Buffers:在YARN中，所有RPC函数的参数均采用Protobuf Buffers定义的，相比Mrv1中基于Writable序列化的方法，
Protobuf Buffers的引入使得YARN在向后兼容性和性能方面均向前迈进了一大步。

    除序列化/反序列化之外，Protobuf Buffers也提供了RPC函数的定义方法，但并未给出具体实现，这需要用户自行实现，
    而YARN则采用额MRV1中的RPC库。
    
    service ContainerManagerService{    // 这里是YARN自带的containerManager协议的定义
        rpc startContainer(StartContainerRequestProto) returns (StartContainerResponseProto);
        ....
    }
    RPC协议，这些协议全是使用Protocol Buffers定义的
    applicationmaster_protocol.proto: 定义了AM和RM之间的协议——Application-MasterProtocol
    applicationclient_protocol.proto: 定义了JobClient和RM之间的协议——ApplicationClientProtocol
    containermanagement_protocol.ptoto:  定义了AM和NM之间的协议——ContainerManagementProtocol
    resourcemanager_administration_protocol.proto: 定义了AM与NM之间的协议——Container-ManagementProtocol
    。。。。。
   
Apache Avro: 序列化框架，同时也实现了RPC的功能。


### 3.3 底层通行库
Stub程序: 客户端和服务器端均包含stub程序，可将之看作代理程序。使得远程函数调用表现的跟本地调用一眼，对用户程序完全透明。
在客户端，表现的像一个本地程序，但不直接执行本地调用，而是将请求信息通过网络模块发送给服务端。此外，当服务器
发送应答后，会解码对应结果。在服务器端，Stub程序一次解码请求消息中的参数，调用相应的服务过程和编码应答结果的
返回值等处理。

    stub程序封装所有请求信息，stub程序解码所有请求信息。
    RPC总体架构:
    * 序列化层: 序列化的主要作用是将结构化对象转换为字节流以便于通过网络进行传输或写入持久化存储，在RPC框架中，主要用于将
    用户请求中的参数或者应答转换成字节流以便跨机器传输。前面介绍的Protocol Buffers和Apache avro均可用在序列化层，Hadoop
    本身也提供了一套序列化框架，一个类只要实现Writable接口即可支持对象序列化与反序列化。
    
    找到这个类，并找到client将它发送到stub的过程
    
介绍以Writable序列化为例(Protocol Buffers的过程类似)，介绍HadoopRPC的远程调用实现方案: Hadoop RPC使用了Java动态代理完成对远程方法的调用：
用户只需要实现java.lang.reflect.InvocationHandler接口，并按照自己需求实现invoke方法即可完成动态代理对象上的方法调用。
但对于Hadoop RPC，函数调用由客户端发出，在服务端执行并返回，因此不能像单机程序那样直接在invoke方法中本地调用相关
函数，在invoke方法中，将函数调用信息(函数名，函数参数列表)等打包成可序列化的WritableRpcEngine.Invocation对象，
并通过网络发送给服务器端，服务器端接受到信息，利用Java发射机制王城函数调用，到底是怎么实现的？？？？？

    Invocation： 封装方法和参数，作为数据传输层
    ClientCache: 存储Client对象
    VersionMissMatch
    Server: Server的实现类
    
    RPC.Invoker: 动态代理的调用实现类，继承了InvocationHandler
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ...
  
                ObjectWritable value = (ObjectWritable)this.client.call(new RPC.Invocation(method, args), this.remoteId);
                ...
    
                return value.get();
            }
     一般的invoke中是这样的
     
         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("---正在执行的方法:"+method);
                method.invoke(proxy,args);
         会有method.invoke()方法，来执行调用
    
    为什么没有，因为执行method.invoke() 方法是在本地JVM中调用；而在Hadoop中，是将数据发送给服务端，服务端将处理的结果再返回给客户端，所以这里的
    invoke方法必将需要进行网络通信。而网络通信是下面这段代码实现:
         ObjectWritable value = (ObjectWritable)this.client.call(new RPC.Invocation(method, args), this.remoteId);
    Invocation类在这里封装了方法名和参数，充当vo。Client的call方法
    Invocation在这里的实现是继承了Writable类，就可以作为对象进行传输，而Protobuf 也可以在这里实现传输
        
        private static class Invocation implements Writable, Configurable； 一个可进行传输的对象
    
    Client中实现的东西到底有什么?
        1. 客户端和服务端的连接是怎样建立的
        2. 客户端怎样给服务端发送数据的
        3. 客户端是怎样获取服务端的返回数据的？
    
    ipc.Client分析
    内部类
        ConnectionId: 唯一确定一个联结
        ParallelResults
        ParallelCall
        Connection: 用以处理远程连接对象，继承了Thread
        Call  用于封装Invocation对象，作为VO，写到服务端，同时也用于存储从服务端返回的数据
        
        (1) 客户端和服务器的连接是怎么建立的
        Client.call 方法
        
        public Writable call(Writable param, Client.ConnectionId remoteId) throws InterruptedException, IOException {
                Client.Call call = new Client.Call(param);  // 封装进去
                Client.Connection connection = this.getConnection(remoteId, call); // 获取远程连接
                connection.sendParam(call);   //发送数据
                boolean interrupted = false;
                synchronized(call) {  
                    while(!call.done) {
                        try {
                            call.wait();//等待结果的返回，在Call类的callComplete()方法里有notify()方法用于唤醒线程
                        } catch (InterruptedException var9) {
                            interrupted = true;
                        }
                    }
        
                    if(interrupted) {
                        Thread.currentThread().interrupt();
                    }
        
                    if(call.error != null) {
                        if(call.error instanceof RemoteException) {
                            call.error.fillInStackTrace();
                            throw call.error;
                        } else {
                            throw this.wrapException(connection.getRemoteAddress(), call.error);
                        }
                    } else {
                        return call.value;
                    }
                }
            }
            
    RPC的网络通信就是这两句
        Client.Connection connection = this.getConnection(remoteId, call); // 获取远程连接
                        connection.sendParam(call);   //发送数据
                        
        
        private Client.Connection getConnection(Client.ConnectionId remoteId, Client.Call call) throws IOException, InterruptedException {
                if(!this.running.get()) {
                    throw new IOException("The client is stopped");
                } else {
                    Client.Connection connection;
                    // 如果connections连接池中有对应的连接对象，就不需要重新创建了；如果没有就需要重新创建一个connections对象
                    // 该连接对象只是存储了remoteId的信息，其实还并没有和服务端建立连接。
                    do {
                        Hashtable var4 = this.connections;
                        synchronized(this.connections) {
                            connection = (Client.Connection)this.connections.get(remoteId);
                            if(connection == null) {
                                connection = new Client.Connection(remoteId);
                                this.connections.put(remoteId, connection);
                            }
                        }
                    } while(!connection.addCall(call));
                    
                    //下面这一行代码才是真正建立连接的过程
                    connection.setupIOstreams();
                    return connection;
                }
            }
         
        
                        while(true) {
                                this.setupConnection();  //建立连接
                                inStream = NetUtils.getInputStream(this.socket);   //获取输入流
                                outStream = NetUtils.getOutputStream(this.socket); // 获取输出流
                                this.writeRpcHeader(outStream);
                                ...
                            
                            //将输入输出流包装成DataInputStream/OutputStream
                            this.in = new DataInputStream(new BufferedInputStream(new Client.Connection.PingInputStream(inStream)));
                            this.out = new DataOutputStream(new BufferedOutputStream(outStream));
                            this.writeHeader();
                            // 更新活动时间
                            this.touch();
                            // 启动线程
                            this.start();
            看如何建立连接的
                private synchronized void setupConnection() throws IOException {
                            short ioFailures = 0;
                            short timeoutFailures = 0;
                
                            while(true) {
                                try {
                                    // 客户端连接就是创建一个普通的socket连接。
                                    this.socket = Client.this.socketFactory.createSocket();
                                    this.socket.setTcpNoDelay(this.tcpNoDelay);
                                    。。。
                                    NetUtils.connect(this.socket, this.server, 20000);
                                    this.socket.setSoTimeout(this.pingInterval);
                                catch (SocketTimeoutException var6) {
                                                    if(this.updateAddress()) {
                                                        ioFailures = 0;
                                                        timeoutFailures = 0;
                                                    }
                                    // 失败重连
                                     this.handleConnectionFailure(timeoutFailures++, 45, var6);
        
        
        （2） 如何发送数据到服务器的？
                conection.sendParam(call)
                public void sendParam(Client.Call call) {
                    ...
                    // 创建一个缓存区
                    d = new DataOutputBuffer();
                    d.writeInt(call.id);
                    // 下面这个方法是反调吗？就是将这个param对象输出到这个缓冲区
                    call.param.write(d);
                    byte[] data = d.getData();
                    int dataLength = d.getLength();
                    this.out.writeInt(dataLength);
                    this.out.write(data, 0, dataLength);
                    this.out.flush();
                    
         (3) 客户端怎样获取服务端的数据的？
            当建立连接并启动这个线程，会运行一个方法
             public void run() {
                        if(Client.LOG.isDebugEnabled()) {
                            Client.LOG.debug(this.getName() + ": starting, having connections " + Client.this.connections.size());
                        }
            
                        while(this.waitForWork()) {
                            this.receiveResponse();
                        }
            
                        this.close();
                        if(Client.LOG.isDebugEnabled()) {
                            Client.LOG.debug(this.getName() + ": stopped, remaining connections " + Client.this.connections.size());
                        }
            
             }
             
            private void receiveResponse() {
                        if(!this.shouldCloseConnection.get()) {
                            this.touch();
            
                            try {
                                 // 阻塞读取id  
                                int e = this.in.readInt();
                               ...
                                // /在calls池中找到发送时的那个对象  
                                Client.Call call = (Client.Call)this.calls.get(Integer.valueOf(e));
                                // 阻塞读取call对象的状态  
                                int state = this.in.readInt();
                                if(state == Status.SUCCESS.state) {
                                    Writable value = (Writable)ReflectionUtils.newInstance(Client.this.valueClass, Client.this.conf);
                                    // 读取数据，将读取的对象赋值给call对象，并将唤醒Client等待线程，客户端就拿到了返回的数据
                                    value.readFields(this.in);
                                    call.setValue(value);
                                    this.calls.remove(Integer.valueOf(e));
                                } else if(state == Status.ERROR.state) {
                                    call.setException(new RemoteException(WritableUtils.readString(this.in), WritableUtils.readString(this.in)));
                                    this.calls.remove(Integer.valueOf(e));
                                } else if(state == Status.FATAL.state) {
                                    this.markClosed(new RemoteException(WritableUtils.readString(this.in), WritableUtils.readString(this.in)));
                                }
                            } catch (IOException var5) {
                                this.markClosed(var5);
                            }
            
                        }
                    }
            

ipc.Server 分析
    
    Call: 用于存储客户端发来的请求
    Responser:响应RPC请求类，请求处理完毕，由Responser发送给请求客户端
    Connection: 连接类，真正的客户端请求读取逻辑在这个类中
    Handler: 请求处理类，会循环阻塞callQueue中的call对象，并对其进行操作。
    
    ipc.Server 是一个抽象类，具体的实例化过程
    this.serviceRpcServer = RPC.getServer(this,dnSocketAddr.getHostName(),dnSocketAddr.getPort(),serviceHandleCount,false,conf,namesystem.getDelegationTokenSecretManager())
    this.server = RPC.getServer(......)
    this.server.start()
    通过RPC的server对象获得的。

Server内部类

    public Server(Object instance, Configuration conf, String bindAddress, int port, int numHandlers, boolean verbose, SecretManager<? extends TokenIdentifier> secretManager) throws IOException {
                super(bindAddress, port, RPC.Invocation.class, numHandlers, conf, classNameBase(instance.getClass().getName()), secretManager);
                this.instance = instance;
                this.verbose = verbose;
            }
    instance就是调用这个getServer的实例类
        RPC.Server server =RPC.getServer(new ClientProtocolImpl(),"127.0.0.1",2181,5,false,new Configuration());
    
    start方法，分别启动responser，listener，handlers
        public synchronized void start() {
                this.responder.start();
                this.listener.start();
                this.handlers = new Server.Handler[this.handlerCount];
        
                for(int i = 0; i < this.handlerCount; ++i) {
                    this.handlers[i] = new Server.Handler(i);
                    this.handlers[i].start(); 
                }
        
            }
            
    
    分析源码得知，Server端采用Listener监听客户端的连接，下面先分析一下Listener的构造函数吧：
    Listener 已经启动
            public Listener() throws IOException {
                this.backlogLength = Server.this.conf.getInt("ipc.server.listen.queue.size", 128);
                this.address = new InetSocketAddress(Server.this.bindAddress, Server.this.port);
                this.acceptChannel = ServerSocketChannel.open();
                this.acceptChannel.configureBlocking(false);
                Server.bind(this.acceptChannel.socket(), this.address, this.backlogLength);
                Server.this.port = this.acceptChannel.socket().getLocalPort();
                this.selector = Selector.open();
                this.readers = new Server.Listener.Reader[Server.this.readThreads];
                this.readPool = Executors.newFixedThreadPool(Server.this.readThreads);
                
                //启动多个reader线程，为了防止请求多时服务端响应延时的问题 
                for(int i = 0; i < Server.this.readThreads; ++i) {
                    Selector readSelector = Selector.open();
                    Server.Listener.Reader reader = new Server.Listener.Reader(readSelector);
                    this.readers[i] = reader;
                    this.readPool.execute(reader);
                }
                 // 注册连接事件  
                this.acceptChannel.register(this.selector, 16);
                this.setName("IPC Server listener on " + Server.this.port);
                this.setDaemon(true);
            }
            
            会一直等待客户端的连接
            public void run() {
                       ...
                        for(; Server.this.running; this.cleanupConnections(false)) {
                            SelectionKey key = null;
            
                            try {
                                this.selector.select();
            
                                for(Iterator e = this.selector.selectedKeys().iterator(); e.hasNext(); key = null) {
                                    key = (SelectionKey)e.next();
                                    e.remove();
            
                                    try {
                                        if(key.isValid() && key.isAcceptable()) {
                                            this.doAccept(key);
                                        }
                                    } catch (IOException var7) {
                                        ;
                                    }
                                }
                            } 
                        ...
                        }
                    }
            doAccept方法
            void doAccept(SelectionKey key) throws IOException, OutOfMemoryError {
                        Server.Connection c = null;
                        ServerSocketChannel server = (ServerSocketChannel)key.channel();
            
                        SocketChannel channel;
                        while((channel = server.accept()) != null) {
                            channel.configureBlocking(false);
                            channel.socket().setTcpNoDelay(Server.this.tcpNoDelay);
                            Server.Listener.Reader reader = this.getReader();
            
                            try {
                                reader.startAdd();
                                SelectionKey readKey = reader.registerChannel(channel);
                                //创建一个连接对象  
                                c = Server.this.new Connection(readKey, channel, System.currentTimeMillis());
                                //将connection对象注入readKey  
                                readKey.attach(c);
                                synchronized(Server.this.connectionList) {
                                    Server.this.connectionList.add(Server.this.numConnections, c);
                                    Server.this.numConnections++;
                                }
            
                                if(Server.LOG.isDebugEnabled()) {
                                    Server.LOG.debug("Server connection from " + c.toString() + "; # active connections: " + Server.this.numConnections + "; # queued calls: " + Server.this.callQueue.size());
                                }
                            } finally {
                                reader.finishAdd();
                            }
                        }
            
                    }

    
    
            Reader对象的run方法
                Listener.this.doRead(key);  
                
                void doRead(SelectionKey key) throws InterruptedException {
                            byte count = 0;
                            Server.Connection c = (Server.Connection)key.attachment();
                            if(c != null) {
                                c.setLastContact(System.currentTimeMillis());
                
                                int count1;
                                try {
                                    count1 = c.readAndProcess();
                                。。。
                
                        public int readAndProcess() throws IOException, InterruptedException
                                        boolean isHeaderRead = this.headerRead;
                                        if(this.useSasl) {
                                            this.saslReadAndProcess(this.data.array());
                                        } else {
                                            this.processOneRpc(this.data.array());
                                        }
                                        
            下面贴出Server.Connection类中的processOneRpc()方法和processData()方法的源码。
                private void processOneRpc(byte[] buf) throws IOException, InterruptedException {
                            if(this.headerRead) {
                                this.processData(buf);
                            } else {
                                this.processHeader(buf);
                                this.headerRead = true;
                                if(!this.authorizeConnection()) {
                                    throw new AccessControlException("Connection from " + this + " for protocol " + this.header.getProtocol() + " is unauthorized for user " + this.user);
                                }
                            }
                
                        }
                private void processData(byte[] buf) throws IOException, InterruptedException {
                            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
                            int id = dis.readInt();
                            if(Server.LOG.isDebugEnabled()) {
                                Server.LOG.debug(" got #" + id);
                            }
                
                            Writable param = (Writable)ReflectionUtils.newInstance(Server.this.paramClass, Server.this.conf);
                            param.readFields(dis);
                            Server.Call call = new Server.Call(id, param, this);
                            Server.this.callQueue.put(call);
                            this.incRpcCount();
                        }
                        
     存入了CallQueue中
     Handle的run方法处理call对象
     
                        public void run() {
                        ...
                            if(e.connection.user == null) {
                                                        value = Server.this.call(e.connection.protocol, e.param, e.timestamp);
                                                    } else {
                                                        value = (Writable)e.connection.user.doAs(new PrivilegedExceptionAction() {
                                                            public Writable run() throws Exception {
                                                                return Server.this.call(e.connection.protocol, e.param, e.timestamp);
                                                            }
                                                        });
                                                        
     
     responsder对象
     void doRespond(Server.Call call) throws IOException {
                 synchronized(call.connection.responseQueue) {
                     call.connection.responseQueue.addLast(call);
                     if(call.connection.responseQueue.size() == 1) {
                        // 返回响应结果，并激活writeSelector
                         this.processResponse(call.connection.responseQueue, true);
                     }
     
                 }
             }
            
                   
    
    
    
    
    
    
    
    
    
    
    
    
    
    

