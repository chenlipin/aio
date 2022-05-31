package top.suilian.aio.service.bitmart;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@DependsOn("beanContext")
public class BitMartParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api-cloud.bitmart.com";
    public String host = "api.hotcoinfin.com";
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
     * 下单
     *
     * @param type
     * @param price
     * @param amount
     * @return
     * @throws UnsupportedEncodingException
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }
                    String uri = "/spot/v1/submit_order";
                    Map<String, String> params = new TreeMap<>();
                    params.put("symbol", exchange.get("market"));
                    params.put("side", type == 1 ? "buy" : "sell");
                    params.put("type", "limit");
                    params.put("size", num + "");
                    params.put("price", price1 + "");
                    String sing = timestamp + "#" + exchange.get("memo") + "#" + JSONObject.toJSONString(params);
                    String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
                    HashMap<String, String> headMap = new HashMap<>();
                    headMap.put("X-BM-TIMESTAMP", timestamp);
                    headMap.put("X-BM-KEY", exchange.get("tpass"));
                    headMap.put("X-BM-SIGN", sings);
                    System.out.println("headMap:" + JSONObject.toJSONString(headMap));
                    try {
                        trade = httpUtil.postByPackcoin(baseUrl + uri, params, headMap);
                        net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
                        if(1000!=jsonObjectss.getInt("code")){
                            setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                String uri = "/spot/v1/submit_order";
                Map<String, String> params = new TreeMap<>();
                params.put("symbol", exchange.get("market"));
                params.put("side", type == 1 ? "buy" : "sell");
                params.put("type", "limit");
                params.put("size", num + "");
                params.put("price", price1 + "");
                String sing = timestamp + "#" + exchange.get("memo") + "#" + JSONObject.toJSONString(params);
                String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
                HashMap<String, String> headMap = new HashMap<>();
                headMap.put("X-BM-TIMESTAMP", timestamp);
                headMap.put("X-BM-KEY", exchange.get("tpass"));
                headMap.put("X-BM-SIGN", sings);
                System.out.println("headMap:" + JSONObject.toJSONString(headMap));
                try {
                    trade = httpUtil.postByPackcoin(baseUrl + uri, params, headMap);
                    net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
                    if(1000!=jsonObjectss.getInt("code")){
                        setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
            }

        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }

        valid = 1;
        return trade;

    }

    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";
        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String uri = "/spot/v1/submit_order";
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", exchange.get("market"));
        params.put("side", type == 1 ? "buy" : "sell");
        params.put("type", "limit");
        params.put("size", num + "");
        params.put("price", price1 + "");
        String sing = timestamp + "#" + exchange.get("memo") + "#" + JSONObject.toJSONString(params);
        String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
        HashMap<String, String> headMap = new HashMap<>();
        headMap.put("X-BM-TIMESTAMP", timestamp);
        headMap.put("X-BM-KEY", exchange.get("tpass"));
        headMap.put("X-BM-SIGN", sings);
        try {
            trade = httpUtil.postByPackcoin(baseUrl + uri, params, headMap);
            net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
            if(1000!=jsonObjectss.getInt("code")){
                setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }

//    //获取委托单
//    public String getTradeOrders(int type) {
//        String trade = null;
//        String uri = "/v1/order/entrust";
//        String httpMethod = "GET";
//        Map<String, Object> params = new TreeMap<>();
//        params.put("AccessKeyId", exchange.get("apikey"));
//        params.put("SignatureVersion", 2);
//        params.put("SignatureMethod", "HmacSHA256");
//        params.put("Timestamp", new Date().getTime());
//        params.put("symbol", exchange.get("market"));
//        params.put("type", type);
//        params.put("count", 100);
//        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
//        params.put("Signature", Signature);
//        String httpParams = null;
//        try {
//            httpParams = splicing(params);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        String trades = httpUtil.get("https://" + host + uri + "?" + httpParams);
//        return trades;
//
//    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String selectOrder(String orderId) {
        String trade = null;
        long timestamp = new Date().getTime();
        String uri = "/spot/v1/order_detail";
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", exchange.get("market"));
        params.put("order_id", orderId);
        String queryStr = "order_id=" + orderId + "&symbol=" + exchange.get("market");
        String sing = timestamp + "#" + exchange.get("memo") + "#" + queryStr;
        String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
        HashMap<String, String> headMap = new HashMap<>();
        headMap.put("X-BM-TIMESTAMP", timestamp + "");
        headMap.put("X-BM-KEY", exchange.get("tpass"));
        headMap.put("X-BM-SIGN", sings);
        System.out.println("headMap:" + JSONObject.toJSONString(headMap));
        trade = httpUtil.getAddHead(baseUrl + uri + "?" + queryStr, headMap);
        logger.info("查询订单：" + orderId + "  结果" + trade);
        net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
        if(1000!=jsonObjectss.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
        }
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
        long timestamp = new Date().getTime();
        String uri = "/spot/v2/cancel_order";
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", exchange.get("market"));
        params.put("order_id", orderId);
        String sing = timestamp + "#" + exchange.get("memo") + "#" + JSONObject.toJSONString(params);
        String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
        HashMap<String, String> headMap = new HashMap<>();
        headMap.put("X-BM-TIMESTAMP", timestamp + "");
        headMap.put("X-BM-KEY", exchange.get("tpass"));
        headMap.put("X-BM-SIGN", sings);
        String trade = null;
        try {
            trade = httpUtil.postByPackcoin(baseUrl + uri, params, headMap);
            net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
            if(1000!=jsonObjectss.getInt("code")){
                setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
            List<String> coinArr = Arrays.asList(coins.split("_"));
            String getbalans = getbalans();
            logger.info("获取余额" + getbalans);
            JSONArray jsonArray = JSONObject.parseObject(getbalans).getJSONObject("data").getJSONArray("wallet");

            String firstBalance = null;
            String lastBalance = null;


            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = JSONObject.parseObject(jsonArray.get(i).toString());
                if (jsonObject.getString("id").equals(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("available")+"_"+jsonObject.getString("frozen");
                } else if (jsonObject.getString("id").equals(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("available")+"_"+jsonObject.getString("frozen");;
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance);
            balances.put(coinArr.get(1), lastBalance);
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
        }
    }


    public String getbalans() {
        String trade = null;
        String uri = "/spot/v1/wallet";
        long timestamp = new Date().getTime();
        String sing = timestamp + "#" + exchange.get("memo");
        String sings = HMAC.sha256_HMAC(sing, exchange.get("apikey"));
        HashMap<String, String> headMap = new HashMap<>();
        headMap.put("X-BM-TIMESTAMP", timestamp + "");
        headMap.put("X-BM-KEY", exchange.get("tpass"));
        headMap.put("X-BM-SIGN", sings);
        System.out.println("headMap:" + JSONObject.toJSONString(headMap));
        trade = httpUtil.getAddHead(baseUrl + uri, headMap);
        net.sf.json.JSONObject jsonObjectss = net.sf.json.JSONObject.fromObject(trade);
        if(1000!=jsonObjectss.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObjectss.getString("message"));
        }
       logger.info("getbalans:"+trade);
        return trade;
    }

    /**
     * 存储撤单信息
     *
     * @param cancelRes
     * @param res
     * @param orderId
     * @param type
     */
    public void setCancelOrder(net.sf.json.JSONObject cancelRes, String res, String orderId, Integer type) {
        int cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
        if (cancelRes != null && (cancelRes.getInt("code") == 1000||cancelRes.getInt("code") == 50030)) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BITMART);
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

    public static String splicing(Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
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
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("1000".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("order_id");
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
        HashMap<String, Integer> hashMap = new HashMap<>();

        for (String order : orders) {
                String result = "";
                result = selectOrder(order);
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(result);
                if ("1000".equals(jsonObject.getString("code"))) {
                    Integer statusCode = jsonObject.getJSONObject("data").getInteger("status");
                    hashMap.put(order, getTradeEnum(statusCode).getStatus());
                }


        }
        return hashMap;
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
            if ("1000".equals(jsonObject.getString("code")) || "50030".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }


    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 4:
                return TradeEnum.NOTRADE;

            case 5:
                return TradeEnum.TRADEING;

            case 6:
                return TradeEnum.NOTRADED;

            case 8:
                return TradeEnum.CANCEL;

            case 7:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }
}
