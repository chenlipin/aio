package top.suilian.aio.service.fchain.randomDepth;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.fchain.FChainParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FChainDepth extends FChainParentService {

    public FChainDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_FCHAIN_DEPTH, id);
    }


    private boolean start = true;
    public int depthCancelOrderNum = 0;


    public void init() {

        if (start) {
            start = false;
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            if (!setPrecision()) {
                return;
            }
            logger.info("设置机器人交易规则结束");
            //随机交易区间
        }


        if (Integer.parseInt(exchange.get("depthCancelNum")) >= depthCancelOrderNum) {
            //获取深度
            List<BigDecimal> depthPrice = getDepth();
            logger.info("获取的随机的买卖单价格:" + depthPrice);

            //买单数量
            BigDecimal buyNum = getOrderAmount();

            //卖单数量
            BigDecimal sellNum = getOrderAmount();
            logger.info("深度买单价格:" + depthPrice.get(0));
            logger.info("深度买单数量:" + buyNum);
            logger.info("深度卖单价格:" + depthPrice.get(1));
            logger.info("深度卖单数量:" + sellNum);


            //挂买
            try {
                String resultBuy = submitTrade(1, depthPrice.get(0), buyNum);
                JSONObject jsonObject = judgeRes(resultBuy, "status", "submitTrade");


                if (jsonObject != null) {
                    logger.info("深度买单:" + depthPrice.get(0) + "---" + buyNum);
                    if (resultBuy != null && !resultBuy.equals("")) {
                        logger.info("挂买单结果:" + resultBuy);
//                                setTradeLog(id, "深度挂买单=>" + resultBuy, 0, "000000");
                        JSONObject buyResultObject = new JSONObject().fromObject(resultBuy);
                        String tradeId = buyResultObject.getString("orderId");
                        try {
                            Thread.sleep(Integer.parseInt(exchange.get("cancelDepthOrderSecond")) * 1000);
                        } catch (InterruptedException e) {
                            exceptionMessage = collectExceptionStackMsg(e);
                            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                        }
                        String str = selectOrder(tradeId);
                        JSONObject selectOrderResult = judgeRes(str, "status", "selectOrder");
                        if (str != null && selectOrderResult != null) {
                            String status = (String) selectOrderResult.get("status");
                            if ("FILLED".equals(status)) {
                                setTradeLog(id, "深度订单id：" + tradeId + "完全成交", 0, "000000");
                                depthCancelOrderNum++;
                            } else if ("CANCELED".equals(status)) {
                                setTradeLog(id, "深度订单id：" + tradeId + "已撤单", 0, "000000");
                            } else {

                                String res = cancelTrade(tradeId);
                                JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                                setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_DEPTH);
                                setTradeLog(id, "撤单[" + tradeId + "]=>" + res, 0, "000000");

                            }
                        }
                    }
                }


            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            }

            //挂卖
            try {
                String resultSell = submitTrade(2, depthPrice.get(1), sellNum);
                JSONObject jsonObject = judgeRes(resultSell, "status", "submitTrade");
                if (jsonObject != null) {
                    logger.info("深度卖单:" + depthPrice.get(1) + "---" + sellNum);
                    if (resultSell != null && !resultSell.equals("")) {
                        JSONObject sellResultObject = new JSONObject().fromObject(resultSell);
//                                setTradeLog(id, "深度挂卖单=>" + resultSell, 0, "000000");
                        logger.info("挂卖单结果" + resultSell);
                        String tradeId = sellResultObject.getString("orderId");

                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String str = selectOrder(tradeId);
                        JSONObject selectOrderResult = judgeRes(str, "status", "selectOrder");
                        if (str != null && selectOrderResult != null) {
                            String status = (String) selectOrderResult.get("status");
                            if ("FILLED".equals(status)) {
                                setTradeLog(id, "深度订单id：" + tradeId + "完全成交", 0, "000000");
                                depthCancelOrderNum++;
                            } else if ("CANCELED".equals(status)) {
                                setTradeLog(id, "深度订单id：" + tradeId + "已撤单", 0, "000000");
                            } else {
                                String res = cancelTrade(tradeId);
                                JSONObject cancelRes = judgeRes(res, "status", "cancelTrade");
                                setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_DEPTH);
                                setTradeLog(id, "撤单[" + tradeId + "]=>" + res, 0, "000000");

                            }
                        }
                    }
                }


            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            }
            setTradeLog(id, "已达撤单数" + depthCancelOrderNum, 0, "000000");
            if (Integer.parseInt(exchange.get("depthCancelNum")) < depthCancelOrderNum) {
                setTradeLog(id, "深度撤单达到上限,停止深度撤单", 0, "000000");
            }
            setTradeLog(id, "当前深度撤单数为" + depthCancelOrderNum, 0, "000000");
        }

        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        clearLog();
    }


    public List<BigDecimal> getDepth() {
        String trades = httpUtil.get(baseUrl + "/openapi/quote/v1/depth?symbol=" + exchange.get("market"));
        JSONObject tradesObj = judgeRes(trades, "bids", "getRandomPrice");
        List<List<String>> buyPrices = new ArrayList<List<String>>();
        List<List<String>> sellPrices = new ArrayList<List<String>>();

        List<BigDecimal> price = new ArrayList<BigDecimal>();

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {

            buyPrices = (List<List<String>>) tradesObj.get("bids");

            sellPrices = (List<List<String>>) tradesObj.get("asks");


            int fromDepth = Integer.valueOf(exchange.get("depthOrderRange").split("_")[0]) - 1;
            int toDepth = Integer.valueOf(exchange.get("depthOrderRange").split("_")[1]) - 1;


            BigDecimal buyMin = new BigDecimal(buyPrices.get(fromDepth).get(0));
            BigDecimal buyMax = new BigDecimal(buyPrices.get(toDepth).get(0));

            BigDecimal sellMin = new BigDecimal(sellPrices.get(fromDepth).get(0));
            BigDecimal sellMax = new BigDecimal(sellPrices.get(toDepth).get(0));


            Integer newScale = Integer.parseInt(precision.get("pricePrecision").toString());

            int maxBuy = buyMin.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();

            int minBuy = buyMax.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();

            int minSell = sellMin.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();

            int maxSell = sellMax.multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();


            logger.info("买" + fromDepth + "价格:" + buyMin);
            logger.info("买" + toDepth + "价格:" + buyMax);

            logger.info("卖" + fromDepth + "价格:" + sellMin);
            logger.info("卖" + toDepth + "价格:" + sellMax);


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

        int minAmount = new BigDecimal(precision.get("minTradeLimit").toString()).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();

        int maxAmount = (new BigDecimal(exchange.get("depthOrderAmount"))).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();

        BigDecimal orderAmount = new BigDecimal(random.nextInt(maxAmount - minAmount + 1) + minAmount).divide(BigDecimal.valueOf(Math.pow(10, newScale)));


        return orderAmount;
    }
}
