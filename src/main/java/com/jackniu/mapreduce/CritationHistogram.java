package com.jackniu.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;
/**
 * Created by JackNiu on 2017/7/3.
 */
public class CritationHistogram extends Configured implements Tool{

    public static class MapClass extends  MapReduceBase implements  Mapper<Text,Text,IntWritable,IntWritable>{
        private final static IntWritable uno= new IntWritable(1);
        private IntWritable  critationCount = new IntWritable();
        public void map(Text text, Text text2, OutputCollector<IntWritable, IntWritable> outputCollector, Reporter reporter) throws IOException {
            critationCount.set(Integer.parseInt(text2.toString()));
            outputCollector.collect(critationCount,uno);
        }
    }

    public static class ReducerClass extends  MapReduceBase implements  Reducer<IntWritable,IntWritable,IntWritable,IntWritable>{

        public void reduce(IntWritable intWritable, Iterator<IntWritable> iterator, OutputCollector<IntWritable, IntWritable> outputCollector, Reporter reporter) throws IOException {
            int count =0;
            while(iterator.hasNext()){
                count += iterator.next().get();
            }
            outputCollector.collect(intWritable,new IntWritable(count));

        }
    }


    public int run(String[] args) throws Exception {
        Configuration  conf = getConf();
        JobConf job = new JobConf(conf,CritationHistogram.class);

        Path in  = new Path(args[0]);
        Path out = new Path(args[1]);
        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);

        job.setJobName("CrittionHistogram");
        job.setMapperClass(MapClass.class);
        job.setReducerClass(ReducerClass.class);

        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);

        JobClient.runJob(job);

        return 0;
    }
    public static void main(String[] args) throws  Exception{
        int res = ToolRunner.run(new Configuration(),new CritationHistogram(),args);
        System.exit(res);
    }
}
