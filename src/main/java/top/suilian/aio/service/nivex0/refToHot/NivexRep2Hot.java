package top.suilian.aio.service.nivex0.refToHot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.DeepVo;
import top.suilian.aio.refer.HotcoinUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.gate.GateParentService;
import top.suilian.aio.service.nivex0.NivexParentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class NivexRep2Hot extends NivexParentService {
    public NivexRep2Hot(
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
        super.logger = getLogger("gate/refer2hot", id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;
    List<String> nowOrderList = new ArrayList<>();
    List<String> lastOrderList = new ArrayList<>();
    BigDecimal point = null;

    boolean up = true;
    int time = 0;

    /**
     * range              同步深度数量
     * relishMax          同步深度交易数量最大值值
     * relishMin          同步深度交易数量最小值
     * relishMark        补单每次补单单数
     * relishPoint       同步交易数量倍数
     */

    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            logger.info("对标策略设置机器人参数开始");
            setParam();
            logger.info("对标策略设置机器人参数结束");
            logger.info("对标策略设置机器人交易规则开始");
            setPrecision();
            logger.info("补单策略设置机器人交易规则结束");
            start = false;
        }
        int i1 = RandomUtils.nextInt(4);
        if (2 == i1) {
            try {
                setBalanceRedis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String trades = getDepth();
        JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");

            JSONObject result = tradesObj.getJSONObject("data").getJSONObject("depth");

            List<List<String>> buyPrices = (List<List<String>>) result.get("b");

            List<List<String>> sellPrices = (List<List<String>>) result.get("a");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));

            if (sellPri.compareTo(buyPri) == 0) {
                //平台撮合功能失败
                setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                return;
            }



        /**
         * range              同步深度数量
         * relishMax          同步深度交易数量最大值值
         * relishMin          同步深度交易数量最小值
         * relishMark         对标交易对
         * relishPoint        同步交易数量倍数
         */


            try {

                Integer range = Integer.valueOf(exchange.get("range"));
                String relishMin = exchange.get("relishMin");
                String relishMax = exchange.get("relishMax");
                String relishMark = exchange.get("relishMark");


                List<DeepVo> history = HotcoinUtils.getHistory(relishMark);

                Map<String, List<DeepVo>> okDepp = HotcoinUtils.getdeep(relishMark);

                BigDecimal okDeepSellPrice = okDepp.get("deepSellList").get(0).getPrice();

                //计算价格比例
                if (point == null) {
                    point = sellPri.divide(okDeepSellPrice, 12, BigDecimal.ROUND_HALF_UP);
                }
                logger.info("nivex-价格：" + sellPri + "--hot价格：" + okDeepSellPrice + "--比例：" + point);

                ArrayList<Order> list = new ArrayList<>();


                // 同步k线
                int y = 0;
                BigDecimal klinePrice = null;
                for (DeepVo deepVo : history) {
                    y++;
                    boolean b = RandomUtils.nextBoolean();
                    //同步交易
                    Order order = new Order();
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    order.setType(b ? 1 : 2);
                    BigDecimal multiply = deepVo.getPrice().multiply(point).setScale(Integer.parseInt(exchange.get("pricePrecision")), RoundingMode.HALF_UP);
                    order.setPrice(multiply);


                    order.setAmount(orderAmount);
                    logger.info("买--Kline对标-hot价格：" + deepVo.getPrice() + "---对标价格" + order.getPrice() + "平台数量：" + deepVo.getAmount() + "---实际数量：" + order.getAmount());
                    list.add(order);
                    Order order1 = new Order();
                    order1.setType(b ? 2 : 1);
                    order1.setPrice(order.getPrice());
                    order1.setAmount(order.getAmount());
                    logger.info("买--Kline对标-hot价格：" + deepVo.getPrice() + "---对标价格" + order1.getPrice() + "平台数量：" + deepVo.getAmount() + "---实际数量：" + order1.getAmount());
                    list.add(order1);
                    klinePrice = order1.getPrice();
                    break;
                }

                //同步深度
                for (int i = 0, j = 0; i < okDepp.get("deepBuyList").size() && j < range; i++) {
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    DeepVo deepBuy = okDepp.get("deepBuyList").get(i);
                    Order order = new Order();
                    order.setType(1);
                    order.setPrice(deepBuy.getPrice().multiply(point));
                    if (klinePrice != null && order.getPrice().compareTo(klinePrice) == 0) {
                        continue;
                    }
                    BigDecimal relishAmount = deepBuy.getAmount().multiply(new BigDecimal(exchange.get("relishAmountPoint")));
                    order.setAmount(relishAmount.compareTo(new BigDecimal(relishMax)) > 0 ? orderAmount : relishAmount);
                    logger.info("买--对标-hot价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice()  + "---数量：" + order.getAmount());
                    j++;
                    list.add(order);
                }

                for (int i = 0, j = 0; i < okDepp.get("deepSellList").size() && j < range; i++) {
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    DeepVo deepBuy = okDepp.get("deepSellList").get(i);
                    Order order = new Order();
                    order.setType(2);
                    order.setPrice(deepBuy.getPrice().multiply(point));
                    if (klinePrice != null && order.getPrice().compareTo(klinePrice) == 0) {
                        continue;
                    }
                    BigDecimal relishAmount = deepBuy.getAmount().multiply(new BigDecimal(exchange.get("relishAmountPoint")));
                    order.setAmount(relishAmount.compareTo(new BigDecimal(relishMin)) > 0 ? orderAmount : relishAmount);
                    logger.info("卖--对标-hot价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice()  + "---数量：" + order.getAmount());
                    j++;
                    list.add(order);
                }
                Collections.shuffle(list);


                //开始挂单
                for (Order order2 : list) {
                    Thread.sleep(1000);
                    String resultJson = submitOrder(order2.getType(), order2.getPrice(), order2.getAmount());
                    JSONObject jsonObject1 = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject1 != null && "200".equals(jsonObject1.getString("status"))) {
                        String orderId = jsonObject1.getJSONObject("data").getString("order_id");
                        nowOrderList.add(orderId);
                    }
                }
            } catch (Exception e) {
                logger.info("失败---" + e.getMessage());
            }
            //撤单 上一波的
            for (String orderId : lastOrderList) {
                try {
                    Thread.sleep(1000);
                    String s = cancelTrade(orderId);
                    logger.info("撤单---" + orderId+"--"+s);
                } catch (Exception e) {
                    logger.info("撤单失败" + orderId + "---" + e.getMessage());
                }
            }
            lastOrderList= JSONArray.parseArray(JSON.toJSONString(nowOrderList), String.class);
            nowOrderList.clear();
            double time = Double.parseDouble(exchange.get("time"));
            try {
                Thread.sleep(getRandom(time, null).intValue() * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }



    }

    @Data
    class Order {
        private Integer type;
        private BigDecimal price;
        private BigDecimal amount;
    }

    public static Double getRandom(double num, Integer precision) {
        precision = 8;
        double randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);

        while (randomNum >= num) {
            randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);
        }
        return randomNum;
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


