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
    private int eatOrder=0;//????????????
    private String transactionRatio="1";

    public void init() {
        logger.info("\r\n------------------------------{" + id + "} ??????------------------------------\r\n");
        cnt++;
        //???????????????????????????????????????????????????????????????????????????
        if (start) {
            logger.info("???????????????????????????");
            setParam();
            logger.info("???????????????????????????");

            logger.info("?????????????????????????????????");
            boolean flag = setPrecision();
            if (!flag) {
                return;
            }

            logger.info("?????????????????????????????????");
            //??????????????????
            while (randomNum==1 || randomNum==Integer.parseInt(exchange.get("priceRange"))) {
                randomNum = (int) Math.ceil(Math.random() * Integer.parseInt(exchange.get("priceRange")));
            }
            start = false;
        }
        setTransactionRatio();
        int index = Integer.valueOf(new Date().getHours());
        //???????????????????????????????????????
        transactionRatio = transactionArr[index];
        if (transactionRatio.equals("0") || transactionRatio.equals("")) {
            transactionRatio = "1";
        }
        logger.info("?????????????????????????????????" + transactionRatio);
        if (runTime < Integer.parseInt(exchange.get("timeSlot"))) {
            String param = null;//????????????
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
                    //????????????????????????
                    setTradeLog(id, "????????????????????????", 0, "FF111A");
                }
            }

            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//?????????????????????

            if (!"0".equals(sellOrderId)) {
                selectOrderDetail(sellOrderId, 0);
                sellOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }
            }


            if (!"0".equals(buyOrderId)) {
                selectOrderDetail(buyOrderId, 0);
                buyOrderId = "0";
                if(maxEatOrder!=0){
                    eatOrder++;
                    setTradeLog(id, "????????????????????????:" + eatOrder + ";?????????????????????:" + maxEatOrder, 0);
                }

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
            logger.info("num(????????????)???" + num);

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
                        String message = "??????" + getRobotName(id) + "???????????????";
                        logger.info(message);
                        judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);

                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                        setTradeLog(id, "??????[" + tradeId + "]=> " + res, 0, "000000");
                    }
                } else if (jsonObject != null && jsonObject.getString("status").equals("account_failure_0002")) {

                    String message = "??????" + getRobotName(id) + "???????????????";
                    logger.info(message);
                    judgeSendMessage(Integer.parseInt(exchange.get("isMobileSwitch")), message, exchange.get("mobile"), Constant.KEY_SMS_INSUFFICIENT);

                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
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
        String trades;
        String param = null;//????????????
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

            logger.info("???????????????" + buyPri + "??????????????????" + sellPri);
            BigDecimal intervalPrice = sellPri.subtract(buyPri);
            logger.info("???????????????????????????" + intervalPrice);

            //?????????????????????
            BigDecimal buyAmount = new BigDecimal(String.valueOf(jsonArrayBuys.getJSONObject(0).getString("amount")));
            BigDecimal sellAmount = new BigDecimal(String.valueOf(jsonArraySells.getJSONObject(0).getString("amount")));
            BigDecimal minAmount = new BigDecimal(exchange.get("minTradeLimit").toString());
            int maxEatOrder = Integer.parseInt(exchange.get("maxEatOrder"));//?????????????????????
            if(maxEatOrder==0){
                logger.info("??????????????????????????????maxEatOrder="+maxEatOrder);
            }else if (maxEatOrder <= eatOrder) {
                setTradeLog(id, "????????????????????????(" + eatOrder + ")=?????????????????????(" + maxEatOrder + "),???????????????????????????", 0);
            }
            //?????????
            if (buyAmount.compareTo(new BigDecimal(exchange.get("buyMinLimitAmount"))) < 1 && maxEatOrder==0) {
                try {
                    if(buyAmount.compareTo(minAmount) == -1){
                        buyAmount = minAmount;
                    }
                    String sellOrder = submitTrade(2, buyPri, buyAmount);
                    setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                    logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");

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
                    setTradeLog(id, "???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]", 0);
                    logger.info("???????????????:??????[" + buyAmount + "],??????:[" + buyPri + "]");

                    JSONObject jsonObject = judgeRes(sellOrder, "status", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                        sellOrderId = jsonObject.getString("data");
                    }
                    return price;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            //?????????
            if (sellAmount.compareTo(new BigDecimal(exchange.get("sellMinLimitAmount"))) < 1 && maxEatOrder ==0) {
                try {
                    if(sellAmount.compareTo(minAmount) == -1){
                        sellAmount = minAmount;
                    }
                    String buyOrder = submitTrade(1, sellPri, sellAmount);
                    setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                    logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");

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
                    setTradeLog(id, "???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]", 0);
                    logger.info("???????????????:??????[" + sellAmount + "],??????:[" + sellPri + "]");

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
                //????????????
                if (new BigDecimal(exchange.get("openIntervalAllAmount")).compareTo(intervalAmount) < 0) {
                    setRobotArgs(id, "isOpenIntervalSwitch", "0");
                    setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                } else {
                    //????????????
                    openInterval(sellPri, jsonArrayBuys, new BigDecimal(exchange.get("openIntervalPrice")));
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
            logger.info("????????????" + exchange.get("priceRange"));
            logger.info("????????????????????????" + newScale);
            BigDecimal interval = nN(disparity.divide(new BigDecimal(exchange.get("priceRange")), newScale, ROUND_DOWN), newScale);
            setTradeLog(id, "????????????-------------------------->" + interval, 1);
            logger.info("????????????" + interval);
            BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, ROUND_DOWN);
            logger.info("???????????????interval(?????????)???" + interval + "???minInterval(???????????????)???" + minInterval + "???randomNum(?????????????????????)???" + randomNum + "???buyPri(????????????)???" + buyPri + "???sellPri(????????????)" + sellPri + "???buyPrice(????????????)???" + buyPrice + "???sellPrice(????????????)???" + sellPrice);
            logger.info("???????????????????????????????????????????????????????????????");
            if (interval.compareTo(minInterval) < 0) {
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
                    price = null;
                }
            } else {
                logger.info("???????????????");
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
                    price = null;
                }
            }
        } else {
            logger.info("???????????????????????? trades(??????????????????)=>" + trades);
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            setTradeLog(id, "??????????????????", 0, "FF111A");
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
                setTradeLog(id, "???????????????????????????????????????,??????????????????", 0, "000000");
                break;
            }
            //????????????

            startOpenInterval(new BigDecimal(allBids.getJSONObject(0).getString("price")), new BigDecimal(allBids.getJSONObject(0).getString("amount")));
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
                String str = selectOrder(orderIdOne);
                JSONObject result = judgeRes(str, "status", "selectOrder");
                if (result != null && result.getString("status").equals("success")) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.getString("status");
                    if ("COMPLE-TRADE".equals(status)) {
                        setTradeLog(id, "??????????????????id???" + tradeId + "????????????", 0, "000000");
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
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
            JSONObject jsonObject = judgeRes(str, "status", "selectOrder");
            if (jsonObject != null && jsonObject.getString("status").equals("success")) {
                JSONObject data = jsonObject.getJSONObject("data");
                String status = data.getString("status");
                if ("COMPLE-TRADE".equals(status)) {
                    setTradeLog(id, "??????id???" + orderId + "????????????", 0, "000000");
                } else if ("WITHDRAWN".equals(status)) {
                    setTradeLog(id, "??????id???" + orderId + "?????????", 0, "000000");
                } else {
                    String res = cancelTrade(orderId);
                    JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                    setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_QUANTIFICATION);
                    setTradeLog(id, "??????[" + orderId + "]=>" + res, 0, "000000");
                    if (Integer.valueOf(exchange.get("orderSumSwitch")) == 1 && type == 1) {    //??????????????????
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
