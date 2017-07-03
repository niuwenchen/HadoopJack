package com.jackniu.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
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
public class WordCount extends Configured implements Tool {

    public static class MapeperClass extends  MapReduceBase implements Mapper<Text,Text,Text,Text>{
        public void map(Text text, Text text2, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {
            outputCollector.collect(text2,text);
        }
    }
    public  static class Reducerclass extends  MapReduceBase implements Reducer<Text,Text,Text,LongWritable> {

        public void reduce(Text text, Iterator<Text> iterator, OutputCollector<Text, LongWritable> outputCollector, Reporter reporter) throws IOException {
            int count = 0;
            while(iterator.hasNext())
            {
                iterator.next();
                count++;
            }
            outputCollector.collect(text,new LongWritable(count));
        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf,WordCount.class);

        job.setJobName("WordCount");
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);
        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.set("key.value.separator.in.input.line",",");

        job.setMapperClass(MapeperClass.class);
        job.setReducerClass(Reducerclass.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        JobClient.runJob(job);
        return 0;
    }

    public static void main(String[] args) throws Exception{
        int res=ToolRunner.run(new Configuration(),new WordCount(),args);
        System.exit(res);
    }
}
