package com.jackniu.yarn.basic.yarn_rpc.yarn_demo.impl.pb.service;

import com.jackniu.yarn.basic.yarn_rpc.yarn_demo.IClientNamenodeProtocol;

/**
 * Created by JackNiu on 2017/8/4.
 */
public class PBServiceImplIClientNamenodeProtocol  implements IClientNamenodeProtocol {
    public String getMetaData(String path) {
        System.out.println("传入的路径：" + path);
        return path + ": 副本数量3-{BLK-1,BLK-2}-NameNode.......";
    }
}
