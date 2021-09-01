package top.suilian.aio.service.happycoin.newKline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.happycoin.HappyCoinParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class NewHappyCoinKline extends HappyCoinParentService {
    public NewHappyCoinKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_HAPPYCOIN_KLINE, id);
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
    private int eatOrder = 0;//吃单数量
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    private BigDecimal tradeRatio = new BigDecimal(5);


    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        cnt++;
        //第一次执行需要设置机器人参数，交易规则，初始区间值
        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            setTransactionRatio();
            if (exchange.get("tradeRatio") != null || !"0".equals(exchange.get("tradeRatio"))) {
                Double ratio = 10 * (1 / (1 + Double.valueOf(exchange.get("tradeRatio"))));
                tradeRatio = new BigDecimal(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");
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
            String trades = httpUtil.get(baseUrl + "/market_dept?symbol=" + exchange.get("market") + "&type=step0");
            JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

            if (tradesObj != null && tradesObj.getInt("code") == 0) {


                JSONObject jsonData = tradesObj.getJSONObject("data");
                JSONObject jsonTick = jsonData.getJSONObject("tick");

                JSONArray jsonArrayAsks = jsonTick.getJSONArray("asks");
                JSONArray jsonArrayBids = jsonTick.getJSONArray("bids");

                List<Double> asks1 = (List<Double>) jsonArrayAsks.get(0);
                List<Double> bids1 = (List<Double>) jsonArrayBids.get(0);

                BigDecimal buyPri = new BigDecimal(String.valueOf(bids1.get(0)));
                BigDecimal sellPri = new BigDecimal(String.valueOf(asks1.get(0)));

                if (buyPri.compareTo(sellPri) >= 0) {
                    //平台撮合功能失败
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }


            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
            if (!"0".equals(sellOrderId)) {

                selectOrderDetail(sellOrderId, 0);
                sellOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0);
                buyOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {

                selectOrderDetail(orderIdOne, 1);
                orderIdOne = "0";
                selectOrderDetail(orderIdTwo, 2);

                orderIdTwo = "0";
                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //撞单达上限后的操作
                    //停止机器人
                    if ("0".equals(exchange.get("orderOperation"))) {

                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//不停止

                        setTradeLog(id, "撤单数次数过多，请注意盘口", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，请注意盘口!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        randomNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//随机暂停后重启
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
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

            setTradeLog(id, "撤单数为" + orderNum, 0, "000000");
            setTradeLog(id, "停止量化撤单数设置为：" + exchange.get("orderSum"), 0, "000000");
            BigDecimal price = getRandomPrice();
            if (price == null) {
                orderIdOne = "0";
                orderIdTwo = "0";
                return;
            }

            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            Double minNum = Double.valueOf(exchange.get("numMinThreshold"));
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));//最大交易量
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));//最小交易量
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));//随机交易量


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));//获取挂单数量
            BigDecimal num = oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(挂单数量)：" + num);

            BigDecimal random = new BigDecimal(Math.random() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            int type = random.compareTo(tradeRatio) < 0 ? 2 : 1;
            logger.info("买单占比：" + tradeRatio + "随机值：" + random + "type=" + type);


            try {
                String resultJson = submitTrade(type, price, num);//挂单
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (resultJson != null && !resultJson.equals("") && jsonObject != null) {
                    if (jsonObject.getString("code").equals("0")) {
                        JSONObject jsonObject2 = jsonObject.getJSONObject("data");
                        String tradeId = jsonObject2.getString("order_id");
                        orderIdOne = tradeId;
                        String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                        JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                        if (jsonObject1 != null && jsonObject1.getString("code").equals("0")) {
                            JSONObject jsonObject3 = jsonObject1.getJSONObject("data");
                            orderIdTwo = jsonObject3.getString("order_id");
                            balanceValid = 1;
                        } else if (jsonObject1.getInt("code") == 10034) {
                            String message = "您的" + getRobotName(id) + "余额不足！";
                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                            setCancelOrder(cancelRes, res, orderIdTwo, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");

                            judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);

                        } else {
                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                            setCancelOrder(cancelRes, res, orderIdTwo, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");

                        }
                    } else if (jsonObject.getInt("code") == 10034) {
                        String message = "您的" + getRobotName(id) + "余额不足！";

                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);
                    }


                }
            } catch (Exception e) {
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

            try {
                Thread.sleep(st * 1000);
            } catch (InterruptedException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }

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
        String trades = httpUtil.get(baseUrl + "/market_dept?symbol=" + exchange.get("market") + "&type=step0");
        JSONObject tradesObj = judgeRes(trades, "data", "getRandomPrice");

        if (tradesObj != null && tradesObj.getInt("code") == 0) {


            JSONObject jsonData = tradesObj.getJSONObject("data");
            JSONObject jsonTick = jsonData.getJSONObject("tick");

            List<List<String>> buyPrices = (List<List<String>>) jsonTick.getJSONArray("bids");

            List<List<String>> sellPrices = (List<List<String>>) jsonTick.getJSONArray("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
            logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);

            //判断盘口买卖检测开关是否开启
            if ("1".equals(exchange.get("isTradeCheck"))) {

                //吃堵盘口的订单
                BigDecimal buyAmount = new BigDecimal(String.valueOf(buyPrices.get(0).get(1)));
                BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).get(1)));
                BigDecimal minAmount = new BigDecimal(String.valueOf(precision.get("minTradeLimit")));
                int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//吃单成交上限数
                if (maxEatOrder == 0) {
                    logger.info("吃单上限功能关闭：maxEatOrder=" + maxEatOrder);
                } else if (maxEatOrder <= eatOrder) {
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
                                String sellOrder = submitTrade(2, buyPri, buyAmount);
                                setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                                logger.info("堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]");

                                JSONObject jsonObject = judgeRes(sellOrder, "data", "submitTrade");
                                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                                    JSONObject jsonData1 = jsonObject.getJSONObject("data");
                                    sellOrderId = jsonData1.getString("order_id");
                                }
                                return price;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
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
                                    String buyOrdre = submitTrade(1, sellPri, sellAmount);
                                    setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                                    logger.info("堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]");

                                    JSONObject jsonObject = judgeRes(buyOrdre, "data", "submitTrade");
                                    if (jsonObject != null && jsonObject.getInt("code") == 0) {
                                        JSONObject jsonData1 = jsonObject.getJSONObject("data");
                                        buyOrderId = jsonData1.getString("order_id");
                                    }
                                    return price;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
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
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启,将不计成本的刷开区间!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");

                } else {
                    //刷开区间
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                }
            }
                //判断是否有价格
                if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                    buyPrice = buyPri;
                    sellPrice = sellPri;
                }

                setTradeLog(id, "区间值-------------------------->" + randomNum, 1);

                BigDecimal disparity = sellPrice.subtract(buyPrice);
                logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
                logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


                Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());
                //计算区间值=差价/等分数
                logger.info("robotId" + id + "----" + "等份数：" + new BigDecimal(exchange.get("priceRange")));
                logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);
                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);


                setTradeLog(id, "区间差值-------------------------->" + interval, 1);
                logger.info("robotId" + id + "----" + "区间值：" + interval);

            if ("0".equals(exchange.get("sheetForm"))) {
                    logger.info("robotId" + id + "----" + "旧版本开始");
                    BigDecimal diff = sellPri.subtract(buyPri);//买一卖一差价
                    BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));//差价乘以小数位
                    int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));//
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
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);

                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            exceptionMessage = collectExceptionStackMsg(e);
                            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                            logger.info("robotId" + id + "----" + exceptionMessage);
                            e.printStackTrace();
                        }
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
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            exceptionMessage = collectExceptionStackMsg(e);
                            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                            logger.info("robotId" + id + "----" + exceptionMessage);
                            e.printStackTrace();
                        }
                        logger.info("robotId" + id + "----" + "新版本回调获取价格");
                        price = null;
                    }
                }


            } else {

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    exceptionMessage = collectExceptionStackMsg(e);
                    setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + exceptionMessage);
                    e.printStackTrace();
                }
                logger.info("robotId" + id + "----" + "异常回调获取价格");
                setTradeLog(id, "robotId" + id + "----" + "异常回调获取价格", 0);
                price = null;
            }


            return price;
        }


        /**
         * 打开区间
         *
         * @param sellPrice
         * @param allBids
         * @param openIntervalPrice
         */
        public void openInterval (BigDecimal sellPrice, List < List < String >> allBids, BigDecimal
        openIntervalPrice){

            BigDecimal price;
            for (List<String> bid : allBids) {

                price = new BigDecimal(String.valueOf(bid.get(0)));
                //买价小于卖价-刷开区间执行
                if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                    break;
                }
                if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                    logger.info("不计成本刷开区间中");
                }else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(String.valueOf(bid.get(1))))) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                    break;
                }
                startOpenInterval(new BigDecimal(String.valueOf(bid.get(0))), new BigDecimal(String.valueOf(bid.get(1))));
            }

        }

        /**
         * 开始刷区间
         *
         * @param buyPri
         * @param buyAmount
         */
        private void startOpenInterval (BigDecimal buyPri, BigDecimal buyAmount){
            try {
                String resultJson = submitTrade(2, buyPri, buyAmount);
                JSONObject jsonObject = judgeRes(resultJson, "data", "submitTrade");
                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    String tradeId = jsonData.getString("order_id");
                    setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                    //查看订单详情
                    Thread.sleep(2000);

                    try {
                        String str = selectOrder(tradeId);
                        JSONObject result = judgeRes(str, "code", "selectOrder");

                        if (result != null && result.getInt("code") == 0) {

                            JSONObject jsondata = result.getJSONObject("data");
                            JSONObject jsonOrderInfo = jsondata.getJSONObject("order_info");
                            int status = jsonOrderInfo.getInt("status");
                            if (status == 2) {
                                setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                            } else if (status == 4) {
                                setTradeLog(id, "刷开区间订单id：" + tradeId + "已撤单", 0, "000000");
                            } else {
                                Thread.sleep(2000);
                                String res = cancelTrade(tradeId);
                                JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                setTradeLog(id, "刷开区间撤单[" + tradeId + "]=>" + res, 0, "000000");
                                setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);

                            }


                        }
                    } catch (UnsupportedEncodingException e) {
                        exceptionMessage = collectExceptionStackMsg(e);
                        setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                        logger.info("robotId" + id + "----" + exceptionMessage);
                        e.printStackTrace();
                    }

                    intervalAmount = intervalAmount.add(buyAmount);
                    setTradeLog(id, "已使用刷开区间币量:" + intervalAmount, 0, "000000");
                }
            } catch (Exception e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
        }


        public void selectOrderDetail (String orderId,int type){
            try {
                String str = selectOrder(orderId);
                JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                if (jsonObject != null && jsonObject.getInt("code") == 0) {

                    JSONObject jsondata = jsonObject.getJSONObject("data");
                    JSONObject jsonOrderInfo = jsondata.getJSONObject("order_info");
                    int status = jsonOrderInfo.getInt("status");
                    if (status == 2) {
                        setTradeLog(id, "订单id：" + orderId + "完全成交", 0, "000000");
                    } else if (status == 4) {//撤单状态码
                        setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "000000");
                    } else {
                        String res = cancelTrade(orderId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
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
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
        }

    }
