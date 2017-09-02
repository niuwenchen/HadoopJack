package com.jackniu.yarnrpc.pb.api;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.jackniu.yarnrpc.pb.proto.CalculateMessage;

/**
 * Created by JackNiu on 2017/9/2.
 */
public class CalculatePBServiceImpl implements CalculatePB {

    public Calculate real;

    public CalculatePBServiceImpl(Calculate impl){
        this.real = impl;
    }

    public CalculateMessage.ResponseProto add(RpcController controller, CalculateMessage.RequestProto request) throws ServiceException {
        CalculateMessage.ResponseProto proto = CalculateMessage.ResponseProto.getDefaultInstance();
        CalculateMessage.ResponseProto.Builder build = CalculateMessage.ResponseProto.newBuilder();
        int add1 = request.getNum1();
        int add2 = request.getNum2();
        int sum = real.add(add1, add2);
        CalculateMessage.ResponseProto result = null;
        build.setResult(sum);
        result = build.build();
        return result;
    }

    public CalculateMessage.ResponseProto minus(RpcController controller, CalculateMessage.RequestProto request) throws ServiceException {
        CalculateMessage.ResponseProto proto = CalculateMessage.ResponseProto.getDefaultInstance();
        CalculateMessage.ResponseProto.Builder build = CalculateMessage.ResponseProto.newBuilder();
        int add1 = request.getNum1();
        int add2 = request.getNum2();
        int sum = real.minus(add1, add2);
        CalculateMessage.ResponseProto result = null;
        build.setResult(sum);
        result = build.build();
        return result;
    }
}
