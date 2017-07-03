package com.jackniu.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class MyJob extends Configured implements Tool {

    public static class MapClass extends MapReduceBase
        implements Mapper<Text,Text,Text,Text>{

        public void map(Text key, Text value, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {
            // 倒排， 之前的kv交换顺序,但是这里的k1，v1到底是什么值
            //
            outputCollector.collect(value,key);
        }
    }
    public static class ReduceClass extends  MapReduceBase implements Reducer<Text,Text,Text,Text>{

        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {
            String csv="";
            while(values.hasNext()){
                if (csv.length() >0)
                    csv +=",";
                csv+=values.next().toString();
            }
            outputCollector.collect(key,new Text(csv));
        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf =getConf();
        JobConf  job = new JobConf(conf,MyJob.class);

        Path in = new Path(args[0]);
        Path out  =new Path(args[1]);
        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);

        job.setJobName("MyJob");
        job.setMapperClass(MapClass.class);
        job.setReducerClass(ReduceClass.class);

        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        // 这个参数控制行分隔符
        job.set("key.value.separator.in.input.line",",");

        JobClient.runJob(job);

        return 0;
    }
    public static void main(String[] args) throws Exception{
        int res = ToolRunner.run(new Configuration(),new MyJob(),args);
        System.exit(res);

    }
}
