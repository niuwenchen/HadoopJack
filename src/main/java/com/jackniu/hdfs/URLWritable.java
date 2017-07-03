package com.jackniu.hdfs;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class URLWritable implements Writable{
    protected URL url;

    public URLWritable(){

    }
    public URLWritable(URL url){
        this.url  = url;
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(url.toString());
    }

    public void readFields(DataInput dataInput) throws IOException {
        url =new URL( dataInput.readUTF());
    }
    public void set(String s) throws MalformedURLException{
        url = new URL(s);
    }
}
