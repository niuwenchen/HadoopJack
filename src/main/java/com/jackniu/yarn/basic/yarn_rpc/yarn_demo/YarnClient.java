package com.jackniu.yarn.basic.yarn_rpc.yarn_demo;

import com.jackniu.yarn.basic.yarn_rpc.yarn_demo.impl.pb.service.IClientNamenodeProtocolPBServiceImpl;
import com.jackniu.yarn.basic.yarn_rpc.yarn_demo.impl.pb.service.PBServiceImplIClientNamenodeProtocol;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC;
import org.apache.hadoop.yarn.ipc.YarnRPC;

import java.net.InetSocketAddress;

/**
 * Created by JackNiu on 2017/8/4.
 */
public class YarnClient {
    public static void main(String[] args) {
        HadoopYarnProtoRPC rpc=(HadoopYarnProtoRPC)YarnRPC.create(new Configuration());
//        rpc.getProxy()
//        rpc.getServer(IClientNamenodeProtocol.class, this, this.clientBindAddress, conf, this.rmDTSecretManager, conf.getInt("yarn.resourcemanager.client.thread-count", 50));

//        server.start();
    }
}
