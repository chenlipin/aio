package top.suilian.aio.service.weex.replenish;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.nivex0.NivexParentService;
import top.suilian.aio.service.weex.WeexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeexReplenish extends WeexParentService {
    public WeexReplenish(
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
        super.logger = getLogger("weex/replenish", id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;

    /**
     * range              区间最大值
     * relishMax          补单数量最大值
     * relishMin          补单数量最小值
     * relishOrderQty     补单每次补单单数
     * maxLeftQty        补单left最大币量
     * maxRightQty       补单right最大币量
     * orderType         补单 1:买单 2:卖单  3：买卖单
     */

    public void init()  {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            logger.info("补单策略设置机器人参数开始");
            setParam();
            logger.info("补单策略设置机器人参数结束");
            logger.info("补单策略设置机器人交易规则开始");
            setPrecision();
            logger.info("补单策略设置机器人交易规则结束");
            start = false;
        }
        int i1 = RandomUtils.nextInt(5);
        if(2==i1){
            try {
                setBalanceRedis();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }


        BigDecimal buyPri=null;
        BigDecimal sellPri=null;

        String trades = httpUtil.get(baseUrl +"/api/v2/market/depth?symbol="+exchange.get("market")+"&type=step0&limit=15");


        //获取深度 判断平台撮合是否成功
        com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

        if (tradesObj != null && tradesObj.getString("code").equals("00000") ) {
            com.alibaba.fastjson.JSONObject tick = tradesObj.getJSONObject("data");

            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

             buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
             sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            if (sellPri.compareTo(buyPri) == 0) {
                //平台撮合功能失败
                setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                return;
            }
        }

            String relishMin = exchange.get("relishMin");
            String relishMax = exchange.get("relishMax");

            //待挂单集合
            List<Order> orderList = new ArrayList<Order>();
            //补单方案
            int orderType = Integer.parseInt(exchange.get("orderType"));
            //最大盘口差值
            BigDecimal maxrange = new BigDecimal(exchange.get("range"));
            //当前盘口差值
            BigDecimal nowRange = sellPri.subtract(buyPri);
            //需要补的差值
            BigDecimal needRange = nowRange.subtract(maxrange);

            Double amountPrecision = Double.parseDouble(precision.get("amountPrecision").toString());
            Double pricePrecision = Double.parseDouble(precision.get("pricePrecision").toString());

            if (nowRange.compareTo(maxrange) > 0) {
                setTradeLog(id, "买一[" + buyPri + "],卖一[" + sellPri + "],差值大于:" + maxrange + ";开始补单", 0, "a61b12");
                setWarmLog(id, 2, "买一[" + buyPri + "],卖一[" + sellPri + "],差值大于:" + maxrange + ";开始补单", "");
                logger.info("买一[" + buyPri + "],卖一[" + sellPri + "],差值" + nowRange + ";大于:" + maxrange);
                BigDecimal bigDecimal1 = new BigDecimal(Math.pow(10, pricePrecision) + "");

                BigDecimal bigDecimal = BigDecimal.ONE.divide(bigDecimal1, pricePrecision.intValue(), RoundingMode.HALF_DOWN);
                //可以挂单的区间数
                BigDecimal ranggeInt = needRange.divide(bigDecimal, 0, RoundingMode.HALF_DOWN);
                //可以挂单的次数
                int relishOrderQty = Math.min(Integer.parseInt(ranggeInt.toString()), Integer.parseInt(exchange.get("relishOrderQty")));
                logger.info("可以挂单的区间数==》" + Integer.parseInt(ranggeInt.toString()));
                logger.info("可以挂单的次数=》" + relishOrderQty);
                BigDecimal basePrice = BigDecimal.ZERO;
                if (orderType != 3) {
                    //挂买或者卖单
                    basePrice = orderType == 1 ? buyPri : sellPri;
                    if (orderType == 1) {
                        logger.info("-----都补买单-------");
                        //买单
                        BigDecimal oneRange = needRange.divide(new BigDecimal(relishOrderQty), pricePrecision.intValue(), BigDecimal.ROUND_HALF_DOWN);
                        for (int i = 0; i < relishOrderQty; i++) {
                            Order order = new Order();
                            Double random = RandomUtilsme.getRandom(Double.parseDouble(oneRange.toString()), pricePrecision.intValue());
                            if(i==relishOrderQty-1){
                                random=Double.parseDouble(oneRange.toString());
                            }
                            logger.info("一个区间的价格：" + oneRange + "---随机的价格" + random);
                            BigDecimal orderPrice = basePrice.add(oneRange.multiply(new BigDecimal(i))).add(new BigDecimal(random.toString()));
                            BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, amountPrecision);
                            order.setPrice(orderPrice);
                            order.setAmount(orderAmount);
                            order.setType(1);
                            orderList.add(order);
                        }
                    } else {
                        //卖单
                        logger.info("-----都补卖单-------");
                        BigDecimal oneRange = needRange.divide(new BigDecimal(relishOrderQty), pricePrecision.intValue(), BigDecimal.ROUND_HALF_DOWN);
                        for (int i = 0; i < relishOrderQty; i++) {
                            Order order = new Order();
                            Double random = RandomUtilsme.getRandom(Double.parseDouble(oneRange.toString()), pricePrecision.intValue());
                            if(i==relishOrderQty-1){
                                random=Double.parseDouble(oneRange.toString());
                            }
                            logger.info("一个区间的价格：" + oneRange + "---随机的价格" + random);
                            BigDecimal orderPrice = basePrice.subtract(oneRange.multiply(new BigDecimal(i))).subtract(new BigDecimal(random.toString()));
                            BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, amountPrecision);
                            order.setPrice(orderPrice);
                            order.setAmount(orderAmount);
                            order.setType(2);
                            orderList.add(order);
                        }
                    }
                    logger.info(JSON.toJSONString(orderList));
                    logger.info("-----------------开始补单-------------------");
                    replenish(orderList);
                    logger.info("-----------------补单结束-------------------");
                    try {
                        Thread.sleep(300000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.info("-----买卖单都补-------");
                    BigDecimal oneRange = needRange.divide(new BigDecimal(relishOrderQty), pricePrecision.intValue(), RoundingMode.HALF_DOWN);
                    for (int i = 0; i < relishOrderQty / 2; i++) {
                        Order order = new Order();
                        Order order1 = new Order();
                        BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, amountPrecision);
                        BigDecimal orderAmount1 = getOrderAmount(relishMin, relishMax, amountPrecision);
                        Double random = RandomUtilsme.getRandom(Double.parseDouble(oneRange.toString()), pricePrecision.intValue());
                        if(i==relishOrderQty / 2-1){
                            random=Double.parseDouble(oneRange.toString());
                        }
                        logger.info("一个区间的价格：" + oneRange.toPlainString() + "---随机的价格" + random.toString());
                        BigDecimal orderPrice = sellPri.subtract(oneRange.multiply(new BigDecimal(i))).subtract(new BigDecimal(random.toString())).setScale(pricePrecision.intValue(), BigDecimal.ROUND_HALF_DOWN);
                        BigDecimal orderPrice1 = buyPri.add(oneRange.multiply(new BigDecimal(i))).add(new BigDecimal(random.toString())).setScale(pricePrecision.intValue(), BigDecimal.ROUND_HALF_DOWN);
                        if(i==relishOrderQty / 2-1){
                            orderPrice1=orderPrice1.add(oneRange);
                        }
                        order.setPrice(orderPrice);
                        order.setAmount(orderAmount);
                        order.setType(2);
                        order1.setPrice(orderPrice1);
                        order1.setAmount(orderAmount1);
                        order1.setType(1);
                        orderList.add(order);
                        orderList.add(order1);
                    }
                }
                logger.info(JSON.toJSONString(orderList));
                logger.info("-----------------开始补单-------------------");
                replenish(orderList);
                logger.info("-----------------补单结束-------------------");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                int anInt = RandomUtils.nextInt(5);
                if (anInt == 3) {
                    setTradeLog(id, "买一[" + buyPri + "],卖一[" + sellPri + "],差值小于:" + maxrange + ";正常无需补单", 0, "1cd66c");
                }
                try {
                    Thread.sleep(8000);
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

    @Data
    class Order {
        private Integer type;
        private BigDecimal price;
        private BigDecimal amount;
    }

    void replenish(List<Order> orderList) {
        RobotArgs robotArgs = robotArgsService.findOne(id, "market");
        String[] split = robotArgs.getRemark().split("_");
        BigDecimal maxLeftQty = new BigDecimal(exchange.get("maxLeftQty"));
        BigDecimal maxRightQty = new BigDecimal(exchange.get("maxRightQty"));


        for (Order order : orderList) {
            BigDecimal maxLeftQty_redis = redisHelper.get("maxLeftQty_" + id) != null ? new BigDecimal(redisHelper.get("maxLeftQty_" + id)) : BigDecimal.ZERO;
            BigDecimal maxRightQty_redis = redisHelper.get("maxRightQty_" + id) != null ? new BigDecimal(redisHelper.get("maxRightQty_" + id)) : BigDecimal.ZERO;

            if (order.type == 1) {
                if (maxRightQty_redis.add(order.getAmount()).compareTo(maxRightQty) > 0) {
                    setTradeLog(id, split[1] + "现在已补单" + maxRightQty_redis + "再补单将超出最大补单量" + maxRightQty + "停止补单", 0, "1cd66c");
                    setWarmLog(id, 2, split[1] + "现在已补单" + maxRightQty_redis + "再补单将超出最大补单量" + maxRightQty + "停止补单", "");
                    break;
                }
            } else {
                if (maxLeftQty_redis.add(order.getAmount()).compareTo(maxLeftQty) > 0) {
                    setTradeLog(id, split[0] + "现在已补单" + maxRightQty_redis + "再补单将超出最大补单量" + maxRightQty + "停止补单", 0, "1cd66c");
                    setWarmLog(id, 2, split[0] + "现在已补单" + maxRightQty_redis + "再补单将超出最大补单量" + maxRightQty + "停止补单", "");
                    break;
                }
            }
            String trade = submitOrder(order.getType(), order.getPrice(), order.getAmount());
            setTradeLog(id, "补单=》挂" + (order.getType() == 1 ? "买" : "卖") + "单[价格：" + order.getPrice() + ": 数量" + order.getAmount() + "]=>" + trade, 0, order.getType() == 1 ? "05cbc8" : "ff6224");
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if ("00000" .equals(jsonObject.getString("code")) ) {
                if (order.type == 1) {
                    redisHelper.setSt("maxRightQty_" + id, maxRightQty_redis.add(order.getAmount()).toString());
                } else {
                    redisHelper.setSt("maxLeftQty_" + id, maxLeftQty_redis.add(order.getAmount()).toString());
                }
            }
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


