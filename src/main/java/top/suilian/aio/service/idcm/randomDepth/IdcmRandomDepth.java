package top.suilian.aio.service.idcm.randomDepth;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.idcm.IdcmParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class IdcmRandomDepth extends IdcmParentService {
    public IdcmRandomDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_IDCM_DEPTH, id);
    }

    boolean start = true;
    public List<HashMap<String, String>> orderIds = new ArrayList<HashMap<String, String>>();


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
        try {
            //批量挂单
            batchSubmitTrade();
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
            setTradeLog(id, "暂停时间" + st + "秒", 0);
            Thread.sleep(st * 1000);
            //批量撤单
            batchCancelTrade();
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        } catch (InterruptedException e) {
            exceptionMessage = collectExceptionStackMsg(e);
            setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }


    /**
     * 批量挂单
     *
     * @throws UnsupportedEncodingException
     */
    public void batchSubmitTrade() throws UnsupportedEncodingException, InterruptedException {
        //获取深度
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("Symbol", exchange.get("market").toUpperCase());
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("X-IDCM-APIKEY", exchange.get("apikey"));
        headers.put("X-IDCM-INPUT", JSON.toJSONString(params));
        String trades = httpUtil.post(baseUrl + "getdepth", params, headers);
        JSONObject tradesObj = judgeRes(trades, "result", "getdepth");
        if (tradesObj != null && tradesObj.getInt("result") == 1) {
            JSONObject data = tradesObj.getJSONObject("data");
            JSONArray buyPrices = data.getJSONArray("bids");
            JSONArray sellPrices = data.getJSONArray("asks");
            Integer endLayer = Integer.parseInt(exchange.get("endLayer"));
            if (buyPrices.size() < endLayer) {
                endLayer = buyPrices.size();
            }
            //买起始单价格
            BigDecimal buyMinPrice = new BigDecimal(buyPrices.getJSONObject(Integer.parseInt(exchange.get("startLayer"))).getString("price"));
            logger.info("买单最小值=>" + buyMinPrice);
            setTradeLog(id, "买单最小值=>" + buyMinPrice, 0);

            //买结束单价格
            BigDecimal buyMaxPrice = new BigDecimal(buyPrices.getJSONObject(endLayer).getString("price"));
            logger.info("买单最大值=>" + buyMaxPrice);
            setTradeLog(id, "买单最大值=>" + buyMaxPrice, 0);

            //卖起始单价格
            BigDecimal sellMinPrice = new BigDecimal(sellPrices.getJSONObject(Integer.parseInt(exchange.get("startLayer"))).getString("price"));
            logger.info("卖单最小值=>" + sellMinPrice);
            setTradeLog(id, "卖单最小值=>" + sellMinPrice, 0);

            //卖结束单价格
            BigDecimal sellMaxPrice = new BigDecimal(sellPrices.getJSONObject(endLayer).getString("price"));
            logger.info("卖单最大值=>" + sellMaxPrice);
            setTradeLog(id, "卖单最大值=>" + sellMaxPrice, 0);

            List<HashMap<String, BigDecimal>> buyTrades = new ArrayList<HashMap<String, BigDecimal>>();
            List<HashMap<String, BigDecimal>> sellTrades = new ArrayList<HashMap<String, BigDecimal>>();
            for (int i = 0; i < Integer.parseInt(exchange.get("tradeNum")); i++) {
                HashMap<String, BigDecimal> buys = new HashMap<String, BigDecimal>();
                buys.put("price", getRandomBigDecimal(buyMaxPrice, buyMinPrice, Integer.parseInt(precision.get("pricePrecision"))));
                buys.put("amount", getRandomBigDecimal(new BigDecimal(exchange.get("depthMaxNum")), new BigDecimal(exchange.get("depthMinNum")), Integer.parseInt(precision.get("amountPrecision"))));
                buyTrades.add(buys);
                HashMap<String, BigDecimal> sells = new HashMap<String, BigDecimal>();
                sells.put("price", getRandomBigDecimal(sellMaxPrice, sellMinPrice, Integer.parseInt(precision.get("pricePrecision"))));
                sells.put("amount", getRandomBigDecimal(new BigDecimal(exchange.get("depthMaxNum")), new BigDecimal(exchange.get("depthMinNum")), Integer.parseInt(precision.get("amountPrecision"))));
                sellTrades.add(sells);
            }
            logger.info("挂买单开始");
            for (int i = 0; i < buyTrades.size(); i++) {
                String buyRes = submitTrade(0, buyTrades.get(0).get("price"), buyTrades.get(0).get("amount"));
                JSONObject buyObj = judgeRes(buyRes, "result", "submitTrade");
                if (buyObj != null && buyObj.getInt("result") == 1) {
                    HashMap<String, String> buyTrade = new HashMap<String, String>();
                    buyTrade.put("orderId", buyObj.getJSONObject("data").getString("orderid"));
                    buyTrade.put("type", "0");
                    orderIds.add(buyTrade);
                }
                Thread.sleep(200);
            }
            logger.info("挂买单结束");

            logger.info("挂卖单开始");
            for (int i = 0; i < sellTrades.size(); i++) {
                String sellRes = submitTrade(1, sellTrades.get(0).get("price"), sellTrades.get(0).get("amount"));
                JSONObject sellObj = judgeRes(sellRes, "result", "submitTrade");
                if (sellObj != null && sellObj.getInt("result") == 1) {
                    HashMap<String, String> sellTrade = new HashMap<String, String>();
                    sellTrade.put("orderId", sellObj.getJSONObject("data").getString("orderid"));
                    sellTrade.put("type", "1");
                    orderIds.add(sellTrade);
                }
                Thread.sleep(200);
            }
            logger.info("挂卖单结束");
        }
    }

    public void batchCancelTrade() throws UnsupportedEncodingException, InterruptedException {
        for (int i = 0; i < orderIds.size(); i++) {
            String orderId = orderIds.get(0).get("orderId");
            Integer type = Integer.parseInt(orderIds.get(0).get("orderId"));

            String res = cancelTrade(orderId, type);
            JSONObject cancelRes = judgeRes(res, "result", "cancelTrade");
            setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_DEPTH);
            setTradeLog(id, "深度撤单[" + orderId + "]=> " + res, 0, "000000");
            Thread.sleep(200);
        }
        orderIds.clear();
    }
}
