package top.suilian.aio.service.pickcoin.newKline;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.pickcoin.PickcoinParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class NewPickcoinKline extends PickcoinParentService {
    public NewPickcoinKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_PICKCOIN_KLINE, id);
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

    private int eatOrder=0;//????????????
    private String transactionRatio="1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    private BigDecimal tradeRatio=new BigDecimal(5);


    public void init() {

        if (start) {
            logger.info("???????????????????????????");
            setParam();
            setTransactionRatio();
            if(exchange.get("tradeRatio")!=null||!"0".equals(exchange.get("tradeRatio"))){
                Double ratio =10*(1/(1+Double.valueOf(exchange.get("tradeRatio"))));
                tradeRatio=new BigDecimal(ratio).setScale(2,BigDecimal.ROUND_HALF_UP);
            }
            logger.info("???????????????????????????");

            logger.info("?????????????????????????????????");
            if (!setPrecision()) {
                return;
            }
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
        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("?????????????????????????????????" + transactionRatio);

        if (runTime < timeSlot) {

            //???????????? ??????????????????????????????
            String trades = httpUtil.get(baseUrl + "/api/spot/v3/instruments/" + exchange.get("market") + "/book");
            JSONObject tradesObj = judgeRes(trades, "asks", "getRandomPrice");

            if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {


                List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

                if (sellPri.compareTo(buyPri) == 0) {
                    //????????????????????????
                    setTradeLog(id, "????????????????????????", 0, "FF111A");
                    return;
                }
            }

            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//?????????????????????
            if (!"0".equals(sellOrderId)) {
                logger.info("????????????sellOrderId???");
                selectOrderDetail(sellOrderId, 0);
                sellOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                logger.info("????????????buyOrderId???");
                selectOrderDetail(buyOrderId, 0);
                buyOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }

            }


            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {
                logger.info("?????????????????????");

                selectOrderDetail(orderIdOne, 1);

                orderIdOne = "0";

                selectOrderDetail(orderIdTwo, 1);


                orderIdTwo = "0";

                String msg = "";
                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    //???????????????????????????
                    //???????????????
                    if ("0".equals(exchange.get("orderOperation"))) {

                        setTradeLog(id, "????????????????????????????????????", 0, "000000");
                        msg = "??????" + getRobotName(this.id) + "????????????????????????!";
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        return;

                    } else if ("1".equals(exchange.get("orderOperation"))) {//?????????

                        setTradeLog(id, "???????????????????????????????????????", 0, "000000");
                        msg = "??????" + getRobotName(this.id) + "??????????????????????????????????????????????????????!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //??????????????????
                        randomNum = 0;

                    } else if ("2".equals(exchange.get("orderOperation"))) {//?????????????????????
                        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("suspendTopLimit")) - Integer.parseInt(exchange.get("suspendLowerLimit"))) + Integer.parseInt(exchange.get("suspendLowerLimit")));
                        setTradeLog(id, "?????????????????????????????????" + st + "??????????????????", 0, "000000");
                        msg = "??????" + getRobotName(this.id) + "?????????????????????????????????????????????????????????????????????!";
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                        //??????????????????
                        randomNum = 0;
                        //??????
                        sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        return;
                    }

                }
            }

            setTradeLog(id, "????????????" + orderNum, 0, "000000");
            if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //??????????????????
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
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
            BigDecimal num =oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(????????????)???" + num);

            BigDecimal random=new BigDecimal(Math.random() * 10 ).setScale(2,BigDecimal.ROUND_HALF_UP);
            int type =random.compareTo(tradeRatio)>0 ? 1 : 2;

            try {
                String resultJson = submitOrder(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "client_oid", "submitTrade");

                if (jsonObject != null && "true".equals(jsonObject.getString("result"))) {

                    String tradeId = jsonObject.getString("client_oid");

                    orderIdOne = tradeId;
                    String resultJson1 = submitOrder(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "client_oid", "submitTrade");

                    if (jsonObject1 != null && "true".equals(jsonObject.getString("result"))) {
                        String tradeId1 = jsonObject1.getString("client_oid");
                        orderIdTwo = tradeId1;
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else {
                        String res = cancelTrade(tradeId);
                        setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "result", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }


                }
            } catch (Exception e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "????????????----------------------------->" + st + "???", 0);
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
                    //   setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    break;
                case 1:
                    if (Integer.parseInt(exchange.get("priceRange")) >= (randomNum + 2)) {
                        randomNum += 1;
                        //      setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum -= 1;
                        //     setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    }
                    break;
                case 2:
                    if (2 <= (randomNum - 1)) {
                        randomNum -= 1;
                        //     setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    } else {
                        randomNum += 1;
                        //     setTradeLog(id, "??????????????????" + value + ":?????????", 1);
                    }
                    break;
            }
        }
        try {
            setBalanceRedis();
        } catch (Exception e) {
            e.printStackTrace();
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
        String trades = httpUtil.get(baseUrl + "/api/spot/v3/instruments/" + exchange.get("market") + "/book");
        JSONObject tradesObj = judgeRes(trades, "asks", "getRandomPrice");

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {


            List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "???????????????" + buyPri + "??????????????????" + sellPri);
            logger.info("robotId" + id + "----" + "???????????????????????????" + intervalPrice);


            //??????????????????????????????????????????
            if ("1".equals(exchange.get("isTradeCheck"))) {

                BigDecimal buyAmount = new BigDecimal(String.valueOf(buyPrices.get(0).get(1)));
                BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).get(1)));
                BigDecimal minAmount = new BigDecimal(String.valueOf(precision.get("minTradeLimit")));
                if (maxEatOrder == 0) {
                    logger.info("??????????????????????????????maxEatOrder=" + maxEatOrder);
                } else if (maxEatOrder <= eatOrder) {
                    setTradeLog(id, "????????????????????????(" + eatOrder + ")=?????????????????????(" + maxEatOrder + "),???????????????????????????", 0);
                }

                //?????????
                if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && "1".equals(exchange.get("isBuyMinLimitAmount"))) {
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            if (buyAmount.compareTo(minAmount) == -1) {
                                buyAmount = minAmount;
                            }
                            try {
                                String sellOrder = submitOrder(2, buyPri, buyAmount);
                                setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                                logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");

                                JSONObject jsonObject = judgeRes(sellOrder, "client_oid", "submitTrade");
                                if (jsonObject != null && "true".equals(jsonObject.get("result"))) {
                                    sellOrderId = jsonObject.getString("client_oid");
                                }

                            } catch (Exception e) {
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
                            if (sellAmount.compareTo(minAmount) == -1) {
                                sellAmount = minAmount;
                            }
                            try {
                                String buyOrder = submitOrder(1, sellPri, sellAmount);
                                setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                                logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");

                                JSONObject jsonObject = judgeRes(buyOrder, "client_oid", "submitTrade");
                                if (jsonObject != null && "true".equals(jsonObject.get("result"))) {
                                    buyOrderId = jsonObject.getString("client_oid");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }else {
                    setTradeLog(id, "??????????????????????????????????????????", 0, "000000");
                    String msg = "????????????????????????????????????" + getRobotName(this.id) + "????????????????????????!";
                    setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                }

            }

            if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                //????????????
                if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount").trim() == null) {
                    //????????????
                    String msg = "??????" + getRobotName(this.id) + "??????????????????????????????,??????????????????????????????!";
                    sendSms(msg, exchange.get("mobile"));
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");

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

            //    setTradeLog(id, "?????????-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "???????????????" + buyPrice + "??????????????????" + sellPrice);
            logger.info("robotId" + id + "----" + "???????????????????????????" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            logger.info("robotId" + id + "----" + "????????????" + exchange.get("priceRange"));
            logger.info("robotId" + id + "----" + "????????????????????????" + newScale);
            BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);


            //     setTradeLog(id, "????????????-------------------------->" + interval, 1);
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
                    //     setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
                    logger.info("robotId" + id + "----" + "???????????????");
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                } else {
                    setTradeLog(id, "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]", 0, "FF111A");
                    logger.info("robotId" + id + "----" + "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]");


                    String msg = "??????" + getRobotName(id) + "??????????????????????????????";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("robotId" + id + "----" + "???????????????????????????");
                    price = null;
                }
            } else {
                logger.info("robotId" + id + "----" + "???????????????");
                //   setTradeLog(id, "???????????????------------------------->" + randomNum, 1);
                BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
                BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
                //   setTradeLog(id, "??????????????????[" + minPrice + "]??????????????????[" + maxPrice + "]", 1);
                logger.info("robotId" + id + "----" + "minPrice(??????????????????)???" + minPrice + "???maxPrice(??????????????????)???" + maxPrice);
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
                logger.info("robotId" + id + "----" + "random(????????????)???" + random);

                price = nN(minPrice.add(random), newScale);
                logger.info("robotId" + id + "----" + "price(?????????)???" + price);

                if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
                    logger.info("?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]");
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

    public void openInterval(BigDecimal sellPrice, List<List<String>> allBids, BigDecimal openIntervalPrice) {

        BigDecimal price;
        for (List<String> bid : allBids) {

            price = new BigDecimal(bid.get(0).toString());
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                break;
            }
            if ("0".equals(exchange.get("openIntervalAllAmount")) || exchange.get("openIntervalAllAmount") == null) {
                logger.info("???????????????????????????");
            } else if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.get(1).toString()))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                break;
            }
            //????????????

            startOpenInterval(new BigDecimal(bid.get(0)), new BigDecimal(bid.get(1)));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitOrder(2, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "client_oid", "submitTrade");
            if (jsonObject != null && "true".equals(jsonObject.getString("result"))) {
                String tradeId = jsonObject.getString("client_oid");
                setTradeLog(id, "???????????????????????????????????????-------------->??????[" + buyPri + "]??????[" + buyAmount + "]", 0);
                //??????????????????
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                String str = selectOrder(tradeId);
                JSONObject jsonObject1 = judgeRes(str, "state", "selectOrder");

                if (jsonObject1 != null) {

                    String status = jsonObject1.getString("state");
                    if (status.equals("2")) {
                        setTradeLog(id, "??????id???" + tradeId + "???????????????", 0, "000000");
                        logger.info("??????id???" + tradeId + "???????????????");
                    } else if (status.equals("-1")) {
                        setTradeLog(id, "??????id???" + tradeId + "?????????", 0, "000000");
                        logger.info("??????id???" + tradeId + "?????????");
                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "result", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????[" + tradeId + "]=>" + res, 0, "000000");
                        logger.info("??????[" + tradeId + "]=>" + res);

                    }
                }

                intervalAmount = intervalAmount.add(buyAmount);
                setTradeLog(id, "???????????????????????????:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
    }



    public void selectOrderDetail(String orderId, int type) {
        try {
            String str = selectOrder(orderId);
            JSONObject jsonObject = judgeRes(str, "state", "selectOrder");

            if (jsonObject != null) {

                String status = jsonObject.getString("state");
                if (status.equals("2")) {
                    setTradeLog(id, "??????id???" + orderId + "???????????????", 0, "000000");
                    logger.info("??????id???" + orderId + "???????????????");
                } else if (status.equals("-1")) {
                    setTradeLog(id, "??????id???" + orderId + "?????????", 0, "000000");
                    logger.info("??????id???" + orderId + "?????????");
                } else {
                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //??????????????????
                        orderNum++;
                    }
                    String res = cancelTrade(orderId);
                    JSONObject cancelRes = judgeRes(res, "result", "cancelTrade");
                    setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    setTradeLog(id, "??????[" + orderId + "]=>" + res, 0, "000000");
                    logger.info("??????[" + orderId + "]=>" + res);

                }
            } else {
                String res = cancelTrade(orderId);
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            e.printStackTrace();
        }
    }
}
