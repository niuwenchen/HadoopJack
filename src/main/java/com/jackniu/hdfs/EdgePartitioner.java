package com.jackniu.hdfs;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Partitioner;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class EdgePartitioner implements Partitioner<Edge,Writable> {
    public int getPartition(Edge edge, Writable writable, int numPartitions) {
        return edge.getDepartureNode().hashCode() %numPartitions;
    }

    public void configure(JobConf jobConf) {

    }
}
