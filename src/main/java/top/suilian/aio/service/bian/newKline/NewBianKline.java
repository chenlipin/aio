package top.suilian.aio.service.bian.newKline;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.bian.BianParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


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


        int pricePrecision = Integer.parseInt(exchange.get("pricePrecision"));
        int amountPrecision = Integer.parseInt(exchange.get("amountPrecision"));

        BigDecimal buyPriceMin = new BigDecimal(exchange.get("buyPriceMin"));
        BigDecimal buyPriceMax = new BigDecimal(exchange.get("buyPriceMax"));
        Integer buyOrderTotal = Integer.parseInt(exchange.get("buyOrderTotal"));

        BigDecimal sellPriceMin = new BigDecimal(exchange.get("sellPriceMin"));
        BigDecimal sellPriceMax = new BigDecimal(exchange.get("sellPriceMax"));
        Integer sellOrderTotal = Integer.parseInt(exchange.get("sellOrderTotal"));



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
        boolean b = RandomUtils.nextBoolean();
        if (b){
            setTradeLog(id, "买一[" + buyPrice + "],卖一[" + sellPrice + "],买盘单数:" + buy.size() + ";卖盘单数："+asks.size(), 0, "a61b12");
        }
        ArrayList<BigDecimal> allBuyOrderPrice = new ArrayList<>();
        ArrayList<BigDecimal> allsellOrderPrice = new ArrayList<>();
        net.sf.json.JSONArray allOpenOrder = openOrders();
        for (int i = 0; i < allOpenOrder.size(); i++) {
            net.sf.json.JSONObject object = allOpenOrder.getJSONObject(i);
            if(object.getString("side").equals("BUY")){
                allBuyOrderPrice.add(new BigDecimal(object.getString("price")));
            }else {
                allsellOrderPrice.add(new BigDecimal(object.getString("price")));
            }
        }

        //买单集合
        logger.info("自有买单集合:"+ JSON.toJSONString(allBuyOrderPrice));
        //卖单-集合
        logger.info("自有卖单集合:"+ JSON.toJSONString(allsellOrderPrice));
        List<BigDecimal> collectSell = allBuyOrderPrice.stream().filter(e -> {
             return e.compareTo(sellPriceMin) >= 0 && e.compareTo(sellPriceMax) <= 0;
        }).collect(Collectors.toList());

        List<BigDecimal> collectBuy = allBuyOrderPrice.stream().filter(e -> {
            return e.compareTo(buyPriceMin) >= 0 && e.compareTo(buyPriceMax) <= 0;
        }).collect(Collectors.toList());
        logger.info("符合条件买单集合:"+ JSON.toJSONString(collectBuy) );
        logger.info("符合条件卖单集合:"+ JSON.toJSONString(collectSell));
        if (collectBuy.size() <= buyOrderTotal ) {
            setTradeLog(id, "开始补单:买盘区间[" + buyPriceMin + "~~" + buyPriceMin + "],区间期望数量:" + buyOrderTotal + ";实际单数："+collectBuy.size(), 0, "a61b12");
            int needTrade = buyOrderTotal - collectBuy.size();
            for (int i = 1; i <= needTrade; i++) {
                Double amount = RandomUtilsme.getRandomAmount(Double.parseDouble(min) ,Double.parseDouble(max));
                Double price = RandomUtilsme.getRandomAmount(Double.parseDouble(buyPriceMin.toString()) ,Double.parseDouble(buyPriceMin.toString()));
                logger.info("补卖单:"+ JSON.toJSONString(price+"==="+amount) );
            }
        }

        if (collectSell.size() <= buyOrderTotal ) {
            setTradeLog(id, "开始补单:卖盘区间[" + sellPriceMin + "~~" + sellPriceMax + "],区间期望数量:" + sellOrderTotal + ";实际单数："+collectSell.size(), 0, "a61b12");
            int needTrade = buyOrderTotal - collectBuy.size();
            for (int i = 1; i <= needTrade; i++) {
                Double amount = RandomUtilsme.getRandomAmount(Double.parseDouble(min) ,Double.parseDouble(max));
                Double price = RandomUtilsme.getRandomAmount(Double.parseDouble(sellPriceMin.toString()) ,Double.parseDouble(sellPriceMin.toString()));
                logger.info("补卖单:"+ JSON.toJSONString(price+"==="+amount) );
            }
        }

        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sleep(15000, Integer.parseInt("1"));
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
