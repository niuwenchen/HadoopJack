package com.jackniu.core_design.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/23.
 */
public class ServerMain {
    public static void main(String[] args) throws IOException {
        RPC.Server server =RPC.getServer(new ClientProtocolImpl(),"127.0.0.1",2181,5,false,new Configuration());
        server.start();
    }
}


