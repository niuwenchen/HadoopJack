package com.jackniu.mapreduce_high;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.contrib.utils.join.DataJoinMapperBase;
import org.apache.hadoop.contrib.utils.join.DataJoinReducerBase;
import org.apache.hadoop.contrib.utils.join.TaggedMapOutput;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Created by JackNiu on 2017/7/10.
 */
public class DataJoin extends Configured implements Tool {
    public static class MapClass extends DataJoinMapperBase {

        // 获取Tag
        protected Text generateInputTag(String inputFile) {
            String dataSource = inputFile.split("-")[0];
            return new Text(dataSource);
        }

        protected TaggedMapOutput generateTaggedMapOutput(Object o) {
            System.out.println(o);// data
            TaggedWritable retv = new TaggedWritable((Text)o);
            retv.setTag(this.inputTag);
            return retv;
        }

//        @Override
//        public void map(Object key, Object value, OutputCollector output, Reporter reporter) throws IOException {
//            TaggedMapOutput aRecord = generateTaggedMapOutput(value);
//            Text groupKey = generateGroupKey(aRecord);
//            output.collect(groupKey,aRecord);
//        }

        protected Text generateGroupKey(TaggedMapOutput taggedMapOutput) {
            String  line = ((Text)taggedMapOutput.getData()).toString();
            String[] tokens = line.split(",");
            String groupKey = tokens[0];
            return new Text(groupKey);
        }
    }

    public static class Reduce extends DataJoinReducerBase{


        protected TaggedMapOutput combine(Object[] tags, Object[] values) {
            if(tags.length<2) return null;
            String joinedStr ="";
            for(int i=0;i<values.length;i++){
                if (i>0) joinedStr +=",";
                TaggedWritable tw = (TaggedWritable)values[i];
                String line = ((Text) tw.getData()).toString();
                String[] tokens =line.split(",",2);
                joinedStr += tokens[1];
            }
            TaggedWritable retv= new TaggedWritable(new Text(joinedStr));
            retv.setTag((Text) tags[0]);
            return retv;
        }
    }

    public static class TaggedWritable extends TaggedMapOutput{
        private Writable data;
        public TaggedWritable(Writable data){
            this.tag=new Text("");
            this.data = data;
        }
        @Override
        public Writable getData() {
            return data;
        }

        public void write(DataOutput dataOutput) throws IOException {
            this.tag.write(dataOutput);
            this.data.write(dataOutput);
        }

        public void readFields(DataInput dataInput) throws IOException {
            this.tag.readFields(dataInput);
            this.data.readFields(dataInput);
        }
    }


    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf,DataJoin.class);

        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        FileInputFormat.setInputPaths(job,in);
        FileOutputFormat.setOutputPath(job,out);
        job.setJobName("DataJoin");
        job.setMapperClass(MapClass.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(TaggedWritable.class);
        job.set("mapred.textoutputformat.separator",",");

        JobClient.runJob(job);
        return 0;

    }

    public static void main(String[] args) throws Exception{
        int res = ToolRunner.run(new Configuration(),new DataJoin(),args);
        System.exit(res);
    }


}
