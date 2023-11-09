package top.suilian.aio.service.mxc.depthRefer;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Refer;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.GateUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.mxc.MxcParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class MxcRefer extends MxcParentService {

    public MxcRefer(
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
        super.logger = getLogger("mxc/refer", id);
    }

    private boolean start = true;
    public List<String> buyPrices = new ArrayList<>();       //买单存放集合
    public List<String> sellPrices = new ArrayList<>();     //卖单存放集合
    public String buyOrder;
    public String sellOrder;
    public HashMap<String, Map<String, String>> depth = new HashMap<>();
    Refer refer = new Refer();
    Map<String, String> bids = new LinkedHashMap<>();
    Map<String, String> asks = new LinkedHashMap<>();
    List<String> buyList=new ArrayList<>();
    List<String> sellList=new ArrayList<>();

    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("对标设置机器人参数开始");
            setParam();
            logger.info("对标设置机器人参数结束");
            logger.info("对标设置机器人交易规则开始");
            setPrecision();
            logger.info("对标设置机器人交易规则结束");
            start = false;
        }
//        String trades = getDepth();
//        if (StringUtils.isEmpty(trades)){
//            setTradeLog(id, "获取GATE深度失败", 0, "FF111A");
//        }
//        JSONObject deepMx = JSONObject.parseObject(trades);
//        if (200!=deepMx.getInteger("code")){
//            setTradeLog(id, "获取GATE深度失败"+(deepMx.getString("msg")==null?"":deepMx.getString("msg")), 0, "FF111A");
//        }
//        JSONObject data1 = deepMx.getJSONObject("data");
//        JSONArray mxAsks = data1.getJSONArray("asks");
//        JSONArray mxBids = data1.getJSONArray("bids");
//        Collection<BigDecimal> buyListMx = mxAsks.stream().map(e -> JSONObject.parseObject(JSONObject.toJSONString(e))).map(e -> e.getBigDecimal("price")).collect(Collectors.toCollection());

        Integer deepNum = Integer.parseInt(exchange.get("deepNum"));
        refer = GateUtils.getTrade(exchange.get("referMarket"));
        depth = GateUtils.getDepth(exchange.get("referMarket"),deepNum);



        BigDecimal maxAmount = new BigDecimal(exchange.get("maxAmount"));
        BigDecimal amountPoint = new BigDecimal(exchange.get("amountPoint"));

        Map<String, String> bids = depth.get("bids");
        Map<String, String> asks = depth.get("asks");
        if (bids != null && asks != null && refer.getPrice() != null) {
            List<String> lhbuyPrices = new ArrayList<>();       //买单存放集合
            List<String> lhsellPrices = new ArrayList<>();     //卖单存放集合
            //先布深度单
            int buyi = 0;
            int selli = 0;
            for (String key : bids.keySet()) {
                buyi++;
                if (buyi > deepNum) {
                    break;
                }
                if (buyList .contains(key)) {
                    break;
                }
                BigDecimal price = new BigDecimal(key);

                BigDecimal amount = new BigDecimal(bids.get(key)).multiply(amountPoint);
                if (price.multiply(amount).compareTo(new BigDecimal("5"))<0){
                    Random r = new Random();
                    double v = r.nextDouble() * 5;
                    BigDecimal add = new BigDecimal("5").add(new BigDecimal(v));
                    amount= add.divide(price,8, RoundingMode.HALF_UP);
                }
                if (maxAmount.compareTo(amount) <= 0) {
                    continue;
                }
                String order = submitOrder(1, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                if (200!=jsonObject.getInteger("code")){
                    String tradeId = jsonObject.getString("data");
                    logger.info("深度对标挂买单 单号：" + tradeId);
                    lhbuyPrices.add(tradeId);
                    sleep(800, Integer.parseInt("1"));
                }
            }
            for (String key : asks.keySet()) {
                selli++;
                if (selli > deepNum) {
                    break;
                }
                if (sellList .contains(key)) {
                    break;
                }
                BigDecimal price = new BigDecimal(key);
                BigDecimal amount = new BigDecimal(asks.get(key)).multiply(amountPoint);
                if (price.multiply(amount).compareTo(new BigDecimal("5"))<0){
                    Random r = new Random();
                    double v = r.nextDouble() * 5;
                    BigDecimal add = new BigDecimal("5").add(new BigDecimal(v));
                    amount= add.divide(price,8, RoundingMode.HALF_UP);
                }
                if (maxAmount.compareTo(amount) <= 0) {
                    continue;
                }
                String order = submitOrder(2, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                if (200!=jsonObject.getInteger("code")){
                    String tradeId = jsonObject.getString("data");
                    logger.info("深度对标挂卖单 单号：" + tradeId);
                    lhsellPrices.add(tradeId);
                    sleep(800, Integer.parseInt("1"));
                }
            }
            //撤单上一轮的深度单子
            for (String buyPrice : buyPrices) {
                try {
                    String s = cancelTradeStr(buyPrice);
                    setTradeLog(id, "深度撤买单 单号：" + buyPrice + "撤单结果：" + (s.equals("true") ? "成功" : "失败")+buyPrice, 0, "67c23a");
                    sleep(800, Integer.parseInt("1"));
                }catch (Exception e){
                    lhbuyPrices.add(buyPrice);
                    setTradeLog(id, "深度撤买单失败---》"+buyPrice, 0, "FF111A");
                }

            }
            for (String sellPrices : sellPrices) {
                try {
                String s = cancelTradeStr(sellPrices);
                setTradeLog(id, "深度撤卖单 单号：" + sellPrices + "撤单结果：" + (s.equals("true") ? "成功" : "失败"), 0, "67c23a");
                sleep(800, Integer.parseInt("1"));
                }catch (Exception e){
                    lhsellPrices.add(sellPrices);
                    setTradeLog(id, "深度撤卖单失败---》"+sellPrices, 0, "FF111A");
                }
            }
            buyPrices = lhbuyPrices;
            sellPrices = lhsellPrices;
            //撤k线单子
            if (StringUtils.isNotEmpty(buyOrder)) {
                String s = cancelTradeStr(buyOrder);
                setTradeLog(id, "对标撤k线单1 单号：" + buyOrder + "撤单结果：" + (s.equals("true") ? "成功" : "失败"), 0, "67c23a");
            }
            if (StringUtils.isNotEmpty(sellOrder)) {
                String s1 = cancelTradeStr(sellOrder);
                setTradeLog(id, "对标撤k线单2 单号：" + sellOrder + "撤单结果：" + (s1.equals("true") ? "成功" : "失败"), 0, "67c23a");
            }
            //挂k线单子
            BigDecimal priceKline = new BigDecimal(refer.getPrice());
            BigDecimal amountKline = new BigDecimal(refer.getAmount()).multiply(amountPoint);
            if (maxAmount.compareTo(amountKline) > 0) {
                String order1 = submitOrder("buy".equals(refer.getIsSell()) ? 2 : 1, priceKline, amountKline);
                JSONObject jsonObject = JSONObject.parseObject(order1);
                if (200!=jsonObject.getInteger("code")){
                    buyOrder = jsonObject.getString("order_id");
                }
                String order2 = submitOrder("buy".equals(refer.getIsSell()) ? 1 : 2, priceKline, amountKline);
                JSONObject jsonObject2 = JSONObject.parseObject(order2);
                if (200!=jsonObject2.getInteger("code")){
                    sellOrder = jsonObject2.getString("order_id");
                }
            }
            buyList.clear();
            sellList.clear();
        } else {
            setTradeLog(id, "未切换交易对，请确认该对标平台存在该交易对", 0, "000000");
        }
        sleep(20000, Integer.parseInt("1"));
        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }



}
