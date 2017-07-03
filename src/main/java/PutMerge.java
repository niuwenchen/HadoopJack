/**
 * Created by JackNiu on 2017/7/3.
 */
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.IOException;

/**
 * Created by lyj on 10/26/16.
 */
public class PutMerge {
    public static void main(String[] args) throws IOException{
        Configuration conf=new Configuration();
        FileSystem hdfs = FileSystem.get(conf);
        LocalFileSystem local = FileSystem.getLocal(conf);
        Path inputDir=new Path(args[0]);
        Path outFile=new Path(args[1]);
        try{
            FileStatus[] fileStatuses = local.listStatus(inputDir);
            FSDataOutputStream fsDataOutputStream = hdfs.create(outFile);
            for (int i = 0; i < fileStatuses.length; i++) {
                System.out.println(fileStatuses[i].getPath().getName());
                FSDataInputStream in = local.open(fileStatuses[i].getPath());
                byte [] buffer=new byte[1024];
                int len=0;
                while((len=in.read(buffer))>0){
                    fsDataOutputStream.write(buffer,0,len);
                }
                in.close();
            }
            fsDataOutputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }


    }
}
