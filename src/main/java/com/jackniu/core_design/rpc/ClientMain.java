package com.jackniu.core_design.rpc;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Created by JackNiu on 2017/7/23.
 */
public class ClientMain {
    public static void main(String[] args) throws IOException {
        CLientProtocol proxy=(CLientProtocol)RPC.getProxy(CLientProtocol.class,CLientProtocol.versionID,new InetSocketAddress(2012),new Configuration());
        int result = proxy.add(5,6);
        String results=proxy.echo("result");
    }
}
