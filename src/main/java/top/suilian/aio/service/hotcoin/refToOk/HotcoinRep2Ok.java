package top.suilian.aio.service.hotcoin.refToOk;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.BianUtils;
import top.suilian.aio.refer.DeepVo;
import top.suilian.aio.service.*;
import top.suilian.aio.service.hotcoin.HotCoinParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }


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


        String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);
        /**
         * range              同步深度数量
         * relishMax          同步深度交易数量最大值值
         * relishMin          同步深度交易数量最小值
         * relishMark         对标交易对
         * relishPoint        同步交易数量倍数
         */

        //获取深度 判断平台撮合是否成功
        com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);
        if (tradesObj != null && tradesObj.getInteger("code") == 200) {
            try {


                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");

                com.alibaba.fastjson.JSONObject tick = data.getJSONObject("depth");
                List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");
                List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");
                BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));

                BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));


                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
                Integer range = Integer.valueOf(exchange.get("range"));
                String relishMin = exchange.get("relishMin");
                String relishMax = exchange.get("relishMax");
                String relishMark = exchange.get("relishMark");
                String market = httpUtil.get("https://api.hotcoinfin.com/v1/trade?count=1&symbol=" + exchange.get("market"));
                JSONObject jsonObject = JSONObject.fromObject(market).getJSONObject("data").getJSONArray("trades").getJSONObject(0);
                BigDecimal price = new BigDecimal(jsonObject.getString("price"));
                BigDecimal amount = new BigDecimal(jsonObject.getString("amount"));
                int type = jsonObject.getString("type").equals("买入") ? 1 : 2;

                List<DeepVo> history = BianUtils.getHistory(relishMark);
                DeepVo HisOrderOk = history.get(0);
                Map<String, List<DeepVo>> okDepp = BianUtils.getdeep(relishMark);
                BigDecimal priceOk = HisOrderOk.getPrice();
                //计算价格比例
                if (point == null) {
                    point = price.divide(priceOk, 6, BigDecimal.ROUND_HALF_UP);
                }
                logger.info("hotcoin-价格：" + price + "--OK价格：" + priceOk + "--比例：" + point);
                //同步深度
                ArrayList<Order> list = new ArrayList<>();
                for (int i = 0; i < okDepp.get("deepBuyList").size() && i <= range; i++) {
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    DeepVo deepBuy = okDepp.get("deepBuyList").get(i);
                    Order order = new Order();
                    order.setType(1);
                    order.setPrice(deepBuy.getPrice().multiply(point));
                    BigDecimal relishAmount = deepBuy.getAmount().multiply(new BigDecimal(exchange.get("relishAmountPoint")));
                    order.setAmount(relishAmount.compareTo(new BigDecimal(relishMax)) > 0 ? orderAmount : relishAmount);
                    logger.info("买--对标-ok价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice() + "平台数量：" + deepBuy.getAmount() + "---实际数量：" + order.getAmount());
                    list.add(order);
                }

                for (int i = 0; i < okDepp.get("deepSellList").size() && i <= range; i++) {
                    BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                    DeepVo deepBuy = okDepp.get("deepSellList").get(i);
                    Order order = new Order();
                    order.setType(2);
                    order.setPrice(deepBuy.getPrice().multiply(point));
                    BigDecimal relishAmount = deepBuy.getAmount().multiply(new BigDecimal(exchange.get("relishAmountPoint")));
                    order.setAmount(relishAmount.compareTo(new BigDecimal(relishMin)) > 0 ? orderAmount : relishAmount);
                    logger.info("卖--对标-ok价格：" + deepBuy.getPrice() + "---对标价格" + order.getPrice() + "平台数量：" + deepBuy.getAmount() + "---实际数量：" + order.getAmount());
                    list.add(order);
                }

                //同步交易
                Order order = new Order();
                BigDecimal orderAmount = getOrderAmount(relishMin, relishMax, 5);
                order.setType(HisOrderOk.getType());
                BigDecimal relishAmount = HisOrderOk.getAmount().multiply(new BigDecimal(exchange.get("relishAmountPoint")));
                order.setPrice(HisOrderOk.getPrice().multiply(point));
                order.setAmount(relishAmount.compareTo(new BigDecimal(relishMax)) > 0 ? orderAmount : relishAmount);
                logger.info("买--Kline对标-ok价格：" + HisOrderOk.getPrice() + "---对标价格" + order.getPrice() + "平台数量：" + HisOrderOk.getAmount() + "---实际数量：" + order.getAmount());
                list.add(order);
                Order order1 = new Order();
                order1.setType(HisOrderOk.getType() == 1 ? 2 : 1);
                order1.setPrice(HisOrderOk.getPrice().multiply(point));
                order1.setAmount(relishAmount.compareTo(new BigDecimal(relishMax)) > 0 ? orderAmount : relishAmount);
                logger.info("买--Kline对标-ok价格：" + HisOrderOk.getPrice() + "---对标价格" + order1.getPrice() + "平台数量：" + HisOrderOk.getAmount() + "---实际数量：" + order1.getAmount());
                list.add(order1);
                //开始挂单
                for (Order order2 : list) {
                    Thread.sleep(1000);
                    String resultJson = submitOrder(order2.getType(), order2.getPrice(), order2.getAmount());
                    JSONObject jsonObject1 = judgeRes(resultJson, "code", "submitTrade");
                    if (jsonObject1 != null && jsonObject1.getInt("code") == 200) {
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
            lastOrderList=nowOrderList;
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


