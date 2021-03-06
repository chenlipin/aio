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
    private int eatOrder = 0;//????????????
    private int timeSlot = 1;


    public void init() {
        logger.info("\r\n------------------------------{" + id + "} ??????------------------------------\r\n");
        cnt++;
        //???????????????????????????????????????????????????????????????????????????
        if (start) {
            logger.info("???????????????????????????");
            setParam();
            logger.info("???????????????????????????");
            logger.info("?????????????????????????????????");
            setPrecision();
            BigDecimal num = new BigDecimal("21321");
            logger.info("?????????????????????????????????");
            //?????????K????????????
            if ("1".equals(exchange.get("sheetForm"))) {
                //?????????
                while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                    randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
                }
                timeSlot = Integer.parseInt(exchange.get("timeSlot"));
            }

            maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//?????????????????????
            start = false;
        }
        setTransactionRatio();
        langunge = Integer.parseInt(exchange.get("langunge"));
        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        System.out.println(transactionRatio);
        setTradeLog(id, "jenkins??????-------------->dddfire028", 0);
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
                    //????????????????????????
                    if (langunge == 1) {
                        setTradeLog(id, "????????????????????????", 0, "FF111A");
                    } else {
                        setTradeLog(id, "Transaction platform cannot make a match", 0, "FF111A");
                    }
                    return;
                }
            }

            //??????????????????
            if (!"0".equals(eatBuyOrder)) {
                selectOrderDetail(eatBuyOrder, 0);
                eatBuyOrder = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(eatSellOrder)) {
                selectOrderDetail(eatSellOrder, 0);
                eatBuyOrder = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {
                selectOrderDetail(orderIdOne, 1);
                eatBuyOrder = "0";

                selectOrderDetail(orderIdTwo, 1);
                eatBuyOrder = "0";

                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //???????????????????????????
                    //???????????????
                    if ("0".equals(exchange.get("orderOperation"))) {
                        if (langunge == 1) {
                            setTradeLog(id, "????????????????????????????????????", 0, "000000");
                            msg = "??????" + getRobotName(this.id) + "????????????????????????!";
                        } else {
                            setTradeLog(id, "The number of retractions has reached the upper limit, halt the quantification", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Quantitative Robot has stopped";
                        }
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);

                    } else if ("1".equals(exchange.get("orderOperation"))) {//?????????
                        if (langunge == 1) {
                            setTradeLog(id, "???????????????????????????????????????", 0, "000000");
                            msg = "??????" + getRobotName(this.id) + "??????????????????????????????????????????????????????!";
                        } else {
                            setTradeLog(id, "Too many times of cancellation, please pay attention to the opening", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Too many times of cancellation of quantification robot, please pay attention to the disk mouth!";
                        }
                        //??????????????????
                        randomNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//?????????????????????
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        if (langunge == 1) {
                            setTradeLog(id, "?????????????????????????????????" + st + "??????????????????", 0, "000000");
                            msg = "??????" + getRobotName(this.id) + "?????????????????????????????????????????????????????????????????????!";
                        } else {
                            setTradeLog(id, "Too many times of cancellation, will pause " + st + " seconds and then automatically resume", 0, "000000");
                            msg = " Your No. " + getRobotName(this.id) + " Too many times of cancellation of quantification robot, it will resume automatically after a pause!";
                        }
                        //??????????????????
                        randomNum = 0;
                        //??????
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
            logger.info("num(????????????)???" + num);

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
                                message = "??????" + getRobotName(id) + "???????????????";
                            } else {
                                message = "Your No. " + getRobotName(id) + " Quantitative Robot has insufficient balance";
                            }


                            judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);
                        }
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        if (langunge == 1) {
                            setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                        } else {
                            setTradeLog(id, "CANCEL[" + tradeId + "]=> " + res, 0, "000000");
                        }

                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        if (langunge == 1) {
                            setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                        } else {
                            setTradeLog(id, "CANCEL[" + tradeId + "]=> " + res, 0, "000000");
                        }
                    }
                } else if (jsonObject != null && jsonObject.getInt("error") == 1) {
                    String msg = jsonObject.getString("msg");
                    if (msg.equals("biz.balance.not.enough")) {
                        String message = "";
                        if (langunge == 1) {
                            message = "??????" + getRobotName(id) + "???????????????";
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
                setTradeLog(id, "????????????----------------------------->" + st + "???", 0);
            } else {
                setTradeLog(id, "Pause times----------------------------->" + st + "s", 0);
            }

            if ("1".equals(exchange.get("sheetForm"))) {
                runTime += (st);
                setTradeLog(id, "??????????????????----------------------------->" + runTime + "???", 1);
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
                    setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                        logger.info(randomNum);
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum -= 1;
                        logger.info(randomNum);
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                        logger.info(randomNum);
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum += 1;
                        logger.info(randomNum);
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
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
        logger.info("\r\n------------------------------{" + id + "} ??????------------------------------\r\n");
    }


    /**
     * ??????????????????
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
            logger.info("???????????????" + buyPri + "??????????????????" + sellPri);
            BigDecimal intervalPrice = sellPri.subtract(buyPri);
            logger.info("???????????????????????????" + intervalPrice);

            //??????????????????????????????????????????
            if ("1".equals(exchange.get("isTradeCheck"))) {

                //?????????????????????

                BigDecimal buyAmount = new BigDecimal(buyPrices.get(0).get(1).toString());
                BigDecimal sellAmount = new BigDecimal(sellPrices.get(0).get(1).toString());

                if (maxEatOrder == 0) {
                    logger.info("??????????????????????????????maxEatOrder=" + maxEatOrder);
                } else if (maxEatOrder <= eatOrder) {
                    setTradeLog(id, "????????????????????????(" + eatOrder + ")=?????????????????????(" + maxEatOrder + "),???????????????????????????", 0);
                }


                //?????????
                if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && "1".equals(exchange.get("isBuyMinLimitAmount"))) {
                    //????????????????????????????????????,??????????????????
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
                                    setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                                } else {
                                    setTradeLog(id, "No space for sell:AMOUNT[" + buyAmount + "],PRICE:[" + buyPri + "]", 0);
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        setTradeLog(id, "??????????????????????????????????????????", 0, "000000");
                        String msg = "????????????????????????????????????" + getRobotName(this.id) + "????????????????????????!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }

                }

                //?????????
                if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && "1".equals(exchange.get("isSellMinLimitAmount"))) {

                    //????????????????????????????????????,??????????????????
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
                                    setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                                } else {
                                    setTradeLog(id, "No space for sell:AMOUNT[" + sellAmount + "],PRICE:[" + sellPri + "]", 0);
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        setTradeLog(id, "??????????????????????????????????????????", 0, "000000");
                        String msg = "????????????????????????????????????" + getRobotName(this.id) + "????????????????????????!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    }
                }

            }


            if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                //????????????
                if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                    //????????????
                    String msg = "??????" + getRobotName(this.id) + "??????????????????????????????,??????????????????????????????!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    if (langunge == 1) {
                        setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                    } else {
                        setTradeLog(id, "The number of brushing intervals has reached the maximum, stopping the brushing intervalCCC", 0, "000000");
                    }

                } else {
                    //????????????
                    String msg = "??????" + getRobotName(this.id) + "??????????????????????????????!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                }
            }
            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
            }
            logger.info("???????????????" + buyPrice + "??????????????????" + sellPrice);
            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("???????????????????????????" + disparity);
            //???????????????
            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            if ("0".equals(exchange.get("sheetForm"))) {
                logger.info("???????????????");
                BigDecimal diff = sellPri.subtract(buyPri);
                BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
                int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
                BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, ROUND_DOWN);
                logger.info("random(????????????)???" + random);
                BigDecimal oldPrice = buyPri.add(random);
                logger.info("??????????????????????????????------->" + oldPrice);
                price = nN(oldPrice, newScale);
                logger.info("??????????????????????????????------->" + price);
                if (price.compareTo(sellPri) < 0 && price.compareTo(buyPri) > 0) {
                    setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
                    logger.info("???????????????");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setTradeLog(id, "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]", 0, "FF111A");
                    logger.info("??????????????????????????????????????????");
                    String msg = "??????" + getRobotName(id) + "??????????????????????????????";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    price = getRandomPrice();
                }
            } else {
                logger.info("???????????????");
                logger.info("????????????" + exchange.get("priceRange"));
                logger.info("????????????????????????" + newScale);
                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);
                setTradeLog(id, "????????????-------------------------->" + interval, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                setTradeLog(id, "??????????????????[" + minPrice + "]??????????????????[" + maxPrice + "]", 1);
                logger.info("minPrice(??????????????????)???" + minPrice + "???maxPrice(??????????????????)???" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("random(????????????)???" + random);
                price = nN(minPrice.add(random), newScale);
                logger.info("price(?????????)???" + price);
                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                    setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
                    logger.info("???????????????");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("????????????????????????????????????????????????????????????????????????");
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    price = getRandomPrice();
                }
            }
        } else {
            logger.info("???????????????????????? trades(??????????????????)=>" + trades);
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
                logger.info("???????????????????????????");
            } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.get(1).toString()))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                continue;
            }
            //????????????

            startOpenInterval(new BigDecimal(bid.get(0).toString()), new BigDecimal(bid.get(1).toString()));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitTrade(2, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "error", "submitTrade");
            if (jsonObject != null && jsonObject.getInt("error") == 0) {
                String tradeId = jsonObject.getString("data");
                setTradeLog(id, "???????????????????????????????????????-------------->??????[" + buyPri + "]??????[" + buyAmount + "]", 0);
                //??????????????????
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                String str = selectOrder(tradeId);
                JSONObject result = judgeRes(str, "error", "selectOrder");
                if (result != null && result.getInt("error") == 0) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.getString("status");
                    if ("EX_ORDER_STATUS_FILLED".equals(status)) {
                        setTradeLog(id, "??????????????????id???" + tradeId + "????????????", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????????????????[" + tradeId + "]=>" + res, 0, "000000");
                    }
                    intervalAmount = intervalAmount.add(buyAmount);
                    setTradeLog(id, "???????????????????????????:" + intervalAmount, 0, "000000");
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
                        setTradeLog(id, "??????id???" + orderId + "????????????", 0, "000000");
                    } else {
                        setTradeLog(id, "The order " + orderId + " has been filled", 0, "000000");
                    }

                } else if ("EX_ORDER_STATUS_CANCELED".equals(status)) {
                    if (langunge == 1) {
                        setTradeLog(id, "??????id???" + orderId + "?????????", 0, "000000");
                    } else {
                        setTradeLog(id, "The order " + orderId + " has been canceled", 0, "000000");

                    }

                } else {
                    String res = cancelTrade(orderId);
                    JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                    setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    if (langunge == 1) {
                        setTradeLog(id, "??????[" + orderId + "]=>" + res, 0, "000000");
                    } else {
                        setTradeLog(id, "CANCEL[" + orderId + "]=>" + res, 0, "000000");
                    }
                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //??????????????????
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
