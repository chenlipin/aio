package top.suilian.aio.service.zg.depthReferToZg;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Refer;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.HuoBiUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.zg.ZGParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class ZgDepthToZg extends ZGParentService {

    public ZgDepthToZg(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_EG_REFER_DEPTH, id);
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
    BigDecimal buyPri = null;
    BigDecimal sellPri = null;
    BigDecimal buyPoint=null;
    BigDecimal sellPoint=null;

    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");

            //获取深度 判断平台撮合是否成功
            String trades = httpUtil.get(baseUrl + "/depth?symbol=" + exchange.get("market") + "&size=5");
            net.sf.json.JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

            if (!"".equals(trades) && !trades.isEmpty() && tradesObj != null) {

                List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

                buyPri = new BigDecimal(buyPrices.get(0).get(0));
                sellPri = new BigDecimal(sellPrices.get(0).get(0));

                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
            }
            Map<String, BigDecimal> deep1 = HuoBiUtils.getDepth1(exchange.get("referMarket"));
            buyPoint = buyPri.divide(deep1.get("bids"), Integer.parseInt(exchange.get("pricePrecision")), BigDecimal.ROUND_HALF_DOWN);
            sellPoint = sellPri.divide(deep1.get("asks"), Integer.parseInt(exchange.get("pricePrecision")), BigDecimal.ROUND_HALF_DOWN);
            logger.info("买的倍数：" + buyPoint + "卖的倍数：" + buyPoint);
            start = false;
        }
        refer = HuoBiUtils.getTrade(exchange.get("referMarket"));
        logger.info(JSONObject.toJSONString(refer));
        depth = HuoBiUtils.getDepth(exchange.get("referMarket"));
        logger.info(JSONObject.toJSONString(depth));
        Integer deepNum=Integer.parseInt(exchange.get("deepNum"));
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
                BigDecimal price = new BigDecimal(key);
                price = price.multiply(buyPoint);

                BigDecimal amount = new BigDecimal(bids.get(key)).multiply(amountPoint);
                String order = submitOrder(2, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                String orderIdTwo = jsonObject.getJSONObject("result").getString("id");
                logger.info("深度挂买单 单号：" +orderIdTwo );
                lhbuyPrices.add(orderIdTwo);
                sleep(500, Integer.parseInt("1"));
            }
            for (String key : asks.keySet()) {
                selli++;
                if (selli > deepNum) {
                    break;
                }
                BigDecimal price = new BigDecimal(key);
                price = price.multiply(sellPoint);
                BigDecimal amount =  new BigDecimal(bids.get(key)).multiply(amountPoint);
                String order = submitOrder(1, price, amount);
                JSONObject jsonObject = JSONObject.parseObject(order);
                String orderIdTwo = jsonObject.getJSONObject("result").getString("id");
                logger.info("深度挂卖单 单号：" +orderIdTwo );
                lhsellPrices.add(orderIdTwo);
                sleep(500, Integer.parseInt("1"));
            }
            //撤单上一轮的深度单子
            for (String buyPrice : buyPrices) {
                String s = cancelTradeStr(buyPrice);
                logger.info("深度撤买单 单号：" + buyPrice + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
                sleep(500, Integer.parseInt("1"));
            }
            for (String sellPrices : sellPrices) {
                String s = cancelTradeStr(sellPrices);
                logger.info("深度撤卖单 单号：" + sellPrices + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
                sleep(500, Integer.parseInt("1"));
            }
            buyPrices = lhbuyPrices;
            sellPrices = lhsellPrices;
            //撤k线单子
            if(StringUtils.isNotEmpty(buyOrder)) {
                String s = cancelTradeStr(buyOrder);
                logger.info("深度k线单1 单号：" + buyOrder + "撤单结果：" + (s.equals("true") ? "成功" : "失败"));
            }
            if(StringUtils.isNotEmpty(sellOrder)) {
                String s1 = cancelTradeStr(sellOrder);
                logger.info("深度k线单2 单号：" + sellOrder + "撤单结果：" + (s1.equals("true") ? "成功" : "失败"));
            }
            //挂k线单子
            BigDecimal priceKline = new BigDecimal(refer.getPrice());
            priceKline = priceKline.multiply(buyPoint);
            BigDecimal amountKline =  new BigDecimal(refer.getAmount()).multiply(amountPoint);
            String order1 = submitOrder(refer.getIsSell().equals("buy") ? 2 : 1, priceKline, amountKline);
            JSONObject jsonObject = JSONObject.parseObject(order1);
            buyOrder = jsonObject.getJSONObject("result").getString("id");

            String order2 = submitOrder(refer.getIsSell().equals("buy") ? 1 : 2, priceKline, amountKline);
            JSONObject jsonObject2 = JSONObject.parseObject(order2);
            sellOrder = jsonObject2.getJSONObject("result").getString("id");
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
