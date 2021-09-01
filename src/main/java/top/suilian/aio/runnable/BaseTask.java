package top.suilian.aio.runnable;

/**
* 基本的Runnable任务
* @Description : TODO
* @Author: mudeliang
* @Date: 2017/9/21 17:01
* @Version 1.0
**/
public abstract class BaseTask implements Runnable{
    /** 线程名称 */
    protected Integer name;
    /** 运行状态 */
    protected boolean running;


    public BaseTask(Integer name) {
        this.name = name;
    }

    @Override
    public void run() {
        running = true;
        dowork();
        running = false;
    }
    /**
    * 处理业务
    * @return
    * @Author
    * @date
    **/
    public abstract void dowork();

    public Integer getName() {
        return name;
    }

    public void setName(Integer name) {
        this.name = name;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
