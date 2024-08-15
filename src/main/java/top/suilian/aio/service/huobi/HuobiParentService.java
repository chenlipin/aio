package top.suilian.aio.service.huobi;

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
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@DependsOn("beanContext")
public class HuobiParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.poloniex.com";
    public String host = "";

    @Override
    public List<getAllOrderPonse> selectOrder() {
        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();
        String trade = getTrade();
        com.alibaba.fastjson.JSONArray array = com.alibaba.fastjson.JSONArray.parseArray(trade);

        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            com.alibaba.fastjson.JSONObject jsonObject = array.getJSONObject(i);

            getAllOrderPonse.setOrderId(jsonObject.getString("id"));
            getAllOrderPonse.setCreatedAt(DateUtils.convertTimestampToString(jsonObject.getLong("createTime")));
            getAllOrderPonse.setPrice(jsonObject.getString("side")+"-"+jsonObject.getString("price"));
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(jsonObject.getString("quantity"));
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
     {
     "id" : "315110412968390656",
     "clientOrderId" : "SSS1716094576774"
     }
     *
     * @param type
     * @param price
     * @param amount
     * @return
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + (type==1?"买":"卖") + "，price(价格)：" + price + "，amount(数量)：" + amount);
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String timestamp =(getTime())+"";


        HashMap<String, String> head = new HashMap<String, String>();
        head.put("key",exchange.get("apikey"));
        head.put("signMethod","HmacSHA256");
        head.put("signVersion","2");
        head.put("signTimestamp",timestamp);
        head.put("recvWindow","5000");

        Map<String, String> params = new TreeMap<String, String>();
        params.put("symbol", exchange.get("market"));
        params.put("type", "LIMIT");
        if (type == 1) {
            params.put("side", "BUY");
        } else {
            params.put("side", "SELL");
        }
        params.put("quantity", num+"");
        params.put("price", price1+"");
        params.put("timeInForce", "GTC");
        params.put("clientOrderId", "SSS" + timestamp);
        params.put("stpMode","None");

        String uri="/orders";
        String strSign = "POST" + "\n" +
                uri + "\n" +
                "requestBody=" + com.alibaba.fastjson.JSONObject.toJSONString(params) +
                "&" + "signTimestamp" + "=" + timestamp;

        String sign = HMAC.generateSignature( exchange.get("tpass"),strSign);
        head.put("signature",sign);

        String trade = null;
        try {
            trade = HttpUtil.doPostMart(baseUrl + uri, com.alibaba.fastjson.JSONObject.toJSONString(params),head);
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if(StringUtils.isEmpty(jsonObject.getString("id"))){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trade;
    }


    public String getBalance() throws UnsupportedEncodingException {

        String timestamp =(getTime())+"";
        HashMap<String, String> head = new HashMap<String, String>();
        head.put("key",exchange.get("apikey"));
        head.put("signMethod","HmacSHA256");
        head.put("signVersion","2");
        head.put("signTimestamp",timestamp);
        head.put("recvWindow","5000");
        String uri="/accounts/balances";

        String strSign = "GET" + "\n" +
                uri + "\n" +
                "signTimestamp" + "=" + timestamp;
        String sign = HMAC.generateSignature( exchange.get("tpass"),strSign);
        head.put("signature",sign);
        String market = HttpUtil.getAddHead(baseUrl + uri, head);
        return market;
    }



    /**
     * 查询订单详情
     {
     "id" : "315110412968390656",
     "clientOrderId" : "SSS1716094576774",
     "symbol" : "REEE_USDT",
     "state" : "NEW",
     "accountType" : "SPOT",
     "side" : "BUY",
     "type" : "LIMIT",
     "timeInForce" : "GTC",
     "quantity" : "20000",
     "price" : "0.0001",
     "avgPrice" : "0",
     "amount" : "0",
     "filledQuantity" : "0",
     "filledAmount" : "0",
     "createTime" : 1716094576921,
     "updateTime" : 1716094576921,
     "orderSource" : "API",
     "loan" : false,
     "cancelReason" : 0
     }
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        String timestamp =(getTime())+"";
        HashMap<String, String> head = new HashMap<String, String>();
        head.put("key",exchange.get("apikey"));
        head.put("signMethod","HmacSHA256");
        head.put("signVersion","2");
        head.put("signTimestamp",timestamp);
        head.put("recvWindow","5000");
        String uri="/orders/"+orderId;

        String strSign = "GET" + "\n" +
                uri + "\n" +
                "signTimestamp" + "=" + timestamp;
        String sign = HMAC.generateSignature( exchange.get("tpass"),strSign);
        head.put("signature",sign);
        String market = HttpUtil.getAddHead(baseUrl + uri, head);
        return market;
    }





    /**
     * 撤单
     *{
     *   "orderId" : "315110412968390656",
     *   "clientOrderId" : "SSS1716094576774",
     *   "state" : "PENDING_CANCEL",
     *   "code" : 200,
     *   "message" : ""
     * }
     * @param orderId
     * @return
     * @throws
     */
    public String cancelTrade(String orderId)  {
        String timestamp =(getTime())+"";
        HashMap<String, String> head = new HashMap<String, String>();
        head.put("key",exchange.get("apikey"));
        head.put("signMethod","HmacSHA256");
        head.put("signVersion","2");
        head.put("signTimestamp",timestamp);
        head.put("recvWindow","5000");
        String uri="/orders/"+orderId;
        String strSign = "DELETE" + "\n" +
                uri + "\n" +
                "signTimestamp" + "=" + timestamp;
        String sign = HMAC.generateSignature( exchange.get("tpass"),strSign);
        head.put("signature",sign);
        String trade = HttpUtil.doDeletes(baseUrl + uri, head);
        logger.info("撤单：" + orderId + "  结果" + trade);
        return trade;
    }


    /**
     * 获取所有尾单
     *
     * [ {
     *   "id" : "314953143895048192",
     *   "clientOrderId" : "",
     *   "symbol" : "BEBE_USDT",
     *   "state" : "NEW",
     *   "accountType" : "SPOT",
     *   "side" : "SELL",
     *   "type" : "LIMIT_MAKER",
     *   "timeInForce" : "GTC",
     *   "quantity" : "20",
     *   "price" : "0.3",
     *   "avgPrice" : "0",
     *   "amount" : "0",
     *   "filledQuantity" : "0",
     *   "filledAmount" : "0",
     *   "createTime" : 1716057081051,
     *   "updateTime" : 1716057081051,
     *   "orderSource" : "API",
     *   "loan" : false
     * } ]
     * @return
     */
    public String getTrade() {

        String timestamp =(getTime()+500)+"";
        HashMap<String, String> head = new HashMap<String, String>();
        head.put("key",exchange.get("apikey"));
        head.put("signMethod","HmacSHA256");
        head.put("signVersion","2");
        head.put("signTimestamp",timestamp);
        head.put("recvWindow","5000");
        String uri="/orders";

        String strSign = "GET" + "\n" +
                uri + "\n" +
                "signTimestamp" + "=" + timestamp  ;
        String sign = HMAC.generateSignature( exchange.get("tpass"),strSign);
        head.put("signature",sign);
        String market = HttpUtil.getAddHead(baseUrl + uri, head);
        return market;
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
            JSONArray jsonArray1 = JSONArray.fromObject(balance1);

            JSONArray jsonArray =JSONObject.fromObject(jsonArray1.get(0)).getJSONArray("balances");
            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;

            List<String> coinArr = Arrays.asList(coins.split("_"));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("currency").equals(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("available");
                    firstBalance1 = jsonObject.getString("hold");
                } else if (jsonObject.getString("currency").equals(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("available");
                    lastBalance1 = jsonObject.getString("hold");
                    if (Double.parseDouble(lastBalance) < 10) {
                        setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                    }
                }
            }

            HashMap<String, String> balances = new HashMap<>();

            balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
            balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
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


    public static String getSignature(String apiSecret, String host, String uri, String httpMethod, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(httpMethod.toUpperCase()).append('\n')
                .append(host.toLowerCase()).append('\n')
                .append(uri).append('\n');
        SortedMap<String, Object> map = new TreeMap<>(params);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            sb.append(key).append('=').append(urlEncode(value)).append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        Mac hmacSha256;
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secKey =
                    new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secKey);
        } catch (Exception e) {
            return null;
        }
        String payload = sb.toString();
        byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        //需要对签名进行base64的编码
        String actualSign = Base64.getEncoder().encodeToString(hash);
        actualSign = actualSign.replace("\n", "");
        return actualSign;
    }


    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UTF-8 encoding not supported!");
        }
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


    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if (StringUtils.isNotEmpty(jsonObject.getString("id"))) {
                orderId = jsonObject.getString("id");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("message");
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
            if ("200".equals(jsonObject.getString("code"))) {
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
