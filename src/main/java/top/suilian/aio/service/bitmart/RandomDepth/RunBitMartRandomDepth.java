package top.suilian.aio.service.bitmart.RandomDepth;

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
public class RunBitMartRandomDepth {
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
        BitMartRandomDepth randomDepth = new BitMartRandomDepth(cancelExceptionService, cancelOrderService, exceptionMessageService, robotArgsService, robotLogService, robotService, tradeLogService, httpUtil, redisHelper, id);
        redisHelper.initRobot(id);
        work = new Work(randomDepth);
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
        BitMartRandomDepth randomDepth;

        public Work(BitMartRandomDepth randomDepth) {
            super(randomDepth.id);
            this.randomDepth = randomDepth;
        }

        @Override
        public void dowork() {
            Robot robot = redisHelper.getRobot(name);
            if (robot != null && redisHelper.getRobot(name).getStatus() == Constant.KEY_ROBOT_STATUS_RUN) {
                String key = "_exception";
                try {
                    randomDepth.init();
                    //清理发送短信
                    if (redisHelper.getParam(randomDepth.id + key) != null) {
                        redisHelper.removeParent(randomDepth.id + key);
                    }
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw, true));
                    String strs = sw.toString();
                    redisHelper.setParam("Exception_" + randomDepth.id, strs);                    //长时间异常，发送短信给我
                    if (redisHelper.getParam(randomDepth.id + key) == null) {
                        redisHelper.setParam(randomDepth.id + key, String.valueOf(System.currentTimeMillis()));
                    } else if (System.currentTimeMillis() - Long.valueOf(redisHelper.getParam(randomDepth.id + key)) > Constant.KEY_SNS_INTERFACE_ERROR_TIME) {
                        redisHelper.setParam(randomDepth.id + key + "_true", "true");
                        commonUtil.sendSms(redisHelper.getRobot(randomDepth.id).getName() + "异常机器人停止");
                        redisHelper.removeParent(randomDepth.id+key);
                    }
                }
            } else {
                killWork(randomDepth.id);
            }
        }
    }
}
