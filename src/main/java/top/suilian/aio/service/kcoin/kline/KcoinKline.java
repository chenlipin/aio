package top.suilian.aio.service.kcoin.kline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.kcoin.KcionParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class KcoinKline extends KcionParentService {

    public KcoinKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_KCOIN_KLINE, id);
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

    private String sellOrderId = "0";
    private String buyOrderId = "0";
    private int eatOrder=0;//吃单数量
    private String transactionRatio="1";

    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        cnt++;
        //第一次执行需要设置机器人参数，交易规则，初始区间值
        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            boolean flag = setPrecision();
            if (!flag) {
                return;
            }

            logger.info("设置机器人交易规则结束");
            //随机交易区间
            while (randomNum==1 || randomNum==Integer.parseInt(exchange.get("priceRange"))) {
                randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
            }
            start = false;
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
            String param = null;//传入参数
            try {
                param = "symbol=" + exchange.get("market") + "&step=" + "STEP1" + "&accessKey=" + URLEncoder.encode(exchange.get("apikey"),"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String trades = HttpUtil.sendGet(baseUrl + "/market/depth?", param);
            JSONObject tradesObj = judgeRes(trades, "status", "getRandomPrice");
            if (tradesObj != null && tradesObj.getString("status").equals("success")) {

                JSONObject jsonData = tradesObj.getJSONObject("data");

                JSONArray jsonArrayBuys = jsonData.getJSONArray("buy");
                JSONArray jsonArraySells = jsonData.getJSONArray("sell");


                BigDecimal buyPri = new BigDecimal(String.valueOf(jsonArrayBuys.getJSONObject(0).getString("price")));
                BigDecimal sellPri = new BigDecimal(String.valueOf(jsonArraySells.getJSONObject(0).getString("price")));


                if (buyPri.compareTo(sellPri) >= 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                }
            }

            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数

            if (!"0".equals(sellOrderId)) {
                selectOrderDetail(sellOrderId, 0);
                sellOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0);
                buyOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }

            }


            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {

                selectOrderDetail(orderIdOne, 1);
                orderIdOne = "0";

                selectOrderDetail(orderIdTwo, 1);
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
            BigDecimal num=oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("num(挂单数量)：" + num);

            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 1 : 0;
            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "status", "submitTrade");
                if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                    String tradeId = jsonObject.getString("data");
                    orderIdOne = tradeId;
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "status", "submitTrade");
                    if (jsonObject1 != null && jsonObject1.getString("status").equals("success")) {
                        orderIdTwo = jsonObject1.getString("data");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                    } else if (jsonObject1 != null && jsonObject1.getString("status").equals("account_failure_0002")) {
                        String message = "您的" + getRobotName(id) + "余额不足！";
                        logger.info(message);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);

                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                    }
                } else if (jsonObject != null && jsonObject.getString("status").equals("account_failure_0002")) {

                    String message = "您的" + getRobotName(id) + "余额不足！";
                    logger.info(message);
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);

                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
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
        String trades;
        String param = null;//传入参数
        try {
            param = "symbol=" + exchange.get("market") + "&step=" + "STEP1" + "&accessKey=" + URLEncoder.encode(exchange.get("apikey"),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        trades = HttpUtil.sendGet(baseUrl + "/market/depth?", param);
        JSONObject tradesObj = judgeRes(trades, "status", "getRandomPrice");
        if (tradesObj != null && tradesObj.getString("status").equals("success")) {

            JSONObject jsonData = tradesObj.getJSONObject("data");

            JSONArray jsonArrayBuys = jsonData.getJSONArray("buy");
            JSONArray jsonArraySells = jsonData.getJSONArray("sell");


            BigDecimal buyPri = new BigDecimal(String.valueOf(jsonArrayBuys.getJSONObject(0).getString("price")));
            BigDecimal sellPri = new BigDecimal(String.valueOf(jsonArraySells.getJSONObject(0).getString("price")));

            logger.info("最新买一：" + buyPri + "，最新卖一：" + sellPri);
            BigDecimal intervalPrice = sellPri.subtract(buyPri);
            logger.info("当前买一卖一差值：" + intervalPrice);

            //吃堵盘口的订单
            BigDecimal buyAmount = new BigDecimal(String.valueOf(jsonArrayBuys.getJSONObject(0).getString("amount")));
            BigDecimal sellAmount = new BigDecimal(String.valueOf(jsonArraySells.getJSONObject(0).getString("amount")));
            BigDecimal minAmount = new BigDecimal(exchange.get("minTradeLimit").toString());
            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
            if(maxEatOrder==0){
                logger.info("吃单上限功能未开启：maxEatOrder="+maxEatOrder);
            }else if (maxEatOrder <= eatOrder) {
                setTradeLog(id, "已吃堵盘口单总数(" + eatOrder + ")=吃单成交上限数(" + maxEatOrder + "),吃单上限，停止吃单", 0);
            }
            //吃买单
            if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && maxEatOrder==0) {
                try {
                    if(buyAmount.compareTo(minAmount) == -1){
                        buyAmount = minAmount;
                    }
                    String sellOrder = submitTrade(2, buyPri, buyAmount);
                    setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                    logger.info("堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]");

                    JSONObject jsonObject = judgeRes(sellOrder, "status", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                        sellOrderId = jsonObject.getString("data");
                    }
                    return price;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else  if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && maxEatOrder > eatOrder) {
                try {
                    if(buyAmount.compareTo(minAmount) == -1){
                        buyAmount = minAmount;
                    }
                    String sellOrder = submitTrade(2, buyPri, buyAmount);
                    setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                    logger.info("堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]");

                    JSONObject jsonObject = judgeRes(sellOrder, "status", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                        sellOrderId = jsonObject.getString("data");
                    }
                    return price;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            //吃卖单
            if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && maxEatOrder ==0) {
                try {
                    if(sellAmount.compareTo(minAmount) == -1){
                        sellAmount = minAmount;
                    }
                    String buyOrder = submitTrade(1, sellPri, sellAmount);
                    setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                    logger.info("堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]");

                    JSONObject jsonObject = judgeRes(buyOrder, "status", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                        buyOrderId = jsonObject.getString("data");
                    }
                    return price;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && maxEatOrder > eatOrder) {
                try {
                    if(sellAmount.compareTo(minAmount) == -1){
                        sellAmount = minAmount;
                    }
                    String buyOrder = submitTrade(1, sellPri, sellAmount);
                    setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                    logger.info("堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]");

                    JSONObject jsonObject = judgeRes(buyOrder, "status", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                        buyOrderId = jsonObject.getString("data");
                    }
                    return price;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


            if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                //刷开区间
                if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                } else {
                    //刷开区间
                    openInterval(sellPri, jsonArrayBuys, new BigDecimal(exchange.get("openIntervalPrice")));
                }
            }

            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
            }
            logger.info("上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("上次买一卖一差值：" + disparity);
            //价格小数位
            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());
            logger.info("等份数：" + exchange.get("priceRange"));
            logger.info("价格保存小数位：" + newScale);
            BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);
            setTradeLog(id, "区间差值-------------------------->" + interval, 1);
            logger.info("区间值：" + interval);
            BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, ROUND_DOWN);
            logger.info("基础数据：interval(区间值)：" + interval + "，minInterval(最小区间值)：" + minInterval + "，randomNum(当前区间随机值)：" + randomNum + "，buyPri(当前买一)：" + buyPri + "，sellPri(当前卖一)" + sellPri + "，buyPrice(上次买一)：" + buyPrice + "，sellPrice(上次卖一)：" + sellPrice);
            logger.info("区间最小值（区间值小于区间最小值走旧版本）");
            if (interval.compareTo(minInterval) < 0) {
                logger.info("旧版本开始");
                BigDecimal diff = sellPri.subtract(buyPri);
                BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
                int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
                BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, ROUND_DOWN);
                logger.info("random(随机增长)：" + random);
                BigDecimal oldPrice = buyPri.add(random);
                logger.info("小数位未处理的新价格------->" + oldPrice);
                price = nN(oldPrice, newScale);
                logger.info("小数位已处理的新价格------->" + price);
                if (price.compareTo(sellPri) < 0 && price.compareTo(buyPri) > 0) {
                    setTradeLog(id, "旧版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("旧版本结束");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setTradeLog(id, "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]", 0, "FF111A");
                    logger.info("旧版本区间过小，回调获取价格");
                    String msg = "您的" + getRobotName(id) + "区间过小，无法量化！";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    price = null;
                }
            } else {
                logger.info("新版本开始");
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                setTradeLog(id, "区间最小价格[" + minPrice + "]区间最大价格[" + maxPrice + "]", 1);
                logger.info("minPrice(区间最小价格)：" + minPrice + "，maxPrice(区间最大价格)：" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("random(随机增长)：" + random);
                price = nN(minPrice.add(random), newScale);
                logger.info("price(新价格)：" + price);
                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                    setTradeLog(id, "新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("新版本结束");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("新版本最新价格超出当前买一卖一，回调重新获取价格");
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    price = null;
                }
            }
        } else {
            logger.info("异常回调获取价格 trades(深度接口返回)=>" + trades);
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            setTradeLog(id, "获取深度异常", 0, "FF111A");
            price = null;
        }
        removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);
        return price;
    }


    public void openInterval(BigDecimal sellPrice, JSONArray allBids, BigDecimal openIntervalPrice) {
        BigDecimal price;
        for (int i = 0; i < allBids.size(); i++) {
            price = new BigDecimal(allBids.getJSONObject(0).getString("price"));
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                break;
            }
            if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(allBids.getJSONObject(0).getString("amount")))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                break;
            }
            //开始挂单

            startOpenInterval(new BigDecimal(allBids.getJSONObject(0).getString("price")), new BigDecimal(allBids.getJSONObject(0).getString("amount")));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitTrade(2, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "error", "submitTrade");
            if (jsonObject != null && jsonObject.getInt("error") == 0) {
                String tradeId = jsonObject.getString("data");
                setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                //查看订单详情
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                String str = selectOrder(orderIdOne);
                JSONObject result = judgeRes(str, "status", "selectOrder");
                if (result != null && result.getString("status").equals("success")) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.getString("status");
                    if ("COMPLE-TRADE".equals(status)) {
                        setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                    }
                    intervalAmount = intervalAmount.add(buyAmount);
                    setTradeLog(id, "已使用刷开区间币量:" + intervalAmount, 0, "000000");
                }
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
    }


    public void selectOrderDetail(String orderId, int type) {

        try {
            String str = selectOrder(orderId);
            JSONObject jsonObject = judgeRes(str, "status", "selectOrder");
            if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                JSONObject data = jsonObject.getJSONObject("data");
                String status = data.getString("status");
                if ("COMPLE-TRADE".equals(status)) {
                    setTradeLog(id, "订单id：" + orderId + "完全成交", 0, "000000");
                } else if ("WITHDRAWN".equals(status)) {
                    setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "000000");
                } else {
                    String res = cancelTrade(orderId);
                    JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                    setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    setTradeLog(id, "撤单[" + orderId + "]=>" + res, 0, "000000");
                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //防褥羊毛开关
                        orderNum++;
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
    }


}
