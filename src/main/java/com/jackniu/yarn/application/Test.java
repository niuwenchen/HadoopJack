package com.jackniu.yarn.application;

import org.apache.hadoop.yarn.api.ApplicationConstants;

/**
 * Created by JackNiu on 2017/8/10.
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(ApplicationConstants.Environment.JAVA_HOME.$());
    }
}
