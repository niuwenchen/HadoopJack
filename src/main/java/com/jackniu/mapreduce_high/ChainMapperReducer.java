package com.jackniu.mapreduce_high;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.ChainMapper;
import org.apache.hadoop.mapred.lib.ChainReducer;
import org.apache.hadoop.util.Tool;

import java.io.IOException;
import java.util.Iterator;


/**
 * Created by JackNiu on 2017/7/10.
 */
public class ChainMapperReducer extends Configured implements Tool{

    private static  class Map1 extends  MapReduceBase implements  Mapper<Text,Text,Text,Text>{

        public void map(Text text, Text text2, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {

        }
    }
    private static  class Map2 extends  MapReduceBase implements  Mapper<Text,Text,Text,Text>{

        public void map(Text text, Text text2, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {

        }
    }

    private static  class Map3 extends  MapReduceBase implements  Mapper<Text,Text,Text,Text>{

        public void map(Text text, Text text2, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {

        }
    }
    private static  class Map4 extends  MapReduceBase implements  Mapper<Text,Text,Text,Text>{

        public void map(Text text, Text text2, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {

        }
    }

    public static class ReduceClass extends  MapReduceBase implements Reducer<Text,Text,Text,Text>{

        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> outputCollector, Reporter reporter) throws IOException {

        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf);

        job.setJobName("ChainJob");
        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);

        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);

        JobConf  map1conf = new JobConf(false); // loadDefaults
        ChainMapper.addMapper(job,Map1.class,Text.class,Text.class,Text.class,Text.class,true,map1conf);// byValue

        JobConf  map2conf = new JobConf(false); // loadDefaults
        ChainMapper.addMapper(job,Map2.class,Text.class,Text.class,Text.class,Text.class,true,map2conf);

        JobConf  reduceConf = new JobConf(false); // loadDefaults
        ChainReducer.setReducer(job,ReduceClass.class,Text.class,Text.class,Text.class,Text.class,true,reduceConf);

        JobConf  map3conf = new JobConf(false); // loadDefaults
        ChainMapper.addMapper(job,Map3.class,Text.class,Text.class,Text.class,Text.class,true,map3conf);

        JobConf  map4conf = new JobConf(false); // loadDefaults
        ChainMapper.addMapper(job,Map4.class,Text.class,Text.class,Text.class,Text.class,true,map4conf);

        JobClient.runJob(job);

        return 0;
    }
}
