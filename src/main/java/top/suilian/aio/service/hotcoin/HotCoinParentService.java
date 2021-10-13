package top.suilian.aio.service.hotcoin;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.dao.ApitradeLogMapper;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.loex.LoexParentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HotCoinParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.hotcoinfin.com";
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


                    String uri = "/v1/order/place";
                    String httpMethod = "GET";
                    Map<String, Object> params = new TreeMap<>();
                    params.put("AccessKeyId", exchange.get("apikey"));
                    params.put("SignatureVersion", 2);
                    params.put("SignatureMethod", "HmacSHA256");
                    params.put("Timestamp", new Date().getTime());
                    params.put("symbol", exchange.get("market"));
                    if (type == 1) {
                        params.put("type", "buy");
                    } else {
                        params.put("type", "sell");
                    }
                    params.put("tradeAmount", num);
                    params.put("tradePrice", price1);
                    String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
                    params.put("Signature", Signature);
                    String httpParams = splicing(params);

                    logger.info("挂单参数" + httpParams);


                    trade = httpUtil.get("https://" + host + uri + "?" + httpParams);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                String uri = "/v1/order/place";
                String httpMethod = "GET";
                Map<String, Object> params = new TreeMap<>();
                params.put("AccessKeyId", exchange.get("apiley"));
                params.put("SignatureVersion", 2);
                params.put("SignatureMethod", "HmacSHA256");
                params.put("Timestamp", new Date().getTime());
                params.put("symbol", exchange.get("market"));
                if (type == 1) {
                    params.put("type", "buy");
                } else {
                    params.put("type", "sell");
                }
                params.put("tradeAmount", num);
                params.put("tradePrice", price1);
                String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
                params.put("Signature", Signature);
                String httpParams = splicing(params);

                logger.info("挂单参数" + httpParams);
                trade = httpUtil.get("https://" + host + uri + "?" + httpParams);


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
        String typeStr = type == 0 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.valueOf(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(exchange.get("amountPrecision").toString()));
        String uri = "/v1/order/place";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        if (type == 1) {
            params.put("type", "buy");
        } else {
            params.put("type", "sell");
        }
        params.put("tradeAmount", num);
        params.put("tradePrice", price1);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        trade = httpUtil.get("https://" + host + uri + "?" + httpParams);
        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }

    //获取委托单
    public String getTradeOrders(int type) {
        String trade = null;
        String uri = "/v1/order/entrust";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        params.put("type", type);
        params.put("count", 100);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String trades = httpUtil.get("https://" + host + uri + "?" + httpParams);
        return trades;

    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {


        String uri = "/v1/order/detailById";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("id", orderId);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = splicing(params);
        String trades = httpUtil.get("https://" + host + uri + "?" + httpParams);
        logger.info("查询订单："+orderId+"  结果"+trades);
        return trades;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        String uri = "/v1/order/cancel";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("id", orderId);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = splicing(params);


        HttpUtil httpUtil = new HttpUtil();
        String res = httpUtil.get("https://" + host + uri + "?" + httpParams);

        return res;
    }


    /**
     * 获取所有尾单
     *
     * @return
     */
    public String getTrade() {

        String uri = "/v1/order/entrust";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        params.put("type", 0);
        params.put("page", 1);
        params.put("count", 500);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
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
        if (balance == null || overdue) {
            List<String> coinArr = Arrays.asList(coins.split("_"));

            String uri = "/v1/balance";
            String httpMethod = "GET";
            Map<String, Object> params = new TreeMap<>();
            params.put("AccessKeyId", exchange.get("apikey"));
            params.put("SignatureVersion", 2);
            params.put("SignatureMethod", "HmacSHA256");
            params.put("Timestamp", new Date().getTime());
            String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
            params.put("Signature", Signature);
            String httpParams = splicing(params);
            String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);
            JSONObject tradesJson = JSONObject.fromObject(trades);
            JSONObject data = tradesJson.getJSONObject("data");
            JSONArray wallet = data.getJSONArray("wallet");

            String firstBalance = null;
            String lastBalance = null;


            for (int i = 0; i < wallet.size(); i++) {
                JSONObject jsonObject = wallet.getJSONObject(i);
                if (jsonObject.getString("shortName").equals(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("total");
                } else if (jsonObject.getString("shortName").equals(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("total");
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance);
            balances.put(coinArr.get(1), lastBalance);
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
    public String submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("ID");
            }
        }
        return orderId;
    }

    @Override
    //TradeEnum   1 未成交 2 部分成交 3 完全成交 4 撤单处理中 5 已撤销
    public Map<String, Integer> selectOrderStr(String orderId) {
        String trade = getTrade();
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(trade);
        com.alibaba.fastjson.JSONArray entrutsHis = jsonObject1.getJSONObject("data").getJSONArray("entrutsHis");
        HashMap<String, Integer> map = new HashMap<>();
        for (Object entrutsHi : entrutsHis) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(entrutsHi.toString());
            map.put(jsonObject.getString("id"), jsonObject.getInteger("statusCode"));
        }

        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> hashMap = new HashMap<>();

        for (String order : orders) {
            Integer integer = map.get(order);
            if (integer != null) {
                hashMap.put(order, getTradeEnum(integer).getStatus());
            } else {
                String result = "";
                try {
                    result = selectOrder(order);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(result);
                if ("200".equals(jsonObject.getString("code"))) {
                    Integer statusCode = jsonObject.getJSONObject("data").getInteger("statusCode");
                    hashMap.put(order, getTradeEnum(statusCode).getStatus());
                }

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
