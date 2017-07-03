package com.jackniu.hdfs;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueLineRecordReader;
import org.apache.hadoop.mapred.RecordReader;


import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class TimeUrlLineRecordReader implements RecordReader<Text,URLWritable> {
    private KeyValueLineRecordReader lineReader;
    private Text lineKey,lineValue;

    public TimeUrlLineRecordReader(JobConf job, FileSplit split) throws IOException{
        lineReader  = new KeyValueLineRecordReader(job,split);
        lineKey = lineReader.createKey();
        lineValue = lineReader.createValue();
    }

    public boolean next(Text text, URLWritable urlWritable) throws IOException {
       if (!lineReader.next(lineKey,lineValue)){
           return false;
       }
       text.set(lineKey);
        urlWritable.set(lineValue.toString());
        return true;
    }

    public Text createKey() {
        return new Text("");
    }

    public URLWritable createValue() {
        return new URLWritable();
    }

    public long getPos() throws IOException {
        return lineReader.getPos();
    }

    public void close() throws IOException {
        lineReader.close();
    }

    public float getProgress() throws IOException {
        return lineReader.getProgress();
    }
}
