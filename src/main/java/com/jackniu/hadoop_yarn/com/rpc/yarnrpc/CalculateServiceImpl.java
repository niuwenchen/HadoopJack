package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class CalculateServiceImpl implements  CalculatePB{
    public ICalculate real;
    public CalculateServiceImpl(ICalculate impl){
        this.real = impl;
    }

    public CalculateMessage.ResponseProto add(RpcController controller, CalculateMessage.RequestProto request) throws ServiceException {
        CalculateMessage.ResponseProto proto = CalculateMessage.ResponseProto.getDefaultInstance();
        CalculateMessage.ResponseProto.Builder build = CalculateMessage.ResponseProto.newBuilder();
        int add1 = request.getNum1();
        int add2= request.getNum2();
        int sum = real.add(add1,add2);
        CalculateMessage.ResponseProto result = null;
        build.setResult(sum);
        return result;
    }

    public CalculateMessage.ResponseProto minus(RpcController controller, CalculateMessage.RequestProto request) throws ServiceException {
        CalculateMessage.ResponseProto proto = CalculateMessage.ResponseProto.getDefaultInstance();
        CalculateMessage.ResponseProto.Builder build = CalculateMessage.ResponseProto.newBuilder();
        int add1 = request.getNum1();
        int add2= request.getNum2();
        int sum = real.minus(add1,add2);
        CalculateMessage.ResponseProto result = null;
        build.setResult(sum);
        return result;
    }
}
