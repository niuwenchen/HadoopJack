package com.jackniu.hdfs;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by JackNiu on 2017/7/3.
 */
public class Edge implements WritableComparable<Edge> {
    private String departureNode;
    private String arriveValNode;
    public String getDepartureNode(){return departureNode;}

    public int compareTo(Edge o) {
        // 返回比较结果
        return (departureNode.compareTo(o.departureNode)!=0)
                ?departureNode.compareTo(o.departureNode)
                :arriveValNode.compareTo(o.arriveValNode);
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(departureNode);
        dataOutput.writeUTF(arriveValNode);
    }

    public void readFields(DataInput dataInput) throws IOException {
        departureNode = dataInput.readUTF();
        arriveValNode = dataInput.readUTF();
    }
}
