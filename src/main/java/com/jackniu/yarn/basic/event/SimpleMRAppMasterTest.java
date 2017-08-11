package com.jackniu.yarn.basic.event;

import org.apache.hadoop.yarn.conf.YarnConfiguration;

/**
 * Created by JackNiu on 2017/8/7.
 */
public class SimpleMRAppMasterTest {
    public static void main(String[] args) throws Exception{
        String jobID = "job_20131215_12";
        SimpleMRAppMaster appMaster = new SimpleMRAppMaster("Simple MRApplication ",jobID,5);
        YarnConfiguration conf = new YarnConfiguration();
        appMaster.serviceInit(conf);
        appMaster.serviceStart();
        appMaster.getDispatcher().getEventHandler().handle(new JobEvent(jobID,
                JobEventType.JOB_KILL));
        appMaster.getDispatcher().getEventHandler().handle(new JobEvent(jobID,
                JobEventType.JOB_INIT));



    }
}
