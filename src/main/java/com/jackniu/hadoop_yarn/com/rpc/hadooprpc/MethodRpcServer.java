package com.jackniu.hadoop_yarn.com.rpc.hadooprpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class MethodRpcServer {
    public static void main(String[] args)  throws Exception{
        Configuration conf = new Configuration();
        RPC.Builder builder =new RPC.Builder(conf);
        builder.setBindAddress("localhost")
                .setPort(8888).setProtocol(MethodProtocol.class)
                .setInstance(new MethodProtocolImpl());
        RPC.Server server=builder.build();
        server.start();
    }
}
