package com.jackniu.yarnrpc.pb.ipc;

import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.jackniu.yarnrpc.pb.proto.CalculateMessage;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JackNiu on 2017/9/2.
 */
public class Server extends Thread {
    private Class<?> protocol;
    private BlockingService impl;
    private int port;
    private ServerSocket ss;

    public Server(Class<?> protocol, BlockingService impl,int port){
        this.protocol = protocol;
        this.impl = impl;
        this.port = port;
    }

    @Override
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
        int testCount =10;
        while(testCount -->0){
            try{
                clientSocket = ss.accept();

                dos = new DataOutputStream(clientSocket.getOutputStream());
                dis = new DataInputStream(clientSocket.getInputStream());
                int dataLen = dis.readInt();
                byte[] dataBuffer = new byte[dataLen];
                int readCount = dis.read(dataBuffer);
                byte[] result = processOneRpc(dataBuffer);

                dos.writeInt(result.length);
                dos.write(result);
                dos.flush();

            }catch(Exception e){
                e.printStackTrace();
            }
        }
        try{
            dos.close();
            dis.close();
            ss.close();
        }catch(Exception e){

        };
    }

    public byte[] processOneRpc(byte[] data) throws Exception{
        CalculateMessage.RequestProto request = CalculateMessage.RequestProto.parseFrom(data);
        String methodName = request.getMethodName();
        Descriptors.MethodDescriptor methodDescriptor = impl.getDescriptorForType().findMethodByName(methodName);
        Message response = impl.callBlockingMethod(methodDescriptor,null,request);
        return response.toByteArray();
    }


}
