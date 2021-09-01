package top.suilian.aio.service.euex.klineReferToZg;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.euex.EuexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EuexKlineToZg extends EuexParentService {
    public EuexKlineToZg(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_EUEX_REFER_KLINE, id);
    }

    private boolean start = true;
    private String nearId = "0";
    private String referBaseUrl = "https://api1.zg.com";
    String orderOne="0";
    String orderTow="0";

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
        String result = httpUtil.get(referBaseUrl + "/trades?symbol=" + exchange.get("market").toUpperCase() + "&size=5");


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

                String resultJson = submitOrder(type.equals("buy") ? 0 : 1, finalPrice, finalAmount);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                //挂单成功
                if (jsonObject != null && jsonObject.getString("code").equals("0000")) {
                    String tradeId = jsonObject.getString("data");
                    orderOne=tradeId;
                    sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String resultJson1 = submitOrder(type.equals("buy") ? 1 : 0, finalPrice,  finalAmount);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getString("code").equals("0000")) {
                        String tradeId1 = jsonObject1.getString("data");
                        orderTow=tradeId1;
                        nearId = timestamp;
                    } else {
                        sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId);
                        JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                        setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_KLINE);
                        setTradeLog(id, "对标K线撤单[" + tradeId + "]=>" + res, 0, "000000");
                    }

                }

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
        logger.info("订单一："+orderOne+"--------"+"订单二："+orderTow);
        if (orderOne!="0"&&orderOne!=null) {
            String oneResult=null;
            JSONObject oneJson=null;
            Boolean flag=true;
            int count=0;
            while (flag&&count<3){
                oneResult = selectOrder(orderOne);
                oneJson = JSONObject.fromObject(oneResult);
                if(oneResult!=null&&oneJson!=null&&oneJson.getString("code").equals("3122")){
                    sleep(1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    count++;
                }else if(oneResult!=null&&oneJson!=null&&oneJson.getString("code").equals("0000")){
                    if (oneJson.getJSONObject("data").getString("status").equals("3")) {
                        logger.info(orderOne + "完全成交");
                        setTradeLog(id, "对标K线完全成交[" + orderOne + "]=>" + oneResult, 0, "000000");
                        flag=false;
                    } else if (oneJson.getJSONObject("data").getString("status").equals("5")) {
                        logger.info(orderOne + "已撤单");
                        setTradeLog(id, "对标K线已撤单[" + orderOne + "]=>" + oneJson, 0, "000000");
                        flag=false;
                    } else {
                        sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                        judgeCancel(orderOne);
                        flag=false;
                    }
                }
            }
            logger.info("-----------查询订单一"+oneResult);

        }
        sleep(1000, Integer.parseInt(exchange.get("isMobileSwitch")));
        if (orderTow!="0"&&orderTow!=null) {
            String twoResult=null;
            JSONObject twoJson=null;
            Boolean flag=true;
            int count=0;
            while (flag&&count<3){
                twoResult = selectOrder(orderTow);
                twoJson = JSONObject.fromObject(twoResult);
                if(twoResult!=null&&twoJson!=null&&twoJson.getString("code").equals("3122")){
                    sleep(1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    count++;
                }else if(twoResult!=null&&twoJson!=null&&twoJson.getString("code").equals("0000")){
                    if (twoJson.getJSONObject("data").getString("status").equals("3")) {
                        logger.info(orderTow + "完全成交");
                        setTradeLog(id, "对标K线完全成交[" + orderTow + "]=>" + twoResult, 0, "000000");
                        flag=false;
                    } else if (twoJson.getJSONObject("data").getString("status").equals("5")) {
                        logger.info(orderTow + "已撤单");
                        setTradeLog(id, "对标K线已撤单[" + orderTow + "]=>" + twoResult, 0, "000000");
                        flag=false;
                    } else {
                        sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                        judgeCancel(orderTow);
                        flag=false;
                    }
                }
            }
            logger.info("-----------查询订单二"+twoJson);

        }

            orderOne="0";
            orderTow="0";


    }
}
