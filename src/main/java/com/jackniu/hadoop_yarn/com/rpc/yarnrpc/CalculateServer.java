package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;

import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class CalculateServer extends Thread {
    private Class<?> protocol;
    private BlockingService impl;
    private int port;
    private ServerSocket ss;

    public CalculateServer(Class<?> protocol, BlockingService protocolImpl,int port){
        this.protocol= protocol;
        this.impl = protocolImpl;
        this.port = port;
    }
    public void run() {
        System.out.println("accept... ");
        Socket clientSocket = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int count = 10;

        while (count -->0) {
            System.out.println("true...");
            try {
                clientSocket = ss.accept();

                dos = new DataOutputStream(clientSocket.getOutputStream());
                System.out.println("error");
                dis = new DataInputStream(clientSocket.getInputStream());

                int dataLen = dis.readInt();
                System.out.println(dataLen);
                byte[] dataBuffer = new byte[dataLen];

                byte[] result = processRpc(dataBuffer);
                System.out.println(result);

                dos.writeInt(result.length);
                dos.write(result);
                dos.flush();
//
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try{
            dos.close();
            dis.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public byte[] processRpc(byte[] dis) throws Exception{
        System.out.println("Here");
        CalculateMessage.RequestProto request = CalculateMessage.RequestProto.parseFrom(dis);
        System.out.println("read Ok!!!");
        String methodName = request.getMethodName();
        System.out.println(methodName);
        Descriptors.MethodDescriptor methodByName = impl.getDescriptorForType().findMethodByName(methodName);
        Message response = impl.callBlockingMethod(methodByName,null,request);
        System.out.println(response);
        System.out.println("here1");
        return response.toByteArray();
    }
}
