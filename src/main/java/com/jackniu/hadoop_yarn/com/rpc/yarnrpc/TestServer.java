package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class TestServer {
    public static void main(String[] args) {
        ServerSocket s = null;
        Socket socket = null;
        try {
            //设定服务端的端口号
            s = new ServerSocket(8080);
            System.out.println("ServerSocket Start:"+s);
            //等待请求,此方法会一直阻塞,直到获得请求才往下走
            socket = s.accept();
            System.out.println("Connection accept socket:"+socket);

            //用于接收客户端发来的请求
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            System.out.println("Close.....");

        }


    }
}
