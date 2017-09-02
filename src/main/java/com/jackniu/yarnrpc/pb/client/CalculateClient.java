package com.jackniu.yarnrpc.pb.client;

import com.jackniu.yarnrpc.pb.api.Calculate;
import com.jackniu.yarnrpc.pb.proto.CalculateMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * Created by JackNiu on 2017/9/2.
 */
public class CalculateClient implements Calculate{
    public int add(int num1, int num2) {
        return socketoperation("add",num1,num2);
    }

    public int minus(int num1, int num2) {
        return socketoperation("minus",num1,num2);
    }

    public int socketoperation(String op,int num1,int num2){
        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        int ret = 0;
        try{
            s= new Socket("localhost", 8888);
            out = new DataOutputStream(s.getOutputStream());
            in = new DataInputStream(s.getInputStream());

            CalculateMessage.RequestProto.Builder builder = CalculateMessage.RequestProto.newBuilder();
            builder.setMethodName(op);
            builder.setNum1(num1);
            builder.setNum2(num2);
            CalculateMessage.RequestProto request = builder.build();

            byte [] bytes = request.toByteArray();
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();

            int dataLen = in.readInt();
            byte[] data = new byte[dataLen];
            int count = in.read(data);
            if(count != dataLen){
                System.err.println("something bad happened!");
            }

            CalculateMessage.ResponseProto result = CalculateMessage.ResponseProto.parseFrom(data);
            System.out.println(num1 + " " + op + " " + num2 + "=" + result.getResult());
            ret = result.getResult();

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                in.close();
                out.close();
                s.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        CalculateClient client = new CalculateClient();
        int testCount = 5;
        Random rand = new Random();
        while(testCount-- > 0){
            int a = rand.nextInt(100);
            int b = rand.nextInt(100);
            client.add(a,b);
            client.minus(a, b);
        }
    }
}
