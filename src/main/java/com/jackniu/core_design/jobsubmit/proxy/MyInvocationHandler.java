package com.jackniu.core_design.jobsubmit.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by JackNiu on 2017/7/24.
 */
public class MyInvocationHandler implements InvocationHandler {
    /*
    * 执行动态代理对象的所有方法时，都会被替换成执行如下的invoke方法
    * proxy: 代表动态代理对象
    * method： 代表正在执行的方法
    * args: 代表实参
    *
    * */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("---正在执行的方法:"+method);
        method.invoke(proxy,args);
        if (args != null){
            System.out.println("--传入的实参为:");
            for (Object arg:args){
                System.out.println(arg);
            }
        }else{

        }
        return null;
    }
}
