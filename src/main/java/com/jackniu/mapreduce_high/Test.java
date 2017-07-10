package com.jackniu.mapreduce_high;

/**
 * Created by JackNiu on 2017/7/10.
 */
public class Test {
    public static void main(String[] args){
        String s="3,Jose Madriz,281-330-8004";
        String[] tokens = s.split(",",1);
        for(String s1:tokens){
            System.out.println(s1);
        }

    }
}
