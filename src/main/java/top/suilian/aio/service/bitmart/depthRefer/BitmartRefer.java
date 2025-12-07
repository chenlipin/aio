package top.suilian.aio.service.bitmart.depthRefer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Refer;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.HotcoinUtils;
import top.suilian.aio.refer.HuoBiUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.bitmart.BitMartParentService;
import top.suilian.aio.service.zg.ZGParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class BitmartRefer extends BitMartParentService {

    public BitmartRefer(
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
        super.logger = getLogger("bitmart/refer", id);
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
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");

            //获取深度 判断平台撮合是否成功
            String trades = httpUtil.get(baseUrl + "/spot/v1/symbols/book?size=20&symbol=" + exchange.get("market"));

            //获取深度 判断平台撮合是否成功
            com.alibaba.fastjson.JSONObject tradesObj = JSON.parseObject(trades);

            if (tradesObj != null && 1000 == tradesObj.getInteger("code")) {

                com.alibaba.fastjson.JSONObject data = tradesObj.getJSONObject("data");
                JSONArray buys1 = data.getJSONArray("buys");
                for (int i = 0; i < buys1.size(); i++) {
                    JSONObject jsonObject = buys1.getJSONObject(i);
                    buyList.add(jsonObject.getString("price"));
                }
                JSONArray sells = data.getJSONArray("sells");
                for (int i = 0; i < sells.size(); i++) {
                    JSONObject jsonObject = sells.getJSONObject(i);
                    sellList.add(jsonObject.getString("price"));
                }
                String buys = com.alibaba.fastjson.JSONObject.toJSONString(buys1.get(0));
                String sell = com.alibaba.fastjson.JSONObject.toJSONString(sells.get(0));

                BigDecimal buyPri = new BigDecimal(net.sf.json.JSONObject.fromObject(buys).getString("price"));
                BigDecimal sellPri = new BigDecimal(net.sf.json.JSONObject.fromObject(sell).getString("price"));

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }
            start = false;
        }
        refer = HotcoinUtils.getTrade(exchange.get("referMarket"));
//        logger.info(JSONObject.toJSONString(refer));
//        depth = HotcoinUtils.getDepth(exchange.get("referMarket"));
        depth = null;
        logger.info(JSONObject.toJSONString(depth));
        Integer deepNum = Integer.parseInt(exchange.get("deepNum"));

        BigDecimal maxAmount = new BigDecimal(exchange.get("maxAmount"));
        BigDecimal amountPoint = new BigDecimal(exchange.get("amountPoint"));

        Map<String, String> bids = depth.get("bids");
        Map<String, String> asks = depth.get("asks");
        if (bids != null && asks != null && refer.getPrice() != null) {
            List<String> lhbuyPrices = new ArrayList<>();       //买单存放集合
            List<String> lhsellPrices = new ArrayList<>();     //卖单存放集合
            Set<Map.Entry<String, String>> bidEntries = bids.entrySet();
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
                if (maxAmount.compareTo(amount) <= 0) {
                    continue;
                }
                String order = submitOrder(1, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                if (jsonObject.getInteger("code") == 1000){
                    JSONObject data = jsonObject.getJSONObject("data");
                    String tradeId = data.getString("order_id");
                    logger.info("深度挂买单 单号：" + tradeId);
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
                if (maxAmount.compareTo(amount) <= 0) {
                    continue;
                }
                String order = submitOrder(2, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                if (jsonObject.getInteger("code") == 1000){
                    JSONObject data = jsonObject.getJSONObject("data");
                    String tradeId = data.getString("order_id");
                    logger.info("深度挂卖单 单号：" + tradeId);
                    lhsellPrices.add(tradeId);
                    sleep(800, Integer.parseInt("1"));
                }
            }
            //撤单上一轮的深度单子
            for (String buyPrice : buyPrices) {
                String s = cancelTradeStr(buyPrice);
                logger.info("深度撤买单 单号：" + buyPrice + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
                sleep(800, Integer.parseInt("1"));
            }
            for (String sellPrices : sellPrices) {
                String s = cancelTradeStr(sellPrices);
                logger.info("深度撤卖单 单号：" + sellPrices + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
                sleep(800, Integer.parseInt("1"));
            }
            buyPrices = lhbuyPrices;
            sellPrices = lhsellPrices;
            //撤k线单子
            if (StringUtils.isNotEmpty(buyOrder)) {
                String s = cancelTradeStr(buyOrder);
                logger.info("撤k线单1 单号：" + buyOrder + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
            }
            if (StringUtils.isNotEmpty(sellOrder)) {
                String s1 = cancelTradeStr(sellOrder);
                logger.info("撤k线单2 单号：" + sellOrder + "撤单结果：" + (s1.equals("true") ? "成功" : "失败"));
            }
            //挂k线单子
            BigDecimal priceKline = new BigDecimal(refer.getPrice());
            BigDecimal amountKline = new BigDecimal(refer.getAmount()).multiply(amountPoint);
            if (maxAmount.compareTo(amountKline) > 0) {
                String order1 = submitOrder("buy".equals(refer.getIsSell()) ? 2 : 1, priceKline, amountKline);
                JSONObject jsonObject = JSONObject.parseObject(order1);
                if (jsonObject.getInteger("code") == 1000){
                    JSONObject data = jsonObject.getJSONObject("data");
                    buyOrder = data.getString("order_id");
                }
                String order2 = submitOrder("buy".equals(refer.getIsSell()) ? 1 : 2, priceKline, amountKline);
                JSONObject jsonObject2 = JSONObject.parseObject(order2);
                if (jsonObject2.getInteger("code") == 1000){
                    JSONObject data = jsonObject2.getJSONObject("data");
                    sellOrder = data.getString("order_id");
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
        clearLog();


    }

    /**
     * 获得数量
     *
     * @param pression
     * @return
     */
    BigDecimal getOrderAmount(double pression) {
        double mind = Double.parseDouble(exchange.get("numMinThreshold"));
        double maxd = Double.parseDouble(exchange.get("numThreshold"));
        long maxQty = (long) (maxd * Math.pow(10, pression));
        long minty = (long) (mind * Math.pow(10, pression));
        long randNumber = minty + (((long) (new Random().nextDouble() * (maxQty - minty))));
        return new BigDecimal(String.valueOf(randNumber / Math.pow(10, pression)));
    }


}
