package top.suilian.aio.service.hoo.RandomDepth;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.hoo.HooParentService;
import top.suilian.aio.service.hotcoin.HotCoinParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class HooRandomDepth extends HooParentService {

    public HooRandomDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_HOO_REFER_DEPTH, id);
    }

    boolean start = true;
    public int depthCancelOrderNum=0;


    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            logger.info("设置深度机器人参数开始");
            setParam();
            logger.info("设置深度机器人参数结束");
            start = false;
        }
        if (Integer.parseInt(exchange.get("depthCancelNum")) >= depthCancelOrderNum) {
            //获取深度
            List<BigDecimal> depthPrice = getDepths();
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
                logger.info("深度 挂买单");
                String resultBuy = submitTrade(1, depthPrice.get(0), buyNum);
                JSONObject buyResultObject = JSONObject.fromObject(resultBuy);
                if(buyResultObject.getInt("code") == 0){
                    setTradeLog(id, "深度 买单价格："+depthPrice.get(0)+",数量："+buyNum+",挂单结果："+buyResultObject, 0, "05cbc8");
                    JudegOrder(buyResultObject);
                }else{
                    setTradeLog(id, "深度 买单价格："+depthPrice.get(0)+",数量："+buyNum+",挂单结果："+buyResultObject, 0, "05cbc8");
                    sleep( 10000, Integer.parseInt(exchange.get("isMobileSwitch")));
                }
            } catch (Exception e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId:" + id + exceptionMessage);
            }
            //挂卖
            try {
                logger.info("卖单");
                String resultSellStr = submitTrade(-1, depthPrice.get(1), sellNum);
                JSONObject resultSell = JSONObject.fromObject(resultSellStr);
                if(resultSell!=null&&resultSell.getInt("code")==0){
                    setTradeLog(id, "深度 卖单价格："+depthPrice.get(1)+",数量："+sellNum+"，挂单结果："+resultSellStr, 0, "ff6224");
                    JudegOrder(resultSell);
                }else {
                    setTradeLog(id, "深度 卖单价格："+depthPrice.get(1)+",数量："+sellNum+"，挂单结果："+resultSellStr, 0, "ff6224");
                    sleep( 10000, Integer.parseInt(exchange.get("isMobileSwitch")));
                }
            } catch (Exception e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
                logger.info("robotId:" + id + exceptionMessage);
                e.printStackTrace();
            }
            setTradeLog(id, "深度 已达撤单数" + depthCancelOrderNum, 0, "000000");
            if (Integer.parseInt(exchange.get("depthCancelNum")) < depthCancelOrderNum) {
                setTradeLog(id, "深度撤单达到上限,停止深度机器人", 0, "000000");
                setRobotArgs(id, "depthSwitch", "0");
            }

        }
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }

    public void JudegOrder(JSONObject object) {
        if (object != null && object.getInt("code")==0) {
            JSONObject data = object.getJSONObject("data");
            String tradeId = data.getString("order_id");
            String orderIdTwotradeNo=data.getString("trade_no");
            int st = (int) (Math.random() * (Integer.parseInt(exchange.get("intervalTopLimit")) - Integer.parseInt(exchange.get("intervalLowerLimit"))) + Integer.parseInt(exchange.get("intervalLowerLimit")));
            sleep(st * 1000, Integer.parseInt(exchange.get("isMobileSwitch")));
            String str = selectOrder(tradeId);
            JSONObject jsonObject = JSONObject.fromObject(str);
            if (jsonObject != null && jsonObject.getInt("code") == 0) {
                JSONObject data1 = jsonObject.getJSONObject("data");
                int status = data1.getInt("status");
                if (status == 4) {
                    setTradeLog(id, "深度订单id：" + tradeId + "完全成交", 0, "000000");
                    depthCancelOrderNum++;
                } else if (status == 6) {
                    setTradeLog(id, "深度订单id：" + tradeId + "已撤单", 0, "000000");
                } else {
                    String res = cancelTrade(tradeId,orderIdTwotradeNo);
                    setTradeLog(id, "深度订单撤单[" + tradeId + "]=>" + res, 0, "000000");
                }
            }
        }
    }


    public List<BigDecimal> getDepths() {
        String depth = getDepth();
        List<BigDecimal> price = new ArrayList<BigDecimal>();
        //获取深度 判断平台撮合是否成功
        JSONObject tradesObj = JSONObject.fromObject(depth);

        if (tradesObj != null && tradesObj.getInt("code") == 0) {


            JSONObject data = tradesObj.getJSONObject("data");
            JSONArray bids = data.getJSONArray("bids");
            JSONArray asks = data.getJSONArray("asks");



            String depthOrderRange = exchange.get("depthOrderRange");

            int fromDepth = Integer.parseInt(depthOrderRange.split("_")[0]) - 1;
            int toDepth =   Integer.parseInt(depthOrderRange.split("_")[1]) - 1;

            bids.getJSONObject(fromDepth).getString("price");
            bids.getJSONObject(toDepth).getString("price");
            BigDecimal buyMinPri = new BigDecimal(bids.getJSONObject(fromDepth).getString("price"));
            BigDecimal buyMaxPri = new BigDecimal(bids.getJSONObject(toDepth).getString("price"));
            logger.info("深度 买"  + "价格:" + buyMinPri + "~~" + buyMaxPri);

            BigDecimal sellMinPri = new BigDecimal(asks.getJSONObject(fromDepth).getString("price"));

            BigDecimal sellMaxPri = new BigDecimal(asks.getJSONObject(toDepth).getString("price"));
            logger.info("深度 卖"  + "价格:" + sellMaxPri+"~~"+sellMinPri);

            Integer newScale = Integer.parseInt(exchange.get("pricePrecision").toString());
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
        Integer newScale = Integer.parseInt(exchange.get("amountPrecision").toString());
        int minAmount = new BigDecimal(exchange.get("depthOrderLowerLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        int maxAmount = new BigDecimal(exchange.get("depthOrderTopLimit")).multiply(BigDecimal.valueOf(Math.pow(10, newScale))).intValue();
        BigDecimal orderAmount = new BigDecimal(random.nextInt(maxAmount - minAmount + 1) + minAmount).divide(BigDecimal.valueOf(Math.pow(10, newScale)));

        return orderAmount;
    }
}
