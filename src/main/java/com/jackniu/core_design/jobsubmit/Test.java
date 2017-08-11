package com.jackniu.core_design.jobsubmit;

/**
 * Created by JackNiu on 2017/7/24.
 */
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapreduce.split.JobSplit;
import org.apache.hadoop.mapreduce.split.JobSplitWriter;
import org.apache.hadoop.mapreduce.split.SplitMetaInfoReader;
import org.apache.hadoop.util.RunJar;
import org.apache.hadoop.mapreduce.Mapper;
//import org.apache.hadoop.mapred.Mapper;

public class Test {
    JobClient jc;
    JobSplit js;
    JobSplitWriter jsw;
    SplitMetaInfoReader smir;

}
