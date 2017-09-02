package com.jackniu.hadoop_yarn.com.rpc.hadooprpc;

import org.apache.hadoop.ipc.VersionedProtocol;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/9/1.
 */
public interface MethodProtocol extends VersionedProtocol{
    public static final long versionID=1L;
    int calculate(int v1,int v2) throws IOException;
}
