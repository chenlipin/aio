package top.suilian.aio.service.bian.newKline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.bian.BianParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;


public class NewBianKline extends BianParentService {
    public NewBianKline(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_BIAN_KLINE, id);
    }

    private boolean start = true;


    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");
            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");
            start = false;
        }

        setBalanceRedis();
        String min = exchange.get("numMinThreshold");
        String max =exchange.get("numThreshold");

        int buysDeepNum = Integer.parseInt(exchange.get("bidsDeepNum"));
        int sellDeepNum = Integer.parseInt(exchange.get("asksDeepNum"));


        int pricePrecision = Integer.parseInt(exchange.get("pricePrecision"));
        int amountPrecision = Integer.parseInt(exchange.get("amountPrecision"));

        BigDecimal buyPriceMin = new BigDecimal(exchange.get("buyPriceMin"));
        BigDecimal buyPriceMax = new BigDecimal(exchange.get("buyPriceMax"));

        BigDecimal sellPriceMin = new BigDecimal(exchange.get("sellPriceMin"));
        BigDecimal sellPriceMax = new BigDecimal(exchange.get("sellPriceMax"));

        //获取行情
        String trade = httpUtil.get(baseUrl + "/api/v3/depth?symbol="+ exchange.get("market")+"&limit="+5000 );
        if (StringUtils.isEmpty(trade)){
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(trade);
        //买
        JSONArray buy = jsonObject.getJSONArray("bids");
        //卖
        JSONArray asks = jsonObject.getJSONArray("asks");

        String buyPrice = buy.getJSONArray(0).getString(0);
        String sellPrice = asks.getJSONArray(0).getString(0);
        setTradeLog(id, "买一[" + buyPrice + "],卖一[" + sellPrice + "],买盘单数:" + buy.size() + ";卖盘单数："+asks.size(), 0, "a61b12");
        if (buy.size()<buysDeepNum){
            int buyOrderTotal = buysDeepNum - buy.size();
            setWarmLog(id, 2, "买盘当前深度单数为:"+  buy.size() +"低于警戒值:"+buysDeepNum+";开始补单", "");
            for (int i = 0; i < buyOrderTotal; i++) {
                BigDecimal orderAmount = getOrderAmount(min, max, 10D);
                BigDecimal price = getRandomRedPacketBetweenMinAndMax(buyPriceMin, buyPriceMax,pricePrecision);
                if (price.compareTo(new BigDecimal(sellPrice))>=0){
                    setTradeLog(id, "卖一[" + sellPrice + "],高于当前补买单价格:" + price + ";补单失败”", 0, "a61b12");
                    continue;
                }
                setTradeLog(id, "补买盘深单价格[" + price + "],数量[" + orderAmount +"]" ,0, "a61b12");
//                String submitTrade = submitTrade(1, price, orderAmount);
                String submitTrade = "X";
                if(StringUtils.isNotEmpty(submitTrade)){
                    JSONObject jsonObject1 = JSONObject.parseObject(submitTrade);
                    if (StringUtils.isNotEmpty(jsonObject1.getString("clientOrderId"))) {
                        setTradeLog(id, "补买盘深单价格[" + price + "],数量[" + orderAmount + "],成功：单号：" +jsonObject1.getString("clientOrderId"), 0, "a61b12");
                    }
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (asks.size()<sellDeepNum){
            int sellOrderTotal = sellDeepNum - asks.size();
            setWarmLog(id, 2, "，卖盘当前深度单数为:"+  buy.size() +"低于警戒值:"+buysDeepNum+";开始补单", "");
            for (int i = 0; i < sellOrderTotal; i++) {
                BigDecimal orderAmount = getOrderAmount(min, max, 10D);
                BigDecimal price = getRandomRedPacketBetweenMinAndMax(sellPriceMin, sellPriceMax,pricePrecision);
                if (price.compareTo(new BigDecimal(buyPrice))<=0){
                    setTradeLog(id, "买一[" + buyPrice + "],高于当前补买单价格:" + price + ";补单失败”", 0, "ff6c37");
                    continue;
                }
                setTradeLog(id, "补卖盘深单价格[" + price + "],数量[" + orderAmount +"]" ,0, "a61b12");
//                String submitTrade = submitTrade(2, price, orderAmount);
                String submitTrade = "X";
                if(StringUtils.isNotEmpty(submitTrade)){
                    JSONObject jsonObject1 = JSONObject.parseObject(submitTrade);
                    if (StringUtils.isNotEmpty(jsonObject1.getString("clientOrderId"))) {
                        setTradeLog(id, "补卖盘深单价格[" + price + "],数量[" + orderAmount + "],成功：单号：" +jsonObject1.getString("clientOrderId"), 0, "a61b12");
                    }
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sleep(30000, Integer.parseInt("1"));
    }

    public static BigDecimal getRandomRedPacketBetweenMinAndMax(BigDecimal min, BigDecimal max,int scale){
        float minF = min.floatValue();
        float maxF = max.floatValue();
        //生成随机数
        BigDecimal db = new BigDecimal(Math.random() * (maxF - minF) + minF);
        //返回保留两位小数的随机数。不进行四舍五入
        return db.setScale(12, RoundingMode.HALF_UP);
    }


    BigDecimal getOrderAmount(String min, String max, double pression) {
        double mind = Double.parseDouble(min);
        double maxd = Double.parseDouble(max);
        long maxQty = (long) (maxd * Math.pow(10, pression));
        long minty = (long) (mind * Math.pow(10, pression));
        long randNumber = minty + (((long) (new Random().nextDouble() * (maxQty - minty))));
        return new BigDecimal(String.valueOf(randNumber / Math.pow(10, pression)));
    }

    public static void main(String[] args) {
        Double ratio = 10 * (1 / (1 + Double.valueOf(0.5)));
        BigDecimal bigDecimal = new BigDecimal(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
        System.out.println(bigDecimal);
    }
}
