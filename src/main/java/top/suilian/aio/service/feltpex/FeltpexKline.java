package top.suilian.aio.service.feltpex;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class FeltpexKline extends FeltpexParentService {
    public FeltpexKline(
            CancelExceptionService cancelExceptionService,
            CancelOrderService cancelOrderService,
            ExceptionMessageService exceptionMessageService,
            RobotArgsService robotArgsService,
            RobotLogService robotLogService,
            RobotService robotService,
            TradeLogService tradeLogService,
            HttpUtil httpUtil,
            RedisHelper redisHelper,
            int id
    ) {
        super.cancelExceptionService = cancelExceptionService;
        super.cancelOrderService = cancelOrderService;
        super.exceptionMessageService = exceptionMessageService;
        super.robotArgsService = robotArgsService;
        super.robotLogService = robotLogService;
        super.robotService = robotService;
        super.tradeLogService = tradeLogService;
        super.httpUtil = httpUtil;
        super.redisHelper = redisHelper;
        super.id = id;
        super.logger = getLogger("felt/newkline", id);
    }

    private BigDecimal intervalAmount = BigDecimal.ZERO;
    private BigDecimal buyPrice = BigDecimal.ZERO;
    private BigDecimal sellPrice = BigDecimal.ZERO;
    private String orderIdOne = "0";
    private String orderIdTwo = "0";
    private String orderIdOnetradeNo = "0";
    private String orderIdTwotradeNo = "0";
    private String sleepOrder = "0";
    private String sleepOrdertradeNo = "0";
    private boolean start = true;
    private int orderNum = 0;
    private int runTime = 0;
    private int randomNum = 1;

    private String sellOrderId = "0";
    private String buyOrderId = "0";
    private String sellOrderIdtradeNo = "0";
    private String buyOrderIdtradeNo = "0";
    private int eatOrder = 0;//吃单数量
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    long ordersleeptime = System.currentTimeMillis();


    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            setTransactionRatio();
//
            logger.info("设置机器人参数结束");
            //判断走K线的方式
            if ("1".equals(exchange.get("sheetForm"))) {
                //新版本
                while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                    randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
                }
                timeSlot = Integer.parseInt(exchange.get("timeSlot"));
            }

            maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
            start = false;
        }
        int index = new Date().getHours();
        //获取当前小时内的单量百分比
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("当前时间段单量百分比：" + transactionRatio);
        if (runTime < timeSlot) {
            String trades = getDepth();
            //获取深度 判断平台撮合是否成功
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

            if (tradesObj != null && tradesObj.getInteger("code") == 1) {
                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");

                BigDecimal buyPri = data.getJSONArray("buyQueue").getJSONObject(0).getBigDecimal("price");
                BigDecimal sellPri =  data.getJSONArray("sellQueue").getJSONObject(data.getJSONArray("sellQueue").size()-1).getBigDecimal("price");

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }


            if (!"0".equals(sellOrderId)) {
                selectOrderDetail(sellOrderId, 0, sellOrderIdtradeNo);
                sellOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0, buyOrderIdtradeNo);
                buyOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(sleepOrder)) {
                selectOrderDetail(sleepOrder, 2, sleepOrdertradeNo);
                sleepOrder = "0";
            }

            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {

                selectOrderDetail(orderIdOne, 1, orderIdOnetradeNo);

                orderIdOne = "0";
                selectOrderDetail(orderIdTwo, 1, orderIdTwotradeNo);
                orderIdTwo = "0";

                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //撞单达上限后的操作
                    //停止机器人
                    if ("0".equals(exchange.get("orderOperation"))) {

                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                        setWarmLog(id, 2, "撤单数达到上限，停止量化", "");
                        msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//不停止

                        setTradeLog(id, "撤单数次数过多，请注意盘口", 0, "000000");
                        setWarmLog(id, 2, "撤单数次数过多，请注意盘口", "");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，请注意盘口!";
                        //judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        orderNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//随机暂停后重启
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        setTradeLog(id, "撤单数次数过多，将暂停" + st + "秒后自动恢复", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，将暂停片刻后自动恢复!";
                        setWarmLog(id, 2, "撤单数次数过多，将暂停" + st + "秒后自动恢复", "");
                        //重置撞单次数
                        orderNum = 0;
                        //暂停
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        return;
                    }

                }
            }
            setTradeLog(id, "撤单数为" + orderNum, 0, "000000");

            if (Integer.parseInt(exchange.get("orderSumSwitch")) == 1) {    //防褥羊毛开关
                setTradeLog(id, "停止量化撤单数设置为：" + exchange.get("orderSum"), 0, "000000");
            }
            BigDecimal price = getRandomPrice();
            if (price == null) {
                orderIdOne = "0";
                orderIdTwo = "0";
                return;
            }

            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            Double minNum = Double.valueOf(exchange.get("numMinThreshold"));
            long max = (long) (numThreshold1 * Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision"))))));
            BigDecimal num = oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(挂单数量)：" + num);

            int type = RandomUtils.nextInt(10) > Double.parseDouble(exchange.get("tradeRatio")) * 10 ? 1 : -1;
            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && jsonObject.getInt("code") == 1) {
                    orderIdOne = jsonObject.getJSONObject("data").getString("orderId");
                    String resultJson1 = submitTrade(type == 1 ? -1 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 1) {
                        orderIdTwo =jsonObject1.getJSONObject("data").getString("orderId");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                        ordersleeptime = System.currentTimeMillis();
                    } else {
                        String res = cancelTrade(orderIdOne);
                        setTradeLog(id, "撤单[" + orderIdOne + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }
                }
            } catch (Exception e) {
                if (!orderIdOne.equals("0")) {
                    String res = cancelTrade(orderIdOne);
                    setTradeLog(id, "撤单[" + orderIdOne + "]=> " + res, 0, "000000");
                    logger.info("撤单" + orderIdOne + ":结果" + res);
                }
                if (!orderIdTwo.equals("0")) {
                    String res = cancelTrade(orderIdTwo);
                    setTradeLog(id, "撤单[" + orderIdTwo + "]=> " + res, 0, "000000");
                    logger.info("撤单" + orderIdTwo + ":结果" + res);
                }
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
            if ("1".equals(exchange.get("sheetForm"))) {
                runTime += (st);
                setTradeLog(id, "累计周期时间----------------------------->" + runTime + "秒", 1);
            }

            sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
        } else {
            runTime = 0;
            List<Integer> string = new ArrayList<>();
            string.add(1);
            string.add(2);
            Integer value = RandomUtils.nextInt(3);
            switch (value) {
                case 0:
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                    } else {
                        randomNum -= 1;
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                    } else {
                        randomNum += 1;
                    }
                    break;
                default:
                    break;
            }
        }
//            setBalanceRedis();
            clearLog();
            logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
        }


        /**
         * 获得随机价格
         *
         * @return
         */
        public BigDecimal getRandomPrice () throws UnsupportedEncodingException {
            BigDecimal price = null;

            String trades = getDepth();
            //获取深度 判断平台撮合是否成功
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

            if (tradesObj != null && tradesObj.getInteger("code") == 1) {

                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");

                BigDecimal buyPri = data.getJSONArray("buyQueue").getJSONObject(0).getBigDecimal("price");
                BigDecimal sellPri = data.getJSONArray("sellQueue").getJSONObject(data.getJSONArray("sellQueue").size() - 1).getBigDecimal("price");

                BigDecimal subtract = sellPri.subtract(buyPri);
                BigDecimal intervalPrice = sellPri.subtract(buyPri);

                logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
                logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);

                //判断盘口买卖检测开关是否开启
                int openIntervalFromPrice = new BigDecimal(exchange.get("openIntervalFromPrice")).compareTo(intervalPrice);
                if ("1".equals(exchange.get("isOpenIntervalSwitch")) && openIntervalFromPrice < 0) {
                    setTradeLog(id, "盘口空间过小", 0, "000000");
                    setWarmLog(id, 2, "盘口空间过小,降低挂单频率", "");
                    int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
                    sleep(st * 1000 + 60 * 1000 * 2, Integer.parseInt(exchange.get("isMobileSwitch")));

                }

                if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                    buyPrice = buyPri;
                    sellPrice = sellPri;
                }


                BigDecimal disparity = sellPrice.subtract(buyPrice);
                logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
                logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


                Integer newScale = Integer.parseInt(exchange.get("pricePrecision").toString());

                logger.info("robotId" + id + "----" + "等份数：" + exchange.get("priceRange"));
                logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);
                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);

                logger.info("robotId" + id + "----" + "区间值：" + interval);


                if ("0".equals(exchange.get("sheetForm"))) {
                    logger.info("robotId" + id + "----" + "旧版本开始");
                    BigDecimal diff = sellPri.subtract(buyPri);
                    BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
                    int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
                    BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, ROUND_DOWN);
                    BigDecimal oldPrice = buyPri.add(random);
                    price = nN(oldPrice, newScale);
                    logger.info("robotId" + id + "----" + "随机增长------->" + random);
                    logger.info("robotId" + id + "----" + "小数位未处理的新价格------->" + oldPrice);
                    logger.info("robotId" + id + "----" + "小数位已处理的新价格------->" + price);
                    if (price.compareTo(sellPri) < 0 && price.compareTo(buyPri) > 0) {
                        logger.info("robotId" + id + "----" + "旧版本结束");
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                    } else {
                        setWarmLog(id, 4, "买一卖一区间过小，无法量化卖1[" + sellPri + "]买1[" + buyPri + "]", "");
                        setTradeLog(id, "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]", 0, "FF111A");
                        logger.info("robotId" + id + "----" + "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]");
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                        logger.info("robotId" + id + "----" + "旧版本回调获取价格");
                        price = null;
                    }
                } else {
                    logger.info("robotId" + id + "----" + "新版本开始");
                    BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                    BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                    logger.info("robotId" + id + "----" + "minPrice(区间最小价格)：" + minPrice + "，maxPrice(区间最大价格)：" + maxPrice);
                    BigDecimal diff = maxPrice.subtract(minPrice);
                    BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                    logger.info("robotId" + id + "----" + "random(随机增长)：" + random);

                    price = nN(minPrice.add(random), newScale);
                    logger.info("robotId" + id + "----" + "price(新价格)：" + price);

                    if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                        logger.info("robotId" + id + "----" + "新版本结束");
                    } else {
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        logger.info("robotId" + id + "----" + "新版本回调获取价格");
                        price = null;
                    }
                }


            } else {

                logger.info("异常回调获取价格 trades(深度接口返回)=>" + trades);
                sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                price = null;
            }
            removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);
            return price;
        }

        public void selectOrderDetail (String orderId,int type, String orderIdtradeNo){
            try {
                String str = selectOrder(orderId);
                JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                if (jsonObject != null && jsonObject.getInt("code") == 1) {

                    int status = jsonObject.getJSONObject("data").getInt("status");
                    if (status == 3) {
                        setTradeLog(id, "订单id：" + orderId + "完全成交", 0, "#67c23a");
                    } else if (status == 0) {
                        setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "#67c23a");
                    } else {
                        String res = cancelTrade(orderId);
                        JSONObject cancelRes = judgeRes(res, "error_code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "撤单[" + orderId + "]=>" + res, 0, "#67c23a");
                        if (Integer.parseInt(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //防褥羊毛开关
                            orderNum++;
                            setWarmLog(id, 2, "订单{" + orderId + "}撤单,撞单数为" + orderNum, "");
                        }
                    }


                }
            } catch (Exception e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                e.printStackTrace();
            }
        }

        public static void main (String[]args){
            for (; ; ) {

                int type = RandomUtils.nextInt(10) > Double.parseDouble("0.8") * 10 ? 1 : -1;
                System.out.println(type);
            }

        }
    }