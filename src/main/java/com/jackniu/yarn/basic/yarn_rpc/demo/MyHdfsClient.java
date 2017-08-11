package com.jackniu.yarn.basic.yarn_rpc.demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by JackNiu on 2017/8/1.
 */
public class MyHdfsClient  {
    public static void main(String[] args) throws IOException {
        Configuration conf= new Configuration();
//        RPC.setProtocolEngine(conf, IClientNamenodeProtocol.class, ProtobufRpcEngine.class);
        IClientNamenodeProtocol proxy= RPC.getProxy(IClientNamenodeProtocol.class, 1, new InetSocketAddress("localhost", 8888), conf);
        String metaData = proxy.getMetaData("/hellohellohelloxxxxxxx.xxx");
        System.out.println("获取到结果："+metaData);
    }
}
