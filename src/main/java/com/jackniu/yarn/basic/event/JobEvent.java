package com.jackniu.yarn.basic.event;

import org.apache.hadoop.yarn.event.AbstractEvent;

/**
 * Created by JackNiu on 2017/8/7.
 */
public class JobEvent extends AbstractEvent<JobEventType> {
    private String jobID;
    public JobEvent(String jobID,JobEventType jobEventType) {
        super(jobEventType);
        this.jobID=jobID;
    }
    public String getJobID(){
        return  this.jobID;
    }
}
