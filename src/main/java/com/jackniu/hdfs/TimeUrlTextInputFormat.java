package com.jackniu.hdfs;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class TimeUrlTextInputFormat  extends FileInputFormat<Text,URLWritable> {
    public RecordReader<Text, URLWritable> getRecordReader(InputSplit inputSplit, JobConf jobConf, Reporter reporter) throws IOException {
        return new TimeUrlLineRecordReader(jobConf,(FileSplit)inputSplit);
    }
}
