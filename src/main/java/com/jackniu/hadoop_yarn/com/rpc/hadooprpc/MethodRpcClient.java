package com.jackniu.hadoop_yarn.com.rpc.hadooprpc;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.net.InetSocketAddress;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class MethodRpcClient {
    public static void main(String[] args) throws Exception{
        MethodProtocol proxy = RPC.getProxy(MethodProtocol.class,MethodProtocol.versionID,
                new InetSocketAddress("localhost",8888),new Configuration());
        int result = proxy.calculate(1,2);
        System.out.println(result);
    }
}
