package com.jackniu.yarn.basic.yarn_rpc.demo;

/**
 * Created by JackNiu on 2017/8/1.
 */
public class MyNameNode implements  IClientNamenodeProtocol {
    public String getMetaData(String path) {
        System.out.println("传入的路径："+path);
        return path+": 副本数量3-{BLK-1,BLK-2}-NameNode.......";
    }
}
