package com.jackniu.hadoop_yarn.com.rpc.yarnrpc;

import com.google.protobuf.BlockingService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/**
 * Created by JackNiu on 2017/9/1.
 */
public class CalculateService implements  ICalculate{
    private CalculateServer server = null;
    private final Class protocol = ICalculate.class;
    private CalculateServiceImpl calculateServiceImpl;

    public int add(int num1, int num2) {
        return num1+ num2;
    }

    public int minus(int num1, int num2) {
        return num1-num2;
    }
    public void init(){
        createServer();
    }

    public void createServer(){
        Class<?> pbServiceImpl = CalculateServiceImpl.class;
        Constructor constructor = null;
        try{
            constructor = pbServiceImpl.getConstructor(protocol);
            constructor.setAccessible(true);
        }catch(Exception e){
            e.printStackTrace();
        }

        Object service = null;
        try{
            service = constructor.newInstance(this);
        }catch (Exception e){
            e.printStackTrace();
        }
        Class<?> pbProtocol = service.getClass().getInterfaces()[0];
        System.out.println(pbProtocol);
        Class<?> protoClazz = Calculate.CalculateService.class;

        Method method = null;
        try{
            method = protoClazz.getMethod("newReflectiveBlockingService",pbProtocol.getInterfaces()[0]);
            method.setAccessible(true);
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            createServer(pbProtocol,(BlockingService) method.invoke(null,service));
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void createServer(Class pbProtocol, BlockingService service){
        server = new CalculateServer(pbProtocol,service,8080);
        System.out.println("start");
        server.start();
    }

    public static void main(String[] args) {
        CalculateService service = new CalculateService();
        service.init();
    }
}
