package com.jackniu.hadoop_yarn.com.proto;

import org.apache.hadoop.mapred.FileOutputFormat;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by JackNiu on 2017/9/1.
 */
public class ProtocolBufferExample {
    public static void main(String[] args)  throws  Exception{
        StudentProtos.Student student = StudentProtos.Student.newBuilder().setName("jack").setID(1).setSex("2")
                .addPhones(StudentProtos.Student.StudentPhone.newBuilder().setNumber("12345").setType(1))
                .addPhones(StudentProtos.Student.StudentPhone.newBuilder().setNumber("67890").setType(2))
                .build();

        System.out.println(student);
        //在发送端发送
        FileOutputStream output = new FileOutputStream("example.txt");
        student.writeTo(output);
        output.close();

        FileInputStream inputStream = new FileInputStream("example.txt");
        StudentProtos.Student receive_student = StudentProtos.Student.parseFrom(inputStream);
        System.out.println(receive_student);
     }
}
