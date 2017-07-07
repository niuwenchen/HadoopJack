package com.jackniu.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import javax.servlet.jsp.tagext.TagExtraInfo;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by JackNiu on 2017/7/7.
 */
public class MapperCombinerReducer extends Configured implements Tool {
    public static class MapClass extends MapReduceBase implements Mapper<LongWritable,Text,Text,Text>{


        public void map(LongWritable longWritable, Text text, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {
            String fields[] = text.toString().split(",",-20);
            String country = fields[4];
            String numClaims = fields[8];
            if(numClaims.length() >0 && !numClaims.startsWith("\"")){
                outputCollector.collect(new Text(country),new Text(numClaims+",1"));
            }
        }
    }
    public static class ReduceClass extends  MapReduceBase implements Reducer<Text,Text,Text,DoubleWritable>{

        public void reduce(Text text, Iterator<Text> iterator, OutputCollector<Text, DoubleWritable> outputCollector, Reporter reporter) throws IOException {
            double sum =0;
            int count =0;
            while(iterator.hasNext()){
                String fields[] = iterator.next().toString().split(",");
                sum += Double.parseDouble(fields[0]);
                count += Integer.parseInt(fields[1]);
            }
            outputCollector.collect(text,new DoubleWritable(sum/count));
        }
    }

    public static class CombinerClass extends  MapReduceBase implements Reducer<Text,Text,Text,Text>{

        public void reduce(Text text, Iterator<Text> iterator, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {
            double sum =0;
            int count =0;
            while(iterator.hasNext())
            {
                String fields[] = iterator.next().toString().split(",");
                sum += Double.parseDouble(fields[0]);
                count += Integer.parseInt(fields[1]);
            }
            outputCollector.collect(text,new Text(sum+","+count));
        }
    }




        public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf,MapperCombinerReducer.class);

        Path in = new Path(args[0]);
        Path out  =new Path(args[1]);
        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);

        job.setJobName("MyJob");
        job.setMapperClass(MapperCombinerReducer.MapClass.class);
        job.setCombinerClass(CombinerClass.class);
        job.setReducerClass(MapperCombinerReducer.ReduceClass.class);

        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

            JobClient.runJob(job);
        return 0;
    }

    public static  void main(String[] args) throws  Exception{
        int res=ToolRunner.run(new Configuration(),new MapperCombinerReducer(),args);
        System.exit(res);
    }
}
