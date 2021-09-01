package top.suilian.aio.service.test.RandomDepth;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.test.TestParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestRandomDepth extends TestParentService {

    public TestRandomDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_TEST_DEPTH, id);
    }

    boolean start = true;
    public int depthCancelOrderNum=0;


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
            List<BigDecimal> depthPrice = getDepth();
            logger.info("获取的随机的买卖单价格:" + depthPrice);
            //买单数量
            BigDecimal buyNum = getOrderAmount();
            logger.info("深度买单价格:" + depthPrice.get(0));
            logger.info("深度买单数量:" + buyNum);

            //卖单数量
            BigDecimal sellNum = getOrderAmount();
            logger.info("深度卖单价格:" + depthPrice.get(1));
            logger.info("深度卖单数量:" + sellNum);


            //挂买
            try {
                logger.info("挂买单");
                String resultBuy = submitOrder(1, depthPrice.get(0), buyNum);
                JSONObject buyResultObject = judgeRes(resultBuy, "error", "submitTrade");
                JudegOrder(buyResultObject);
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId:" + id + exceptionMessage);
            }
            //挂卖
            try {
                String resultSell = submitOrder(2, depthPrice.get(1), sellNum);
                JSONObject sellResultObject = judgeRes(resultSell, "error", "submitTrade");
                JudegOrder(sellResultObject);
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId:" + id + exceptionMessage);
                e.printStackTrace();
            }
            setTradeLog(id, "已达撤单数" + depthCancelOrderNum, 0, "000000");
            if (Integer.parseInt(exchange.get("depthCancelNum")) < depthCancelOrderNum) {
                setTradeLog(id, "深度撤单达到上限,停止深度撤单", 0, "000000");
                setRobotArgs(id, "depthSwitch", "0");
            }

        }

//        try {
//            setBalanceRedis();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }

    public void JudegOrder(JSONObject object) throws UnsupportedEncodingException {
        if (object != null && object.getInt("error") == 0) {
            String tradeId = object.getString("data");
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("intervalTopLimit")) - Integer.parseInt(exchange.get("intervalLowerLimit"))) + Integer.parseInt(exchange.get("intervalLowerLimit")));
            sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
            String str = selectOrder(tradeId);
            JSONObject selectOrderResoult = judgeRes(str, "error", "selectOrder");
            if (selectOrderResoult != null) {
                if (selectOrderResoult.getInt("error") == 0) {
                    JSONObject selectOrderObj = selectOrderResoult.getJSONObject("data");
                    String status = selectOrderObj.getString("status");
                    if ("EX_ORDER_STATUS_FILLED".equals(status)) {
                        logger.info("深度订单id：" + tradeId + "完全成交");
                        setTradeLog(id, "深度订单id：" + tradeId + "完全成交", 0, "000000");
                        depthCancelOrderNum++;
                    } else if ("EX_ORDER_STATUS_CANCELED".equals(status)) {
                        logger.info("深度订单id：" + tradeId + "已撤单");
                        setTradeLog(id, "深度订单id：" + tradeId + "已撤单", 0, "000000");
                    } else {
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "error", "depthCancelTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_DEPTH);
                        setTradeLog(id, "深度撤单[" + tradeId + "]=>" + res, 0, "000000");
                    }
                }
            }
        }
    }


    public List<BigDecimal> getDepth() {
        List<List<Object>> buyPrices = new ArrayList<List<Object>>();
        List<List<Object>> sellPrices = new ArrayList<List<Object>>();
        List<BigDecimal> price = new ArrayList<BigDecimal>();
        String trades = httpUtil.get("http://120.77.223.226:8017/bihuex.php/depth?market=" + exchange.get("market"));

        JSONObject tradesObj = judgeRes(trades, "error", "getRandomPrice");
        if (tradesObj != null && tradesObj.getInt("error") == 0) {
            JSONObject data = tradesObj.getJSONObject("data");
            JSONObject marketData = data.getJSONObject(exchange.get("market"));
            buyPrices = (List<List<Object>>) marketData.get("buy");
            sellPrices = (List<List<Object>>) marketData.get("sell");

            int fromDepth = Integer.valueOf(exchange.get("depthOrderRange").split("_")[0]) - 1;
            int toDepth = Integer.valueOf(exchange.get("depthOrderRange").split("_")[1]) - 1;

            BigDecimal buyMinPri = new BigDecimal(buyPrices.get(fromDepth).get(0).toString());
            logger.info("买" + fromDepth + "价格:" + buyMinPri);

            BigDecimal buyMaxPri = new BigDecimal(buyPrices.get(toDepth).get(0).toString());
            logger.info("买" + toDepth + "价格:" + buyMaxPri);

            BigDecimal sellMinPri = new BigDecimal(sellPrices.get(fromDepth).get(0).toString());
            logger.info("卖" + fromDepth + "价格:" + sellMinPri);

            BigDecimal sellMaxPri = new BigDecimal(sellPrices.get(toDepth).get(0).toString());
            logger.info("卖" + toDepth + "价格:" + sellMaxPri);

            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());
            int maxBuy = buyMinPri.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
            int minBuy = buyMaxPri.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
            int minSell = sellMinPri.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
            int maxSell = sellMaxPri.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
            Random random = new Random();
            BigDecimal randBuyPrice = new BigDecimal(random.nextInt(maxBuy - minBuy + 1) + minBuy).divide(BigDecimal.valueOf(Math.pow(10, newScale)));
            BigDecimal randSellPrice = new BigDecimal(random.nextInt(maxSell - minSell + 1) + minSell).divide(BigDecimal.valueOf(Math.pow(10, newScale)));
            logger.info("深度买单价格:" + randBuyPrice);
            logger.info("深度卖单价格:" + randSellPrice);
            price.add(randBuyPrice);
            price.add(randSellPrice);
        }
        return price;
    }

    public BigDecimal getOrderAmount() {
        Random random = new Random();
        Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());
        int minAmount = new BigDecimal(exchange.get("depthOrderLowerLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        int maxAmount = new BigDecimal(exchange.get("depthOrderTopLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        BigDecimal orderAmount = new BigDecimal(random.nextInt(maxAmount - minAmount + 1) + minAmount).divide(BigDecimal.valueOf(Math.pow(10, newScale)));

        return orderAmount;
    }
}
