package com.im.schedule.queue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    
    private static ThreadPoolManager tpm = new ThreadPoolManager();
     
    // 线程池维护线程的最少数量
    private final static int CORE_POOL_SIZE = 4;
     
    // 线程池维护线程的最大数量
    private final static int MAX_POOL_SIZE = 10;
     
    // 线程池维护线程所允许的空闲时间
    private final static int KEEP_ALIVE_TIME = 0;
     
    // 线程池所使用的缓冲队列大小
    private final static int WORK_QUEUE_SIZE = 100;
     
    // 消息缓冲队列
    Queue<ThreadQueueTask> msgQueue = new LinkedList<ThreadQueueTask>();
     
    // 访问消息缓存的调度线程
    // 查看是否有待定请求，如果有，则创建一个新的AccessDBThread，并添加到线程池中
    final Runnable accessBufferThread = new Runnable() {
          
        public void run() { 
            if(hasMoreAcquire()){
            	ThreadQueueTask task = msgQueue.poll(); 
                threadPool.execute( task );
            }
        }
    };
     
     
    final RejectedExecutionHandler handler = new RejectedExecutionHandler(){
 
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println("消息放入队列中重新等待执行");
            msgQueue.offer((ThreadQueueTask)r);
        }
    };
     
    // 管理数据库访问的线程池
     
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,new ArrayBlockingQueue( WORK_QUEUE_SIZE ), this.handler);
     
    // 调度线程池
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 100 );
     
      
    final ScheduledFuture<?> taskHandler = scheduler.scheduleAtFixedRate(accessBufferThread, 0, 1, TimeUnit.SECONDS);
 
    public static ThreadPoolManager newInstance() {
         
        return tpm;
    }
     
    private ThreadPoolManager(){}
     
    private boolean hasMoreAcquire(){
        return !msgQueue.isEmpty();
    }
     
    public void addTask( ThreadQueueTask task ) {  
        threadPool.execute( task );
        
    }
}