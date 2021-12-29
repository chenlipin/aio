package top.suilian.aio.service.coinnoe.newKline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.CommonUtil;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Robot;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.runnable.StopableTask;
import top.suilian.aio.service.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Component
public class RunNewCoinnoeKline {
    //region    Service
    @Autowired
    CancelExceptionService cancelExceptionService;
    @Autowired
    CancelOrderService cancelOrderService;
    @Autowired
    ExceptionMessageService exceptionMessageService;
    @Autowired
    RobotArgsService robotArgsService;
    @Autowired
    RobotLogService robotLogService;
    @Autowired
    RobotService robotService;
    @Autowired
    TradeLogService tradeLogService;
    //endregion


    //region    Utils
    @Autowired
    HttpUtil httpUtil;
    @Autowired
    RedisHelper redisHelper;
    @Autowired
    CommonUtil commonUtil;
    //endregion


    private Work work;
    private List<Work> works = new ArrayList<Work>();

    /****
     * 启动线程
     */
    public void init(int id) {
        //实例化策略对象
        NewCoinoneKline kline = new NewCoinoneKline(cancelExceptionService, cancelOrderService, exceptionMessageService, robotArgsService, robotLogService, robotService, tradeLogService, httpUtil, redisHelper, id);
        redisHelper.initRobot(id);
        work = new Work(kline);
        works.add(work);
        Thread thread = new Thread(work);
        thread.start();
        thread.setName(Constant.KEY_THREAD_KLINE + id);
    }

    /**
     * 停止线程
     *
     * @param id
     * @return
     */
    public boolean stopWork(int id) {
        for (Work work : works) {
            if (work.getName() == id) {
                robotService.setRobotStatus(id, Constant.KEY_ROBOT_STATUS_STOP);
                work.stop();
                works.remove(work);
                robotService.stopRobot(id);
                redisHelper.removeParent(String.valueOf(id));
                redisHelper.remove(Constant.KEY_ROBOT + id);
                return true;
            }
        }
        return false;
    }

    /**
     * 强杀线程
     *
     * @param id
     * @return
     */
    public boolean killWork(int id) {
        for (Work work : works) {
            if (work.getName() == id) {
                Thread thread = commonUtil.getThreadByName(Constant.KEY_THREAD_KLINE + id);
                if (thread == null) {
                    return true;
                } else {
                    //杀掉线程
                    thread.stop();
                    work.setStatus(StopableTask.Status.stoped);
                    robotService.stopRobot(id);
                    redisHelper.removeParent(String.valueOf(id));
                    redisHelper.remove(Constant.KEY_ROBOT + id);
                    return true;
                }
            }
        }

        return false;
    }

    class Work extends StopableTask<Work> {
        NewCoinoneKline kline;

        public Work(NewCoinoneKline kline) {
            super(kline.id);
            this.kline = kline;
        }

        @Override
        public void dowork() {
            Robot robot = redisHelper.getRobot(name);

            if (robot != null && redisHelper.getRobot(name).getStatus() == Constant.KEY_ROBOT_STATUS_RUN) {
                String key = "_exception";
                try {
                    kline.init();
                    //清理发送短信
                    if (redisHelper.getParam(kline.id + key) != null) {
                        redisHelper.removeParent(kline.id + key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw, true));
                    String strs = sw.toString();
                    redisHelper.setParam("Exception_" + kline.id, strs);                    //长时间异常，发送短信给我
                    if (redisHelper.getParam(kline.id + key) == null) {
                        redisHelper.setParam(kline.id + key, String.valueOf(System.currentTimeMillis()));
                    } else if (System.currentTimeMillis() - Long.parseLong(redisHelper.getParam(kline.id + key)) > Constant.KEY_SNS_INTERFACE_ERROR_TIME && redisHelper.getParam(kline.id + key + "_true") == null) {
                        redisHelper.setParam(kline.id + key + "_true", "true");
                        String name = redisHelper.getRobot(kline.id).getName();
                        commonUtil.sendSms(name + "异常机器人停止");
//                        redisHelper.removeParent(kline.id + key);
                    }else if(System.currentTimeMillis() - Long.parseLong(redisHelper.getParam(kline.id + key)) > 30 * 60 * 1000){
                        redisHelper.setParam(kline.id + key, String.valueOf(System.currentTimeMillis()));
                        String name = redisHelper.getRobot(kline.id).getName();
                        commonUtil.sendSms(name + "异常机器人停止");
                    }
                }
            } else {
                killWork(kline.id);
            }
        }
    }
}
