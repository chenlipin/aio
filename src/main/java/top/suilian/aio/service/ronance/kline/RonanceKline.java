package top.suilian.aio.service.ronance.kline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.ronance.RonanceParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.math.BigDecimal.ROUND_DOWN;

public class RonanceKline extends RonanceParentService {
    public RonanceKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_RONANCE_KLINE, id);
    }

    private BigDecimal intervalAmount = BigDecimal.ZERO;
    private BigDecimal buyPrice = BigDecimal.ZERO;
    private BigDecimal sellPrice = BigDecimal.ZERO;
    private String orderIdOne = "0";
    private String orderIdTwo = "0";
    private boolean start = true;
    private int orderNum = 0;
    private int runTime = 0;
    private int randomNum = 1;
    private String transactionRatio="1";


    public void init() {
        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");
            start = false;

            //随机交易区间
            while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
            }
        }
        setTransactionRatio();
        int index = Integer.valueOf(new Date().getHours());
        //获取当前小时内的单量百分比
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("当前时间段单量百分比：" + transactionRatio);

        if (runTime < Integer.parseInt(timeSlot)) {
            System.out.println(runTime + "runtime---------timeSlot:" + timeSlot);


            //获取深度 判断平台撮合是否成功
            logger.info(market);
            String trades = httpUtil.get(baseUrl + "/api/v1/market/depthData?market=" + market + "&depth=0");
            JSONObject tradesObj = judgeRes(trades, "code", "judgeGetRandomPrice");

            if (tradesObj != null && tradesObj.getInt("code") == 200) {
                JSONObject data = tradesObj.getJSONObject("data");
                List<JSONObject> buyPrices = (List<JSONObject>) data.get("asks");
                List<JSONObject> sellPrices = (List<JSONObject>) data.get("bids");

                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).getString("price")));
                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).getString("price")));

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }


            if (orderSumSwitch == 1) {    //防褥羊毛开关
                if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {
                    try {
                        String str = selectOrder(orderIdOne);
                        JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                        if (jsonObject != null && jsonObject.getInt("code") == 200) {
                            JSONArray jsonArray = jsonObject.getJSONArray("data");

                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject orderInfo = jsonArray.getJSONObject(i);
                                if (orderInfo.getString("id").equals(orderIdOne)) {
                                    int status = orderInfo.getInt("status");
                                    if (status == 3) {
                                        setTradeLog(id, "订单id：" + orderIdOne + "完全成交", 0, "000000");
                                    } else if (status == 4) {
                                        setTradeLog(id, "订单id：" + orderIdOne + "已撤单", 0, "000000");
                                    } else {
                                        String res = cancelTrade(orderIdOne);
                                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                        setTradeLog(id, "撤单[" + orderIdOne + "]=>" + res, 0, "000000");
                                        orderNum++;
                                        orderIdOne = "0";
                                    }

                                } else if (orderInfo.getString("id").equals(orderIdTwo)) {
                                    int status = orderInfo.getInt("status");
                                    if (status == 3) {
                                        setTradeLog(id, "订单id：" + orderIdTwo + "完全成交", 0, "000000");
                                    } else if (status == 4) {
                                        setTradeLog(id, "订单id：" + orderIdTwo + "已撤单", 0, "000000");
                                    } else {
                                        String res = cancelTrade(orderIdTwo);
                                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                        setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
                                        orderNum++;
                                        orderIdTwo = "0";

                                    }
                                }

                            }

                        }
                    } catch (UnsupportedEncodingException e) {
                        exceptionMessage = collectExceptionStackMsg(e);
                        setExceptionMessage(id, exceptionMessage, isMobileSwitch);
                        logger.info("robotId" + id + "----" + exceptionMessage);
                        e.printStackTrace();
                    }


                    if (orderNum >= Integer.parseInt(orderSum)) {
                        if (this.isMobileSwitch == 1) {
                            String msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                            JSONObject rest = sendSms(msg, this.mobile);
                            if (rest.getInt("code") != 0) {
                                sendSms(msg, this.mobile);
                            }
                        }
                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        return;
                    }
                }
            }

            setTradeLog(id, "撤单数为" + orderNum, 0, "000000");
            setTradeLog(id, "停止量化撤单数设置为：" + orderSum, 0, "000000");
            BigDecimal price = getRandomPrice();

            if (price == null) {
                orderIdOne = "0";
                orderIdTwo = "0";
                return;
            }
            Double numThreshold1 = Double.valueOf(numThreshold);
            Double minNum = Double.valueOf(numMinThreshold);
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
            BigDecimal num =oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(挂单数量)：" + num);

            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 1 : 2;


            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && jsonObject.getInt("code") == 200) {
                    JSONObject data = jsonObject.getJSONObject("data");

                    String tradeId = data.getString("orderId");
                    orderIdOne = tradeId;
                    String resultJson1 = submitTrade(type == 1 ? 0 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 200) {
                        JSONObject data1 = jsonObject1.getJSONObject("data");
                        orderIdTwo = data1.getString("orderId");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else if (resultJson.indexOf("Balance insufficient") != -1) {


                        String message = "您的" + getRobotName(id) + "余额不足！";
                        judgeSendMessage(isMobileSwitch, message, mobile, Constant.KEY_SMS_INSUFFICIENT);


                    } else {
                        String res = cancelTrade(tradeId);
                        setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, isMobileSwitch);
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
            int st = (int) (Math.random() * (endTime - startTime) + startTime);
            setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
            runTime += (st);
            setTradeLog(id, "累计周期时间----------------------------->" + runTime + "秒", 1);
            sleep(st * 1000, isMobileSwitch);
        } else {
            runTime = 0;
            List<Integer> string = new ArrayList<>();
            string.add(1);
            string.add(2);
            Integer value = string.get((int) Math.round(Math.random() * (string.size() - 1)));
            switch (value) {
                case 0:
                    setTradeLog(id, "当前随机值（" + value + ":横盘）", 1);
                    break;
                case 1:
                    if (priceRange >= (randomNum + 2)) {
                        randomNum += 1;
                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    } else {
                        randomNum -= 1;
                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    } else {
                        randomNum += 1;
                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    }
                    break;
            }
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
        String trades = httpUtil.get(baseUrl + "/api/v1/market/depthData?market=" + market + "&depth=0");
        JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

        if (tradesObj != null && tradesObj.getInt("code") == 200) {
            JSONObject data = tradesObj.getJSONObject("data");
            List<JSONObject> buyPrices = (List<JSONObject>) data.get("asks");
            List<JSONObject> sellPrices = (List<JSONObject>) data.get("bids");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).getString("price")));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).getString("price")));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
            logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);


            if (isOpenIntervalSwitch == 1 && intervalPrice.compareTo(openIntervalFromPrice) < 1) {
                //刷开区间

                if (openIntervalAllAmount.compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                } else {
                    //刷开区间
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启!";
                    sendSms(msg, this.mobile);
                    openInterval(sellPri, buyPrices, openIntervalPrice);
                }

            }
            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
            }

            setTradeLog(id, "区间值-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
            logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            logger.info("robotId" + id + "----" + "等份数：" + priceRange);
            logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);
            BigDecimal interval = nN(disparity.divide(BigDecimal.valueOf(priceRange), newScale, ROUND_DOWN), newScale);


            setTradeLog(id, "区间差值-------------------------->" + interval, 1);
            logger.info("robotId" + id + "----" + "区间值：" + interval);

            BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, ROUND_DOWN);
            logger.info("robotId" + id + "----" + "基础数据：interval(区间值)：" + interval + "，minInterval(最小区间值)：" + minInterval + "，randomNum(当前区间随机值)：" + randomNum + "，buyPri(当前买一)：" + buyPri + "，sellPri(当前卖一)" + sellPri + "，buyPrice(上次买一)：" + buyPrice + "，sellPrice(上次卖一)：" + sellPrice);
            logger.info("robotId" + id + "----" + "区间最小值（区间值小于区间最小值走旧版本）");
            if (interval.compareTo(minInterval) < 0) {
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
                    setTradeLog(id, "旧版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "旧版本结束");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setTradeLog(id, "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]", 0, "FF111A");
                    logger.info("robotId" + id + "----" + "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]");


                    String message = "您的" + getRobotName(id) + "区间过小，无法量化！";

                    String msg = "您的" + getRobotName(id) + "区间过小，无法量化！";
                    judgeSendMessage(isMobileSwitch, msg, mobile, Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, isMobileSwitch);
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("robotId" + id + "----" + "旧版本回调获取价格");
                    price = null;
                }
            } else {
                logger.info("robotId" + id + "----" + "新版本开始");
                setTradeLog(id, "随机区间值------------------------->" + randomNum, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                setTradeLog(id, "区间最小价格[" + minPrice + "]区间最大价格[" + maxPrice + "]", 1);
                logger.info("robotId" + id + "----" + "minPrice(区间最小价格)：" + minPrice + "，maxPrice(区间最大价格)：" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("robotId" + id + "----" + "random(随机增长)：" + random);

                price = nN(minPrice.add(random), newScale);
                logger.info("robotId" + id + "----" + "price(新价格)：" + price);

                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                    setTradeLog(id, "新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "新版本结束");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    sleep(2000, isMobileSwitch);
                    logger.info("robotId" + id + "----" + "新版本回调获取价格");
                    price = null;
                }
            }


        } else {

            sleep(2000, isMobileSwitch);

            logger.info("robotId" + id + "----" + "异常回调获取价格");
            setTradeLog(id, "获取价格异常", 0);
            price = null;
        }

        removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);

        return price;
    }


    public void openInterval(BigDecimal sellPrice, List<JSONObject> allBids, BigDecimal openIntervalPrice) {
        BigDecimal price;
        for (JSONObject bid : allBids) {

            price = new BigDecimal(bid.getString("price"));
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                continue;
            }
            if (openIntervalAllAmount.compareTo(intervalAmount.add(new BigDecimal(bid.getString("quantity")))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                continue;
            }
            //开始挂单
            startOpenInterval(new BigDecimal(bid.getString("price")), new BigDecimal(bid.getString("quantity")));
        }

    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = null;
            resultJson = submitTrade(0, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");
            if (jsonObject != null && jsonObject.getString("code").equals("200")) {
                JSONObject data = jsonObject.getJSONObject("data");
                String tradeId = data.getString("orderId");
                setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                //查看订单详情
                sleep(200, isMobileSwitch);

                try {
                    String str = selectOrder(tradeId);
                    JSONObject result = judgeRes(str, "code", "selectOrder");

                    if (result != null && result.getInt("code") == 200) {
                        JSONArray jsonArray = result.getJSONArray("data");

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JSONObject orderInfo = jsonArray.getJSONObject(i);
                            if (orderInfo.getString("id").equals(tradeId)) {
                                int status = orderInfo.getInt("status");
                                if (status == 3) {
                                    setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                                } else if (status == 4) {
                                    setTradeLog(id, "刷开区间订单id：" + tradeId + "已撤单", 0, "000000");
                                } else {

                                    sleep(200, isMobileSwitch);

                                    String res = cancelTrade(tradeId);
                                    JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                    setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                    setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                                }

                            }
                        }


                    }
                } catch (UnsupportedEncodingException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, isMobileSwitch);
                    logger.info("robotId" + id + "----" + exceptionMessage);
                    e.printStackTrace();
                }

                intervalAmount = intervalAmount.add(buyAmount);
                setTradeLog(id, "已使用刷开区间币量:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, isMobileSwitch);
            logger.info("robotId" + id + "----" + exceptionMessage);
            e.printStackTrace();
        }
    }

    /**
     * 交易规则获取
     */
    public void setPrecision() {
        precision.put("minTradeLimit", robotArg(id, "minTradeLimit"));
        precision.put("amountPrecision", robotArg(id, "amountPrecision"));
        precision.put("pricePrecision", robotArg(id, "pricePrecision"));
    }
}
