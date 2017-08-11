package com.jackniu.core_design.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by JackNiu on 2017/7/23.
 */
public class ClientMain {
    public static void main(String[] args) throws IOException {
        CLientProtocol proxy=(CLientProtocol)RPC.getProxy(CLientProtocol.class,CLientProtocol.versionID,new InetSocketAddress("127.0.0.1",2181),new Configuration());
        int result = proxy.add(5,6);
        String results=proxy.echo("result");
        System.out.println(result);
    }
}




