package top.suilian.aio.service;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.ChuangLanSmsUtil;
import top.suilian.aio.Util.CommonUtil;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.*;
import top.suilian.aio.model.request.SmsSendRequest;
import top.suilian.aio.redis.RedisHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DependsOn("beanContext")
public class BaseService {
    //region    Service
    public CancelExceptionService cancelExceptionService = BeanContext.getBean(CancelExceptionService.class);
    public CancelOrderService cancelOrderService = BeanContext.getBean(CancelOrderService.class);
    public ExceptionMessageService exceptionMessageService = BeanContext.getBean(ExceptionMessageService.class);
    public RobotArgsService robotArgsService = BeanContext.getBean(RobotArgsService.class);
    public RobotLogService robotLogService = BeanContext.getBean(RobotLogService.class);
    public RobotService robotService = BeanContext.getBean(RobotService.class);
    public TradeLogService tradeLogService = BeanContext.getBean(TradeLogService.class);
    //endregion org.apache.log4j.Logger

    //region    Utils
    public CommonUtil commonUtil = BeanContext.getBean(CommonUtil.class);
    public HttpUtil httpUtil = BeanContext.getBean(HttpUtil.class);
    public Logger logger = Logger.getLogger(BaseService.class);
    public RedisHelper redisHelper = BeanContext.getBean(RedisHelper.class);
    //endregion

    //region    ??????
    public Map<String, String> exchange = new HashMap<String, String>();
    public int id;
    //endregion

    /**
     * ?????????????????????
     */
    public void setParam() {
        List<RobotArgs> robotArgsList = robotArgsService.findAll(id);
        for (RobotArgs robotArgs : robotArgsList) {
            exchange.put(String.valueOf(robotArgs.getVariable()), robotArgs.getValue());
        }
    }

    /**
     * ?????????????????????
     */
    public void setParam(Integer id) {
        List<RobotArgs> robotArgsList = robotArgsService.findAll(id);
        for (RobotArgs robotArgs : robotArgsList) {
            exchange.put(String.valueOf(robotArgs.getVariable()), robotArgs.getValue());
        }
    }


    public Map<String, String> getParam() {

        return exchange;

    }


    /**
     * ?????????????????????
     */
    public int setRobotStatus(Integer id, int status) {
        return robotService.setRobotStatus(id, status);
    }

    /**
     * ??????????????????
     *
     * @param robotId
     * @param message
     * @param isMobile
     * @return
     */
    public int setExceptionMessage(int robotId, String message, int isMobile) {
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setRobotId(robotId);
        exceptionMessage.setMessage(message);
        exceptionMessage.setIsMobile(isMobile);
        logger.info("setExceptionMessage" + JSON.toJSONString(exceptionMessage));
        return exceptionMessageService.insert(exceptionMessage);
    }

    /**
     * ???????????????
     */
    public int setTradeLog(Integer id, String remark, int status, String color) {
        TradeLog tradeLog = new TradeLog();
        tradeLog.setRobotId(id);
        tradeLog.setRemark(remark);
        tradeLog.setColor(color);
        tradeLog.setStatus(status);
        logger.info("setTradeLog???" + JSON.toJSONString(tradeLog));
        return tradeLogService.insert(tradeLog);
    }

    /**
     * 0.????????????
     * 2.??????
     * 3.api??????
     * 4.??????????????????  ????????????
     * 5.?????????????????????
     * 99.??????
     */
    public int setWarmLog(Integer robotId, int status, String msg, String warmDetailMsg) {
        WarmLog warmLog = new WarmLog();
            warmLog.setRobotId(robotId);
            warmLog.setType(status);
            warmLog.setWarmMsg(msg);
            warmLog.setWarmDetailMsg(warmDetailMsg);
            tradeLogService.insertWarmLog(warmLog);
        logger.info("setWarmLog???" + JSON.toJSONString(warmLog));
        return 0;
    }

    public int setTradeLog(Integer id, String remark, int status) {
        TradeLog tradeLog = new TradeLog();
        tradeLog.setRobotId(id);
        tradeLog.setRemark(remark);
        tradeLog.setColor("000000");
        tradeLog.setStatus(status);
        logger.info("setTradeLog??? " + JSON.toJSONString(tradeLog));
        return tradeLogService.insert(tradeLog);
    }

    /**
     * ?????????????????????
     */
    public int setRobotArgs(Integer robotId, String variable, String value) {
        return robotArgsService.update(robotId, variable, value);
    }

    /**
     * ?????????????????????
     */
    public String getRobotName(Integer id) {
        Robot robot = robotService.findById(id);
        return robot.getName();
    }

    /**
     * ??????????????????
     *
     * @param robotId
     * @param orderId
     * @param cancelNum
     * @param type
     * @param isMobile
     * @param status
     * @param remark
     * @return
     */
    public int insertCancel(Integer robotId, String orderId, Integer cancelNum, Integer type, Integer isMobile, Integer status, String remark, Integer exchangeId) {
        Integer cancelOrderId = insertCancelOrder(robotId, orderId, cancelNum, type, isMobile, status, exchangeId);
        return insertCancelException(cancelOrderId, remark);
    }

    /**
     * ??????????????????
     *
     * @param robotId
     * @param orderId
     * @param cancelNum
     * @param type
     * @param isMobile
     * @param status
     * @return
     */
    public int insertCancelOrder(Integer robotId, String orderId, Integer cancelNum, Integer type, Integer isMobile, Integer status, Integer exchangeId) {
        //?????? ???orderId???????????????

        CancelOrder cancelOrder;

        cancelOrder = cancelOrderService.findbyOrderId(robotId, orderId);

        if (cancelOrder == null) {
            cancelOrder = new CancelOrder();
            cancelOrder.setRobotId(robotId);
            cancelOrder.setOrderId(orderId);
            cancelOrder.setExchange(exchangeId);
            cancelOrder.setCancelNum(cancelNum);
            cancelOrder.setType(type);
            cancelOrder.setIsMobile(isMobile);
            cancelOrder.setStatus(status);
            cancelOrderService.insert(cancelOrder);
        } else {
            cancelOrder.setStatus(status);
            cancelOrderService.update(cancelOrder);
        }

        return cancelOrder.getCancelOrderId();
    }


    /**
     * ??????????????????
     *
     * @param cancelOrderId
     * @param remark
     * @return
     */
    public int insertCancelException(Integer cancelOrderId, String remark) {
        CancelException cancelException = new CancelException();
        cancelException.setCancelOrderId(cancelOrderId);
        if (remark == null) {
            remark = "null";
        }
        cancelException.setRemark(remark);
        return cancelExceptionService.insert(cancelException);
    }

    /**
     * ???????????????
     */
    public String robotArg(Integer id, String variable) {
        RobotArgs robotArg = robotArgsService.findOne(id, variable);
        System.out.println(robotArg);
        if (robotArg != null) {
            return robotArg.getValue();
        }
        return null;
    }


    public void clearLog() {
        //????????????????????????
        String time = redisHelper.getClearLogParam(Constant.KEY_ROBOT_CLEAR_LOG + id);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long start = new Date().getTime() - Constant.KEY_CLEAR_LOG;
        RobotArgs robotArgs = robotArgsService.findOne(id, "market");
        String coins = robotArgs.getRemark();
        if (time == null) {
            redisHelper.setClearLogParam(Constant.KEY_ROBOT_CLEAR_LOG + id, coins);
        } else {
            //?????? redis????????????
            long lastTime = redisHelper.getLastTime(Constant.KEY_ROBOT_CLEAR_LOG + id);
            if (System.currentTimeMillis() - lastTime > Constant.KEY_CLEAR_START_LOG) {
                String startTime = sd.format(start);
                tradeLogService.deletedByTime(id, startTime);
                //?????? KEY_ROBOT_CLEAR_LOG
                redisHelper.setClearLogParam(Constant.KEY_ROBOT_CLEAR_LOG + id, coins);
            }
        }
    }


    /**
     * ??????????????????
     *
     * @param robotId
     * @param orderId
     * @param status
     * @param remark
     * @return
     */
    public int addCancelNum(Integer robotId, String orderId, Integer status, String remark) {
        CancelOrder cancelOrder = cancelOrderService.findbyOrderId(robotId, orderId);
        Integer requestNum = cancelOrder.getCancelNum();
        cancelOrder.setStatus(status);
        cancelOrder.setCancelNum(requestNum + 1);
        cancelOrderService.update(cancelOrder);
        if (requestNum + 1 == 4) {
            sendSms(robotId + "?????????:" + orderId + "????????????");
        }
        int cancelOrderId = cancelOrder.getCancelOrderId();
        return insertCancelException(cancelOrderId, remark);
    }

    public void delCancaled() {

    }


    /**
     * ?????????????????????
     */
    public BigDecimal nN(BigDecimal doubleValue, int newScale) {
        if (newScale == 0) {
            return new BigDecimal(Integer.valueOf(doubleValue.intValue()));
        } else {
            String pattern = "#.";
            for (int i = 0; i < newScale; i++) {
                pattern += "0";
            }
            try {
                DecimalFormat df = new DecimalFormat(pattern);
                df.setRoundingMode(RoundingMode.DOWN);
                String result = df.format(doubleValue);
                String index = result.substring(0, 1);
                if (".".equals(index)) {
                    result = "0" + result;
                }
                int inde = firstIndexOf(result, ".");
                return new BigDecimal(result.substring(0, inde + newScale + 1));
            } catch (Exception e) {
                int inde = firstIndexOf(String.valueOf(doubleValue), ".");
                int cnt = String.valueOf(doubleValue).length() - 1;
                int cnt1 = inde + newScale + 1;
                if (cnt < cnt1) {
                    cnt1 = cnt;
                }
                return new BigDecimal(String.valueOf(doubleValue).substring(0, cnt1));
            }
        }
    }

    /**
     * ???????????????pattern???str???????????????????????????
     *
     * @param str
     * @param pattern
     * @return
     */
    public static int firstIndexOf(String str, String pattern) {
        for (int i = 0; i < (str.length() - pattern.length()); i++) {
            int j = 0;
            while (j < pattern.length()) {
                if (str.charAt(i + j) != pattern.charAt(j))
                    break;
                j++;
            }
            if (j == pattern.length())
                return i;
        }
        return -1;
    }


    /**
     * ????????????
     *
     * @param msg
     * @param phone return JSONObject
     */
    public static JSONObject sendSms(String msg, String phone) {
        SmsSendRequest smsSingleRequest = new SmsSendRequest(msg, phone);
        String requestJson = JSON.toJSONString(smsSingleRequest);
        String response = ChuangLanSmsUtil.sendSmsByPost(requestJson);
        JSONObject responseObj = JSONObject.fromObject(response);
        sendSms(msg);
        return responseObj;
    }


    /**
     * ???????????????????????????
     *
     * @param msg
     */
    public static void sendSms(String msg) {

    }


    /**
     * ????????????????????????
     */
    public static String collectExceptionStackMsg(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        String strs = sw.toString();
        return strs;
    }

    /**
     * ??????????????????????????? ??????5????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param res
     * @param code
     * @param action
     * @return
     */
    public JSONObject judgeRes(String res, String code, String action) {
//        logger.info("log?????????" + res.replace("\t", "").replace("\n", ""));
        if (StringUtils.isNotEmpty(res)) {
            JSONObject resJson = JSONObject.fromObject(res);
            return resJson;
        }
        return null;
    }

    /**
     * ????????????????????????json string
     *
     * @param string
     * @return
     */
    private boolean isjson(String string) {
        try {
            JSONObject jsonStr = JSONObject.fromObject(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * ????????????
     *
     * @param isMobileSwitch
     * @param message
     * @param mobile
     * @param type
     * @return
     */
    public void judgeSendMessage(int isMobileSwitch, String message, String mobile, int type) {

    }

    /**
     * ????????????????????????
     *
     * @param type
     */
    public void removeSmsRedis(int type) {
        String key = null;
        switch (type) {
            case Constant.KEY_SMS_SMALL_INTERVAL:       //????????????
                key = id + "_smallInterval";
                break;
            case Constant.KEY_SMS_INSUFFICIENT:        //????????????
                key = id + "_insufficient";
                break;
            case Constant.KEY_SMS_INTERFACE_ERROR:      //??????????????????
                key = id + "_error";
                break;
        }
        if (redisHelper.getParam(key) != null) {
            redisHelper.removeParent(key);
        }
    }

    /**
     * ????????????  ??????
     *
     * @param ms
     * @param isMobileSwitch
     */
    public void sleep(Integer ms, int isMobileSwitch) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            setExceptionMessage(id, collectExceptionStackMsg(e), isMobileSwitch);
        }
    }

    /**
     * ????????????????????????
     *
     * @param name
     * @param id
     * @return
     */
    public Logger getLogger(String name, Integer id) {
        Logger logger = Logger.getLogger(id.toString());
        logger.removeAllAppenders();
        PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss,SSS} %5p %c{1}:%L - %m%n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String filePath = "./logs/" + name + "/";
        ThreadFileAppender fileAppender = null;
        try {
            fileAppender = new ThreadFileAppender(layout, filePath, id.toString(), "'.'yyyy-MM-dd");
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileAppender.setAppend(true);
        fileAppender.setImmediateFlush(true);
        fileAppender.setThreshold(Level.INFO);

        // ?????????logger
        logger.setLevel(Level.INFO);
        logger.addAppender(fileAppender);

        return logger;
    }

    public static class ThreadFileAppender extends DailyRollingFileAppender {
        public ThreadFileAppender(Layout layout, String filePath, String fileName, String datePattern)
                throws IOException {
            super(layout, filePath + fileName + ".log", datePattern);
        }
    }


    /**
     * ????????????BigDecimal??????????????????
     *
     * @param max
     * @param min
     * @param newScale
     * @return
     */
    public BigDecimal getRandomBigDecimal(BigDecimal max, BigDecimal min, Integer newScale) {
        return nN(((max.subtract(min)).multiply(BigDecimal.valueOf(Math.random()))).add(min), newScale);
    }
}
