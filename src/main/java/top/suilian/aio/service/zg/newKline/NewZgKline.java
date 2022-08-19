package top.suilian.aio.service.zg.newKline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.zg.ZGParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class NewZgKline extends ZGParentService {
    public NewZgKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_ZG_KLINE, id);
    }

    private BigDecimal intervalAmount = BigDecimal.ZERO;
    private BigDecimal buyPrice = BigDecimal.ZERO;
    private BigDecimal sellPrice = BigDecimal.ZERO;
    private String orderIdOne = "0";
    private String orderIdTwo = "0";


    private BigDecimal orderOneAmount = BigDecimal.ZERO;
    private BigDecimal orderTwoAmount = BigDecimal.ZERO;
    private BigDecimal buyAmounts = BigDecimal.ZERO;
    private BigDecimal sellAmounts = BigDecimal.ZERO;


    private boolean start = true;
    private int orderNum = 0;
    private int runTime = 0;
    private int randomNum = 1;
    private String sellOrderId = "0";
    private String buyOrderId = "0";
    private int eatOrder = 0;//吃单数量
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    private BigDecimal tradeRatio = new BigDecimal(5);


    public void init() {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            setTransactionRatio();
            logger.info("设置买卖单比例");
            if (exchange.get("tradeRatio") != null || !"0".equals(exchange.get("tradeRatio"))) {
                Double ratio = 10 * (1 / (1 + Double.valueOf(exchange.get("tradeRatio"))));
                tradeRatio = new BigDecimal(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            logger.info("设置机器人参数结束");
            logger.info("设置机器人交易规则开始");
            if (!setPrecision()) {
                return;
            }
            logger.info("设置机器人交易规则结束");
            if ("1".equals(exchange.get("isdeepRobot"))) {
                logger.info("深度机器人交易开始");
                runZGDepth.init(id + 1);
            }
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

        int index = Integer.valueOf(new Date().getHours());
        //获取当前小时内的单量百分比
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("当前时间段单量百分比：" + transactionRatio);

        if (runTime < timeSlot) {


            //获取深度 判断平台撮合是否成功
            String trades = httpUtil.get(baseUrl + "/depth?symbol=" + exchange.get("market") + "&size=5");
            JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

            if (trades != null && !trades.isEmpty() && tradesObj != null) {

                List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

                BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0));
                BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0));

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }


            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {


                try {
                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String str = selectOrder(orderIdOne);
                    JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                    if (jsonObject != null && jsonObject.getInt("code") == 0) {


                        if (jsonObject != null && jsonObject.getInt("code") == 0) {
                            BigDecimal oneAmount = BigDecimal.ZERO;
                            JSONObject jsonArray = jsonObject.getJSONObject("result");

                            String records = jsonArray.getString("records");

                            if (records == null || records.equals("null")) {
                                //订单未成交  ---- 撤单
                                String res = cancelTrade(orderIdOne);
                                JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                setCancelOrder(cancelRes, res, orderIdOne + "_" + orderOneAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                setTradeLog(id, "撤单[" + orderIdOne + "]=>" + res, 0, "000000");
                                int code = cancelRes.getInt("code");
                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                    orderNum++;
                                }
                            } else {
                                JSONArray recordsArray = JSONArray.fromObject(records);
                                for (int i = 0; i < recordsArray.size(); i++) {
                                    System.out.println(recordsArray.size() + "长度:1" + i);
                                    JSONObject everyOrder = recordsArray.getJSONObject(i);
                                    BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                                    oneAmount = oneAmount.add(everyOrderAmount);
                                }

                                logger.info("robotId" + id + "----订单1挂单数量:" + orderOneAmount);
                                logger.info("robotId" + id + "----订单1成交数量:" + oneAmount);

                                int result = orderOneAmount.compareTo(oneAmount);

                                if (result == 0) {
                                    setTradeLog(id, "订单id：" + orderIdOne + "完全成交", 0, "000000");
                                } else {
                                    String res = cancelTrade(orderIdOne);
                                    JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                    setCancelOrder(cancelRes, res, orderIdOne + "_" + orderOneAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                    setTradeLog(id, "撤单[" + orderIdOne + "]=>" + res, 0, "000000");
                                    int code = cancelRes.getInt("code");

                                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                        orderNum++;
                                    }
                                }

                            }


                        }

                    } else {
                        String res = cancelTrade(orderIdOne);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderIdOne + "_" + orderOneAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "查询订单失败撤单[" + orderIdOne + "]=>" + res, 0, "000000");
                        int code = cancelRes.getInt("code");
                    }
                } catch (UnsupportedEncodingException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                    e.printStackTrace();
                }

                orderIdOne = "0";
                try {
                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String str = selectOrder(orderIdTwo);
                    JSONObject jsonObject = judgeRes(str, "code", "selectOrder");

                    if (jsonObject != null && jsonObject.getInt("code") == 0) {

                        BigDecimal twoAmount = BigDecimal.ZERO;


                        JSONObject jsonArray = jsonObject.getJSONObject("result");

                        String records = jsonArray.getString("records");

                        if (records == null || records.equals("null")) {
                            String res = cancelTrade(orderIdTwo);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                            setCancelOrder(cancelRes, res, orderIdTwo + "_" + orderTwoAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
                            int code = cancelRes.getInt("code");

                            if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                orderNum++;
                            }

                        } else {
                            JSONArray recordsArray = JSONArray.fromObject(records);

                            for (int i = 0; i < recordsArray.size(); i++) {
                                System.out.println(recordsArray.size() + "长度:2");

                                JSONObject everyOrder = recordsArray.getJSONObject(i);
                                BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                                twoAmount = twoAmount.add(everyOrderAmount);
                            }

                            logger.info("robotId" + id + "----订单2挂单数量:" + orderTwoAmount);
                            logger.info("robotId" + id + "----订单2成交数量:" + twoAmount);
                            int result = orderTwoAmount.compareTo(twoAmount);


                            if (result == 0) {
                                setTradeLog(id, "订单id：" + orderIdTwo + "完全成交", 0, "000000");
                            } else {
                                String res = cancelTrade(orderIdTwo);
                                JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                setCancelOrder(cancelRes, res, orderIdTwo + "_" + orderTwoAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
                                int code = cancelRes.getInt("code");

                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                    orderNum++;
                                }
                            }
                        }


                    } else {
                        String res = cancelTrade(orderIdTwo);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderIdTwo + "_" + orderTwoAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "查询订单失败撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
                        int code = cancelRes.getInt("code");
                    }
                } catch (UnsupportedEncodingException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + exceptionMessage);
                    e.printStackTrace();
                }

                orderIdTwo = "0";

                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //撞单达上限后的操作
                    //停止机器人
                    if ("0".equals(exchange.get("orderOperation"))) {
                        setWarmLog(id,2,"撤单数达到上限，停止量化","");
                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//不停止
                        setWarmLog(id,2,"撤单数次数过多，请注意盘口","");
                        setTradeLog(id, "撤单数次数过多，请注意盘口", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，请注意盘口!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        randomNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//随机暂停后重启
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        setWarmLog(id,2,"撤单数次数过多，将暂停\" + st + \"秒后自动恢复","");
                        setTradeLog(id, "撤单数次数过多，将暂停" + st + "秒后自动恢复", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，将暂停片刻后自动恢复!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        randomNum = 0;
                        //暂停
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        return;
                    }

                }
            }

            if (!buyOrderId.equals("0")) {
                int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
                try {
                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String str = selectOrder(buyOrderId);
                    JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                    if (jsonObject != null && jsonObject.getInt("code") == 0) {


                        if (jsonObject != null && jsonObject.getInt("code") == 0) {
                            BigDecimal buyAmount = BigDecimal.ZERO;
                            JSONObject jsonArray = jsonObject.getJSONObject("result");

                            String records = jsonArray.getString("records");

                            if (records == null || records.equals("null")) {
                                //订单未成交  ---- 撤单
                                String res = cancelTrade(buyOrderId);
                                JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                setCancelOrder(cancelRes, res, buyOrderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                // setTradeLog(id, "撤单[" + buyOrderId + "]=>" + res, 0, "000000");
                                int code = cancelRes.getInt("code");
                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                    orderNum++;
                                }
                            } else {
                                JSONArray recordsArray = JSONArray.fromObject(records);
                                for (int i = 0; i < recordsArray.size(); i++) {
                                    System.out.println(recordsArray.size() + "长度:1" + i);
                                    JSONObject everyOrder = recordsArray.getJSONObject(i);
                                    BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                                    buyAmount = buyAmount.add(everyOrderAmount);
                                }

                                logger.info("robotId" + id + "----吃买单挂单数量:" + buyAmounts);
                                logger.info("robotId" + id + "-----吃买单成交数量:" + buyAmount);

                                int result = buyAmounts.compareTo(buyAmount);

                                if (result == 0) {
                                    setTradeLog(id, "订单id：" + buyOrderId + "吃买单完全成交", 0, "000000");
                                } else {
                                    String res = cancelTrade(buyOrderId);
                                    JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                    setCancelOrder(cancelRes, res, buyOrderId + "_" + orderOneAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                    setTradeLog(id, "撤单[" + buyOrderId + "]=>" + res, 0, "000000");
                                    int code = cancelRes.getInt("code");
                                }

                            }


                        }

                    }
                } catch (UnsupportedEncodingException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                    e.printStackTrace();
                } finally {
                    buyOrderId = "0";
                }

            }

            if (!sellOrderId.equals("0")) {
                int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
                try {
                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String str = selectOrder(sellOrderId);
                    JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                    if (jsonObject != null && jsonObject.getInt("code") == 0) {


                        if (jsonObject != null && jsonObject.getInt("code") == 0) {
                            BigDecimal sellAmount = BigDecimal.ZERO;
                            JSONObject jsonArray = jsonObject.getJSONObject("result");

                            String records = jsonArray.getString("records");

                            if (records == null || records.equals("null")) {
                                //订单未成交  ---- 撤单
                                String res = cancelTrade(sellOrderId);
                                JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                setCancelOrder(cancelRes, res, sellOrderId + "_" + orderOneAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                setTradeLog(id, "撤单[" + sellOrderId + "]=>" + res, 0, "000000");
                                int code = cancelRes.getInt("code");
                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                    orderNum++;
                                }
                            } else {
                                JSONArray recordsArray = JSONArray.fromObject(records);
                                for (int i = 0; i < recordsArray.size(); i++) {
                                    System.out.println(recordsArray.size() + "长度:1" + i);
                                    JSONObject everyOrder = recordsArray.getJSONObject(i);
                                    BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                                    sellAmount = sellAmount.add(everyOrderAmount);
                                }

                                logger.info("robotId" + id + "----吃卖单挂单数量:" + sellAmounts);
                                logger.info("robotId" + id + "-----吃卖单成交数量:" + sellAmount);

                                int result = sellAmounts.compareTo(sellAmounts);

                                if (result == 0) {
                                    setTradeLog(id, "订单id：" + orderIdOne + "吃卖单完全成交", 0, "000000");
                                } else {
                                    String res = cancelTrade(sellOrderId);
                                    JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                    setCancelOrder(cancelRes, res, sellOrderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                    // setTradeLog(id, "撤单[" + sellOrderId + "]=>" + res, 0, "000000");
                                    int code = cancelRes.getInt("code");

                                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && code == 0) {    //防褥羊毛开关
                                        orderNum++;
                                    }
                                }

                            }


                        }

                    }
                } catch (UnsupportedEncodingException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                    e.printStackTrace();
                } finally {
                    sellOrderId = "0";
                }


            }

            setTradeLog(id, "撤单数为" + orderNum, 0, "000000");
            if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //防褥羊毛开关
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
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
            BigDecimal num = oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(挂单数量)：" + num);
            BigDecimal random=new BigDecimal(Math.random() * 10 ).setScale(2,BigDecimal.ROUND_HALF_UP);

            int type =random.compareTo(tradeRatio)<0 ? 2:1;

            try {
                String resultJson = submitTrade(type, price, num);


                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                    String tradeId = jsonObject.getJSONObject("result").getString("id");
                    orderIdOne = tradeId;
                    orderOneAmount = num;
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 0) {
                        orderIdTwo = jsonObject1.getJSONObject("result").getString("id");
                        orderTwoAmount = num;
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else {
                        String res = cancelTrade(tradeId);
                        setTradeLog(id, "量化撤单[" + tradeId + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId + "_" + num, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);

                    }


                }


            } catch (UnsupportedEncodingException e) {
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
            Integer value = string.get((int) Math.round(Math.random() * (string.size() - 1)));
            switch (value) {
                case 0:
                 //   setTradeLog(id, "当前随机值（" + value + ":横盘）", 1);
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                    //    setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    } else {
                        randomNum -= 1;
                  //      setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                   //     setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    } else {
                        randomNum += 1;
                  //      setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    }
                    break;
            }
        }

        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }


    /**
     * 获得随机价格
     *
     * @return
     */
    public BigDecimal getRandomPrice() {
        BigDecimal price = null;
        String trades = httpUtil.get(baseUrl + "/depth?symbol=" + exchange.get("market") + "&size=5");
        JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {

            List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

            BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0));
            BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
            logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);

            //判断盘口买卖检测开关是否开启
            if ("1".equals(exchange.get("isTradeCheck"))) {
                //吃堵盘口的订单
                BigDecimal buyAmount = new BigDecimal(buyPrices.get(0).get(1).toString());
                BigDecimal sellAmount = new BigDecimal(sellPrices.get(0).get(1).toString());
                BigDecimal minAmount = new BigDecimal(precision.get("minTradeLimit").toString());

                if (maxEatOrder == 0) {
                    logger.info("吃单上限功能未开启");
                } else if (maxEatOrder <= eatOrder) {
                    setWarmLog(id,2,"吃单上限，停止吃单","");
                    setTradeLog(id, "已吃堵盘口单总数(" + eatOrder + ")=吃单成交上限数(" + maxEatOrder + "),吃单上限，停止吃单", 0);
                }

                //吃买单
                if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && "1".equals(exchange.get("isBuyMinLimitAmount"))) {
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            if (buyAmount.compareTo(minAmount) == -1) {
                                buyAmount = minAmount;
                            }
                            try {
                                String sellOrder = submitTrade(1, buyPri, buyAmount);
                                setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                                logger.info("堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]");

                                JSONObject jsonObject = judgeRes(sellOrder, "code", "submitTrade");
                                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                                    buyOrderId = jsonObject.getJSONObject("result").getString("id");
                                    buyAmounts = buyAmount;
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        setWarmLog(id,2,"出现疑似堵盘口订单，停止量化","");
                        setTradeLog(id, "出现疑似堵盘口订单，停止量化", 0, "000000");
                        String msg = "出现疑似堵盘口订单，您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }
                }
                //吃卖单
                if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && "1".equals(exchange.get("isSellMinLimitAmount"))) {
                    //判断出现堵盘口单时的操作,是停止还是吃
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            if (sellAmount.compareTo(minAmount) == -1) {
                                sellAmount = minAmount;
                            }
                            try {
                                String buyOrder = submitTrade(2, sellPri, sellAmount);
                                setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                                logger.info("堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]");

                                JSONObject jsonObject = judgeRes(buyOrder, "code", "submitTrade");
                                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                                    sellOrderId = jsonObject.getString("orderId");
                                    sellAmounts = sellAmount;
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        setWarmLog(id,2,"出现疑似堵盘口订单，停止量化","");
                        setTradeLog(id, "出现疑似堵盘口订单，停止量化", 0, "000000");
                        String msg = "出现疑似堵盘口订单，您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }
                }
            }

            if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                //刷开区间
                if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount").trim() == null) {
                    //刷开区间

                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setWarmLog(id,2,"刷开区间的数量已达到最大值,停止刷开区间","");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");

                } else {
                    //刷开区间
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                }


            }
            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
            }

          //  setTradeLog(id, "区间值-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
            logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            logger.info("robotId" + id + "----" + "等份数：" + exchange.get("priceRange"));
            logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);
            BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);


           // setTradeLog(id, "区间差值-------------------------->" + interval, 1);
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
                 //   setTradeLog(id, "旧版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "旧版本结束");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setWarmLog(id,2,"刷买一卖一区间过小，无法量化","");
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
             //   setTradeLog(id, "随机区间值------------------------->" + randomNum, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
            //    setTradeLog(id, "区间最小价格[" + minPrice + "]区间最大价格[" + maxPrice + "]", 1);
                logger.info("robotId" + id + "----" + "minPrice(区间最小价格)：" + minPrice + "，maxPrice(区间最大价格)：" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("robotId" + id + "----" + "random(随机增长)：" + random);

                price = nN(minPrice.add(random), newScale);
                logger.info("robotId" + id + "----" + "price(新价格)：" + price);

                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
             //       setTradeLog(id, "新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "新版本结束");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + "新版本回调获取价格");
                    while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                        randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
                    }
                    price = null;
                }
            }


        } else {

            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            logger.info("robotId" + id + "----" + "异常回调获取价格");
            price = null;
        }

        removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);

        return price;
    }


    public void openInterval(BigDecimal sellPrice, List<List<String>> allBids, BigDecimal openIntervalPrice) {

        BigDecimal price;
        for (List<String> bid : allBids) {

            price = new BigDecimal(bid.get(0).toString());
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                break;
            }
            if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                logger.info("不计成本刷开区间中");
            } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.get(1).toString()))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                continue;
            }
            //开始挂单

            startOpenInterval(new BigDecimal(bid.get(0)), new BigDecimal(bid.get(1)));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitOrder(1, buyPri, buyAmount);
            JSONObject jsonObjectSubmit = judgeRes(resultJson, "code", "submitTrade");
            if (jsonObjectSubmit != null && jsonObjectSubmit.getInt("code") == 0) {

                String tradeId = jsonObjectSubmit.getString("orderId");
                setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                //查看订单详情
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                String str = selectOrder(tradeId);


                JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                    BigDecimal oneAmount = BigDecimal.ZERO;
                    JSONObject jsonArray = jsonObject.getJSONObject("result");

                    String records = jsonArray.getString("records");

                    if (records == null || records.equals("null")) {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId + "_" + buyAmount, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                    } else {
                        JSONArray recordsArray = JSONArray.fromObject(records);
                        for (int i = 0; i < recordsArray.size(); i++) {
                            System.out.println(recordsArray.size() + "长度:1" + i);
                            JSONObject everyOrder = recordsArray.getJSONObject(i);
                            BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                            oneAmount = oneAmount.add(everyOrderAmount);
                        }

                        logger.info("robotId" + id + "----刷开订单挂单数量:" + buyAmount);
                        logger.info("robotId" + id + "----刷开订单成交数量:" + oneAmount);

                        int result = buyAmount.compareTo(oneAmount);

                        if (result == 0) {
                            setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                        } else {
                            sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                        }
                    }
                }

                intervalAmount = intervalAmount.add(buyAmount);
                setTradeLog(id, "已使用刷开区间币量:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
    }

}
