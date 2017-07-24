package com.jackniu.core_design.rpc;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/23.
 */
public class ClientProtocolImpl implements  CLientProtocol {
    public String echo(String value) throws IOException {
        return value;
    }

    public int add(int v1, int v2) throws IOException {
        return v1+v2;
    }

    public long getProtocolVersion(String s, long l) throws IOException {
        return versionID;
//        return ClientProtocol.versionID;
    }

}
