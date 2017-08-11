package com.jackniu.yarn.basic.yarn_rpc.yarn_demo;

/**
 * Created by JackNiu on 2017/8/1.
 */
public interface IClientNamenodeProtocol {
    public final long versionID=1L;

    public String getMetaData(String path);
}
