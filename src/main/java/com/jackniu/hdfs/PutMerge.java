package com.jackniu.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class PutMerge {
    public static void main(String[] args) throws Exception{
        Configuration conf = new Configuration();
        FileSystem hdfs = FileSystem.get(conf);
        FileSystem local = FileSystem.getLocal(conf);

        Path inputDir = new Path(args[0]);
        Path hdfsFile= new Path(args[1]);
        try{
            FileStatus[] inputFiles=local.listStatus(inputDir);
            FSDataOutputStream out = hdfs.create(hdfsFile); // 生成HDFS输出流
            for (int i=0;i<inputFiles.length;i++)
            {
                FSDataInputStream in=local.open(inputFiles[i].getPath());
                byte buffer[] = new byte[1024];
                int bytesRead =0;
                while( (bytesRead = in.read(buffer))>0){
                    out.write(buffer,0,bytesRead);
                }
                in.close();
            }
            out.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
