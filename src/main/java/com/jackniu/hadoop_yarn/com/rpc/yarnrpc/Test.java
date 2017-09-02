package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;


import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.proto.ResourceTracker;
import org.apache.hadoop.yarn.server.resourcemanager.ApplicationMasterService;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceTrackerService;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class Test {
   ResourceTrackerService ss;
   ApplicationMasterService sc;
   RPC rpc;
}
