package top.suilian.aio.service.s.kline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.s.SParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.ROUND_DOWN;
import static top.suilian.aio.Util.HMAC.genHMAC;

public class SKline extends SParentService {
    public SKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_S_KLINE, id);
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
    private String transactionRatio = "1";


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


        if (runTime < Integer.parseInt(exchange.get("timeSlot"))) {


            //判断平台撮合

            String toSign = getToSign(exchange.get("apikey"));
            String apiUrl = "/v1/market/depth";
            HashMap<String, Object> params = new HashMap();
            String sign = "POST\nexapi.s.top\n" + apiUrl + "\n" + toSign;
            params.put("symbol", exchange.get("market"));
            params.put("size", "50");
            String signE = genHMAC(sign, exchange.get("tpass"));
            String url = "https://exapi.s.top" + apiUrl + "?" + toSign +
                    "&Signature=" +
                    signE;


            String trades = null;
            try {
                trades = httpUtil.post(url, params);
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                e.printStackTrace();
            }

            JSONObject tradesObj = judgeRes(trades, "status", "getRandomPrice");
            if (tradesObj != null && tradesObj.getString("status").equals("ok")) {
                JSONObject data = tradesObj.getJSONObject("data");

                List<List<String>> buyPrices = (List<List<String>>) data.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) data.get("asks");

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
                    String str = selectOrder(orderIdOne);
                    JSONObject jsonObject = judgeRes(str, "status", "selectOrder");
                    if (jsonObject != null && jsonObject.getString("status").equals("ok")) {
                        JSONObject resultData = jsonObject.getJSONObject("data");

                        if (resultData != null && jsonObject.get("status").equals("ok")) {
                            String status = (String) resultData.get("state");
                            if ("filled".equals(status)) {
                                setTradeLog(id, "订单id：" + orderIdOne + "完全成交", 0, "000000");
                            } else if ("canceled".equals(status)) {
                                setTradeLog(id, "订单id：" + orderIdOne + "已撤单", 0, "000000");
                            } else {
                                String res = cancelTrade(orderIdOne);
                                JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                                setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                setTradeLog(id, "撤单[" + orderIdOne + "]=>" + res, 0, "000000");
                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //防褥羊毛开关
                                    //判断 是否撤销成功
                                    if (cancelRes.getString("status").equals("ok")) {
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
                }
                orderIdOne = "0";
                try {
                    String str = selectOrder(orderIdTwo);
                    JSONObject jsonObject = judgeRes(str, "status", "selectOrder");
                    if (jsonObject != null && jsonObject.getString("status").equals("ok")) {
                        JSONObject resultData = jsonObject.getJSONObject("data");

                        if (resultData != null && jsonObject.get("status").equals("ok")) {
                            String status = (String) resultData.get("state");
                            if ("filled".equals(status)) {
                                setTradeLog(id, "订单id：" + orderIdTwo + "完全成交", 0, "000000");
                            } else if ("canceled".equals(status)) {
                                setTradeLog(id, "订单id：" + orderIdTwo + "已撤单", 0, "000000");
                            } else {
                                String res = cancelTrade(orderIdTwo);
                                JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                                setCancelOrder(cancelRes, res, orderIdTwo, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
                                if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //防褥羊毛开关
                                    if (cancelRes.getString("status").equals("ok")) {
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
                }

                orderIdTwo = "0";

                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    String msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                    setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                    return;
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

            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 1 : 2;


            try {
                String resultJson = submitTrade(type, price, num);
                sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                if (resultJson != null && !resultJson.equals("")) {
                    JSONObject jsonObject = new JSONObject().fromObject(resultJson);
                    if (jsonObject.get("status").equals("ok")) {
                        String tradeId = jsonObject.getString("data");
                        orderIdOne = tradeId;
                        String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                        JSONObject jsonObject1 = judgeRes(resultJson1, "status", "submitTrade");


                        if (resultJson1 != null && jsonObject1.getString("status").equals("ok")) {
                            orderIdTwo = jsonObject1.getString("data");
                            removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                        } else {
                            String res = cancelTrade(tradeId);
                            setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                            JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);

                        }


                    }

                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                e.printStackTrace();
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
            runTime += (st);
            setTradeLog(id, "累计周期时间----------------------------->" + runTime + "秒", 1);
            sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));

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
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
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
        String toSign = getToSign(exchange.get("apikey"));
        String apiUrl = "/v1/market/depth";
        HashMap<String, Object> params = new HashMap();
        String sign = "POST\nexapi.s.top\n" + apiUrl + "\n" + toSign;
        params.put("symbol", exchange.get("market"));
        params.put("size", "50");
        String signE = genHMAC(sign, exchange.get("tpass"));
        String url = "https://exapi.s.top" + apiUrl + "?" + toSign +
                "&Signature=" +
                signE;


        String trades = null;
        try {
            trades = httpUtil.post(url, params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        if (!"".equals(trades) && trades != null && !trades.isEmpty()) {

            JSONObject tradesObj = JSONObject.fromObject(trades);

            if (tradesObj.get("status").equals("ok")) {
                JSONObject data = tradesObj.getJSONObject("data");

                List<List<String>> buyPrices = (List<List<String>>) data.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) data.get("asks");

                BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0));
                BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0));

                BigDecimal intervalPrice = sellPri.subtract(buyPri);


                logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
                logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);


                if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                    //刷开区间
                    if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                        setRobotArgs(id, "isOpenIntervalSwitch", "0");
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

                setTradeLog(id, "区间值-------------------------->" + randomNum, 1);

                BigDecimal disparity = sellPrice.subtract(buyPrice);
                logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
                logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


                Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

                logger.info("robotId" + id + "----" + "等份数：" + exchange.get("priceRange"));
                logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);

                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);

                setTradeLog(id, "区间差值-------------------------->" + interval, 1);
                logger.info("robotId" + id + "----" + "区间值：" + interval);

                BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, BigDecimal.ROUND_UP);

                logger.info("robotId" + id + "----" + "基础数据：interval(区间值)：" + interval + "，minInterval(最小区间值)：" + minInterval + "，randomNum(当前区间随机值)：" + randomNum + "，buyPri(当前买一)：" + buyPri + "，sellPri(当前卖一)" + sellPri + "，buyPrice(上次买一)：" + buyPrice + "，sellPrice(上次卖一)：" + sellPrice);
                logger.info("robotId" + id + "----" + "区间最小值（区间值小于区间最小值走旧版本）");


                if (interval.compareTo(minInterval) < 0) {
                    logger.info("robotId" + id + "----" + "旧版本开始");
                    BigDecimal diff = sellPri.subtract(buyPri);
                    BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
                    int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
                    BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, BigDecimal.ROUND_UP);
                    System.out.println("随机增长------->" + random);
                    BigDecimal oldPrice = buyPri.add(random);
                    System.out.println("oldPrice------->" + oldPrice);
                    price = nN(oldPrice, newScale);
                    System.out.println("格式化后价格------->" + price);
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


                        String msg = "您的" + getRobotName(id) + "区间过小，无法量化！";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
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
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        logger.info("robotId" + id + "----" + "新版本回调获取价格");
                        price = null;
                    }
                }

            }


        } else {

            logger.info("异常回调获取价格 trades(深度接口返回)=>" + trades);
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            setTradeLog(id, "获取深度失败", 0);
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
                continue;
            }
            if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.get(1).toString()))) < 0) {
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
            String resultJson = submitTrade(2, buyPri, buyAmount);
            if (resultJson != null && !resultJson.equals("")) {
                JSONObject jsonObject = new JSONObject().fromObject(resultJson);
                String tradeId = jsonObject.getString("orderId");
                setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                //查看订单详情
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                try {
                    String str = selectOrder(tradeId);
                    System.out.println("参看tradeId--------->" + str);
                    if (str != null) {
                        JSONObject result = new JSONObject().fromObject(str);


                        String status = (String) result.get("status");
                        if ("FILLED".equals(status)) {
                            setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                        } else {
                            sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));

                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                        }


                    }
                } catch (UnsupportedEncodingException e) {
                    logger.info(e.getMessage());
                    e.printStackTrace();
                }

                intervalAmount = intervalAmount.add(buyAmount);
                setTradeLog(id, "已使用刷开区间币量:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            e.printStackTrace();
        }
    }


}
