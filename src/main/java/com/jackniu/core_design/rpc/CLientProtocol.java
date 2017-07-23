package com.jackniu.core_design.rpc;

import sun.plugin2.main.server.HeartbeatThread;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/23.
 */
public interface CLientProtocol  extends org.apache.hadoop.ipc.VersionedProtocol{
    public static final long versionID=1L;
    String echo(String value) throws IOException;
    int add(int v1,int v2) throws IOException;

}
