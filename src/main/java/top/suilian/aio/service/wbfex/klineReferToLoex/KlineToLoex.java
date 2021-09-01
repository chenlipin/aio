package top.suilian.aio.service.wbfex.klineReferToLoex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.wbfex.WbfexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

public class KlineToLoex extends WbfexParentService {

    public KlineToLoex(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_WBFEX_REFER_KLINE, id);
    }
    private boolean start = true;
    private String nearId = "0";
    private String referBaseUrl = "https://openapi.loex.io";
    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            if (!setPrecision()) {
                return;
            }
            logger.info("设置机器人交易规则结束");
            //随机交易区间
            start = false;
        }

        //获取对标平台 最新交易记录
        String result = httpUtil.get(referBaseUrl + "/open/api/get_trades?symbol=" + exchange.get("market"));

        JSONObject nearTrade = judgeRes(result, "code", "getRandomPrice");

        if (nearTrade != null && nearTrade.getInt("code") == 0) {
            JSONArray data = nearTrade.getJSONArray("data");

            JSONObject nearLog = data.getJSONObject(0);

            String amount = nearLog.getString("amount");
            String price = nearLog.getString("price");
            String logId = nearLog.getString("id");
            String type = nearLog.getString("type");
            BigDecimal toPrice = new BigDecimal(price);
            BigDecimal toAmount = new BigDecimal(amount);

            setTradeLog(id, "对标参数:id:" + logId + "价格:" + price + "数量:" + amount + "类型:" + type, 1);

            if (!nearId.equals(logId)) {
                //挂单  并判断买卖单
                //先挂 卖单

                BigDecimal finalPrice = nN(toPrice,Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalAmount = nN(toAmount,Integer.parseInt(precision.get("amountPrecision").toString()));


                String resultJson = submitOrder(type.equals("buy") ? 2 : 1, finalPrice, finalAmount);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                System.out.println(exchange.get("isMobileSwitch"));
                //挂单成功
                if (jsonObject != null && jsonObject.getInt("code") == 0) {
                    JSONObject submitData = jsonObject.getJSONObject("data");
                    String tradeId = submitData.getString("order_id");

                    String resultJson1 = submitOrder(type.equals("buy") ? 1 : 2, finalPrice, finalAmount);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getInt("code") == 0) {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        nearId = logId;
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_KLINE);
                        setTradeLog(id, "对标K线撤单[" + tradeId + "]=>" + res, 0, "000000");
                    }

                }

            } else {
                setTradeLog(id, "无最新成交记录", 1);
            }


        }

        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();
        sleep(10000, Integer.parseInt(exchange.get("isMobileSwitch")));
    }
}
