package top.suilian.aio.service.mxc.newKline;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.mxc.MxcParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class NewMxcKline extends MxcParentService  {
    public NewMxcKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_MXC_KLINE, id);
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
            if (!setPrecision()) {
                return;
            }
            logger.info("设置机器人交易规则结束");

            /**
             * 深度
             */
            if ("1".equals(exchange.get("isdeepRobot"))) {
                logger.info("深度机器人交易开始");
                runMxcDeep.init(id + 1);
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
            String trades = getDepth();
            JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

            if (trades != null && !trades.isEmpty() && tradesObj != null) {


                JSONObject result = tradesObj.getJSONObject("data");

                //JSONObject tick = data.getJSONObject("tick");
                List<JSONObject> buyPrices = (List<JSONObject>) result.get("bids");

                List<JSONObject> sellPrices = (List<JSONObject>) result.get("asks");

                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).getString("price")));
                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).getString("price")));

                if (sellPri.compareTo(buyPri) == 0) {
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

                selectOrderDetail(orderIdTwo, 1);
                orderIdTwo = "0";
                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //撞单达上限后的操作
                    //停止机器人
                    if ("0".equals(exchange.get("orderOperation"))) {

                        setWarmLog(id,2,"撤单数达到上限，停止量化","");
                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//不停止
                        setWarmLog(id,2,"撤单数次数过多，请注意盘口","");
                        setTradeLog(id, "撤单数次数过多，请注意盘口", 0, "000000");
                        msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，请注意盘口!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        orderNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//随机暂停后重启
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        setTradeLog(id, "撤单数次数过多，将暂停" + st + "秒后自动恢复", 0, "f57272");
                        setWarmLog(id,2,"量化机器人撤单数次数过多，将暂停片刻后自动恢复!","");
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //重置撞单次数
                        orderNum = 0;
                        //暂停
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        return;
                    }

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

            BigDecimal random = new BigDecimal(Math.random() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            int type = random.compareTo(tradeRatio) > 0 ? 1 : 2;


            try {
                String resultJson = submitTrade(type == 1 ? 1 : 2, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && "200".equals(jsonObject.getString("code"))) {

                    String tradeId = jsonObject.getString("data");
                    orderIdOne = tradeId;
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && "200".equals(jsonObject1.getString("code"))) {
                        orderIdTwo = jsonObject1.getString("data");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else {
                        String res = cancelTrade(tradeId);
                        setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "67c23a");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }


                } else if (jsonObject != null && "30002".equals(jsonObject.getString("code"))) {
                    setWarmLog(id,3,"交易量过小","");
                    setTradeLog(id, "交易量过小=> " + jsonObject, 0, "f57272");
                }
            } catch (Exception e) {
                logger.info("xxxx");
                if((!"0".equals(orderIdOne))&&"0".equals(orderIdTwo)){
                    String res = cancelTrade(orderIdOne);
                    setTradeLog(id, "1撤单[" + orderIdOne + "]=> " + res, 0, "67c23a");
                }
                if((!"0".equals(orderIdTwo))&&"0".equals(orderIdOne)){
                    String res = cancelTrade(orderIdTwo);
                    setTradeLog(id, "2撤单[" + orderIdTwo + "]=> " + res, 0, "67c23a");
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
            Integer value = string.get((int) Math.round(Math.random() * (string.size() - 1)));
            switch (value) {
                case 0:
                   // setTradeLog(id, "当前随机值（" + value + ":横盘）", 1);
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                     //   setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    } else {
                        randomNum -= 1;
                     //   setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                       // setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    } else {
                        randomNum += 1;
                      //  setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
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
        String trades = getDepth();
        JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {

            JSONObject result = tradesObj.getJSONObject("data");


            List<JSONObject> buyPrices = (List<JSONObject>) result.get("bids");

            List<JSONObject> sellPrices = (List<JSONObject>) result.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).getString("price")));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).getString("price")));


            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "最新买一：" + buyPri + "，最新卖一：" + sellPri);
            logger.info("robotId" + id + "----" + "当前买一卖一差值：" + intervalPrice);

            //判断盘口买卖检测开关是否开启
            if ("1".equals(exchange.get("isTradeCheck"))) {
                //吃堵盘口的订单
                BigDecimal buyAmount = new BigDecimal(String.valueOf(buyPrices.get(0).getString("quantity")));
                BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).getString("quantity")));
                BigDecimal minAmount = new BigDecimal(String.valueOf(precision.get("minTradeLimit")));

                if (maxEatOrder == 0) {
                    logger.info("吃单上限功能未开启：maxEatOrder=" + maxEatOrder);
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
                                String sellOrder = submitTrade(2, buyPri, buyAmount);
                                setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                                logger.info("堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]");

                                JSONObject jsonObject = judgeRes(sellOrder, "code", "submitTrade");
                                if (jsonObject != null && "200".equals(jsonObject.getString("code"))) {
                                    sellOrderId = jsonObject.getString("data");
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        setWarmLog(id,2,"出现疑似堵盘口订单，停止量化","");
                        setTradeLog(id, "出现疑似堵盘口订单，停止量化", 0, "f57272");
                        String msg = "出现疑似堵盘口订单，您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }
                }

                //吃卖单
                //吃卖单
                if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && "1".equals(exchange.get("isSellMinLimitAmount"))) {
                    //判断出现堵盘口单时的操作,是停止还是吃
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            if (sellAmount.compareTo(minAmount) == -1) {
                                sellAmount = minAmount;
                            }
                            try {
                                String buyOrder = submitTrade(1, sellPri, sellAmount);
                                setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                                logger.info("堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]");

                                JSONObject jsonObject = judgeRes(buyOrder, "status", "submitTrade");
                                if (jsonObject != null) {
                                    buyOrderId = jsonObject.getString("order_id");
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        setWarmLog(id,2,"出现疑似堵盘口订单，停止量化","");
                        setTradeLog(id, "出现疑似堵盘口订单，停止量化", 0, "f57272");
                        String msg = "出现疑似堵盘口订单，您的" + getRobotName(this.id) + "量化机器人已停止!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }
                }

            }
            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
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
                    setWarmLog(id,2,"刷开区间的数量已达到最大值,停止刷开区间","");
                    setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "f57272");

                } else {
                    //刷开区间
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                }


            }

           // setTradeLog(id, "区间值-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
            logger.info("robotId" + id + "----" + "上次买一卖一差值：" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            logger.info("robotId" + id + "----" + "等份数：" + exchange.get("priceRange"));
            logger.info("robotId" + id + "----" + "价格保存小数位：" + newScale);
            BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);


          //  setTradeLog(id, "区间差值-------------------------->" + interval, 1);
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
                    setWarmLog(id,2,"买一卖一区间过小，无法量化","");
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
               // setTradeLog(id, "随机区间值------------------------->" + randomNum, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                //setTradeLog(id, "区间最小价格[" + minPrice + "]区间最大价格[" + maxPrice + "]", 1);
                logger.info("robotId" + id + "----" + "minPrice(区间最小价格)：" + minPrice + "，maxPrice(区间最大价格)：" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("robotId" + id + "----" + "random(随机增长)：" + random);

                price = nN(minPrice.add(random), newScale);
                logger.info("robotId" + id + "----" + "price(新价格)：" + price);

                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                   // setTradeLog(id, "新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "新版本结束");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + "新版本回调获取价格");
                    price = null;
                    while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                        randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
                    }
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

    public void openInterval(BigDecimal sellPrice, List<JSONObject> allBids, BigDecimal openIntervalPrice) {

        BigDecimal price;
        for (JSONObject bid : allBids) {

            price = new BigDecimal(bid.getString("price"));
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                break;
            }
            if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                logger.info("不计成本刷开区间中");
            } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.getString("quantity")))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "f57272");
                break;
            }
            //开始挂单

            startOpenInterval(new BigDecimal(bid.getString("price")), new BigDecimal(bid.getString("quantity")));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitTrade(2, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");
            if (jsonObject != null && "200".equals(jsonObject.getString("code"))) {
                String tradeId = jsonObject.getString("data");
                setTradeLog(id, "买一卖一区间过小，刷开区间-------------->卖单[" + buyPri + "]数量[" + buyAmount + "]", 0);
                //查看订单详情
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                String str = selectOrder(tradeId);
                String status = null;
                JSONObject selectResult = judgeRes(str, "code", "selectOrder");
                if (selectResult != null && "200".equals(selectResult.getString("code"))) {

                    JSONArray orderInfo = selectResult.getJSONArray("data");

                    if (orderInfo != null) {
                        for (int i = 0; i < orderInfo.size(); i++) {
                            if (tradeId.equals(orderInfo.getJSONObject(i).getString("id"))) {
                            status = orderInfo.getJSONObject(i).getString("state");
                                break;
                            }
                        }

                        if ("FILLED".equals(status)) {
                            setTradeLog(id, "订单id：" + tradeId + "完全成交", 0, "#67c23a");
                        } else if ("CANCELED".equals(status)) {
                            setTradeLog(id, "订单id：" + tradeId + "已撤单", 0, "#67c23a");
                        } else {
                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                            setTradeLog(id, "撤单[" + tradeId + "]=>" + res, 0, "000000");

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


    public void selectOrderDetail(String orderId, int type) {
        try {
            String status = null;
            String str = selectOrder(orderId);
            JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
            if (jsonObject != null && "200".equals(jsonObject.getString("code"))) {

                JSONArray orderInfo = jsonObject.getJSONArray("data");

                if (orderInfo != null) {
                    for (int i = 0; i < orderInfo.size(); i++) {
                        if (orderId.equals(orderInfo.getJSONObject(i).getString("id"))) {
                            status = orderInfo.getJSONObject(i).getString("state");
                            break;
                        }
                    }

                    if ("FILLED".equals(status)) {
                        setTradeLog(id, "订单id：" + orderId + "完全成交", 0, "#67c23a");
                    } else if ("CANCELED".equals(status)) {
                        setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "#67c23a");
                    } else {
                        String res = cancelTrade(orderId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "撤单[" + orderId + "]=>" + res, 0, "000000");
                        setWarmLog(id,2,"订单撞单+1,单号:"+orderId,"");
                        if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //防褥羊毛开关
                            orderNum++;
                        }
                    }

                } else {
                    setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "000000");

                }


            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            e.printStackTrace();
        }
    }
}
