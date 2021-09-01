package top.suilian.aio.service.hoo.klineReferBitai;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONTokener;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.hoo.HooParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

public class KlineToBitai extends HooParentService {

    public KlineToBitai(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_HOO_REFER_KLINE, id);
    }
    private boolean start = true;
    private String nearId = "0";
    private String referBaseUrl = "https://api.bitai.io";
    String oneOrder="0";
    String oneTradeNo="0";
    String twoOrder="0";
    String twoTradeNo="0";
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

        String result = httpUtil.get(referBaseUrl + "/openapi/quote/v1/trades?symbol=" +exchange.get("refersymbol")+"&limit=1");

        JSONArray nearJson = JSONArray.fromObject(result);


        if (nearJson != null && nearJson.getJSONObject(0).getString("time") != null) {

            JSONObject nearLog = nearJson.getJSONObject(0);

            String amount = nearLog.getString("qty");
            String price = nearLog.getString("price");
            String logId = nearLog.getString("time");
            boolean type = nearLog.getBoolean("isBuyerMaker");
            BigDecimal toPrice = new BigDecimal(price);
            BigDecimal toAmount = (new BigDecimal(amount)).multiply(new BigDecimal(exchange.get("percentage")));

            setTradeLog(id, "对标参数:id:" + logId + "价格:" + price + "数量:" + amount + "类型:" + type, 1);

            if (!nearId.equals(logId)) {
                //挂单  并判断买卖单
                //先挂 卖单

                BigDecimal finalPrice = nN(toPrice,Integer.parseInt(precision.get("pricePrecision").toString()));
                BigDecimal finalAmount = nN(toAmount,Integer.parseInt(precision.get("amountPrecision").toString()));



                String resultJson = submitOrder(type ? 2 : 1, finalPrice, finalAmount);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitOrder");

                System.out.println(exchange.get("isMobileSwitch"));
                //挂单成功
                if (jsonObject != null && jsonObject.getInt("code")==0) {
                    JSONObject dataJson=jsonObject.getJSONObject("data");
                    String tradeId = dataJson.getString("order_id");
                    String tradeNo = dataJson.getString("trade_no");
                    oneOrder=tradeId;
                    oneTradeNo=tradeNo;

                    String resultJson1 = submitOrder(type ? 1 : 2, finalPrice, finalAmount);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitOrder");

                    if (jsonObject1 != null && jsonObject1.getInt("code")==0) {
                        JSONObject dataJson1=jsonObject1.getJSONObject("data");
                        twoOrder=dataJson1.getString("order_id");
                        twoTradeNo=dataJson1.getString("trade_no");
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        nearId = logId;
                    } else {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String res = cancelTrade(tradeId,tradeNo);
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
        //2委托中，3部分成交，4全部成交，5部分成交后撤消，6全部撤消
        logger.info("订单一："+oneOrder+"--------"+"订单二："+twoOrder);
        if (oneOrder!="0"||twoOrder!="0"){
            String oneResult=selectOrder(oneOrder);
            Object object = new JSONTokener(oneResult).nextValue();
            if (object instanceof JSONObject) {
                JSONObject oneJson = JSONObject.fromObject(oneResult);
                logger.info("订单1详情"+oneResult);
                JSONObject oneData = oneJson.getJSONObject("data");
                if(oneData.getInt("status")==4){
                    setTradeLog(id, "订单[" + oneOrder + "]完全成交"  , 0, "000000");
                }else if(oneData.getInt("status")==2||oneData.getInt("status")==3) {
                    String rt= cancelTrade(oneOrder,oneTradeNo);
                    JSONObject jsonObject=JSONObject.fromObject(rt);
                    if(jsonObject.getInt("code")==0){
                        setTradeLog(id, "订单[" + oneOrder + "]撤单成功=>"+rt  , 0, "000000");
                    }else {
                        setTradeLog(id, "订单[" + oneOrder + "]撤单失败=》"+rt , 0, "000000");
                    }
                }
            }else{
                logger.info("查询订单一："+oneOrder+"失败=======》"+oneResult);
            }

            String twoResult=selectOrder(twoOrder);
            Object object1 =  new JSONTokener(twoResult).nextValue();
            if (object1 instanceof JSONObject) {
                JSONObject twoJson = JSONObject.fromObject(twoResult);
                logger.info("订单2详情"+twoResult);
                JSONObject twoData =twoJson.getJSONObject("data");
                if(twoData.getInt("status")==4){
                    setTradeLog(id, "订单[" + twoOrder + "]完全成交"  , 0, "000000");
                }else if(twoData.getInt("status")==2||twoData.getInt("status")==3) {
                    String rt= cancelTrade(twoOrder,twoTradeNo);
                    JSONObject jsonObject=JSONObject.fromObject(rt);
                    if(jsonObject.getInt("code")==0){
                        setTradeLog(id, "订单[" + twoOrder + "]撤单成功=>"+rt  , 0, "000000");
                    }else {
                        setTradeLog(id, "订单[" + twoOrder + "]撤单失败=》"+rt , 0, "000000");
                    }
                }
            }else{
                logger.info("查询订单二："+twoOrder+"失败=======》"+twoResult);
            }



         }
       }


}
