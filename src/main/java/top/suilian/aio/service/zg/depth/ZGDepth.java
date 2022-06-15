package top.suilian.aio.service.zg.depth;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.zg.ZGParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZGDepth extends ZGParentService {
    public ZGDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_ZG_DEPTH, id);
    }

    boolean start = true;
    public int depthCancelOrderNum = 0;
    public String buyOrder1 = "";
    public String buyOrder2 = "";
    public String buyOrder3 = "";
    public String sellOrder1 = "";
    public String sellOrder2 = "";
    public String sellOrder3 = "";


    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");

            setPrecision();

            logger.info("设置机器人交易规则结束");
            start = false;
        }

        if (Integer.parseInt(exchange.get("depthCancelNum")) >= depthCancelOrderNum) {
            //获取深度
            String trades = httpUtil.get(baseUrl + "/depth?symbol=" + exchange.get("market") + "&size=10");
            JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");

            if (!trades.isEmpty() && tradesObj != null) {

                List<List<String>> buyPrices = (List<List<String>>) tradesObj.get("bids");

                List<List<String>> sellPrices = (List<List<String>>) tradesObj.get("asks");

                BigDecimal buyPri = new BigDecimal(buyPrices.get(0).get(0));
                BigDecimal buyPri1 = new BigDecimal(buyPrices.get(5).get(0));
                BigDecimal sellPri = new BigDecimal(sellPrices.get(0).get(0));
                BigDecimal sellPri1 = new BigDecimal(sellPrices.get(5).get(0));
                if (sellPri.compareTo(buyPri) == 0) {
                    //平台撮合功能失败
                    setTradeLog(id, "交易平台无法撮合", 0, "FF111A");
                    return;
                }
                BigDecimal buy1 = getRandomRedPacketBetweenMinAndMax(buyPri1, buyPri);
                BigDecimal sell1 = getRandomRedPacketBetweenMinAndMax(sellPri, sellPri1);

                BigDecimal buy2 = getRandomRedPacketBetweenMinAndMax(buyPri1, buyPri);
                BigDecimal sell2 = getRandomRedPacketBetweenMinAndMax(sellPri, sellPri1);

                BigDecimal buy3 = getRandomRedPacketBetweenMinAndMax(buyPri1, buyPri);
                BigDecimal sell3 = getRandomRedPacketBetweenMinAndMax(sellPri, sellPri1);

                try {
                    String resultJson1 = submitOrder(2, buy1, getOrderAmount());
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");
                    if (jsonObject1 != null && jsonObject1.getInt("code") == 0) {
                        buyOrder1 = jsonObject1.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度挂买单1[" + buy1 + "]=> "+buyOrder1 , 0);
                    Thread.sleep(200);
                    /**-------------------------------------------------------------------------*/
                    String resultJson2 = submitOrder(1, sell1, getOrderAmount());
                    JSONObject jsonObject2 = judgeRes(resultJson2, "code", "submitTrade");
                    if (jsonObject2 != null && jsonObject2.getInt("code") == 0) {
                        sellOrder1 = jsonObject2.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度挂卖单1[" + sell1 + "]=> "+sellOrder1 , 0);
                    Thread.sleep(300);
                    /**-------------------------------------------------------------------------*/
                    String resultJson3 = submitOrder(2, buy2, getOrderAmount());
                    JSONObject jsonObject3 = judgeRes(resultJson3, "code", "submitTrade");
                    if (jsonObject3 != null && jsonObject3.getInt("code") == 0) {
                        buyOrder2 = jsonObject3.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度挂买单2[" + buy2 + "]=> "+buyOrder2 , 0);
                    Thread.sleep(200);
                    /**-------------------------------------------------------------------------*/
                    String resultJson4 = submitOrder(1, sell2, getOrderAmount());
                    JSONObject jsonObject4 = judgeRes(resultJson4, "code", "submitTrade");
                    if (jsonObject4 != null && jsonObject4.getInt("code") == 0) {
                        sellOrder2 = jsonObject4.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度卖单2[" + sell2 + "]=> "+buyOrder2 , 0);
                    /**-------------------------------------------------------------------------*/
                    String resultJson5 = submitOrder(2, buy3, getOrderAmount());
                    JSONObject jsonObject5 = judgeRes(resultJson5, "code", "submitTrade");
                    if (jsonObject5 != null && jsonObject5.getInt("code") == 0) {
                        buyOrder3 = jsonObject5.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度挂买单2[" + buy3 + "]=> "+buyOrder2 , 0);
                    Thread.sleep(200);
                    /**-------------------------------------------------------------------------*/
                    String resultJson6 = submitOrder(1, sell3, getOrderAmount());
                    JSONObject jsonObject6 = judgeRes(resultJson6, "code", "submitTrade");
                    if (jsonObject6 != null && jsonObject6.getInt("code") == 0) {
                        sellOrder3 = jsonObject6.getJSONObject("result").getString("id");
                    }
                    setTradeLog(id, "深度挂卖单3[" + sell3 + "]=> "+sellOrder3 , 0);
                    Thread.sleep(200);
                    cancelTrade(buyOrder1);
                    setTradeLog(id, "深度撤买单1", 0);
                    Thread.sleep(500);
                    cancelTrade(buyOrder2);
                    setTradeLog(id, "深度撤买单2", 0);
                    cancelTrade(buyOrder3);
                    setTradeLog(id, "深度撤买单3", 0);
                    Thread.sleep(200);
                    cancelTrade(sellOrder1);
                    setTradeLog(id, "深度撤卖单1", 0);
                    Thread.sleep(300);
                    cancelTrade(sellOrder2);
                    setTradeLog(id, "深度撤卖单2", 0);
                    cancelTrade(sellOrder3);
                    setTradeLog(id, "深度撤卖单3", 0);
                } catch (Exception e) {

                    try {
                        if (StringUtils.isNotEmpty(buyOrder1)) {
                            cancelTrade(buyOrder1);
                        }
                        if (StringUtils.isNotEmpty(buyOrder2)) {
                            cancelTrade(buyOrder2);
                        }
                        if (StringUtils.isNotEmpty(buyOrder3)) {
                            cancelTrade(buyOrder3);
                        }

                        if (StringUtils.isNotEmpty(sellOrder1)) {
                            cancelTrade(sellOrder1);
                        }
                        if (StringUtils.isNotEmpty(sellOrder2)) {
                            cancelTrade(sellOrder2);
                        }
                        if (StringUtils.isNotEmpty(sellOrder3)) {
                            cancelTrade(sellOrder3);
                        }

                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();

                    }
                    e.printStackTrace();
                }

            }

            try {
                setBalanceRedis();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");

        }


    }

    public BigDecimal getOrderAmount() {

        Integer newScale = Integer.parseInt(precision.get("amountPrecision").toString());
        long minAmount = new BigDecimal(exchange.get("depthOrderLowerLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        long maxAmount = new BigDecimal(exchange.get("depthOrderTopLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        BigDecimal bigDecimal = getRandomRedPacketBetweenMinAndMax(new BigDecimal(exchange.get("depthOrderLowerLimit")), new BigDecimal(exchange.get("depthOrderTopLimit")));
        return bigDecimal;
    }

    public static BigDecimal getRandomRedPacketBetweenMinAndMax(BigDecimal min, BigDecimal max) {
        float minF = min.floatValue();
        float maxF = max.floatValue();
        //生成随机数
        BigDecimal db = new BigDecimal(Math.random() * (maxF - minF) + minF);
        //返回保留6位小数的随机数。不进行四舍五入
        return db.setScale(12, RoundingMode.DOWN);
    }

}
