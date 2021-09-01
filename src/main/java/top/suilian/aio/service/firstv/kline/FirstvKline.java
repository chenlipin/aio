//package top.suilian.aio.service.firstv.kline;
//
//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
//import top.suilian.aio.Util.Constant;
//import top.suilian.aio.Util.HttpUtil;
//import top.suilian.aio.redis.RedisHelper;
//import top.suilian.aio.service.*;
//import top.suilian.aio.service.firstv.FirstvParentService;
//
//import java.io.UnsupportedEncodingException;
//import java.math.BigDecimal;
//import java.util.*;
//
//import static java.math.BigDecimal.ROUND_DOWN;
//
//public class FirstvKline extends FirstvParentService {
//    public FirstvKline(
//            CancelExceptionService cancelExceptionService,
//            CancelOrderService cancelOrderService,
//            ExceptionMessageService exceptionMessageService,
//            RobotArgsService robotArgsService,
//            RobotLogService robotLogService,
//            RobotService robotService,
//            TradeLogService tradeLogService,
//            HttpUtil httpUtil,
//            RedisHelper redisHelper,
//            int id
//    ) {
//        super.cancelExceptionService = cancelExceptionService;
//        super.cancelOrderService = cancelOrderService;
//        super.exceptionMessageService = exceptionMessageService;
//        super.robotArgsService = robotArgsService;
//        super.robotLogService = robotLogService;
//        super.robotService = robotService;
//        super.tradeLogService = tradeLogService;
//        super.httpUtil = httpUtil;
//        super.redisHelper = redisHelper;
//        super.id = id;
//        super.logger = getLogger(Constant.KEY_LOG_PATH_FCHAIN_KLINE, id);
//    }
//
//    private BigDecimal intervalAmount = BigDecimal.ZERO;
//    private BigDecimal buyPrice = BigDecimal.ZERO;
//    private BigDecimal sellPrice = BigDecimal.ZERO;
//    private String orderIdOne = "0";
//    private String orderIdTwo = "0";
//    private String typeOne = "0";
//    private String typeTwo = "0";
//    private boolean start = true;
//    private int orderNum = 0;
//    private int runTime = 0;
//    private int randomNum;
//
//
//    public void init() {
//
//        if (start) {
//            start = false;
//            logger.info("设置机器人参数开始");
//            setParam();
//            logger.info("设置机器人参数结束");
//
//            logger.info("设置机器人交易规则开始");
//            setPrecision();
//            logger.info("设置机器人交易规则结束");
//            //随机交易区间
//            randomNum = (int) Math.ceil(Math.random() * priceRange.intValue());
//        }
//        if (runTime < Integer.parseInt(timeSlot)) {
//
//            //判断 平台撮合是否成功
//            String trades = httpUtil.get(baseUrl + "/api/open/depth/" + market.toUpperCase());
//            JSONObject tradesObj = judgeRes(trades, "error", "getRandomPrice");
//
//
//
//
//
//
//
//
//
//
//
//            if (orderSumSwitch == 1) {    //防褥羊毛开关
//                if (!"".equals(orderIdOne) && !"".equals(orderIdTwo)) {
//                    try {
//                        String str = selectOrder(orderIdOne, typeOne);
//                        System.out.println(typeOne);
//                        System.out.println("是否成交orderIdOne--------->" + str);
//                        if (str != null) {
//                            JSONObject jsonObject = new JSONObject().fromObject(str);
//                            JSONObject resultData = jsonObject.getJSONObject("data").getJSONObject("order");
//                            if (resultData != null && jsonObject.get("msg").equals("success")) {
//                                String status = resultData.getString("status");
//                                if ("1".equals(status)) {
//                                    setTradeLog(id, "订单id：" + orderIdOne + "完全成交", 0, "000000");
//                                } else if ("2".equals(status)) {
//                                    setTradeLog(id, "订单id：" + orderIdOne + "已撤单", 0, "000000");
//                                } else {
//                                    String res = cancelTrade(orderIdOne, typeOne);
//                                    JSONObject cancelResult = JSONObject.fromObject(res);
//                                    String cancelStatus = (String) cancelResult.get("msg");
//                                    if (cancelStatus.equals("success")) {
//                                        setTradeLog(id, "撤单[" + orderIdOne + "]=>" + res, 0, "000000");
//                                        orderNum++;
//                                    }
//
//                                }
//                            }
//
//
//                        }
//                    } catch (UnsupportedEncodingException e) {
//                        logger.info(e.getMessage());
//                        e.printStackTrace();
//                    }
//
//                    try {
//                        String str = selectOrder(orderIdTwo, typeTwo);
//                        System.out.println(typeTwo);
//                        System.out.println("是否成交orderIdTwo--------->" + str);
//                        if (str != null) {
//                            JSONObject jsonObject = new JSONObject().fromObject(str);
//                            JSONObject resultData = jsonObject.getJSONObject("data").getJSONObject("order");
//                            if (resultData != null && jsonObject.get("msg").equals("success")) {
//                                String status = resultData.getString("status");
//                                if ("1".equals(status)) {
//                                    setTradeLog(id, "订单id：" + orderIdTwo + "完全成交", 0, "000000");
//                                } else if ("2".equals(status)) {
//                                    setTradeLog(id, "订单id：" + orderIdTwo + "已撤单", 0, "000000");
//                                } else {
//                                    String res = cancelTrade(orderIdTwo, typeTwo);
//                                    JSONObject cancelResult = JSONObject.fromObject(res);
//                                    String cancelStatus = (String) cancelResult.get("msg");
//                                    if (cancelStatus.equals("success")) {
//                                        setTradeLog(id, "撤单[" + orderIdTwo + "]=>" + res, 0, "000000");
//                                        orderNum++;
//                                    }
//
//                                }
//                            }
//
//
//                        }
//                    } catch (UnsupportedEncodingException e) {
//                        logger.info(e.getMessage());
//                        e.printStackTrace();
//                    }
//                    if (orderNum >= Integer.parseInt(orderSum)) {
//                        if (this.isMobileSwitch == 1) {
//                            String msg = "您的" + getRobotName(this.id) + "量化机器人已停止!";
//                            JSONObject rest = sendSms(msg, this.mobile);
//                            if (rest.getInt("code") != 0) {
//                                sendSms(msg, this.mobile);
//                            }
//                        }
//                        setTradeLog(id, "撤单数达到上限，停止量化", 0, "000000");
//                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
//                        break;
//                    }
//                }
//            }
//
//            setTradeLog(id, "撤单数为" + orderNum, 0, "000000");
//            setTradeLog(id, "停止量化撤单数设置为：" + orderSum, 0, "000000");
//            BigDecimal price = getRandomPrice();
//            Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
//            Double numThreshold1 = Double.valueOf(numThreshold);
//
//            Double minNum = Double.valueOf(numMinThreshold);
//
////                    BigDecimal num = BigDecimal.valueOf(numThreshold1).subtract(BigDecimal.valueOf(minTradeLimit)).multiply(BigDecimal.valueOf(Math.random())).add(BigDecimal.valueOf(minTradeLimit));
//
//
//            Random rand = new Random();
//
//            int max = (int) (numThreshold1 * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
//            int min = (int) (minNum * Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision")))));
//            int randNumber = rand.nextInt(max - min + 1) + min;
//
//            BigDecimal num = new BigDecimal(String.valueOf(randNumber / Math.pow(10, Double.valueOf(String.valueOf(precision.get("amountPrecision"))))));
//
////                    BigDecimal num = BigDecimal.valueOf(numThreshold1).subtract(BigDecimal.valueOf(minTradeLimit)).multiply(BigDecimal.valueOf(Math.random())).add(BigDecimal.valueOf(minTradeLimit));
//
////                    Double num = Math.ceil(Math.random() * Integer.parseInt(numThreshold) + 1) + Math.random();
//
////                    var num = parseInt((Math.random() * numThreshold + 1), 10) + Math.random();
//
//            int type = Math.ceil(Math.random() * 10 + 1) > 5 ? 0 : 1;
//
//
//            try {
//                String resultJson = submitTrade(type, price, num);
//                Thread.sleep(300);
//                if (resultJson != null && !resultJson.equals("")) {
//                    JSONObject jsonObject = new JSONObject().fromObject(resultJson);
//                    if (jsonObject.get("msg").equals("success")) {
//                        String tradeId = jsonObject.getJSONObject("data").getString("order_id");
//                        orderIdOne = tradeId;
//                        typeOne = String.valueOf(type);
//                        String resultJson1 = submitTrade(type == 0 ? 1 : 0, price, num);
//                        JSONObject jsonObject2 = new JSONObject().fromObject(resultJson1);
//                        if (resultJson1 == null || !jsonObject2.get("msg").equals("success")) {
//                            String res = cancelTrade(tradeId, typeOne);
//                            setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
//                        } else {
//                            if (jsonObject2.get("msg").equals("success")) {
//
//                                orderIdTwo = jsonObject2.getJSONObject("data").getString("order_id");
//                                typeTwo = String.valueOf(type == 0 ? 1 : 0);
//                            } else {
//                                String res = cancelTrade(tradeId, typeOne);
//
//                                setTradeLog(id, "撤单[" + tradeId + "]=> " + res, 0, "000000");
//                            }
//                        }
//                    }
//
//                }
//            } catch (UnsupportedEncodingException e) {
//                logger.info(e.getMessage());
//                e.printStackTrace();
//            }
//            int st = (int) (Math.random() * (endTime - startTime) + startTime);
//            setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
//            runTime += (st);
//            setTradeLog(id, "累计周期时间----------------------------->" + runTime + "秒", 1);
//            try {
//                Thread.sleep(st * 1000);
//            } catch (InterruptedException e) {
//                logger.info(e.getMessage());
//                e.printStackTrace();
//            }
//        } else {
//            runTime = 0;
//            List<Integer> string = new ArrayList<>();
//            string.add(1);
//            string.add(2);
//            Integer value = string.get((int) Math.round(Math.random() * (string.size() - 1)));
//            switch (value) {
//                case 0:
//                    setTradeLog(id, "当前随机值（" + value + ":横盘）", 1);
//                    break;
//                case 1:
//                    if (priceRange >= (randomNum + 1)) {
//                        randomNum += 1;
//                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
//                    } else {
//                        randomNum -= 1;
//                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
//                    }
//                    break;
//                case 2:
//                    if (1 <= (randomNum - 1)) {
//                        randomNum -= 1;
//                        setTradeLog(id, "当前随机值（" + value + ":跌幅）", 1);
//                    } else {
//                        randomNum += 1;
//                        setTradeLog(id, "当前随机值（" + value + ":涨幅）", 1);
//                    }
//                    break;
//            }
//        }
//        clearLog();
//
//    }
//
//
//    /**
//     * 获得随机价格
//     *
//     * @return
//     */
//    public BigDecimal getRandomPrice() {
//        BigDecimal price = null;
//
//
//        String trades = httpUtil.get(baseUrl + "/api/open/depth/" + market.toUpperCase());
//
//
//        if (!"".equals(trades) && trades != null && !trades.isEmpty()) {
//
//            JSONObject tradesObj = JSONObject.fromObject(trades);
//
//            if (tradesObj.get("msg").equals("success") && tradesObj.get("code").equals("0")) {
//                JSONObject data = tradesObj.getJSONObject("data");
//
//                List<List<String>> buyPrices = (List<List<String>>) data.get("bids");
//
//                List<List<String>> sellPrices = (List<List<String>>) data.get("asks");
//
//                JSONObject buyPriceOne = JSONObject.fromObject(buyPrices.get(0));
//                JSONObject sellPriceOne = JSONObject.fromObject(sellPrices.get(0));
//
//
//                BigDecimal buyPri = new BigDecimal(buyPriceOne.getString("price"));
//                BigDecimal sellPri = new BigDecimal(sellPriceOne.getString("price"));
//
//                BigDecimal intervalPrice = sellPri.subtract(buyPri);
//
//                if (isOpenIntervalSwitch == 1 /*&& intervalPrice.compareTo(openIntervalFromPrice) < 1*/) {
//                    //刷开区间
//                    if (openIntervalAllAmount.compareTo(intervalAmount) < 0) {
//                        setRobotArgs(id, "isOpenIntervalSwitch", "0");
//                        setTradeLog(id, "刷开区间的数量已达到最大值,停止刷开区间", 0, "000000");
//                    } else {
//                        //刷开区间
//                        openInterval(sellPri, buyPrices, openIntervalPrice);
//                    }
//
//                }
//                if ((buyPrice == BigDecimal.ZERO && sellPrice == BigDecimal.ZERO) || runTime == 0) {
//                    buyPrice = buyPri;
//                    sellPrice = sellPri;
//                }
//
//                setTradeLog(id, "区间值-------------------------->" + randomNum, 1);
//
//                BigDecimal disparity = sellPrice.subtract(buyPrice);
//
//                Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());
//
//                BigDecimal interval = nN(disparity.divide(BigDecimal.valueOf(priceRange), newScale, BigDecimal.ROUND_UP), newScale);
//
//                setTradeLog(id, "区间差值-------------------------->" + interval, 1);
//                BigDecimal minInterval = new BigDecimal("1").divide(BigDecimal.valueOf(Math.pow(10, newScale)), newScale, BigDecimal.ROUND_UP);
//                if (interval.compareTo(minInterval) < 0) {
//                    BigDecimal diff = sellPri.subtract(buyPri);
//                    BigDecimal ss = diff.multiply(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))));
//                    int randomInt = (int) (1 + Math.random() * (ss.intValue() - 2 + 1));
//                    BigDecimal random = new BigDecimal(String.valueOf(randomInt)).divide(BigDecimal.valueOf(Math.pow(10, Double.valueOf(newScale))), newScale, BigDecimal.ROUND_UP);
//                    System.out.println("随机增长------->" + random);
//                    BigDecimal oldPrice = buyPri.add(random);
//                    System.out.println("oldPrice------->" + oldPrice);
//                    price = nN(oldPrice, newScale);
//                    System.out.println("格式化后价格------->" + price);
//                    logger.info("格式化后价格------->" + price);
//                    logger.info("sellPri------->" + sellPri);
//                    logger.info("buyPri------->" + buyPri);
//                    if (price.compareTo(sellPri) < 0 && price.compareTo(buyPri) > 0) {
//                        setTradeLog(id, "旧版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
//                        System.out.println("旧版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]");
//                        buyPrice = BigDecimal.ZERO;
//                        sellPrice = BigDecimal.ZERO;
//                    } else {
//                        setTradeLog(id, "买一卖一区间过小，无法量化------------------->卖1[" + sellPri + "]买1[" + buyPri + "]", 0, "FF111A");
//
//                        if (isMobileSwitch == 1 && valid == 1) {
//                            if (fristInto == 0) {
//                                fristInto = 1;
//                                fristTime = new Date().getTime();
//                            } else if (fristInto == 1) {
//                                lastInto = 1;
//                                lastTime = new Date().getTime();
//                            }
//                            if ((lastTime - fristTime) > 5 * 1000 * 60) {
//                                String msg = "您的" + getRobotName(id) + "区间过小，无法量化！";
//
//                                JSONObject rest = sendSms(msg, mobile);
//                                if (rest.getInt("code") != 0) {
//                                    sendSms(msg, mobile);
//                                }
//                                fristInto = 0;
//                                lastInto = 0;
//                                valid = 0;
//                            }
//                        }
//                        try {
//                            Thread.sleep(20000);
//                        } catch (InterruptedException e) {
//                            logger.info(e.getMessage());
//                            e.printStackTrace();
//                        }
//                        buyPrice = BigDecimal.ZERO;
//                        sellPrice = BigDecimal.ZERO;
//                        price = getRandomPrice();
//                    }
//                } else {
//                    setTradeLog(id, "随机区间值------------------------->" + randomNum, 1);
//                    BigDecimal minPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum - 1)));
//                    BigDecimal maxPrice = buyPrice.add(interval.multiply(BigDecimal.valueOf(randomNum)));
//                    setTradeLog(id, "区间最小价格[" + minPrice + "]区间最大价格[" + maxPrice + "]", 1);
//                    BigDecimal diff = maxPrice.subtract(minPrice);
//                    BigDecimal random = diff.subtract(diff.multiply(BigDecimal.valueOf(Math.random())));
//
//                    price = nN(minPrice.add(random), newScale);
//
//                    if (price.compareTo(buyPri) > 0 && price.compareTo(sellPri) < 0) {
//                        setTradeLog(id, "新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]", 1);
//                        System.out.println("新版本------------------->卖1[" + sellPri + "]买1[" + buyPri + "]新[" + price + "]");
//                    } else {
//                        buyPrice = BigDecimal.ZERO;
//                        sellPrice = BigDecimal.ZERO;
//                        try {
//                            Thread.sleep(20000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        price = getRandomPrice();
//                    }
//                }
//
//            }
//
//
//        } else {
//
//            try {
//                Thread.sleep(20000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            price = getRandomPrice();
//        }
//
//
//        return price;
//    }
//
//
//    public void openInterval(BigDecimal sellPrice, List<List<String>> allBids, BigDecimal openIntervalPrice) {
//
//    }
//
//    private void startOpenInterval(BigDecimal buyPri, BigDecimal buyAmount) {
//    }
//
//    /**
//     * 机器人参数获取
//     */
//    public void setParam() {
//        this.orderSum = robotArg(id, "orderSum");
//        this.timeSlot = robotArg(id, "timeSlot");
//        this.priceRange = Double.valueOf(robotArg(id, "priceRange"));
//        this.numThreshold = robotArg(id, "numThreshold");
//        this.market = robotArg(id, "market");
//        this.apikey = robotArg(id, "apikey");
//        this.tpass = robotArg(id, "tpass");
//        this.orderSumSwitch = Integer.parseInt(robotArg(id, "orderSumSwitch"));
//        this.startTime = Integer.parseInt(robotArg(id, "startTime"));
//        this.endTime = Integer.parseInt(robotArg(id, "endTime"));
//        this.mobile = robotArg(id, "mobile");
//        this.isMobileSwitch = Integer.parseInt(robotArg(id, "isMobileSwitch"));
////        this.isOpenIntervalSwitch = Integer.parseInt(robotArg(id, "isOpenIntervalSwitch"));
////        this.openIntervalAllAmount = new BigDecimal(robotArg(id, "openIntervalAllAmount"));
////        this.openIntervalPrice = new BigDecimal(robotArg(id, "openIntervalPrice"));
//        this.loginFiled = robotArg(id, "loginFiled");
//        this.password = robotArg(id, "password");
//        this.numMinThreshold = robotArg(id, "numMinThreshold");
//    }
//
//    /**
//     * 交易规则获取
//     */
//    public void setPrecision() {
//        precision.put("pricePrecision", 4);
//
//        precision.put("amountPrecision", 4);
//
//        precision.put("minTradeLimit", 0.1);
//    }
//}
