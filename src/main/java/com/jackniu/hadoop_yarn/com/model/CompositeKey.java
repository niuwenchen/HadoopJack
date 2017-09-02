package com.jackniu.hadoop_yarn.com.model;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by JackNiu on 2017/9/2.
 */
public class CompositeKey implements WritableComparable<CompositeKey>{

    private Text joinKey;  // 记录的key
    private Text dataIndex;  // 文件标识


    public int compareTo(CompositeKey o) {
        int result = this.joinKey.compareTo(o.joinKey);
        if (result ==0){
            result = this.dataIndex.compareTo(o.dataIndex);
        }
        return result;
    }

    public void write(DataOutput out) throws IOException {
        this.joinKey.write(out);
        this.dataIndex.write(out);

    }

    public void readFields(DataInput in) throws IOException {
        this.joinKey.readFields(in);
        this.dataIndex.readFields(in);
    }
}
