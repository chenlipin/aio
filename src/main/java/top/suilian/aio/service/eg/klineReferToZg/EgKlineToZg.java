package top.suilian.aio.service.eg.klineReferToZg;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Refer;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.BiboxUtils;
import top.suilian.aio.refer.GateUtils;
import top.suilian.aio.refer.HuoBiUtils;
import top.suilian.aio.refer.ZgUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.eg.EgParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EgKlineToZg extends EgParentService {

    public EgKlineToZg(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_EG_REFER_KLINE, id);
    }

    private boolean start = true;
    private String nearId = "0";
    private String exchangeRate="0";
    private boolean isCnyOrMkc=false;
    private boolean isCnyFlag=true;

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
        String baseUrl = exchange.get("baseUrl");
        if(isCnyFlag) {
            String s1 = exchange.get("market").split("_")[1];
            if (s1.equals("cny")||s1.equals("mkc")) {
                isCnyOrMkc = true;
            }
            isCnyFlag = false;
        }
        if(isCnyOrMkc) {
            //获取平台的usdt_cny汇率
            String s = httpUtil.get("http://api.zb.live/data/v1/ticker?market=usdt_qc");
            logger.info("获取汇率"+s);
            JSONObject jsonObject2 = judgeRes(s, "ticker", "getExchangeRate");
            if (jsonObject2 != null) {
                exchangeRate = jsonObject2.getJSONObject("ticker").getString("last");
            }else{
                logger.info("平台usdt_cny汇率获取失败  返回再次获取");
                sleep(500,0);
                return;
            }
        }
        Refer refer = new Refer();
        //获取对标平台 最新交易记录
        if ("0".equals(exchange.get("referexchange"))){
          refer= ZgUtils.getTrade(exchange.get("refersymbol"));
            logger.info("对标K线:"+refer.toString());

        } else if ("1".equals(exchange.get("referexchange"))) {
           refer= HuoBiUtils.getTrade(exchange.get("refersymbol"));
           logger.info("对标K线:"+refer.toString());

        } else if ("2".equals(exchange.get("referexchange"))) {
            refer= BiboxUtils.getTrade(exchange.get("refersymbol"));
            logger.info("对标K线:"+refer.toString());
        } else if ("3".equals(exchange.get("referexchange"))) {
            refer= GateUtils.getTrade(exchange.get("refersymbol"));
            logger.info("对标K线:"+refer.toString());
        }
        if(refer!=null&&refer.getPrice()!=null) {


            BigDecimal toPrice = new BigDecimal(refer.getPrice());
            BigDecimal toAmount = (new BigDecimal(refer.getAmount())).multiply(new BigDecimal(exchange.get("percentage")));
            String strDateFormat = "yyyy-MM-dd HH:mm:ss";

            SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);


            setTradeLog(id, "对标参数:时间:" + dateFormat.format(new Date(Long.parseLong(refer.getId()))) + "价格:" + refer.getPrice() + "数量:" + refer.getAmount() + "类型:" + refer.getIsSell(), 1);

            if (!nearId.equals(refer.getId())) {
                BigDecimal finalPrice=BigDecimal.ZERO;
                BigDecimal finalAmount=BigDecimal.ZERO;
                //挂单  并判断买卖单
                //先挂 卖单
                if(isCnyOrMkc){
                    finalPrice = nN(toPrice.multiply(new BigDecimal(exchangeRate)), Integer.parseInt(precision.get("pricePrecision").toString()));
                    finalAmount  = nN(toAmount, Integer.parseInt(precision.get("amountPrecision").toString()));
                }else {
                     finalPrice = nN(toPrice, Integer.parseInt(precision.get("pricePrecision").toString()));
                    finalAmount  = nN(toAmount, Integer.parseInt(precision.get("amountPrecision").toString()));
                }

                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));

                String resultJson = submitOrder(refer.getIsSell().equals("buy") ? 0 : 1, finalPrice, finalAmount);
                JSONObject jsonObject = judgeRes(resultJson, "code", "submitTrade");

                //挂单成功
                if (jsonObject != null && jsonObject.getString("code").equals("0000")) {
                    String tradeId = jsonObject.getString("data");

                    String resultJson1 = submitOrder(refer.getIsSell().equals("buy") ? 1 : 0, finalPrice, finalAmount);
                    JSONObject jsonObject1 = judgeRes(resultJson1, "code", "submitTrade");

                    if (jsonObject1 != null && jsonObject1.getString("code").equals("0000")) {
                        sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                        nearId = refer.getId();
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
        }else {
            setTradeLog(id, "未切换交易对，请确认该对标平台存在该交易对" , 0, "000000");
        }
        sleep(10000, Integer.parseInt(exchange.get("isMobileSwitch")));
        setBalanceRedis();
        clearLog();

    }

}
