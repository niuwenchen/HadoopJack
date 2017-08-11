package com.jackniu.yarn.basic.yarn_rpc.demo;

/**
 * Created by JackNiu on 2017/8/1.
 */
public interface IClientNamenodeProtocol {
    public final long versionID=1L;

    public String getMetaData(String path);
}
