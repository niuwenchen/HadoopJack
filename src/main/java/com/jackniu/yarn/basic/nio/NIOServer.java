package com.jackniu.yarn.basic.nio;

import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by JackNiu on 2017/7/31.
 */
public class NIOServer {
    //通道管理器
    private Selector selector;

    /*获取一个ServerSocket通道，并对该通道做一些初始化的工作
    * */
    public void initServer(int port)  throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        // 将该通道对应的ServerSocket绑定到Port端口
        serverChannel.socket().bind(new InetSocketAddress(port));
        // 获得一个通道管理器
        this.selector = Selector.open();
        // 将通道管理器和该通都绑定，并为该通道注册SelectionKey.OP_ACCEPT事件，注册事件后，
        // 当该事件到达时，selector.select()会返回，如果该事件没到达，则selector.select会一直阻塞
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * 采用轮询的方式监听selector上是否有需要处理的事件，如果有，则进行处理
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void listen() throws Exception{
        System.out.println("服务端启动成功");
        while(true){
            // 当注册的事件到达时，方法返回；否则，该方法会一直阻塞
            selector.select();
            Iterator iterator = this.selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey key = (SelectionKey) iterator.next();
                iterator.remove();
                // 客户端请求连接事件
                if(key.isAcceptable()){
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    // 给客户端发送消息
                    channel.write(ByteBuffer.wrap("向客户端发送了一条信息".getBytes()));
                    // 在和客户端联结成功后，为了可以接收到客户端的信息，需要给通道设置读的权限
                    channel.register(this.selector,SelectionKey.OP_READ);
                    // 获得了可读的事件
                }else if (key.isReadable()){
                    read(key);
                }
            }
        }
    }
    /**
     * 处理读取客户端发来的信息 的事件
     * @param key
     * @throws IOException
     */
    public void read(SelectionKey key) throws IOException{
        // 服务器可读取消息:得到事件发生的Socket通道
        SocketChannel channel = (SocketChannel) key.channel();
        // 创建读取的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(10);
        channel.read(buffer);
        byte[] data = buffer.array();
        String msg = new String(data).trim();
        System.out.println("服务端收到信息："+msg);
        ByteBuffer outBuffer = ByteBuffer.wrap(msg.getBytes());
        channel.write(outBuffer);// 将消息回送给客户端
    }

    public static void main(String[] args) throws Exception {
        NIOServer server = new NIOServer();
        server.initServer(8000);
        server.listen();
    }
}
