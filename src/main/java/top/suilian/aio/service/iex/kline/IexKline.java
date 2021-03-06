package top.suilian.aio.service.iex.kline;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.iex.IexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class IexKline extends IexParentService {
    public IexKline(
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
        super.logger = getLogger("iex/newkline", id);
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
    private int eatOrder = 0;//????????????
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    long ordersleeptime = System.currentTimeMillis();


    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("???????????????????????????");
            setParam();
            setTransactionRatio();
            logger.info("???????????????????????????");
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
        int index = new Date().getHours();
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("?????????????????????????????????" + transactionRatio);
        if (runTime < timeSlot) {
            String trades = getDepth();
            //???????????? ??????????????????????????????
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

            if (tradesObj != null && tradesObj.getInteger("code") ==200 ) {
                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");
                JSONArray bid = data.getJSONArray("bid");
                JSONArray ask= data.getJSONArray("ask");


                BigDecimal buyPri = new BigDecimal(String.valueOf(bid.getJSONObject(0).getString("price")));
                BigDecimal sellPri = new BigDecimal(String.valueOf(ask.getJSONObject(0).getString("price")));


                if (sellPri.compareTo(buyPri) == 0) {
                    //????????????????????????
                    setTradeLog(id, "????????????????????????", 0, "FF111A");
                    return;
                }
            }


            if (!"0".equals(sellOrderId)) {
                selectOrderDetail(sellOrderId, 0, sellOrderIdtradeNo);
                sellOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0, buyOrderIdtradeNo);
                buyOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
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
                    //???????????????????????????
                    //???????????????
                    if ("0".equals(exchange.get("orderOperation"))) {

                        setTradeLog(id, "????????????????????????????????????", 0, "000000");
                        setWarmLog(id, 2, "????????????????????????????????????", "");
                        msg = "??????" + getRobotName(this.id) + "????????????????????????!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//?????????

                        setTradeLog(id, "???????????????????????????????????????", 0, "000000");
                        setWarmLog(id, 2, "???????????????????????????????????????", "");
                        msg = "??????" + getRobotName(this.id) + "??????????????????????????????????????????????????????!";
                        //judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //??????????????????
                        orderNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//?????????????????????
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        setTradeLog(id, "?????????????????????????????????" + st + "??????????????????", 0, "000000");
                        msg = "??????" + getRobotName(this.id) + "?????????????????????????????????????????????????????????????????????!";
                        setWarmLog(id, 2, "?????????????????????????????????" + st + "??????????????????", "");
                        //??????????????????
                        orderNum = 0;
                        //??????
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        return;
                    }

                }
            }
            setTradeLog(id, "????????????" + orderNum, 0, "000000");

            if (Integer.parseInt(exchange.get("orderSumSwitch")) == 1) {    //??????????????????
                setTradeLog(id, "?????????????????????????????????" + exchange.get("orderSum"), 0, "000000");
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
            logger.info("robotId" + id + "----" + "num(????????????)???" + num);

            int type = RandomUtils.nextInt(10) > Double.parseDouble(exchange.get("tradeRatio")) * 10 ? 1 : -1;
            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && jsonObject.getInt("code") == 200) {
                    orderIdOne = jsonObject.getJSONObject("data").getString("orderId");
                    String resultJson1 = submitTrade(type == 1 ? -1 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 200) {
                        orderIdTwo =jsonObject1.getJSONObject("data").getString("orderId");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                        ordersleeptime = System.currentTimeMillis();
                    } else {
                        String res = cancelTrade(orderIdOne);
                        setTradeLog(id, "??????[" + orderIdOne + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }
                }
            } catch (Exception e) {
                if (!orderIdOne.equals("0")) {
                    String res = cancelTrade(orderIdOne);
                    setTradeLog(id, "??????[" + orderIdOne + "]=> " + res, 0, "000000");
                    logger.info("??????" + orderIdOne + ":??????" + res);
                }
                if (!orderIdTwo.equals("0")) {
                    String res = cancelTrade(orderIdTwo);
                    setTradeLog(id, "??????[" + orderIdTwo + "]=> " + res, 0, "000000");
                    logger.info("??????" + orderIdTwo + ":??????" + res);
                }
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "????????????----------------------------->" + st + "???", 0);
            setBalanceRedis();
            if ("1".equals(exchange.get("sheetForm"))) {
                runTime += (st);
                setTradeLog(id, "??????????????????----------------------------->" + runTime + "???", 1);
            }

            sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
        } else {
            String trades = getDepth();
            //???????????? ??????????????????????????????
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

//            if (tradesObj != null && tradesObj.getInteger("code") == 0) {
//                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");
//                JSONArray bids = data.getJSONArray("bids");
//                JSONArray asks = data.getJSONArray("asks");
//                BigDecimal buyPri = new BigDecimal(bids.getJSONObject(0).getString("price"));
//                BigDecimal sellPri = new BigDecimal(asks.getJSONObject(0).getString("price"));
//                BigDecimal subtract = sellPri.subtract(buyPri);
//                logger.info("?????? buy1???"+buyPri +"-sell1:"+sellPri +"--subtract:"+subtract +"??????"+subtract.divide(sellPri,RoundingMode.HALF_UP).setScale(12,  RoundingMode.HALF_UP).toPlainString());
//                if (subtract.divide(sellPri,RoundingMode.HALF_UP).setScale(12,  RoundingMode.HALF_UP).compareTo(new BigDecimal("0.05"))>0){
//                    logger.info("????????????0.05");
//                    Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
//                    Double minNum = Double.valueOf(exchange.get("numMinThreshold"));
//                    long max = (long) (numThreshold1 * Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision")))));
//                    long min = (long) (minNum * Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision")))));
//                    long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));
//                    BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.parseDouble(String.valueOf(exchange.get("amountPrecision"))))));
//                    BigDecimal num = oldNum.multiply(new BigDecimal(transactionRatio));
//
//                    BigDecimal price = sellPri.multiply(new BigDecimal("0.95"));
//                    String resultJson1 = submitTrade( -1, price, num);
//                    setTradeLog(id, "??????????????? ??????"+buyPri+"---??????"+sellPri +"---???????????????"+price, 1);
//                    sleep(10 * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
//                }
//            }
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
            logger.info("\r\n------------------------------{" + id + "} ??????------------------------------\r\n");
        }


        /**
         * ??????????????????
         *
         * @return
         */
        public BigDecimal getRandomPrice () throws UnsupportedEncodingException {
            BigDecimal price = null;

            String trades = getDepth();
            //???????????? ??????????????????????????????
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

            if (tradesObj != null && tradesObj.getInteger("code") ==200 ) {
                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");
                JSONArray bid = data.getJSONArray("bid");
                JSONArray ask= data.getJSONArray("ask");


                BigDecimal buyPri = new BigDecimal(String.valueOf(bid.getJSONObject(0).getString("price")));
                BigDecimal sellPri = new BigDecimal(String.valueOf(ask.getJSONObject(0).getString("price")));
                long l = 1000 * 60 * 3 + (RandomUtils.nextInt(10) * 1000L);
                logger.info("????????????:" + System.currentTimeMillis() + "--ordersleeptime:" + ordersleeptime + "--?????????" + l);
                if (System.currentTimeMillis() - ordersleeptime > l) {
                    logger.info("???????????????");
                    boolean type = RandomUtils.nextBoolean();
                    String resultJson = submitTrade(type ? 1 : -1, type ? sellPri : buyPri, new BigDecimal(exchange.get("minTradeLimit")));
                    JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject != null && jsonObject.getInt("code") == 0) {
                        JSONObject data1 = jsonObject.getJSONObject("data");
                        orderIdTwo = data1.getString("order_id");
                        orderIdTwotradeNo = data1.getString("trade_no");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);
                        ordersleeptime = System.currentTimeMillis();
                        logger.info("?????????????????? ????????????" + (type ? "buy" : "sell") + "?????????" + exchange.get("minTradeLimit") + "?????????" + (type ? sellPri : buyPri));
                    }
                }
                BigDecimal intervalPrice = sellPri.subtract(buyPri);

                logger.info("robotId" + id + "----" + "???????????????" + buyPri + "??????????????????" + sellPri);
                logger.info("robotId" + id + "----" + "???????????????????????????" + intervalPrice);

                //??????????????????????????????????????????
//                if ("1".equals(exchange.get("isTradeCheck"))) {
//
//                    //?????????????????????
//                    BigDecimal buyAmount =new BigDecimal(String.valueOf(buyPrices.get(0).get(1)));
//                    BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).get(1)));
//                    BigDecimal minAmount = new BigDecimal(exchange.get("minTradeLimit").toString());
//                    if (maxEatOrder == 0) {
//                        logger.info("??????????????????????????????maxEatOrder=" + maxEatOrder);
//                    } else if (maxEatOrder <= eatOrder) {
//                        setWarmLog(id, 2, "????????????????????????(" + eatOrder + ")=?????????????????????(" + maxEatOrder + "),???????????????????????????", "");
//                        setTradeLog(id, "????????????????????????(" + eatOrder + ")=?????????????????????(" + maxEatOrder + "),???????????????????????????", 0);
//                    }
//
//                    //?????????
//                    if ("1".equals(exchange.get("isTradeCheck")) && buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && "1".equals(exchange.get("isBuyMinLimitAmount"))) {
//                        if ("0".equals(exchange.get("isSuspendTrade"))) {
//                            if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
//                                if (buyAmount.compareTo(minAmount) == -1) {
//                                    buyAmount = minAmount;
//                                }
//                                try {
//                                    String sellOrder = submitTrade(-1, buyPri, buyAmount);
//                                    setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
//                                    logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");
//
//                                    JSONObject jsonObject = judgeRes(sellOrder, "error_code", "submitTrade");
//                                    if (jsonObject != null && jsonObject.getInt("error_code") == 200) {
//                                        sellOrderId = jsonObject.getJSONObject("data").getString("order_id");
//                                    }
//                                    return price;
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        } else {
//                            setTradeLog(id, "??????????????????????????????????????????", 0, "000000");
//                            setWarmLog(id, 2, "??????????????????????????????????????????", "");
//                            setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
//                        }
//                    }
//
//                    //?????????
//                    if ("1".equals(exchange.get("isTradeCheck")) && sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && "1".equals(exchange.get("isSellMinLimitAmount"))) {
//                        //????????????????????????????????????,??????????????????
//                        if ("0".equals(exchange.get("isSuspendTrade"))) {
//                            if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
//                                if (sellAmount.compareTo(minAmount) == -1) {
//                                    sellAmount = minAmount;
//                                }
//                                try {
//                                    String buyOrder = submitTrade(1, sellPri, sellAmount);
//                                    setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
//                                    logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");
//
//                                    JSONObject jsonObject = judgeRes(buyOrder, "error_code", "submitTrade");
//                                    if (jsonObject != null && jsonObject.getInt("error_code") == 0) {
//                                        orderIdOne = jsonObject.getJSONObject("data").getString("order_id");
//                                    }
//                                    return price;
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        } else {
//                            setTradeLog(id, "??????????????????????????????????????????", 0, "000000");
//                            setWarmLog(id, 2, "??????????????????????????????????????????", "");
//                            setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
//                        }
//                    }
//                }
                int openIntervalFromPrice = new BigDecimal(exchange.get("openIntervalFromPrice")).compareTo(intervalPrice);
                if ("1".equals(exchange.get("isOpenIntervalSwitch")) && openIntervalFromPrice < 0) {
                    setTradeLog(id, "??????????????????", 0, "000000");
                    setWarmLog(id, 2, "??????????????????,??????????????????", "");
                    int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
                    sleep(st * 1000 + 60 * 1000 * 2, Integer.parseInt(exchange.get("isMobileSwitch")));

                }

                if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                    buyPrice = buyPri;
                    sellPrice = sellPri;
                }


                BigDecimal disparity = sellPrice.subtract(buyPrice);
                logger.info("robotId" + id + "----" + "???????????????" + buyPrice + "??????????????????" + sellPrice);
                logger.info("robotId" + id + "----" + "???????????????????????????" + disparity);


                Integer newScale = Integer.parseInt(exchange.get("pricePrecision").toString());

                logger.info("robotId" + id + "----" + "????????????" + exchange.get("priceRange"));
                logger.info("robotId" + id + "----" + "????????????????????????" + newScale);
                BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);

                logger.info("robotId" + id + "----" + "????????????" + interval);


                if ("0".equals(exchange.get("sheetForm"))) {
                    logger.info("robotId" + id + "----" + "???????????????");
                    BigDecimal diff = sellPri.subtract(buyPri);
                    BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
                    int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
                    BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, ROUND_DOWN);
                    BigDecimal oldPrice = buyPri.add(random);
                    price = nN(oldPrice, newScale);
                    logger.info("robotId" + id + "----" + "????????????------->" + random);
                    logger.info("robotId" + id + "----" + "??????????????????????????????------->" + oldPrice);
                    logger.info("robotId" + id + "----" + "??????????????????????????????------->" + price);
                    if (price.compareTo(sellPri) < 0 && price.compareTo(buyPri) > 0) {
                        logger.info("robotId" + id + "----" + "???????????????");
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                    } else {
                        setWarmLog(id, 4, "??????????????????????????????????????????1[" + sellPri + "]???1[" + buyPri + "]", "");
                        setTradeLog(id, "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]", 0, "FF111A");
                        logger.info("robotId" + id + "----" + "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]");
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                        logger.info("robotId" + id + "----" + "???????????????????????????");
                        price = null;
                    }
                } else {
                    logger.info("robotId" + id + "----" + "???????????????");
                    BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                    BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                    logger.info("robotId" + id + "----" + "minPrice(??????????????????)???" + minPrice + "???maxPrice(??????????????????)???" + maxPrice);
                    BigDecimal diff = maxPrice.subtract(minPrice);
                    BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                    logger.info("robotId" + id + "----" + "random(????????????)???" + random);

                    price = nN(minPrice.add(random), newScale);
                    logger.info("robotId" + id + "----" + "price(?????????)???" + price);

                    if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                        logger.info("robotId" + id + "----" + "???????????????");
                    } else {
                        buyPrice = BigDecimal.ZERO;
                        sellPrice = BigDecimal.ZERO;
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        logger.info("robotId" + id + "----" + "???????????????????????????");
                        price = null;
                    }
                }


            } else {

                logger.info("???????????????????????? trades(??????????????????)=>" + trades);
                sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                price = null;
            }

            removeSmsRedis(Constant.KEY_SMS_SMALL_INTERVAL);

            return price;
        }

        public void selectOrderDetail (String orderId,int type, String orderIdtradeNo){
//            try {
//                String str = selectOrder(orderId);
//                JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
//                if (jsonObject != null && jsonObject.getInt("error_code") == 0) {
//
//                    JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
//                    int status = data.getInt("status");
//                    if (status == 2) {
//                        setTradeLog(id, "??????id???" + orderId + "????????????", 0, "#67c23a");
//                    } else if (status == -1) {
//                        setTradeLog(id, "??????id???" + orderId + "?????????", 0, "#67c23a");
//                    } else {
//                        String res = cancelTrade(orderId);
//                        JSONObject cancelRes = judgeRes(res, "error_code", "cancelTrade");
//                        setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
//                        setTradeLog(id, "??????[" + orderId + "]=>" + res, 0, "#67c23a");
//                        if (Integer.parseInt(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //??????????????????
//                            orderNum++;
//                            setWarmLog(id, 2, "??????{" + orderId + "}??????,????????????" + orderNum, "");
//                        }
//                    }
//
//
//                }
//            } catch (Exception e) {
//                exceptionMessage = collectExceptionStackMsg(e);
//                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
//                e.printStackTrace();
//            }
        }

        public static void main (String[]args){
            for (; ; ) {

                int type = RandomUtils.nextInt(10) > Double.parseDouble("0.8") * 10 ? 1 : -1;
                System.out.println(type);
            }

        }
    }
