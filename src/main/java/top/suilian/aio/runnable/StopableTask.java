package top.suilian.aio.runnable;


import org.apache.log4j.Logger;
import top.suilian.aio.redis.RedisHelper;

/**
 * 可停止的任务
 *
 * @Description : TODO
 * @Author: mudeliang
 * @Date: 2017/9/21 17:02
 * @Version 1.0
 **/
public abstract class StopableTask<T extends StopableTask> extends BaseTask {
    /**
     * 日志
     */
    Logger logger = Logger.getLogger(StopableTask.class);

    /**
     * 状态枚举
     */
    public static enum Status {
        ready, running, stopping, stoped
    }

    /**
     * 运行状态
     */
    protected Status status = Status.ready;


    public StopableTask(Integer name) {
        super(name);
    }

    @Override
    public void run() {
        if (status == Status.ready) {//未停止未运行,执行初始化
            logger.warn("{" + name + "} 开始");
            status = Status.running;
            running = true;
        }
        while (status == Status.running) {
            dowork();
        }
        running = false;
        status = Status.stoped;
        logger.warn("{" + name + "} 停止");
    }

    public void stop() {
        if (status == Status.stoped) {
            logger.warn("{" + name + "} 已停止");
            return;
        }
        if (status == Status.ready) {
            logger.warn("{" + name + "} 未开始");
            status = Status.stoped;
            return;
        }
        if (status == Status.running) {
            logger.warn("{" + name + "} 准备停止...");
            status = Status.stopping;
            //等待停止
            long time = System.currentTimeMillis();
            while (status == Status.stopping) {
                try {
                    logger.warn("{" + name + "} 等待任务处理结束... {" + (System.currentTimeMillis() - time) + "} ms");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
