package top.suilian.aio.service.weex;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.DateUtils;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@DependsOn("beanContext")
public class WeexParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api-spot.weex.com";
    public String host = "";
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    private static final ZoneId ZONE_GMT = ZoneId.of("Z");



    @Override
    public List<getAllOrderPonse> selectOrder() {
        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();
        String trade = getTrade();
        JSONObject jsonObject1 = JSONObject.fromObject(trade);
        JSONArray array = jsonObject1.getJSONObject("data").getJSONArray("orderInfoResultList");

        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            JSONObject jsonObject = array.getJSONObject(i);

            getAllOrderPonse.setOrderId(jsonObject.getString("orderId"));
            getAllOrderPonse.setCreatedAt(DateUtils.convertTimestampToString(jsonObject.getLong("cTime")));
            getAllOrderPonse.setPrice(jsonObject.getString("side")+"-"+new BigDecimal(jsonObject.getString("price")).stripTrailingZeros().toPlainString());
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(new BigDecimal(jsonObject.getString("quantity")).stripTrailingZeros().toPlainString());
            getAllOrderPonses.add(getAllOrderPonse);
        }
        return getAllOrderPonses;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

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


    /**
     *
     * @param type
     * @param price
     * @param amount
     * @return {"status":"ok","data":"1133371409337541"}
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + (type==1?"买":"卖") + "，price(价格)：" + price + "，amount(数量)：" + amount);
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        long timeMillis = System.currentTimeMillis();
        String strSign =timeMillis+ "POST" +"/api/v2/trade/orders";

        Map<String, String> reqparam = new TreeMap<String, String>();
        reqparam.put("symbol", exchange.get("market"));
        reqparam.put("side",type==1? "buy":"sell");
        reqparam.put("orderType","limit");
        reqparam.put("force","normal");
        reqparam.put("price", price1+"");
        reqparam.put("quantity",num+"");
        reqparam.put("clientOrderId","sl"+timeMillis);
        strSign=strSign+JSON.toJSONString(reqparam);
        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ACCESS-KEY",exchange.get("apikey"));
        params.put("ACCESS-TIMESTAMP",timeMillis+"");
        params.put("ACCESS-PASSPHRASE", exchange.get("memo"));
        params.put("Content-Type", "application/json");
        params.put("ACCESS-SIGN", sign);
        params.put("locale", "zh-CN");

        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/api/v2/trade/orders", params, reqparam);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
            if(!"00000".equals(jsonObject.getString("code"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return trade;
    }



    protected String setAccount() {
        String strSign = "GET" + "\n" +
                "api.huobi.pro" + "\n" +
                "/v1/account/accounts" + "\n";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("AccessKeyId",exchange.get("apikey"));
        params.put("SignatureMethod","HmacSHA256");
        params.put("SignatureVersion","2");
        params.put("Timestamp",gmtNow());
        String splicingStr = splicingStr(params);
        strSign=strSign+splicingStr;
        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));
        sign = urlEncode(sign);
        String trade = null;
        trade = HttpUtil.get("https://api.huobi.pro/v1/account/accounts?" + splicingStr + "&Signature=" + sign);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if("ok".equals(jsonObject.getString("status"))){
            String string = jsonObject.getJSONArray("data").getJSONObject(0).getString("id");
            exchange.put("account-id",string);
            System.out.println("account-id-----"+string);
        }

        return trade;
    }


    public String getBalance() throws UnsupportedEncodingException {
        long timeMillis = System.currentTimeMillis();
        String strSign =timeMillis+ "GET" +"/api/v2/account/assets";

        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ACCESS-KEY",exchange.get("apikey"));
        params.put("ACCESS-TIMESTAMP",timeMillis+"");
        params.put("ACCESS-PASSPHRASE", exchange.get("memo"));
        params.put("Content-Type", "application/json");
        params.put("ACCESS-SIGN", sign);
        params.put("locale", "zh-CN");
        String trade = null;
        try {
            trade = HttpUtil.getAddHead(baseUrl + "/api/v2/account/assets", params);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if(!"00000".equals(jsonObject.getString("code"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return trade;
    }



    /**
     * @param orderId
     * @return
     * @throws
     * {
     *     "status": "ok",
     *     "data": {
     *         "id": 1133371409337541,
     *         "symbol": "trxusdt",
     *         "account-id": 62914536,
     *         "client-order-id": "",
     *         "amount": "100",
     *         "market-amount": "0",
     *         "ice-amount": "0",
     *         "is-ice": false,
     *         "price": "0.12",
     *         "created-at": 1723729310315,
     *         "type": "buy-limit",
     *         "field-amount": "0",
     *         "field-cash-amount": "0",
     *         "field-fees": "0",
     *         "finished-at": 0,
     *         "updated-at": 1723729310315,
     *         "source": "spot-api",
     *         "state": "submitted",
     *         "canceled-at": 0,
     *         "canceled-source": null,
     *         "canceled-source-desc": null
     *     }
     * }
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        long timeMillis = System.currentTimeMillis();
        String strSign =timeMillis+ "POST" +"/api/v2/trade/orderInfo";

        Map<String, String> reqparam = new TreeMap<String, String>();
        reqparam.put("orderId", orderId);
        strSign=strSign+JSON.toJSONString(reqparam);
        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ACCESS-KEY",exchange.get("apikey"));
        params.put("ACCESS-TIMESTAMP",timeMillis+"");
        params.put("ACCESS-PASSPHRASE", exchange.get("memo"));
        params.put("Content-Type", "application/json");
        params.put("ACCESS-SIGN", sign);
        params.put("locale", "zh-CN");
        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/api/v2/trade/orderInfo", params, reqparam);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            setTradeLog(id, "查询订单[orderId：" + orderId + "]=>" + trade, 0);
            if(!"00000".equals(jsonObject.getString("code"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return trade;
        }






    /**
     * 撤单
     * @param orderId
     * @return
     * {"status":"ok","data":"1133371409337541"}
     * @throws
     */
    public String cancelTrade(String orderId)  {

        long timeMillis = System.currentTimeMillis();
        String strSign =timeMillis+ "POST" +"/api/v2/trade/cancel-order";

        Map<String, String> reqparam = new TreeMap<String, String>();
        reqparam.put("orderId", orderId);
        reqparam.put("symbol", exchange.get("market"));
        strSign=strSign+JSON.toJSONString(reqparam);
        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ACCESS-KEY",exchange.get("apikey"));
        params.put("ACCESS-TIMESTAMP",timeMillis+"");
        params.put("ACCESS-PASSPHRASE", exchange.get("memo"));
        params.put("Content-Type", "application/json");
        params.put("ACCESS-SIGN", sign);
        params.put("locale", "zh-CN");
        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/api/v2/trade/cancel-order", params, reqparam);
            JSONObject jsonObject = JSONObject.fromObject(trade);
           logger.info("撤销订单::"+orderId+",cancelTrade返回参数：" + trade);
            if(!"00000".equals(jsonObject.getString("code"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return trade;
    }


    /**
     * 获取所有尾单
     * @return
     */
    public String getTrade() {

        long timeMillis = System.currentTimeMillis();
        String strSign =timeMillis+ "POST" +"/api/v2/trade/open-orders";

        Map<String, String> reqparam = new TreeMap<String, String>();
        reqparam.put("symbol", exchange.get("market"));
        strSign=strSign+JSON.toJSONString(reqparam);
        String sign = HMAC.sha256_HMACAndBase(strSign, exchange.get("tpass"));

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ACCESS-KEY",exchange.get("apikey"));
        params.put("ACCESS-TIMESTAMP",timeMillis+"");
        params.put("ACCESS-PASSPHRASE", exchange.get("memo"));
        params.put("Content-Type", "application/json");
        params.put("ACCESS-SIGN", sign);
        params.put("locale", "zh-CN");
        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/api/v2/trade/open-orders", params, reqparam);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            logger.info("获取未成交列表返回参数：" + trade);
            if(!"00000".equals(jsonObject.getString("code"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return trade;
    }


    /**
     * 获取余额
     */

    public void setBalanceRedis() throws UnsupportedEncodingException {
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
            String balance1 = getBalance();
            JSONObject jsonObject1 = JSONObject.fromObject(balance1);
            JSONArray jsonArray1 = jsonObject1.getJSONArray("data");

            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;

            List<String> coinArr = Arrays.asList(coins.split("_"));
            for (int i = 0; i < jsonArray1.size(); i++) {
                JSONObject jsonObject = jsonArray1.getJSONObject(i);
                if (jsonObject.getString("coinName").equals(coinArr.get(0))) {
                        firstBalance1 = jsonObject.getString("available");
                        firstBalance = jsonObject.getString("frozen");
                } else if (jsonObject.getString("coinName").equals(coinArr.get(1))) {
                        lastBalance1 = jsonObject.getString("available");
                        lastBalance = jsonObject.getString("frozen");
                }
            }

            HashMap<String, String> balances = new HashMap<>();

            balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
            balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
            setTradeLog(id, "余额==+"+ JSON.toJSONString(balances), 0,  "05cbc8");
            logger.info("余额："  + "  结果" + JSON.toJSONString(balances));
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
        if (cancelRes != null && cancelRes.getInt("code") == 200) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_HOTCOIN);
    }


    Long getTime(){
//        String s = httpUtil.get("https://api.poloniex.com/timestamp");
//        JSONObject jsonObject = JSONObject.fromObject(s);
//        return jsonObject.getLong("serverTime");
       return System.currentTimeMillis();
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {

        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }


    public static String splicing(Map<String, Object> params)   {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            try {
                httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }


    /**
     * {"status":"ok","data":"1133371409337541"}
     * @param type 类型
     * @param price 价格
     * @param amount 数量
     * @return
     */
    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("00000".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("orderId");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res", "false");
                hashMap.put("orderId", msg);
            }
        }
        return hashMap;
    }

    @Override
    //TradeEnum   1 未成交 2 部分成交 3 完全成交 4 撤单处理中 5 已撤销
    public Map<String, Integer> selectOrderStr(String orderId) {


        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        for (String order : orders) {
            stringIntegerHashMap.put(order, 1);
        }
        return stringIntegerHashMap;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        try {
            cancelTrade = cancelTrade(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("00000".equals(jsonObject.getString("code"))) {
                return "true";
            }else {
                return jsonObject.getString("msg");
            }
        }
        return "false";
    }


    static String gmtNow() {
        return Instant.ofEpochSecond(epochNow()).atZone(ZONE_GMT).format(DT_FORMAT);
    }
    private static long epochNow() {
        return Instant.now().getEpochSecond();
    }
    public static String splicingStr(Map<String, String> params)   {
        StringBuilder httpParams = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            try {
                httpParams.append(key).append("=").append(urlEncode(value)).append("&");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
