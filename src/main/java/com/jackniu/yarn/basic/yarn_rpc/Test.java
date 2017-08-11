package com.jackniu.yarn.basic.yarn_rpc;

import org.apache.hadoop.hbase.mapreduce.CopyTable;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.factories.impl.pb.RpcClientFactoryPBImpl;
import org.apache.hadoop.yarn.factories.impl.pb.RpcServerFactoryPBImpl;
import org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.server.api.ResourceTracker;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
//import org.apache.hadoop.yarn.server
//import org.apache.hadoop.yarn.proto.ResourceTracker;


/**
 * Created by JackNiu on 2017/8/1.
 */
public class Test {
    YarnRPC yarnrpc;
    ProtobufRpcEngine rpcengine;
    HadoopYarnProtoRPC rpc;
    RpcClientFactoryPBImpl cli;
    RpcServerFactoryPBImpl clii;
//    ResourceTracker.ResourceTrackerService ser;
    ResourceTracker s;
    org.apache.hadoop.yarn.proto.ResourceTracker.ResourceTrackerService s2;

    CopyTable x;
    RPC rpc1;
    NodeManager nm;
//    ResourceManager rs;


}
