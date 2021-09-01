package top.suilian.aio.service.test.kline;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.test.TestParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class TestKline extends TestParentService {
    public TestKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_TEST_KLINE, id);
    }

    private BigDecimal intervalAmount = BigDecimal.ZERO;
    private BigDecimal buyPrice = BigDecimal.ZERO;
    private BigDecimal sellPrice = BigDecimal.ZERO;
    private String orderIdOne = "0";
    private String orderIdTwo = "0";
    private String eatBuyOrder = "0";
    private String eatSellOrder = "0";
    private boolean start = true;
    private int orderNum = 0;
    private int runTime = 0;
    private int randomNum = 1;
    private int langunge = 1;
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int eatOrder = 0;//吃单数量
    private int timeSlot = 1;


    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        cnt++;
        //第一次执行需要设置机器人参数，交易规则，初始区间值
        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");
            logger.info("设置机器人交易规则开始");
            setPrecision();
            BigDecimal num = new BigDecimal("21321");
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
        setTransactionRatio();
        langunge = Integer.parseInt(exchange.get("langunge"));
        int index = Integer.valueOf(new Date().getHours());
        //获取当前小时内的单量百分比
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        System.out.println(transactionRatio);
        setTradeLog(id, "jenkins测试-------------->dddfire028", 0);
        logger.info(" langunage========>" + (langunge == 1 ? "CN" : "EN"));

        if (runTime < timeSlot) {
            String trades = httpUtil.get("http://120.77.223.226:8017/bihuex.php/depth?market=" + exchange.get("market"));
            JSONObject tradesObj = judgeRes(trades, "error", "getRandomPrice");
            if (tradesObj != null && tradesObj.getInt("error") == 0) {
                JSONObject data = tradesObj.getJSONObject("data");
                JSONObject marketData = data.getJSONObject(exchange.get("market"));
                List<List<Object>> buyPrices = (List<List<Object>>) marketData.get("buy");
                List<List<Object>> sellPrices = (List<List<Object>>) marketData.get("sell");
                BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0).toString());
                BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0).toString());
                if (buyPri.compareTo(sellPri) >= 0) {
                    //平台撮合功能失败
                    if (langunge == 1) {
                        setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    } else {
                        setTradeLog(id, "Transaction platform cannot make a match", 0, "FF111A");
                    }
                    return;
                }
            }

            //判断订单状态
            if (!"0".equals(eatBuyOrder)) {
                selectOrderDetail(eatBuyOrder, 0);
                eatBuyOrder = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(eatSellOrder)) {
                selectOrderDetail(eatSellOrder, 0);
                eatBuyOrder = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "已吃堵盘口单总数:" + eatOrder + ";吃单成交上限数:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {
                selectOrderDetail(orderIdOne, 1);
                eatBuyOrder = "0";

                selectOrderDetail(orderIdTwo, 1);
                eatBuyOrder = "0";

                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //撞单达上限后的操作
                    //停止机器人
                    if ("0".equals(exchange.get("orderOperation"))) {
                        if (langunge == 1) {
                            setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
                            msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
                        } else {
                            setTradeLog(id, "The number of retractions has reached the upper limit, halt the quantification", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Quantitative Robot has stopped";
                        }
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);

                    } else if ("1".equals(exchange.get("orderOperation"))) {//不停止
                        if (langunge == 1) {
                            setTradeLog(id, "撤单数次数过多，请注意盘口", 0, "000000");
                            msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，请注意盘口!";
                        } else {
                            setTradeLog(id, "Too many times of cancellation, please pay attention to the opening", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Too many times of cancellation of quantification robot, please pay attention to the disk mouth!";
                        }
                        //重置撞单次数
                        randomNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//随机暂停后重启
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        if (langunge == 1) {
                            setTradeLog(id, "撤单数次数过多，将暂停" + st + "秒后自动恢复", 0, "000000");
                            msg = "您的" + getRobotName(this.id) + "量化机器人撤单数次数过多，将暂停片刻后自动恢复!";
                        } else {
                            setTradeLog(id, "Too many times of cancellation, will pause " + st + " seconds and then automatically resume", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Too many times of cancellation of quantification robot, it will resume automatically after a pause!";
                        }
                        //重置撞单次数
                        randomNum = 0;
                        //暂停
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));

                    }

                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);

                    return;
                }


            }
            BigDecimal price = getRandomPrice();
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            Double minNum = Double.valueOf(exchange.get("numMinThreshold"));
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));

            BigDecimal oldnum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
            BigDecimal num = oldnum.multiply(new BigDecimal(transactionRatio));
            logger.info("num(挂单数量)：" + num);

            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 1 : 0;
            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "error", "submitTrade");
                if (jsonObject != null && jsonObject.getInt("error") == 0) {
                    String tradeId = jsonObject.getString("data");
                    orderIdOne = tradeId;
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "error", "submitTrade");
                    if (jsonObject1 != null && jsonObject1.getInt("error") == 0) {
                        orderIdTwo = jsonObject1.getString("data");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                    } else if (jsonObject != null && jsonObject.getInt("error") == 1) {
                        String msg = jsonObject.getString("msg");
                        if (msg.equals("biz.balance.not.enough")) {
                            String message = "";
                            if (langunge == 1) {
                                message = "您的" + getRobotName(id) + "余额不足！";
                            } else {
                                message = "Your No. " + getRobotName(id) + " Quantitative Robot has insufficient balance";
                            }


                            judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);
                        }
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        if (langunge == 1) {
                            setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                        } else {
                            setTradeLog(id, "CANCEL[" + tradeId + "]=> " + res, 0, "000000");
                        }

                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        if (langunge == 1) {
                            setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
                        } else {
                            setTradeLog(id, "CANCEL[" + tradeId + "]=> " + res, 0, "000000");
                        }
                    }
                } else if (jsonObject != null && jsonObject.getInt("error") == 1) {
                    String msg = jsonObject.getString("msg");
                    if (msg.equals("biz.balance.not.enough")) {
                        String message = "";
                        if (langunge == 1) {
                            message = "您的" + getRobotName(id) + "余额不足！";
                        } else {
                            message = "Your No. " + getRobotName(id) + " Quantitative Robot has insufficient balance";
                        }
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            if (langunge == 1) {
                setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
            } else {
                setTradeLog(id, "Pause times----------------------------->" + st + "s", 0);
            }

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
                    setTradeLog(id, "当前随机值（" + value + ":横盘）", 1);
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                        logger.info(randomNum);
                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    } else {
                        randomNum -= 1;
                        logger.info(randomNum);
                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                        logger.info(randomNum);
                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
                    } else {
                        randomNum += 1;
                        logger.info(randomNum);
                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
                    }
                    break;
            }
        }
//        try {
//            setBalanceRedis();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
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

        trades = httpUtil.get("http://120.77.223.226:8017/bihuex.php/depth?market=" + exchange.get("market"));

        JSONObject tradesObj = judgeRes(trades, "error", "getRandomPrice");
        if (tradesObj != null && tradesObj.getInt("error") == 0) {
            JSONObject data = tradesObj.getJSONObject("data");
            JSONObject marketData = data.getJSONObject(exchange.get("market"));
            List<List<Object>> buyPrices = (List<List<Object>>) marketData.get("buy");
            List<List<Object>> sellPrices = (List<List<Object>>) marketData.get("sell");
            BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0).toString());
            BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0).toString());
            logger.info("最新买一：" + buyPri + "，最新卖一：" + sellPri);
            BigDecimal intervalPrice = sellPri.subtract(buyPri);
            logger.info("当前买一卖一差值：" + intervalPrice);

            //判断盘口买卖检测开关是否开启
            if ("1".equals(exchange.get("isTradeCheck"))) {

                //吃堵盘口的订单

                BigDecimal buyAmount = new BigDecimal(buyPrices.get(0).get(1).toString());
                BigDecimal sellAmount = new BigDecimal(sellPrices.get(0).get(1).toString());

                if (maxEatOrder == 0) {
                    logger.info("吃单上限功能未启动：maxEatOrder=" + maxEatOrder);
                } else if (maxEatOrder <= eatOrder) {
                    setTradeLog(id, "已吃堵盘口单总数(" + eatOrder + ")=吃单成交上限数(" + maxEatOrder + "),吃单上限，停止吃单", 0);
                }


                //吃买单
                if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && "1".equals(exchange.get("isBuyMinLimitAmount"))) {
                    //判断出现堵盘口单时的操作,是停止还是吃
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            try {
                                String resultJson = submitTrade(2, buyPri, buyAmount);
                                JSONObject jsonObject = judgeRes(resultJson, "error", "submitTrade");
                                if (jsonObject != null && jsonObject.getInt("error") == 0) {
                                    String tradeId = jsonObject.getString("data");
                                    eatSellOrder = tradeId;
                                }
                                if (langunge == 1) {
                                    setTradeLog(id, "堵盘口买单:数量[" + buyAmount + "],价格:[" + buyPri + "]", 0);
                                } else {
                                    setTradeLog(id, "No space for sell:AMOUNT[" + buyAmount + "],PRICE:[" + buyPri + "]", 0);
                                }

                            } catch (UnsupportedEncodingException e) {
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
                            try {
                                String resultJson = submitTrade(1, sellPri, sellAmount);
                                JSONObject jsonObject = judgeRes(resultJson, "error", "submitTrade");
                                if (jsonObject != null && jsonObject.getInt("error") == 0) {
                                    String tradeId = jsonObject.getString("data");
                                    eatSellOrder = tradeId;
                                }
                                if (langunge == 1) {
                                    setTradeLog(id, "堵盘口卖单:数量[" + sellAmount + "],价格:[" + sellPri + "]", 0);
                                } else {
                                    setTradeLog(id, "No space for sell:AMOUNT[" + sellAmount + "],PRICE:[" + sellPri + "]", 0);
                                }

                            } catch (UnsupportedEncodingException e) {
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
                if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                    //刷开区间
                    String msg = "您的" + getRobotName(this.id) + "刷开量化机器人已开启,将不计成本的刷开区间!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    if (langunge == 1) {
                        setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
                    } else {
                        setTradeLog(id, "The number of brushing intervals has reached the maximum, stopping the brushing intervalCCC", 0, "000000");
                    }

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
            logger.info("上次买一：" + buyPrice + "，上次卖一：" + sellPrice);
            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("上次买一卖一差值：" + disparity);
            //价格小数位
            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            if ("0".equals(exchange.get("sheetForm"))) {
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
                    price = getRandomPrice();
                }
            } else {
                logger.info("新版本开始");
                logger.info("等份数：" + exchange.get("priceRange"));
                logger.info("价格保存小数位：" + newScale);
                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);
                setTradeLog(id, "区间差值-------------------------->" + interval, 1);
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
                    price = getRandomPrice();
                }
            }
        } else {
            logger.info("异常回调获取价格 trades(深度接口返回)=>" + trades);
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            price = getRandomPrice();
        }
        removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);
        return price;
    }


    public void openInterval(BigDecimal sellPrice, List<List<Object>> allBids, BigDecimal openIntervalPrice) {
        BigDecimal price;
        for (List<Object> bid : allBids) {
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

            startOpenInterval(new BigDecimal(bid.get(0).toString()), new BigDecimal(bid.get(1).toString()));
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
                String str = selectOrder(tradeId);
                JSONObject result = judgeRes(str, "error", "selectOrder");
                if (result != null && result.getInt("error") == 0) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.getString("status");
                    if ("EX_ORDER_STATUS_FILLED".equals(status)) {
                        setTradeLog(id, "刷开区间订单id：" + tradeId + "完全成交", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
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
            JSONObject jsonObject = judgeRes(str, "error", "selectOrder");
            if (jsonObject != null && jsonObject.getInt("error") == 0) {
                JSONObject data = jsonObject.getJSONObject("data");
                String status = data.getString("status");
                if ("EX_ORDER_STATUS_FILLED".equals(status)) {
                    if (langunge == 1) {
                        setTradeLog(id, "订单id：" + orderId + "完全成交", 0, "000000");
                    } else {
                        setTradeLog(id, "The order " + orderId + " has been filled", 0, "000000");
                    }

                } else if ("EX_ORDER_STATUS_CANCELED".equals(status)) {
                    if (langunge == 1) {
                        setTradeLog(id, "订单id：" + orderId + "已撤单", 0, "000000");
                    } else {
                        setTradeLog(id, "The order " + orderId + " has been canceled", 0, "000000");

                    }

                } else {
                    String res = cancelTrade(orderId);
                    JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                    setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    if (langunge == 1) {
                        setTradeLog(id, "撤单[" + orderId + "]=>" + res, 0, "000000");
                    } else {
                        setTradeLog(id, "CANCEL[" + orderId + "]=>" + res, 0, "000000");
                    }
                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //防褥羊毛开关
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
