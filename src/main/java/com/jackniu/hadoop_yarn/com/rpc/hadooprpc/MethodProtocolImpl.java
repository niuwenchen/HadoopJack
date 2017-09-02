package com.jackniu.hadoop_yarn.com.rpc.hadooprpc;

import org.apache.hadoop.ipc.ProtocolSignature;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class MethodProtocolImpl implements MethodProtocol
{
    public int calculate(int v1, int v2) throws IOException {
        return  v1+v2;
    }


    public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
        return  MethodProtocol.versionID;
    }

    public ProtocolSignature getProtocolSignature(String protocol, long clientVersion, int clientMethodsHash) throws IOException {
        return new ProtocolSignature(MethodProtocol.versionID,null);
    }
}
