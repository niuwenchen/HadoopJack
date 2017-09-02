package com.jackniu.yarnrpc.pb.server.business;

import com.google.protobuf.BlockingService;
import com.jackniu.yarnrpc.pb.api.Calculate;
import com.jackniu.yarnrpc.pb.ipc.Server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by JackNiu on 2017/9/2.
 */
public class CalculateService implements Calculate {

    private Server server = null;
    private final Class protocol = Calculate.class;
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final String protoPackage ="com.jackniu.yarnrpc.pb.proto";
    private final String host = "localhost";
    private final int port = 8888;

    public CalculateService(){

    }

    public int add(int num1, int num2) {
        return num1+num2;
    }

    public int minus(int num1, int num2) {
        return num1-num2;
    }

    // return ： org.jackniu.yarnrpc.pb.api.CalculatePBServiceImpl
    public Class<?> getPbServiceImplClass(){
        String packageName = protocol.getPackage().getName();
        String className = protocol.getSimpleName();
        String pbServiceImplName =  packageName + "." + className +  "PBServiceImpl";
        Class<?> clazz = null;
        try{
            clazz = Class.forName(pbServiceImplName, true, classLoader);
        }catch(ClassNotFoundException e){
            System.err.println(e.toString());
        }
        System.out.println(clazz);
        return clazz;
    }

    public Class<?> getProtoClass(){
        String className = protocol.getSimpleName();
        String protoClazzName =  protoPackage + "." + className + "$" + className + "Service";
        Class<?> clazz = null;
        try{
            clazz = Class.forName(protoClazzName, true, classLoader);
        }catch(ClassNotFoundException e){
            System.err.println(e.toString());
        }
        System.out.println(clazz);
        return clazz;
    }

    public void createServer() {
        Class<?> pbServiceImpl = getPbServiceImplClass();
        Constructor<?> constructor = null;
        try {
            constructor = pbServiceImpl.getConstructor(protocol);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            System.err.print(e.toString());
        }

        Object service = null;  // instance of CalculatePBServiceImpl
        try {
            service = constructor.newInstance(this);
        } catch (InstantiationException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

        Class<?> pbProtocol = service.getClass().getInterfaces()[0];
        System.out.println(pbProtocol);
        Class<?> protoClazz = getProtoClass();
        System.out.println(protoClazz);
        Method method = null;
        try {
            method = protoClazz.getMethod("newReflectiveBlockingService", pbProtocol.getInterfaces()[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            createServer(pbProtocol,(BlockingService) method.invoke(null,service));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void createServer(Class pbProtocol,BlockingService service){
        server= new Server(pbProtocol,service,port);
        server.start();
    }

    public void init(){
        createServer();
    }

    public static void main(String[] args) {
        CalculateService cs= new CalculateService();
        cs.init();

    }

    }
