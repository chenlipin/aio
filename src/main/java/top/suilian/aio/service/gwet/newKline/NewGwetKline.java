package top.suilian.aio.service.gwet.newKline;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.gwet.GwetParentService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;


public class NewGwetKline extends GwetParentService {
    public NewGwetKline(
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
    ){
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_GWET_KLINE, id);
    }

    private BigDecimal intervalAmount = BigDecimal.ZERO;
    private BigDecimal buyPrice = BigDecimal.ZERO;
    private BigDecimal sellPrice = BigDecimal.ZERO;
    private String orderIdOne = "0";
    private String orderIdTwo = "0";
    private String ordeONoOne = "0";
    private String ordeONoTwo = "0";
    private boolean start = true;
    private int orderNum = 0;
    private int runTime = 0;
    private int randomNum = 1;

    private String sellONonum = "0";
    private String buyONonum = "0";
    private String sellOrderId = "0";
    private String buyOrderId = "0";
    private int eatOrder = 0;//????????????
    private String transactionRatio = "1";
    private int maxEatOrder = 0;
    private int timeSlot = 1;
    private BigDecimal tradeRatio = new BigDecimal(5);

    public void init() {
        if (start) {
            logger.info("???????????????????????????");
            setParam();
            setTransactionRatio();
            if (exchange.get("tradeRatio") != null || !"0".equals(exchange.get("tradeRatio"))) {
                Double ratio = 10 * (1 / (1 + Double.valueOf(exchange.get("tradeRatio"))));
                tradeRatio = new BigDecimal(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
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

        try {
            String trade1=submitTrade(2,new BigDecimal("1"),new BigDecimal("2"));
            String[] strarray1=trade1.split("/");
            JSONObject jsonObject1 = judgeRes(strarray1[0], "code", "submitTrade");
            String tradeId = String.valueOf(jsonObject1.getLong("data"));
            selectOrder(tradeId);
            String trade2=submitTrade(1,new BigDecimal("1"),new BigDecimal("1"));
            String[] strarray2=trade1.split("/");
            JSONObject jsonObject2 = judgeRes(strarray2[0], "code", "submitTrade");
            //String tradeId1 = String.valueOf(jsonObject1.getInt("data"));
            selectOrder(tradeId);
            String trade3=submitTrade(1,new BigDecimal("1"),new BigDecimal("1"));
            String[] strarray3=trade3.split("/");
            JSONObject jsonObject3 = judgeRes(strarray3[0], "code", "submitTrade");
            //String tradeId3 = String.valueOf(jsonObject1.getInt("data"));
            selectOrder(tradeId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("?????????????????????????????????" + transactionRatio);

        if (runTime < timeSlot) {
            String trades = httpUtil.get("https://www.gwet.io/m/depth/" + exchange.get("market"));
            JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

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
                selectOrderDetail(sellOrderId, 0,sellONonum);
                sellOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0,buyONonum);
                buyOrderId = "0";
                if (maxEatOrder != 0) {
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }

            }

            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {

                selectOrderDetail(orderIdOne, 1,ordeONoOne);

                orderIdOne = "0";

                selectOrderDetail(orderIdTwo, 1,ordeONoTwo);

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
            BigDecimal num = oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(????????????)???" + num);

            BigDecimal random = new BigDecimal(Math.random() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            int type = random.compareTo(tradeRatio) > 0 ? 1:2;

            try {
                String resultJson = submitTrade(type, price, num);
                String[] strarray=resultJson.split("/");
                JSONObject jsonObject = judgeRes(strarray[0], "msg", "submitTrade");

                if (jsonObject != null && jsonObject.getString("msg").equals("success")) {

                    String tradeId = String.valueOf(jsonObject.getInt("data"));
                    String OnoNum = strarray[1];

                    orderIdOne = tradeId;
                    ordeONoOne = strarray[1];
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    String[] strarray1=resultJson1.split("/");
                    JSONObject jsonObject1 = judgeRes(strarray1[0], "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getString("msg").equals("success")) {
                        orderIdTwo = jsonObject1.getString("data");
                        ordeONoTwo = strarray1[1];
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else {
                        String res = cancelTrade(tradeId,OnoNum);
                        setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId" + id + "----" + exceptionMessage);
                e.printStackTrace();
            }catch (IOException e){
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


        }else{
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
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
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
        String trades = httpUtil.get("https://www.gwet.io/m/depth/" + exchange.get("market"));
        JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");
        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {

            List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");
            List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "???????????????" + buyPri + "??????????????????" + sellPri);
            logger.info("robotId" + id + "----" + "???????????????????????????" + intervalPrice);

            if ("1".equals(exchange.get("isTradeCheck"))) {
                BigDecimal buyAmount = new BigDecimal(String.valueOf(buyPrices.get(0).get(1)));
                BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).get(1)));
                BigDecimal minAmount = new BigDecimal(exchange.get("minTradeLimit").toString());

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
                                String sellOrder = submitTrade(2, buyPri, buyAmount);
                                String[] strarray=sellOrder.split("/");
                                setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                                logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");
                                JSONObject jsonObject = judgeRes(strarray[0], "msg", "submitTrade");
                                if (jsonObject != null && jsonObject.getString("msg").equals("success")) {
                                    sellOrderId = jsonObject.getString("data");
                                    sellONonum = strarray[1];
                                }
                                return price;
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

                if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && "1".equals(exchange.get("isSellMinLimitAmount"))) {
                    //????????????????????????????????????,??????????????????
                    if ("0".equals(exchange.get("isSuspendTrade"))) {
                        if (maxEatOrder == 0 || maxEatOrder > eatOrder) {
                            if (sellAmount.compareTo(minAmount) == -1) {
                                sellAmount = minAmount;
                            }
                            try {
                                String buyOrder = submitTrade(1, sellPri, sellAmount);
                                String[] strarray=buyOrder.split("/");
                                setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                                logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");
                                JSONObject jsonObject = judgeRes(strarray[0], "msg", "submitTrade");
                                if (jsonObject != null && jsonObject.getString("msg").equals("success")) {
                                    buyOrderId = jsonObject.getString("data");
                                    buyONonum = strarray[1];
                                }
                                return price;
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

            setTradeLog(id, "?????????-------------------------->" + randomNum, 1);

            BigDecimal disparity = sellPrice.subtract(buyPrice);
            logger.info("robotId" + id + "----" + "???????????????" + buyPrice + "??????????????????" + sellPrice);
            logger.info("robotId" + id + "----" + "???????????????????????????" + disparity);


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

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
                    setTradeLog(id, "?????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]???[" + price + "]", 1);
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
                    setTradeLog(id, "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]", 0, "FF111A");
                    logger.info("robotId" + id + "----" + "???????????????????????????????????????------------------->???1[" + sellPri + "]???1[" + buyPri + "]");


                    String msg = "??????" + getRobotName(id) + "??????????????????????????????";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + "???????????????????????????");
                    price = null;
                }
            }
        }else{
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
            String resultJson = submitTrade(2, buyPri, buyAmount);
            String[] strarray=resultJson.split("/");
            JSONObject jsonObject = judgeRes(strarray[0], "status", "submitTrade");
            if (resultJson != null && !resultJson.equals("") && jsonObject != null) {
                String tradeId = jsonObject.getString("data");
                setTradeLog(id, "???????????????????????????????????????-------------->??????[" + buyPri + "]??????[" + buyAmount + "]", 0);
                //??????????????????
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                String str = selectOrder(tradeId);
                JSONObject result = judgeRes(str, "msg", "selectOrder");

                if (str != null && result.getInt("state")==1) {

                    String status = (String) result.get("total");
                    if ("1".equals(status)) {
                        setTradeLog(id, "??????????????????id???" + tradeId + "????????????", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId,strarray[1]);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????????????????[" + tradeId + "]=>" + res, 0, "000000");
                    }


                }


                intervalAmount = intervalAmount.add(buyAmount);
                setTradeLog(id, "???????????????????????????:" + intervalAmount, 0, "000000");
            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }catch (IOException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
    }


    public void selectOrderDetail(String OrderId, int type,String ONoNum) {
        try{
            String str=selectOrder(OrderId);
            JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
            if (jsonObject != null && jsonObject.getInt("state")==1) {
                List<JSONObject> data=jsonObject.getJSONArray("trades");

            }

        }catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            e.printStackTrace();
        }
    }
}
