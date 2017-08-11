package com.jackniu.yarn.basic.event;

import org.apache.hadoop.yarn.event.AbstractEvent;

/**
 * Created by JackNiu on 2017/8/7.
 */
public class TaskEvent extends AbstractEvent<TaskEventType> {
    private String taskId;
    public TaskEvent(String taskId,TaskEventType taskEventType) {
        super(taskEventType);
        this.taskId = taskId;
    }
    public String getTaskId()
    {
        return this.taskId;

    }

}
