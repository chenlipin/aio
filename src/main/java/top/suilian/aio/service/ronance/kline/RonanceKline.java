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
            logger.info("???????????????????????????");
            setParam();
            logger.info("???????????????????????????");

            logger.info("?????????????????????????????????");
            setPrecision();
            logger.info("?????????????????????????????????");
            start = false;

            //??????????????????
            while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
            }
        }
        setTransactionRatio();
        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("?????????????????????????????????" + transactionRatio);

        if (runTime < Integer.parseInt(timeSlot)) {
            System.out.println(runTime + "runtime---------timeSlot:" + timeSlot);


            //???????????? ??????????????????????????????
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
                    //????????????????????????
                    setTradeLog(id, "????????????????????????", 0, "FF111A");
                    return;
                }
            }


            if (orderSumSwitch == 1) {    //??????????????????
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
                                        setTradeLog(id, "??????id???" + orderIdOne + "????????????", 0, "000000");
                                    } else if (status == 4) {
                                        setTradeLog(id, "??????id???" + orderIdOne + "?????????", 0, "000000");
                                    } else {
                                        String res = cancelTrade(orderIdOne);
                                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                        setTradeLog(id, "??????[" + orderIdOne + "]=>" + res, 0, "000000");
                                        orderNum++;
                                        orderIdOne = "0";
                                    }

                                } else if (orderInfo.getString("id").equals(orderIdTwo)) {
                                    int status = orderInfo.getInt("status");
                                    if (status == 3) {
                                        setTradeLog(id, "??????id???" + orderIdTwo + "????????????", 0, "000000");
                                    } else if (status == 4) {
                                        setTradeLog(id, "??????id???" + orderIdTwo + "?????????", 0, "000000");
                                    } else {
                                        String res = cancelTrade(orderIdTwo);
                                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                        setCancelOrder(cancelRes, res, orderIdOne, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                        setTradeLog(id, "??????[" + orderIdTwo + "]=>" + res, 0, "000000");
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
                            String msg = "??????" + getRobotName(this.id) + "????????????????????????!";
                            JSONObject rest = sendSms(msg, this.mobile);
                            if (rest.getInt("code") != 0) {
                                sendSms(msg, this.mobile);
                            }
                        }
                        setTradeLog(id, "????????????????????????????????????", 0, "000000");
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        return;
                    }
                }
            }

            setTradeLog(id, "????????????" + orderNum, 0, "000000");
            setTradeLog(id, "?????????????????????????????????" + orderSum, 0, "000000");
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
            logger.info("robotId" + id + "----" + "num(????????????)???" + num);

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


                        String message = "??????" + getRobotName(id) + "???????????????";
                        judgeSendMessage(isMobileSwitch, message, mobile, Constant.KEY_SMS_INSUFFICIENT);


                    } else {
                        String res = cancelTrade(tradeId);
                        setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
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
            setTradeLog(id, "????????????----------------------------->" + st + "???", 0);
            runTime += (st);
            setTradeLog(id, "??????????????????----------------------------->" + runTime + "???", 1);
            sleep(st * 1000, isMobileSwitch);
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
                    if (priceRange >= (randomNum + 2)) {
                        randomNum += 1;
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum -= 1;
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum += 1;
                        setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    }
                    break;
            }
        }
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
        String trades = httpUtil.get(baseUrl + "/api/v1/market/depthData?market=" + market + "&depth=0");
        JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

        if (tradesObj != null && tradesObj.getInt("code") == 200) {
            JSONObject data = tradesObj.getJSONObject("data");
            List<JSONObject> buyPrices = (List<JSONObject>) data.get("asks");
            List<JSONObject> sellPrices = (List<JSONObject>) data.get("bids");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).getString("price")));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).getString("price")));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "???????????????" + buyPri + "??????????????????" + sellPri);
            logger.info("robotId" + id + "----" + "???????????????????????????" + intervalPrice);


            if (isOpenIntervalSwitch == 1 && intervalPrice.compareTo(openIntervalFromPrice) < 1) {
                //????????????

                if (openIntervalAllAmount.compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                } else {
                    //????????????
                    String msg = "??????" + getRobotName(this.id) + "??????????????????????????????!";
                    sendSms(msg, this.mobile);
                    openInterval(sellPri, buyPrices, openIntervalPrice);
                }

            }
            if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
                buyPrice = buyPri;
                sellPrice = sellPri;
            }

            setTradeLog(id, "?????????-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "???????????????" + buyPrice + "??????????????????" + sellPrice);
            logger.info("robotId" + id + "----" + "???????????????????????????" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            logger.info("robotId" + id + "----" + "????????????" + priceRange);
            logger.info("robotId" + id + "----" + "????????????????????????" + newScale);
            BigDecimal interval = nN(disparity.divide(BigDecimal.valueOf(priceRange), newScale, ROUND_DOWN), newScale);


            setTradeLog(id, "????????????-------------------------->" + interval, 1);
            logger.info("robotId" + id + "----" + "????????????" + interval);

            BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, ROUND_DOWN);
            logger.info("robotId" + id + "----" + "???????????????interval(?????????)???" + interval + "???minInterval(???????????????)???" + minInterval + "???randomNum(?????????????????????)???" + randomNum + "???buyPri(????????????)???" + buyPri + "???sellPri(????????????)" + sellPri + "???buyPrice(????????????)???" + buyPrice + "???sellPrice(????????????)???" + sellPrice);
            logger.info("robotId" + id + "----" + "???????????????????????????????????????????????????????????????");
            if (interval.compareTo(minInterval) < 0) {
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
                    setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "???????????????");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setTradeLog(id, "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]", 0, "FF111A");
                    logger.info("robotId" + id + "----" + "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]");


                    String message = "??????" + getRobotName(id) + "??????????????????????????????";

                    String msg = "??????" + getRobotName(id) + "??????????????????????????????";
                    judgeSendMessage(isMobileSwitch, msg, mobile, Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, isMobileSwitch);
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("robotId" + id + "----" + "???????????????????????????");
                    price = null;
                }
            } else {
                logger.info("robotId" + id + "----" + "???????????????");
                setTradeLog(id, "???????????????------------------------->" + randomNum, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                setTradeLog(id, "??????????????????[" + minPrice + "]??????????????????[" + maxPrice + "]", 1);
                logger.info("robotId" + id + "----" + "minPrice(??????????????????)???" + minPrice + "???maxPrice(??????????????????)???" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("robotId" + id + "----" + "random(????????????)???" + random);

                price = nN(minPrice.add(random), newScale);
                logger.info("robotId" + id + "----" + "price(?????????)???" + price);

                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                    setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "???????????????");
                } else {
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    sleep(2000, isMobileSwitch);
                    logger.info("robotId" + id + "----" + "???????????????????????????");
                    price = null;
                }
            }


        } else {

            sleep(2000, isMobileSwitch);

            logger.info("robotId" + id + "----" + "????????????????????????");
            setTradeLog(id, "??????????????????", 0);
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
                setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                continue;
            }
            //????????????
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
                setTradeLog(id, "???????????????????????????????????????-------------->??????[" + buyPri + "]??????[" + buyAmount + "]", 0);
                //??????????????????
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
                                    setTradeLog(id, "??????????????????id???" + tradeId + "????????????", 0, "000000");
                                } else if (status == 4) {
                                    setTradeLog(id, "??????????????????id???" + tradeId + "?????????", 0, "000000");
                                } else {

                                    sleep(200, isMobileSwitch);

                                    String res = cancelTrade(tradeId);
                                    JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                                    setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                                    setTradeLog(id, "??????????????????[" + tradeId + "]=>" + res, 0, "000000");
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
                setTradeLog(id, "???????????????????????????:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, isMobileSwitch);
            logger.info("robotId" + id + "----" + exceptionMessage);
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     */
    public void setPrecision() {
        precision.put("minTradeLimit", robotArg(id, "minTradeLimit"));
        precision.put("amountPrecision", robotArg(id, "amountPrecision"));
        precision.put("pricePrecision", robotArg(id, "pricePrecision"));
    }
}
