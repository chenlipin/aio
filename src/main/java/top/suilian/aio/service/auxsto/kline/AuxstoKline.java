package top.suilian.aio.service.auxsto.kline;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.auxsto.AuxstoParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_DOWN;

public class AuxstoKline extends AuxstoParentService {
    public AuxstoKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_AUXSTO_KLINE, id);
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
    private String transactionRatio = "1";

    public void init() {

        if (start) {
            start = false;
            logger.info("???????????????????????????");
            setParam();
            logger.info("???????????????????????????");

            logger.info("?????????????????????????????????");
            setPrecision();
            logger.info("?????????????????????????????????");
            //??????????????????
            while (randomNum == 1 || randomNum == Integer.parseInt(exchange.get("priceRange"))) {
                randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
            }

        }
        setTransactionRatio();
        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio=transactionArr[index];
        if(transactionRatio.equals("0")||transactionRatio.equals("")){
            transactionRatio="1";
        }
        logger.info("?????????????????????????????????"+transactionRatio);


        if (runTime < Integer.parseInt(exchange.get("timeSlot"))) {


            //???????????? ??????????????????????????????
            String trades = httpUtil.get("https://openapi.auxsto.com/open/api/market_dept?symbol=" + exchange.get("market") + "&type=step0");
            JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

            if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {


                JSONObject data = tradesObj.getJSONObject("data");

                JSONObject tick = data.getJSONObject("tick");
                List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

                if (sellPri.compareTo(buyPri) == 0) {
                    //????????????????????????
                    setTradeLog(id, "????????????????????????", 0, "FF111A");
                    return;
                }
            }


            if (!"0".equals(sellOrderId)) {
                selectOrderDetail(sellOrderId, 0);
                sellOrderId = "0";
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0);
                buyOrderId = "0";

            }


            if (!"0".equals(orderIdOne) && !"0".equals(orderIdTwo)) {

                selectOrderDetail(orderIdOne, 1);
                orderIdOne = "0";

                selectOrderDetail(orderIdTwo, 1);

                orderIdTwo = "0";

                if (orderNum >= Integer.parseInt(exchange.get("orderSum"))) {
                    String msg = "??????" + getRobotName(this.id) + "????????????????????????!";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_CANCEL_MAX_STOP);
                    setTradeLog(id, "????????????????????????????????????", 0, "000000");
                    setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                    return;
                }
            }

            setTradeLog(id, "????????????" + orderNum, 0, "000000");
            if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //??????????????????
                setTradeLog(id, "?????????????????????????????????" + exchange.get("orderSum"), 0, "000000");
            }
            BigDecimal price = getRandomPrice();


            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            Double minNum = Double.valueOf(exchange.get("numMinThreshold"));
            long max = (long) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long min = (long) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
            long randNumber = min + (((long) (new Random().nextDouble() * (max - min))));


            BigDecimal oldNum = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
            BigDecimal num=oldNum.multiply(new BigDecimal(transactionRatio));
            logger.info("robotId" + id + "----" + "num(????????????)???" + num);

            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 1 : 2;


            try {
                String resultJson = submitTrade(type, price, num);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                if (jsonObject != null && jsonObject.getInt("code") == 0) {


                    JSONObject data = jsonObject.getJSONObject("data");
                    String tradeId = data.getString("order_id");

                    orderIdOne = tradeId;
                    String resultJson1 = submitTrade(type == 1 ? 2 : 1, price, num);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 0) {
                        JSONObject data1 = jsonObject1.getJSONObject("data");
                        orderIdTwo = data1.getString("order_id");
                        removeSmsRedis(Constant.KEY_SMS_INSUFFICIENT);

                    } else {
                        String res = cancelTrade(tradeId);
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
            }
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "????????????----------------------------->" + st + "???", 0);
            runTime += (st);
            setTradeLog(id, "??????????????????----------------------------->" + runTime + "???", 1);
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
        String trades = httpUtil.get("https://openapi.auxsto.com/open/api/market_dept?symbol=" + exchange.get("market") + "&type=step0");
        JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {

            JSONObject data = tradesObj.getJSONObject("data");

            JSONObject tick = data.getJSONObject("tick");
            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal intervalPrice = sellPri.subtract(buyPri);

            logger.info("robotId" + id + "----" + "???????????????" + buyPri + "??????????????????" + sellPri);
            logger.info("robotId" + id + "----" + "???????????????????????????" + intervalPrice);


            //?????????????????????
            BigDecimal buyAmount = new BigDecimal(String.valueOf(buyPrices.get(0).get(1)));
            BigDecimal sellAmount = new BigDecimal(String.valueOf(sellPrices.get(0).get(1)));

            /*//?????????
            if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1) {
                try {
               String sellOrder =    submitTrade(2, buyPri, buyAmount);
                    setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                    logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");
                     JSONObject jsonObject = judgeRes(sellOrder, "status", "submitTrade");
            if (resultJson != null && !resultJson.equals("") && jsonObject != null) {
                sellOrderId = jsonObject.getString("order_id");}
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            //?????????
            if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1) {
                try {
                String buyOrder =     submitTrade(1, sellPri, sellAmount);
                    setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                    logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");
                    JSONObject jsonObject = judgeRes(buyOrder, "status", "submitTrade");
            if (resultJson != null && !resultJson.equals("") && jsonObject != null) {
                buyOrderId = jsonObject.getString("order_id");}
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


            if (Integer.parseInt(exchange.get("isOpenIntervalSwitch")) == 1 && intervalPrice.compareTo(new BigDecimal(exchange.get("openIntervalFromPrice"))) < 1) {
                //????????????

                if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                } else {

            String msg = "??????" + getRobotName(this.id) + "??????????????????????????????!";
            sendSms(msg, exchange.get("mobile"));          //????????????
                    openInterval(sellPri, buyPrices, new BigDecimal(exchange.get("openIntervalPrice")));
                }

            }*/
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


                    String msg = "??????" + getRobotName(id) + "??????????????????????????????";
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), msg, exchange.get("mobile"), Constant.KEY_SMS_SMALL_INTERVAL);
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    buyPrice = BigDecimal.ZERO;
                    sellPrice = BigDecimal.ZERO;
                    logger.info("robotId" + id + "----" + "???????????????????????????");
                    price = getRandomPrice();
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
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    logger.info("robotId" + id + "----" + "???????????????????????????");
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


    public void openInterval(BigDecimal sellPrice, List<List<String>> allBids, BigDecimal openIntervalPrice) {

        BigDecimal price;
        for (List<String> bid : allBids) {

            price = new BigDecimal(bid.get(0).toString());
            if (price.compareTo(sellPrice.subtract(openIntervalPrice)) < 0) {
                continue;
            }
            if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount.add(new BigDecimal(bid.get(1).toString()))) < 0) {
                setRobotArgs(id, "isOpenIntervalSwitch", "0");
                setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                continue;
            }
            //????????????

            startOpenInterval(new BigDecimal(bid.get(0)), new BigDecimal(bid.get(1)));
        }
    }

    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
        try {
            String resultJson = submitTrade(2, buyPri, buyAmount);
            JSONObject jsonObject = judgeRes(resultJson, "status", "submitTrade");
            if (resultJson != null && !resultJson.equals("") && jsonObject != null) {
                String tradeId = jsonObject.getString("order_id");
                setTradeLog(id, "???????????????????????????????????????-------------->??????[" + buyPri + "]??????[" + buyAmount + "]", 0);
                //??????????????????
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));


                String str = selectOrder(tradeId);
                JSONObject result = judgeRes(str, "status", "selectOrder");

                if (str != null && result != null) {

                    String status = (String) result.get("status");
                    if ("FILLED".equals(status)) {
                        setTradeLog(id, "??????????????????id???" + tradeId + "????????????", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
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
        }
    }


    public void selectOrderDetail(String orderId, int type) {
        try {
            String str = selectOrder(orderId);
            JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
            if (jsonObject != null && jsonObject.getInt("code") == 0) {

                JSONObject data = jsonObject.getJSONObject("data");

                JSONObject orderInfo = data.getJSONObject("order_info");


                if (orderInfo != null) {

                    String status = orderInfo.getString("status_msg");


                    if (status.equals("????????????")) {
                        setTradeLog(id, "??????id???" + orderId + "????????????", 0, "000000");
                    } else if (status.equals("?????????")) {
                        setTradeLog(id, "??????id???" + orderId + "?????????", 0, "000000");
                    } else {
                        String res = cancelTrade(orderId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelTrade");
                        setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????[" + orderId + "]=>" + res, 0, "000000");
                        if (type == 1) {
                            if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1) {    //??????????????????
                                orderNum++;
                            }
                        }
                    }

                } else {
                    setTradeLog(id, "??????id???" + orderId + "?????????", 0, "000000");

                }


            }
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            e.printStackTrace();
        }
    }

}
