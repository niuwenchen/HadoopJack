package com.jackniu.core_design.jobsubmit.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Created by JackNiu on 2017/7/24.
 */
public class ProxyText {
    public static void main(String[] args) throws Exception{
        InvocationHandler handler = new MyInvocationHandler();
        Person p =(Person) Proxy.newProxyInstance(Person.class.getClassLoader(),
                new Class[]{Person.class},handler);
        // 动态代理对象的walk和sayhello
        p.walk();
        p.sayHello();
    }
}
