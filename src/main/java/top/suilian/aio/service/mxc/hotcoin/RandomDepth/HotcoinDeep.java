package top.suilian.aio.service.mxc.hotcoin.RandomDepth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.mxc.hotcoin.HotCoinParentService;
import top.suilian.aio.vo.Order;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class HotcoinDeep extends HotCoinParentService {
    public HotcoinDeep(
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
        super.logger = getLogger("coinw/deepNew", id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;
    private String buyOrederId = "";
    private String sellOrederId = "";
    private List<Order> orderVOS = new ArrayList<>();
    private List<Order> orderVOSLast = new ArrayList<>();
    //撞单数量
    private Integer tradeNum=0;

    /**
     * range              区间最大值
     * relishMax          补单数量最大值
     * relishMin          同步深度交易数量最小值
     */

    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            logger.info("深度变化策略设置机器人参数开始");
            setParam();
            logger.info("深度变化设置机器人参数结束");
            logger.info("深度变化设置机器人交易规则开始");
            setPrecision();
            logger.info("深度变化设置机器人交易规则结束");
            start = false;
        }
//        Map<String, String> paramKline = getParamKline();
//        if (!"1".equals(paramKline.get("isdeepRobot"))) {
//            try {
//                logger.info("没开启深度机器人  返回-------------");
//                Thread.sleep(60 * 1000);
//                return;
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        int i1 = RandomUtils.nextInt(5);
        if (2 == i1) {
            try {
                setBalanceRedis();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (orderVOSLast.size() >= 10) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("orderVOSLast错误-------------");
        }
        String depthOrderRange = exchange.get("depthOrderRange");
        String depthCancelNum = exchange.get("depthCancelNum");
        if (tradeNum>=Integer.parseInt(depthCancelNum)){
            setWarmLog(id,2,"闪单撞单达到最大值停止","闪单撞单达到最大值停止");
            try {
                Thread.sleep(60*1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        //撤掉盘口单子
        if (orderVOSLast.size() > 0) {
            List<Order> collect = orderVOSLast.stream().filter(Order::getFirstCancle).collect(Collectors.toList());
            String collect1 = collect.stream().map(Order::getOrderId).collect(Collectors.joining("", "", ""));
            logger.info("撤盘口单--" + collect1);
            for (Order order : collect) {
                if (order.getOrderId() != null) {
                    try {
                        cancelTrade(order.getOrderId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String relishMin = exchange.get("depthOrderLowerLimit");
        String relishMax = exchange.get("depthOrderTopLimit");

        String uri = "/v1/depth";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        params.put("step", 3060);

        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        int deepNum1 = Integer.parseInt(depthOrderRange.split("_")[0]);
        int deepNum2 = Integer.parseInt(depthOrderRange.split("_")[1]);



        String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);


        //获取深度 判断平台撮合是否成功
        com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

        if (tradesObj != null && tradesObj.getInteger("code") == 200) {


            com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");

            com.alibaba.fastjson.JSONObject tick = data.getJSONObject("depth");
            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal buyPriDeep1 = new BigDecimal(String.valueOf(buyPrices.get(deepNum1).get(0)));
            BigDecimal sellPriDeep2 = new BigDecimal(String.valueOf(sellPrices.get(deepNum1).get(0)));

            BigDecimal buyPriRange = new BigDecimal(String.valueOf(buyPrices.get(deepNum2).get(0)));

            BigDecimal sellPriRange = new BigDecimal(String.valueOf(sellPrices.get(deepNum2).get(0)));

            if (sellPri.compareTo(buyPri) <= 0) {
                //平台撮合功能失败
                setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                return;
            }

            int randomRange = RandomUtils.nextInt(5);
            if (randomRange == 0) {
                randomRange = 1;
            }
            int randomRange1 = RandomUtils.nextInt(5);
            if (randomRange1 == 0) {
                randomRange1 = 1;
            }
            int pricePrecision = Integer.parseInt(exchange.get("pricePrecision").toString());

            double pow = Math.pow(10, pricePrecision);
            BigDecimal change = new BigDecimal("1").divide(new BigDecimal(pow), pricePrecision, BigDecimal.ROUND_HALF_UP);
            change = change.multiply(new BigDecimal(randomRange));

            BigDecimal change1 = new BigDecimal("1").divide(new BigDecimal(pow), pricePrecision, BigDecimal.ROUND_HALF_UP);
            change1 = change1.multiply(new BigDecimal(randomRange1));

            for (int i = 0; i < 2; i++) {
                Order orderBuy = new Order();
                BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                BigDecimal orderPrice = getOrderAmount(buyPriRange.toString(), buyPriDeep1.toString(), pricePrecision);
                orderBuy.setType(1);
                orderBuy.setAmount(orderAmount);
                orderBuy.setPrice(orderPrice.setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 买-开始深度单-深度价格：" + buyPriRange + "~~" + buyPriDeep1 + "--补价格" + orderBuy.getPrice());
                orderVOS.add(orderBuy);

                Order orderSell = new Order();
                BigDecimal orderAmountSell = getOrderAmount(relishMin, relishMax, 5);
                BigDecimal orderPriceSell = getOrderAmount(sellPriDeep2.toString(), sellPriRange.toString(), pricePrecision);
                orderSell.setType(2);
                orderSell.setAmount(orderAmountSell);
                orderSell.setPrice(orderPriceSell.setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 卖-开始深度单-深度价格：" + sellPriDeep2 + "~~" + sellPriRange + "--补价格" + orderSell.getPrice());
                orderVOS.add(orderSell);
            }


            logger.info("-----------------开始补单-------------------");
            try {
                for (Order orderVO : orderVOS) {
                    String resultJson = submitOrder(orderVO.getType(), orderVO.getPrice(), orderVO.getAmount());

                    JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject != null && jsonObject.getInt("code") == 200) {
                        String orderId = jsonObject.getJSONObject("data").getString("ID");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        orderVO.setOrderId(orderId);
                    }
                }
                logger.info("补单详情" + com.alibaba.fastjson.JSONObject.toJSONString(orderVOS));
            } catch (Exception e) {

            }

            logger.info("-----------------补单结束-------------------");

            logger.info("-----------------开始撤单-------------------");

            if (orderVOSLast.size() > 0) {
                logger.info("-开始撤单详情" + com.alibaba.fastjson.JSONObject.toJSONString(orderVOSLast));
                List<Order> collect = orderVOSLast.stream().filter(e -> !e.getFirstCancle()).collect(Collectors.toList());
                for (Order order : collect) {
                    if (order.getOrderId() != null) {
                        try {
                            String str = selectOrder(order.getOrderId());
                            JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                            if (jsonObject != null && jsonObject.getInt("code") == 200) {
                                JSONObject datas = jsonObject.getJSONObject("data");
                                int status = datas.getInt("statusCode");
                                if (status==2||status==3){
                                    tradeNum++;
                                    logger.info("闪单撞单--" + order.getOrderId());
                                    setWarmLog(id,2,"闪单撞单,订单{"+order.getOrderId()+"}撤单,撞单数为"+tradeNum,"");
                                }
                            }
                            Thread.sleep(1000);
                            String s = cancelTrade(order.getOrderId());
                            logger.info("撤单--" + order.getOrderId()+"结果："+s);
                        } catch (Exception e) {
                            logger.info("-----------------失败-------------------" + e.getMessage());
                        }
                    }
                }
            }
            orderVOSLast = JSONArray.parseArray(com.alibaba.fastjson.JSONObject.toJSONString(orderVOS), Order.class);
            orderVOS.clear();
            logger.info("-----------------撤单结束-------------------");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    BigDecimal getOrderAmount(String min, String max, double pression) {
        double mind = Double.parseDouble(min);
        double maxd = Double.parseDouble(max);
        if (mind > maxd) {
            double c = maxd;
            maxd = mind;
            mind = c;
        }
        long maxQty = (long) (maxd * Math.pow(10, pression));
        long minty = (long) (mind * Math.pow(10, pression));
        long randNumber = minty + (((long) (new Random().nextDouble() * (maxQty - minty))));
        return new BigDecimal(String.valueOf(randNumber / Math.pow(10, pression)));
    }


}


