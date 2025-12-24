package top.suilian.aio.service.hotcoin.refToOk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.BianUtils;
import top.suilian.aio.refer.DeepVo;
import top.suilian.aio.refer.WeexUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.weex.refToOk.WeexRep2Ok;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class HotcoinRep2Ok extends HotCoinParentService {
    public HotcoinRep2Ok(
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
        super.logger = getLogger("hotcoin/replenish", id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;
    List<String> nowOrderList = new ArrayList<>();
    List<String> lastOrderList = new ArrayList<>();
    BigDecimal point=null;

    boolean up=true;
    int time=0;

    /**
     * range              同步深度数量
     * relishMax          同步深度交易数量最大值值
     * relishMin          同步深度交易数量最小值
     * relishMark        补单每次补单单数
     * relishPoint       同步交易数量倍数
     */

    public void init()  {
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
        int i1 = RandomUtils.nextInt(5);
        if(2==i1){
            try {
                setBalanceRedis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }




        /**
         * range              同步深度数量
         * relishMax          同步深度交易数量最大值值
         * relishMin          同步深度交易数量最小值
         * relishMark         对标交易对
         * relishPoint        同步交易数量倍数
         */


        //获取深度 判断平台撮合是否成功
        String trades = httpUtil.get("https://api.hotcoinfin.com/v1/depth?step=20&symbol=" +exchange.get("market").toLowerCase());
        JSONObject tradesObj = JSONObject.fromObject(trades);
        if (tradesObj != null && tradesObj.getString("code").equals("200") ) {
            try {


                JSONObject tick = tradesObj.getJSONObject("data").getJSONObject("depth");

                List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));
                logger.info("买--：" + buyPri + "---卖" + sellPri);

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }

                Integer range = Integer.valueOf(exchange.get("range"));
                String relishMin = exchange.get("relishMin");
                String relishMax = exchange.get("relishMax");
                String relishMark = exchange.get("relishMark");

                String deepMin = exchange.get("deepMin");
                String deepMax = exchange.get("deepMax");


                List<DeepVo> history = WeexUtils.getHistory(relishMark);
                Map<String, List<DeepVo>> okDepp = WeexUtils.getdeep(relishMark);
                BigDecimal okDeepSellPrice = okDepp.get("deepSellList").get(0).getPrice();

                //计算价格比例
                if (point == null) {
                    point = new BigDecimal("1");
                }
                logger.info("weex-价格：" + sellPri + "--OK价格：" + okDeepSellPrice + "--比例：" + point);
                List<Order> list = new ArrayList<>();



                // 同步k线

                BigDecimal klinePrice=null;
                for (DeepVo deepVo : history) {
                    boolean b = RandomUtils.nextBoolean();
                    //同步交易
                    Order order = new Order();
                    order.setFirst(1);
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    order.setType(b?1:2);
                    BigDecimal multiply = deepVo.getPrice().multiply(point).setScale(Integer.parseInt(exchange.get("pricePrecision")), RoundingMode.HALF_UP);
                    order.setPrice(multiply);
                    order.setAmount(orderAmount);
                    logger.info("买--Kline对标-ok价格：" + deepVo.getPrice() + "---对标价格" + order.getPrice()  + "---数量：" + order.getAmount());
                    list.add(order);
                   Order order1 = new Order();
                    order1.setType(b?2:1);
                    order1.setPrice(order.getPrice());
                    order1.setFirst(1);
                    order1.setAmount(order.getAmount());
                    logger.info("买--Kline对标-ok价格：" + deepVo.getPrice() + "---对标价格" + order1.getPrice() + "---数量：" + order1.getAmount());
                    list.add(order1);
                    klinePrice=order1.getPrice();
                    break;
                }

                //同步深度
                for (int i = 0,j=0; i < okDepp.get("deepBuyList").size() && j < range; i++) {
                    BigDecimal orderAmount = getOrderAmount(deepMin, deepMax, 5);
                    DeepVo deepBuy = okDepp.get("deepBuyList").get(i);
                    Order order = new Order();
                    order.setType(1);
                    order.setPrice(deepBuy.getPrice().multiply(point).setScale(Integer.parseInt(exchange.get("pricePrecision")),RoundingMode.HALF_UP));
                    if (klinePrice!=null&&order.getPrice().compareTo(klinePrice)==0){
                        continue;
                    }

                    order.setAmount(orderAmount);
                    if (i==0){
                        order.setFirst(2);
                    }
                    logger.info("买--对标-ok价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice()  + "数量：" + order.getAmount());

                    j++;
                    list.add(order);
                }

                for (int i = 0,j=0; i < okDepp.get("deepSellList").size() &&  j< range; i++) {
                    BigDecimal orderAmount = getOrderAmount(deepMin, deepMax, 5);
                    DeepVo deepBuy = okDepp.get("deepSellList").get(i);
                    Order order = new Order();
                    order.setType(2);
                    order.setPrice(deepBuy.getPrice().multiply(point).setScale(Integer.parseInt(exchange.get("pricePrecision")),RoundingMode.HALF_UP));
                    if (klinePrice!=null&&order.getPrice().compareTo(klinePrice)==0){
                        continue;
                    }
                    order.setAmount(orderAmount);
                    if (i==0){
                        order.setFirst(2);

                    }
                    logger.info("卖--对标-ok价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice() );
                    j++;
                    list.add(order);
                }

                Collections.shuffle(list);
                list = list.stream()
                        .sorted(Comparator.comparing(Order::getFirst))
                        .collect(Collectors.toList());


                //开始挂单
                for (Order order2 : list) {
                    Thread.sleep(1000);
                    String resultJson =null;
                    if (!StringUtils.isEmpty(exchange.get("test"))){
                        resultJson="{\"code\":200,\"msg\":\"委托成功\",\"time\":1763558284055,\"data\":{\"ID\":2511024846264825}}";
                    }else {
                        resultJson = submitOrder(order2.getType(), order2.getPrice(), order2.getAmount());
                    }

                    JSONObject jsonObject1 = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject1 != null && "200".equals(jsonObject1.getString("code"))) {
                        String orderId = jsonObject1.getJSONObject("data").getString("ID");
                        nowOrderList.add(orderId);
                    }
                }
            }catch (Exception e){
                logger.info("失败---"+e.getMessage());
            }
            //撤单 上一波的
            for (String orderId : lastOrderList) {
                try {
                    String s = cancelTrade(orderId);
                    logger.info("撤单---"+orderId);
                }catch (Exception e){
                    logger.info("撤单失败"+orderId+"---"+e.getMessage());
                }
            }


            lastOrderList= JSONArray.parseArray(JSON.toJSONString(nowOrderList), String.class);

            nowOrderList.clear();
            double time = Double.valueOf(exchange.get("time"));
            try {
                Thread.sleep(getRandom(time,null).intValue()* 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }



    }

    @Data
    class Order {
        private Integer type;
        private BigDecimal price;
        private BigDecimal amount;
        private Integer first=8;
    }
    public static Double getRandom(double num, Integer precision) {
        precision=8;
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


