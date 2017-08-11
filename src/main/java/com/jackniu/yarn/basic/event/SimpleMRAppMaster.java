package com.jackniu.yarn.basic.event;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.EventHandler;

/**
 * Created by JackNiu on 2017/8/7.
 */
// 中央异步调度器，接受Job和Task事件类型，并交给事件处理器处理
    @SuppressWarnings("unchecked")
public class SimpleMRAppMaster extends CompositeService{
    private Dispatcher dispatcher;
    private String jobID;
    private  int taskNumber;
    private  String[] taskIds;
    public SimpleMRAppMaster(String name,String jobID,int taskNumber) {
        super(name);

        this.jobID = jobID;
        this.taskNumber = taskNumber;
        taskIds= new String[this.taskNumber];
        for(int i=0;i<taskNumber;i++) {
            String s = this.jobID + "_task_" + i;
            System.out.println(s);
            this.taskIds[i] = s;
        }
    }

    @Override
    protected void serviceInit(Configuration conf) throws Exception {
       dispatcher = new AsyncDispatcher(); // 定义一个中央调度器
        //分别注册Job和Task事件调度器
        dispatcher.register(JobEventType.class,new JobEventDispatcher());
        dispatcher.register(TaskEventType.class,new TaskEventDispatcher());
        // addService: 将这个dispatcher 发布为服务
        addService((Service) dispatcher);
        super.serviceInit(conf);
    }

    public Dispatcher getDispatcher(){
        return this.dispatcher;
    }

    @Override
    protected void serviceStart() throws Exception {
        super.serviceStart();
    }

    private class JobEventDispatcher implements EventHandler<JobEvent>{

        public void handle(JobEvent event) {
            if(event.getType() == JobEventType.JOB_INIT){
                System.out.println("Receive JOB_KILL event, killing all the tasks");
                for(int i=0;i<taskNumber;i++)
                    dispatcher.getEventHandler().handle(new TaskEvent(taskIds[i],TaskEventType.T_KILL));
            }else if(event.getType() == JobEventType.JOB_INIT){
                System.out.println("Receive JOB_INIT event, initing all the tasks");
                for(int i=0;i<taskNumber;i++)
                    dispatcher.getEventHandler().handle(new TaskEvent(taskIds[i],TaskEventType.T_SCHEDULE));
            }
        }
    }
    private class TaskEventDispatcher implements  EventHandler<TaskEvent>{
        public void handle(TaskEvent event) {
            if(event.getType() == TaskEventType.T_KILL){
                System.out.println("Receive T_KILL event of task "+ event.getTaskId());
            }else if(event.getType() == TaskEventType.T_SCHEDULE){
                System.out.println("Recevie T_SCHEDULE  event of task "+ event.getTaskId());
            }
        }
    }
}
