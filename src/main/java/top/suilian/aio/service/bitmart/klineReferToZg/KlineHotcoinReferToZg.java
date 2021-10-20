package top.suilian.aio.service.bitmart.klineReferToZg;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.bitmart.BitMartParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public class KlineHotcoinReferToZg extends BitMartParentService {
    public KlineHotcoinReferToZg(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_HOTCOIN_REFER_KLINE, id);
    }

    private boolean start = true;
    private String nearId = "0";
    private String referBaseUrl = "https://api1.zg.com";
    String tradeId = "0";

    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");
            //随机交易区间
            start = false;
        }

        //获取对标平台 最新交易记录
        String result = httpUtil.get(referBaseUrl + "/trades?symbol=" + exchange.get("referSymbol") + "&size=5");


        if (result != null && result.indexOf("amount") != -1) {

            JSONArray jsonArray = JSONArray.fromObject(result);

            JSONObject nearLog = jsonArray.getJSONObject(0);

            String amount = nearLog.getString("amount");
            String price = nearLog.getString("price");

            String timestamp = nearLog.getString("timestamp");

            String type = nearLog.getString("side");


            BigDecimal toPrice = new BigDecimal(price);
            BigDecimal toAmount = new BigDecimal(amount);

            String strDateFormat = "yyyy-MM-dd HH:mm:ss";

            SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);


            setTradeLog(id, "对标参数:价格:" + price + "数量:" + amount + "类型:" + type, 1);

            if (!nearId.equals(timestamp)) {
                //挂单  并判断买卖单
                //先挂 卖单

                BigDecimal finalPrice = nN(toPrice, Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalAmount = nN(toAmount, Integer.parseInt(precision.get("amountPrecision").toString()));

                sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));

                String resultJson = submitOrder(type.equals("buy") ? 1 : 0, finalPrice, finalAmount);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitOrder");

                //挂单成功
                if (jsonObject != null && jsonObject.getInt("code") == 200) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    tradeId = data.getString("ID");

                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String resultJson1 = submitOrder(type.equals("buy") ? 0 : 1, finalPrice, finalAmount);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 200) {
                        nearId = timestamp;
                    } else {
                        sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                        if ("0".equals(tradeId)) {
                            logger.info("当前没有下单");
                        } else {
                            String res = cancelTrade(tradeId);
                            JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_KLINE);
                            setTradeLog(id, "对标K线撤单[" + tradeId + "]=>" + res, 0, "000000");
                        }

                    }

                }
                tradeId = "0";

            } else {
                setTradeLog(id, "无最新成交记录", 1);
            }
            sleep(10000, Integer.parseInt(exchange.get("isMobileSwitch")));

        }

        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();


    }


}
