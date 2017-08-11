package com.jackniu.yarn.basic.yarn_rpc.demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/8/1.
 */
public class PublishServiceUtil {
    public static void main(String[] args) throws IOException {
        Configuration conf= new Configuration();
//        RPC.setProtocolEngine(conf, IClientNamenodeProtocol.class, ProtobufRpcEngine.class);
        RPC.Builder builder =new RPC.Builder(conf);
        builder.setBindAddress("localhost")
                .setPort(8888).setProtocol(IClientNamenodeProtocol.class)
                .setInstance(new MyNameNode());
        RPC.Server server=builder.build();
        server.start();
    }
}
