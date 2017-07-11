package com.jackniu.mapreduce_high;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

/**
 * Created by JackNiu on 2017/7/10.
 */
public class BloomFilter<E> implements Writable {
    private BitSet bf;
    private int bitArraySize=100000000;
    private int numHashFunc=6;

    public BloomFilter(){
        bf = new BitSet(bitArraySize);
    }

    public void add(E obj){
        int[] indexes = getHashIndexes(obj);

        for (int index:indexes){
            bf.set(index);
        }
    }
    public boolean contains(E obj){
        int[] indexes = getHashIndexes(obj);
        for(int index:indexes){
            if (bf.get(index) == false){
                return false;
            }
        }
        return true;
    }

    public  void union(BloomFilter<E> other){
        bf.or(other.bf);
    }


    // 散列函数实现位置，MD5散列，增加一个对象就是置一个位数组中的某些位为1
    private int[] getHashIndexes(E obj) {
        int[] indexes = new int[numHashFunc];
        long  seed=0;
        byte[] digest;
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(obj.toString().getBytes());
            digest= md.digest();
            for (int i=0;i<6;i++){
                seed = seed ^(((long)digest[i] & 0xFF)<<(8*i));
            }
        }catch(NoSuchAlgorithmException e){}
        return indexes;
    }

    public void write(DataOutput dataOutput) throws IOException {
        int byteArraySize = (int)(bitArraySize/8);
        byte[] byteArray = new byte[byteArraySize];
        for(int i=0;i<byteArraySize;i++){
            byte nextElement = 0;
            for (int j=0;j<8;j++){
                if(bf.get(8*i+j)){
                    nextElement |= 1<<j;
                }
            }
            byteArray[i]=nextElement;
        }
        dataOutput.write(byteArray);
    }

    public void readFields(DataInput dataInput) throws IOException {
        int byteArraySize = (int)(bitArraySize/8);
        byte[] byteArray = new byte[byteArraySize];
        for(int i=0;i<byteArraySize;i++){
            byte nextByte = byteArray[i];
            for (int j=0;j<8;j++){
                if(((int)nextByte &(1<<j))!=0){
                    bf.set(8*i+j);
                }
            }
        }
    }
}
