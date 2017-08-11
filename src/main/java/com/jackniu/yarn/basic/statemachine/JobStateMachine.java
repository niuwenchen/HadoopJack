package com.jackniu.yarn.basic.statemachine;

import org.apache.hadoop.mapreduce.v2.app.job.event.JobEvent;
import org.apache.hadoop.yarn.event.EventHandler;

import java.util.concurrent.locks.Lock;

/**
 * Created by JackNiu on 2017/8/7.
 */
public class JobStateMachine implements EventHandler<JobEvent> {
//    private final String jobID;
//    private EventHandler eventHandler;
//    private final Lock writeLock;
//    private final Lock readLock;



    public void handle(JobEvent event) {

    }
}
