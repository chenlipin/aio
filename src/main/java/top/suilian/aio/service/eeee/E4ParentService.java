package top.suilian.aio.service.eeee;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.Source;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@DependsOn("beanContext")
public class E4ParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.eeeedex.com/";
    public String host = "";

    @Override
    public List<getAllOrderPonse> selectOrder() {
        return null;
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
     * {
     *     "status": 200,
     *     "source": "API",
     *     "msg": "OK",
     *     "data": {
     *         "order_id": 119872293,
     *         "user_id": 40221944,
     *         "client_user_id": "",
     *         "symbol": "aabtc_usdt",
     *         "side": "buy",
     *         "price": "1",
     *         "total": "11",
     *         "over_num": "11",
     *         "deal_num": "0",
     *         "deal_total": "0",
     *         "deal_avg_price": "0",
     *         "fee": "0.00000000",
     *         "status": 0
     *     },
     *     "seconds": 1715350180,
     *     "microtime": 1715350180197,
     *     "unique_id": "663e2aa40d1b6659469435",
     *     "host": "127.0.0.1"
     * }
     */
    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String timestamp = System.currentTimeMillis()/1000 + "";
        String typeStr = type == 1 ? "买" : "卖";

        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String uri = "/trade/tradeLimitOrder";
        String httpMethod = "POST";

        Map<String, String> params = new TreeMap<>();
        params.put("api_key", exchange.get("apikey"));
        params.put("nonce",(100000+RandomUtils.nextInt(100000))+"");
        params.put("timestamp",timestamp);

        params.put("symbol", exchange.get("market"));
        params.put("number", num.toString());
        if (type == 1) {
            params.put("side", "buy");
        } else {
            params.put("side", "sell");
        }
        params.put("price", price1.toString());
        String splicing="";
        try {
             splicing = splicing(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String tpass = splicing + exchange.get("tpass");
        String sign = HMAC.MD5(tpass);

        params.put("sign",sign);
        logger.info("挂单参数" + params);
        String orderId="";
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("aa","1");
        try {
            trade = HttpUtil.sendMultipartFormData(baseUrl+uri,params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     *
     * {
     *     "status": 200,
     *     "source": "API",
     *     "msg": "OK",
     *     "data": {
     *         "order_id": 119872293,
     *         "user_id": 40221944,
     *         "symbol": "aabtc_usdt",
     *         "side": "buy",
     *         "price": "1",
     *         "total": "11",
     *         "over_num": "11",
     *         "deal_num": "0",
     *         "deal_total": "0",
     *         "deal_avg_price": "0",
     *         "fee": "0.00000000",
     *         "status": 0,
     *         "created_at": "2024-05-10 22:09:40",
     *         "updated_at": "2024-05-10 22:09:40"
     *     },
     *     "seconds": 1715350647,
     *     "microtime": 1715350647604,
     *     "unique_id": "663e2c778e98e1502653283",
     *     "host": "127.0.0.1"
     * }
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        String timestamp = System.currentTimeMillis()/1000 + "";


        Map<String, String> params = new TreeMap<>();
        params.put("api_key", exchange.get("apikey"));
        params.put("nonce",(100000+RandomUtils.nextInt(100000))+"");
        params.put("timestamp",timestamp);

        params.put("order_id", orderId);
        String splicing="";
        try {
            splicing = splicing(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String tpass = splicing + exchange.get("tpass");
        String sign = HMAC.MD5(tpass);

        params.put("sign",sign);
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("aa","1");
        String  trade="";
        String uri = "/Trade/orderInfo";
        try {
            trade = HttpUtil.sendMultipartFormData(baseUrl+uri,params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return trade;
    }



    public String getBalance() throws UnsupportedEncodingException {

        String timestamp = System.currentTimeMillis()/1000 + "";
        Map<String, String> params = new TreeMap<>();
        params.put("api_key", exchange.get("apikey"));
        params.put("nonce",(100000+RandomUtils.nextInt(100000))+"");
        params.put("timestamp",timestamp);

        String splicing="";
        try {
            splicing = splicing(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String tpass = splicing + exchange.get("tpass");
        String sign = HMAC.MD5(tpass);

        params.put("sign",sign);
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("aa","1");
        String  trade="";
        String uri = "/Assets/getUserAssets";
        try {
            trade = HttpUtil.sendMultipartFormData(baseUrl+uri,params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     *
     * {
     *     "status": 200,
     *     "source": "API",
     *     "msg": "OK",
     *     "data": {
     *         "order_id": "119872293",
     *         "user_id": 40221944,
     *         "symbol": "aabtc_usdt",
     *         "side": "buy",
     *         "price": "1",
     *         "total": "11",
     *         "over_num": "11",
     *         "deal_num": "0",
     *         "deal_total": "0",
     *         "deal_avg_price": "0",
     *         "fee": "0.00000000",
     *         "status": 3,
     *         "created_at": "2024-05-10 22:09:40",
     *         "updated_at": "2024-05-10 22:09:40"
     *     },
     *     "seconds": 1715350996,
     *     "microtime": 1715350996422,
     *     "unique_id": "663e2dd454a751455006810",
     *     "host": "127.0.0.1"
     * }
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
        String timestamp = System.currentTimeMillis()/1000 + "";
        Map<String, String> params = new TreeMap<>();
        params.put("api_key", exchange.get("apikey"));
        params.put("nonce",(100000+RandomUtils.nextInt(100000))+"");
        params.put("timestamp",timestamp);

        params.put("order_id", orderId);
        String splicing="";
        try {
            splicing = splicing(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String tpass = splicing + exchange.get("tpass");
        String sign = HMAC.MD5(tpass);

        params.put("sign",sign);
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("aa","1");
        String  trade="";
        String uri = "/Trade/cancelOrder";
        try {
            trade = HttpUtil.sendMultipartFormData(baseUrl+uri,params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("撤单：" + orderId + "  结果" + trade);
        return trade;

    }


    /**
     * 获取所有尾单
     *
     * @return
     */
    public String getTrade() {

        String uri = "/v1/order/entrust";
        String httpMethod = "GET";
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2+"");
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime()+"");
        params.put("symbol", exchange.get("market"));
        params.put("type", "0");
        params.put("page", "1");
        params.put("count", "500");
        String Signature = "";
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpUtil httpUtil = new HttpUtil();
        String res = httpUtil.get("https://" + host + uri + "?" + httpParams);
        System.out.println(res);
        return res;
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

            String balance1 = getBalance();
            JSONArray jsonArray = JSONObject.fromObject(balance1).getJSONArray("data");
            Double firstBalance = null;
            Double lastBalance = null;
            Double firstBalance1 = null;
            Double lastBalance1 = null;

            List<String> coinArr = Arrays.asList(coins.split("_"));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("name").equals(coinArr.get(0))) {
                    firstBalance = jsonObject.getDouble("num");
                    firstBalance1 = jsonObject.getDouble("lock_num");
                } else if (jsonObject.getString("name").equals(coinArr.get(1))) {
                    lastBalance = jsonObject.getDouble("num");
                    lastBalance1 = jsonObject.getDouble("lock_num");
                    if (lastBalance < 10) {
                        setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                    }
                }
            }
            HashMap<String, String> balances = new HashMap<>();

            balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
            balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
        logger.info("余额"+ com.alibaba.fastjson.JSONObject.toJSONString(balances));
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);

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

    public static String splicing(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
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
            JSONObject jsonObject = judgeRes(submitOrder, "code", "submitTrade");

            if (jsonObject.getString("status") .equals("200")) {
                String string = jsonObject.getJSONObject("data").getString("order_id");
                hashMap.put("res", "true");
                hashMap.put("orderId", string);
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
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("CLOSED".equals(jsonObject.getString("status"))) {
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

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().substring(0,6));
    }
}
