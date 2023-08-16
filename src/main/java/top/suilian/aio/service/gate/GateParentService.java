package top.suilian.aio.service.gate;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

@Service
@DependsOn("beanContext")
public class GateParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://data.gateapi.io/api2/1";
    public RunHotcoinRandomDepth runHotcoinRandomDepth = BeanContext.getBean(RunHotcoinRandomDepth.class);

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];

    //设置交易量百分比
    public void setTransactionRatio() {
        String transactionRatio = exchange.get("transactionRatio");
        if (transactionRatio != null) {
            String str[] = transactionRatio.split(",");
            if (str.length > 0 && str.length <= 24) {
                int j = str.length;
                for (int i = 0; i < j; i++) {
                    transactionArr[i] = str[i].trim();
                }
                if (j < 24) {
                    for (; j < 24; j++) {
                        transactionArr[j] = "1";
                    }
                }
            } else if (str.length > 24) {
                for (int i = 0; i < 24; i++) {
                    transactionArr[i] = str[i].trim();
                }
            }
        } else {
            for (int i = 0; i < 24; i++) {
                transactionArr[i] = "1";
            }
        }
    }



    public  String getDepth(){
        String trades = httpUtil.get("https://data.gateapi.io/api2/1/orderBook/"+exchange.get("market") );
        return trades;
    }

    /**
     * {
     *     "result":"true",
     *     "message":"Success",
     *     "code":0,
     *     "ctime":1656256880.5868,
     *     "side":2,
     *     "orderNumber":173530967733,
     *     "rate":"0.0002",
     *     "leftAmount":"5000.00000000",
     *     "filledAmount":"0",
     *     "market":"TBE_USDT",
     *     "iceberg":"0",
     *     "filledRate":"0.000200000",
     *     "feePercentage":0.003,
     *     "feeValue":"0",
     *     "feeCurrency":"TBE",
     *     "fee":"0 TBE"
     * }
     * @param type
     * @param price
     * @param amount
     * @return
     */
    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String uri = "https://api.gateio.la/api2/1/private/"+(type==1?"buy":"sell");
        Map<String, String> params = new TreeMap<>();
        params.put("currencyPair", exchange.get("market"));
        params.put("rate", price1+"");
        params.put("amount", num+"");
        HashMap<String, String> head = new HashMap<>();
        head.put("Content-Type","application/x-www-form-urlencoded");
        head.put("key",exchange.get("apikey"));
        String httpParams = splicing(params);
        String sign = HMAC.Hmac_SHA512(httpParams, exchange.get("tpass"));
        head.put("sign",sign);
        try {
            trade = HttpUtil.post( uri ,params,head);
            System.out.println(trade);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(0!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("message"));
        }
        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) {
        String uri = "https://api.gateio.la/api2/1/private/getOrder";
        Map<String, String> params = new TreeMap<>();
        params.put("currencyPair", exchange.get("market"));
        params.put("orderNumber", orderId);
        HashMap<String, String> head = new HashMap<>();
        head.put("Content-Type","application/x-www-form-urlencoded");
        head.put("key",exchange.get("apikey"));
        String httpParams = splicing(params);
        String sign = HMAC.Hmac_SHA512(httpParams, exchange.get("tpass"));
        head.put("sign",sign);
        String trade=null;
        try {
            trade = HttpUtil.post( uri ,params,head);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(0!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("message"));
        }
        logger.info("查询订单："+orderId+"  结果"+trade);
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return {"result":true,"code":0,"message":"Success"}
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        String uri = "https://api.gateio.la/api2/1/private/cancelOrder";
        Map<String, String> params = new TreeMap<>();
        params.put("currencyPair", exchange.get("market"));
        params.put("orderNumber", orderId);
        HashMap<String, String> head = new HashMap<>();
        head.put("Content-Type","application/x-www-form-urlencoded");
        head.put("key",exchange.get("apikey"));
        String httpParams = splicing(params);
        String sign = HMAC.Hmac_SHA512(httpParams, exchange.get("tpass"));
        head.put("sign",sign);
        String trade=null;
        try {
            trade = HttpUtil.post( uri ,params,head);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(0!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("message"));
        }
        logger.info("撤销订单："+orderId+"  结果"+trade);
        return trade;
    }

    public String getDeep(){
        String uri = "https://api.gateio.la/api2/1/private/balances";
        Map<String, String> params = new TreeMap<>();
        HashMap<String, String> head = new HashMap<>();
        head.put("Content-Type","application/x-www-form-urlencoded");
        head.put("key",exchange.get("apikey"));
        String httpParams = splicing(params);
        String sign = HMAC.Hmac_SHA512(httpParams, exchange.get("tpass"));
        head.put("sign",sign);
        String trade=null;
        try {
            trade = HttpUtil.post( uri ,params,head);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        return trade;
    }




    public static String splicing(Map<String, String> params)  {
        StringBuilder httpParams = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(value).append("&");
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }


    /**
     * 获取余额
     */

    public void setBalanceRedis()  {
        String coins = redisHelper.getBalanceParam(Constant.KEY_ROBOT_COINS + id);
        if (coins == null) {
            RobotArgs robotArgs = robotArgsService.findOne(id, "market");
            coins = robotArgs.getRemark();
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_COINS + id, robotArgs.getRemark());
        }
        String balance = redisHelper.getBalanceParam(Constant.KEY_ROBOT_BALANCE + id);
        boolean overdue = false;
        if (balance != null) {
            long lastTime = redisHelper.getLastTime(Constant.KEY_ROBOT_BALANCE + id);
            if (System.currentTimeMillis() - lastTime > Constant.KEY_BALACE_TIME) {
                overdue = true;
            }
        }
        if (balance == null || overdue) {
            List<String> coinArr = Arrays.asList(coins.toUpperCase().split("_"));

            String deep = getDeep();
            logger.info("获取余额"+deep);

            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;
            com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(deep);
            com.alibaba.fastjson.JSONObject available = object.getJSONObject("available");
            com.alibaba.fastjson.JSONObject locked = object.getJSONObject("locked");
            firstBalance= available.getString(coinArr.get(0));
            firstBalance1= locked.getString(coinArr.get(0));
            lastBalance= available.getString(coinArr.get(1));
            lastBalance1= locked.getString(coinArr.get(1));
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance+"_"+firstBalance1);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalance1);
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
        }
    }

    /**
     * 存储撤单信息
     *
     * @param cancelRes
     * @param res
     * @param orderId
     * @param type
     */
    public void setCancelOrder(JSONObject cancelRes, String res, String orderId, Integer type) {
        int cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_HOTCOIN);
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {

        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }





    @Override
    public Map<String,String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getString("orderNumber");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("message");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        return null;
    }


    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        try {
            cancelTrade = cancelTrade(orderId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("200".equals(jsonObject.getString("code"))||"300".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }


    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 1:
                return TradeEnum.NOTRADE;

            case 2:
                return TradeEnum.TRADEING;

            case 3:
                return TradeEnum.NOTRADED;

            case 4:
                return TradeEnum.CANCEL;

            case 5:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }
}
