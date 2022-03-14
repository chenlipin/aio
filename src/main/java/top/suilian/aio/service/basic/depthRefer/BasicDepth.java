package top.suilian.aio.service.basic.depthRefer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.BianUtils;
import top.suilian.aio.refer.DeepVo;
import top.suilian.aio.service.*;
import top.suilian.aio.service.basic.OrderVO;
import top.suilian.aio.service.basic.BasicParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class BasicDepth extends BasicParentService {

    public BasicDepth(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_CL_REFER_DEPTH, id);
    }

    private boolean start = true;
    private JSONArray orders=null;
    private JSONArray lastOrders=null;
    public HashMap<String, Map<String, String>> depth = new HashMap<>();
    public void init() {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");
            start = false;
        }
        submitOrder(null);
        Map<String, List<DeepVo>> deep = BianUtils.getdeep(exchange.get("referMarket"));
        List<DeepVo> history = BianUtils.getHistory(exchange.get("referMarket"));

        BigDecimal amountPoint = new BigDecimal(exchange.get("amountPoint"));
        int deepNum = Integer.parseInt(exchange.get("deepNum"));

        List<DeepVo> deepBuyList = deep.get("deepBuyList");
        List<DeepVo> deepSellList = deep.get("deepSellList");

        ArrayList<OrderVO> orderVOS = new ArrayList<>();
        //深度单子
        for (int i = 0; i < deepBuyList.size()&&i<=deepNum-1; i++) {
            //买单
            OrderVO orderVOBuy = new OrderVO();
            DeepVo deepVoBuy = deepBuyList.get(i);
            orderVOBuy.setPair(exchange.get("market"));
            BigDecimal price = nN(deepVoBuy.getPrice(), Integer.parseInt(exchange.get("pricePrecision").toString())).stripTrailingZeros();
            BigDecimal num = nN(deepVoBuy.getAmount().multiply(amountPoint), Integer.parseInt(exchange.get("amountPrecision").toString())).stripTrailingZeros();
            orderVOBuy.setPrice(price.stripTrailingZeros().toPlainString());
            orderVOBuy.setNumber(num.stripTrailingZeros().toPlainString());
            orderVOBuy.setType("buy");
            //卖单
            DeepVo deepVoSell = deepSellList.get(i);
            OrderVO orderVOSell = new OrderVO();
            orderVOSell.setPair(exchange.get("market"));
            BigDecimal price1 = nN(deepVoSell.getPrice(), Integer.parseInt(exchange.get("pricePrecision").toString())).stripTrailingZeros();
            BigDecimal num1 = nN(deepVoSell.getAmount().multiply(amountPoint), Integer.parseInt(exchange.get("amountPrecision").toString())).stripTrailingZeros();
            orderVOSell.setPrice(price1.stripTrailingZeros().toPlainString());
            orderVOSell.setNumber(num1.stripTrailingZeros().toPlainString());
            orderVOSell.setType("sell");

            orderVOS.add(orderVOBuy);
            orderVOS.add(orderVOSell);
        }
        //k线
        for (DeepVo deepVo : history) {
            boolean flag = RandomUtils.nextBoolean();
            OrderVO order = new OrderVO();
            BigDecimal price1 = nN(deepVo.getPrice(), Integer.parseInt(exchange.get("pricePrecision").toString())).stripTrailingZeros();
            BigDecimal num1 = nN(deepVo.getAmount().multiply(amountPoint), Integer.parseInt(exchange.get("amountPrecision").toString())).stripTrailingZeros();

            order.setPrice(price1.stripTrailingZeros().toPlainString());
            order.setNumber(num1.stripTrailingZeros().toPlainString());
            order.setPair(exchange.get("market"));
            order.setType(flag?"buy":"sell");

            OrderVO order2 = new OrderVO();
            order2.setPrice(price1.stripTrailingZeros().toPlainString());
            order2.setNumber(num1.stripTrailingZeros().toPlainString());
            order2.setPair(exchange.get("market"));
            order2.setType(flag?"sell":"buy");

            orderVOS.add(order);
            orderVOS.add(order2);
        }
        //挂单
        String order = submitOrder(orderVOS);
        JSONObject jsonObject = JSONObject.parseObject(order);
        if (jsonObject.getInteger("jsonObject").equals(200)){
            orders = jsonObject.getJSONArray("data");
        }
        sleep(3000, Integer.parseInt("1"));
       if (lastOrders.size()>0){
           cancalOrder(lastOrders,exchange.get("market"));
           lastOrders=orders;
       }
        int st = (int) (Math.random() * (Integer.parseInt(exchange.get("endTime")) - Integer.parseInt(exchange.get("startTime"))) + Integer.parseInt(exchange.get("startTime")));
        setTradeLog(id, "暂停时间----------------------------->" + st + "秒", 0);
        sleep(20000, Integer.parseInt("1"));
        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();


    }

    /**
     * 获得数量
     *
     * @param pression
     * @return
     */
    BigDecimal getOrderAmount(double pression) {
        double mind = Double.parseDouble(exchange.get("numMinThreshold"));
        double maxd = Double.parseDouble(exchange.get("numThreshold"));
        long maxQty = (long) (maxd * Math.pow(10, pression));
        long minty = (long) (mind * Math.pow(10, pression));
        long randNumber = minty + (((long) (new Random().nextDouble() * (maxQty - minty))));
        return new BigDecimal(String.valueOf(randNumber / Math.pow(10, pression)));
    }


}
