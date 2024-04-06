package top.suilian.aio.service.bika.deepChange;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.basic.OrderVO;
import top.suilian.aio.service.bika.BikaaParentService;
import top.suilian.aio.vo.Order;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BikaDeep extends BikaaParentService {
    public BikaDeep(
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
        super.logger = getLogger("bika/replenish", id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;
    private String buyOrederId = "";
    private String sellOrederId = "";
    private List<Order> orderVOS = new ArrayList<>();
    private List<Order> orderVOSLast = new ArrayList<>();

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
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("orderVOSLast错误-------------");
        }
        System.out.println("------------"+JSON.toJSONString(orderVOSLast));
        //撤掉盘口单子
        if (orderVOSLast.size() > 0) {
            List<Order> collect = orderVOSLast.stream().filter(Order::getFirstCancle).collect(Collectors.toList());
            String collect1 = collect.stream().map(Order::getOrderId).collect(Collectors.joining("", "", ""));
            logger.info("撤盘口单--" + collect1);
            for (Order order : collect) {
                if (order.getOrderId() != null) {
                    try {
                        cancelTrade(order.getOrderId());
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String trades = httpUtil.get(baseUrl + "/sapi/v1/depth?symbol=" + exchange.get("market").toUpperCase());
        String relishMin = exchange.get("relishMin");
        String relishMax = exchange.get("relishMax");

        //获取深度 判断平台撮合是否成功
        com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

        if (tradesObj != null) {

            List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

            int range = Integer.parseInt(exchange.get("range").toString());

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            BigDecimal buyPriRange = new BigDecimal(String.valueOf(buyPrices.get(range).get(0)));
            BigDecimal sellPriRange = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            if (sellPri.compareTo(buyPri) <= 0) {
                //平台撮合功能失败
                setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                return;
            }

            int typeChange = RandomUtils.nextInt(3);
            int randomRange = RandomUtils.nextInt(5);
            if (randomRange == 0) {
                randomRange = 1;
            }
            int randomRange1 = RandomUtils.nextInt(5);
            if (randomRange1 == 0) {
                randomRange1 = 1;
            }
            int amountPrecision = Integer.parseInt(exchange.get("amountPrecision").toString());
            int pricePrecision = Integer.parseInt(exchange.get("pricePrecision").toString());

            double pow = Math.pow(10, pricePrecision);
            BigDecimal change = new BigDecimal("1").divide(new BigDecimal(pow), pricePrecision, BigDecimal.ROUND_HALF_UP);
            change = change.multiply(new BigDecimal(randomRange));

            BigDecimal change1 = new BigDecimal("1").divide(new BigDecimal(pow), pricePrecision, BigDecimal.ROUND_HALF_UP);
            change1 = change1.multiply(new BigDecimal(randomRange1));


            if (typeChange == 0) {
                //买盘盘口增加
                Order order = new Order();
                order.setFirstCancle(true);
                BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                order.setType(1);
                order.setAmount(orderAmount);
                order.setPrice(buyPri.add(change).setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 买-开始补盘口单-盘口价格：" + buyPri + "随机-" + change.setScale(4, RoundingMode.HALF_UP).toPlainString() + "--补价格" + order.getPrice().setScale(pricePrecision, RoundingMode.HALF_UP));
                orderVOS.add(order);
                //卖盘盘口增加
                Order order1 = new Order();
                order1.setFirstCancle(true);
                BigDecimal orderAmount1 = getOrderAmount(relishMin, relishMax, 5);
                order1.setAmount(orderAmount1);
                order1.setType(2);
                order1.setPrice(sellPri.subtract(change1).setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 卖-开始补盘口单-盘口价格：" + sellPri + "随机-" + change1.toPlainString() + "--补价格" + order1.getPrice().setScale(pricePrecision, RoundingMode.HALF_UP));
                orderVOS.add(order1);
            } else if (typeChange == 1) {
                //买盘盘口增加
                Order order = new Order();
                order.setFirstCancle(true);
                BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                order.setType(1);
                order.setAmount(orderAmount);
                order.setPrice(buyPri.add(change));
                logger.info("- 买-开始补盘口单-盘口价格：" + buyPri + "随机-" + change.toPlainString() + "--补价格" + order.getPrice().setScale(pricePrecision, RoundingMode.HALF_UP));
                orderVOS.add(order);
            } else {
                //卖盘盘口增加
                Order order1 = new Order();
                order1.setFirstCancle(true);
                BigDecimal orderAmount1 = getOrderAmount(relishMin, relishMax, 5);
                order1.setAmount(orderAmount1);
                order1.setType(2);
                order1.setPrice(sellPri.subtract(change1).setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 买-开始补盘口单-盘口价格：" + buyPri + "随机-" + change1 + "--补价格" + order1.getPrice().setScale(pricePrecision, RoundingMode.HALF_UP));
                orderVOS.add(order1);
            }

            for (int i = 0; i < 2; i++) {
                Order orderBuy = new Order();
                BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                BigDecimal orderPrice = getOrderAmount(buyPriRange.toString(), buyPri.toString(), 5);
                orderBuy.setType(1);
                orderBuy.setAmount(orderAmount);
                orderBuy.setPrice(orderPrice.setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 买-开始深度单-深度价格：" + buyPriRange + "~~" + buyPri + "--补价格" + orderBuy.getPrice());
                orderVOS.add(orderBuy);

                Order orderSell = new Order();
                BigDecimal orderAmountSell = getOrderAmount(relishMin, relishMax, 5);
                BigDecimal orderPriceSell = getOrderAmount(sellPri.toString(), sellPriRange.toString(), 5);
                orderSell.setType(2);
                orderSell.setAmount(orderAmountSell);
                orderSell.setPrice(orderPriceSell.setScale(pricePrecision, RoundingMode.HALF_UP));
                logger.info("- 卖-开始深度单-深度价格：" + sellPri + "~~" + sellPriRange + "--补价格" + orderSell.getPrice());
                orderVOS.add(orderSell);
            }


            logger.info("-----------------开始补单-------------------");
            try {
                for (Order orderVO : orderVOS) {
                    String resultJson = submitOrder(orderVO.getType(), orderVO.getPrice(), orderVO.getAmount());
                    JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject != null && jsonObject.getString("origQty") != null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        String orderId = jsonObject.getJSONArray("orderId").getString(0);
                        orderVO.setOrderId(orderId);
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
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
                            cancelTrade(order.getOrderId());
                        } catch (Exception e) {
                            logger.info("-----------------失败-------------------" + e.getMessage());
                        }
                    }
                }
            }
            logger.info("-----------------开始撤单111-------------------");
            orderVOSLast = JSONArray.parseArray(com.alibaba.fastjson.JSONObject.toJSONString(orderVOS), Order.class);
            orderVOS.clear();
            logger.info("-----------------撤单结束-------------------");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    BigDecimal getOrderAmount(String min, String max, double pression) {
        double mind = Double.parseDouble(min);
        double maxd = Double.parseDouble(max);
        long maxQty = (long) (maxd * Math.pow(10, pression));
        long minty = (long) (mind * Math.pow(10, pression));
        long randNumber = minty + (((long) (new Random().nextDouble() * (maxQty - minty))));
        return new BigDecimal(String.valueOf(randNumber / Math.pow(10, pression)));
    }




}


