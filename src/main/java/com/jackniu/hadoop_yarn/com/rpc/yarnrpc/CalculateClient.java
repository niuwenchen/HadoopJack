package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class CalculateClient implements ICalculate {

    public int domission(String op, int a, int b) {
        Socket s = null;
        Socket clientSocket = null;
        DataOutputStream out = null;
        DataInputStream dis = null;

        try {
            s = new Socket("localhost", 8080);
            out = new DataOutputStream(s.getOutputStream());

            dis = new DataInputStream(s.getInputStream());
            CalculateMessage.RequestProto.Builder builder = CalculateMessage.RequestProto.newBuilder();
            builder.setMethodName(op);
            builder.setNum1(a);
            builder.setNum2(b);
            CalculateMessage.RequestProto request = builder.build();
            System.out.println(request);
          byte [] bytes = request.toByteArray();
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();




            CalculateMessage.ResponseProto response = CalculateMessage.ResponseProto.parseFrom(dis);
            int ret = response.getResult();
            return ret;

            // 进行普通IO操作
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                out.close();
                s.close();
                clientSocket.close();
            }catch(Exception e)
            {
                e.printStackTrace();
            }


        }
        return 0;

    }

    public int add(int num1, int num2) {
        return domission("add",num1,num2);
    }

    public int minus(int num1, int num2) {
        return domission("minus",num1,num2);
    }

    public static void main(String[] args) {
        CalculateClient client = new CalculateClient();
        int testCount =5;
        Random random = new Random();
        while(testCount -->0){
            int a= random.nextInt(100);
            int b =random.nextInt(200);
            int result = client.add(a,b);
//            client.minus(a,b);
            System.out.println(result);
        }
    }
}
