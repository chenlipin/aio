package top.suilian.aio.service.xoxoex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XoxoexParentService extends BaseService {
    static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    static final ZoneId ZONE_GMT = ZoneId.of("Z");
    public String baseUrl = "https://www.xoxoex.co/api";
    public String host = "www.xoxoex.co";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    String accountId = "0";
    public String[] transactionArr = new String[24];

    //设置交易量百分比
    public void setTransactionRatio(){
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
        String time = gmtNow();
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }
                    String uri = "/v1/order/orders/place";
                    String httpMethod = "POST";
                    Map<String, Object> params = new TreeMap<>();
                    params.put("AccessKeyId", exchange.get("apikey"));
                    params.put("SignatureVersion", 2);
                    params.put("SignatureMethod", "HmacSHA256");
                    params.put("Timestamp", time);
                    String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
                    params.put("Signature", Signature);
                    String httpParams = splicing(params);
                    TreeMap<String, Object> map = new TreeMap<>();
                    if (type == 1) {
                        map.put("type", "buy-limit");
                    } else {
                        map.put("type", "sell-limit");
                    }
                    map.put("account-id", getAccountId());
                    map.put("symbol", exchange.get("market"));
                    map.put("amount", amount);
                    map.put("price", price);
                   // map.put("source", "api");
                    String order = splicing(map);


                    logger.info("挂单参数" + order);
                    trade = httpUtil.postes(baseUrl + uri + "?" + httpParams, map);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                String uri = "/v1/order/orders/place";
                String httpMethod = "POST";
                Map<String, Object> params = new TreeMap<>();
                params.put("AccessKeyId", exchange.get("apikey"));
                params.put("SignatureVersion", 2);
                params.put("SignatureMethod", "HmacSHA256");
                params.put("Timestamp", time);
                String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
                params.put("Signature", Signature);
                String httpParams = splicing(params);
                TreeMap<String, Object> map = new TreeMap<>();
                if (type == 1) {
                    map.put("type", "buy-limit");
                } else {
                    map.put("type", "sell-limit");
                }
                map.put("account-id", getAccountId());
                System.out.println(getAccountId());
               // map.put("source", "api");
                map.put("symbol", exchange.get("market"));
                map.put("amount", amount);
                map.put("price", price);
                String order = splicing(map);
                logger.info("挂单参数" + order);

                trade = httpUtil.postes(baseUrl + uri + "?" + httpParams, map);

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

    public String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String time = gmtNow();
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        String uri = "/v1/order/orders/place";
        String httpMethod = "POST";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", time);
        String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
        params.put("Signature", Signature);
        String httpParams = splicing(params);
        TreeMap<String, Object> map = new TreeMap<>();
        if (type == 1) {
            map.put("type", "buy-limit");
        } else {
            map.put("type", "sell-limit");
        }
        map.put("account-id", getAccountId());
        map.put("symbol", exchange.get("market"));
        map.put("amount", amount);
        map.put("price", price);
        // map.put("source", "api");
        String order = splicing(map);


        logger.info("挂单参数" + order);
        trade = httpUtil.postes(baseUrl + uri + "?" + httpParams, map);
        logger.info("挂单结束："+trade);
        return trade;
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        String time = gmtNow();
        String uri = "/v1/order/orders/"+orderId;
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
        params.put("Signature", Signature);
        String httpParams = splicing(params);
        String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);
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
        String time = gmtNow();
        String uri = "/v1/order/orders/"+orderId+"/submitcancel";
        String httpMethod = "post";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", time);
        String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
        params.put("Signature", Signature);
        Map<String, Object> map = new TreeMap<>();
        map.put("order-id", orderId);
        String httpParams = splicing(params);
        HttpUtil httpUtil = new HttpUtil();
        String res = httpUtil.postes(baseUrl+uri+"?"+httpParams,map);
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
            String time = gmtNow();
            String uri = "/v1/account/accounts/"+getAccountId()+"/balance";
            String httpMethod = "GET";
            Map<String, Object> params = new TreeMap<>();
            params.put("AccessKeyId", exchange.get("apikey"));
            params.put("SignatureVersion", 2);
            params.put("SignatureMethod", "HmacSHA256");
            params.put("Timestamp",time);
            String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
            params.put("Signature", Signature);
            String httpParams = splicing(params);
            String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);
            JSONObject tradesJson = JSONObject.fromObject(trades);
            if(tradesJson!=null&&tradesJson.getJSONObject("data").getJSONArray("list")!=null){
                JSONObject data = tradesJson.getJSONObject("data");
                JSONArray wallet = data.getJSONArray("list");

                String firstBalance = null;
                String lastBalance = null;


                for (int i = 0; i < wallet.size(); i++) {
                    JSONObject jsonObject = wallet.getJSONObject(i);
                    if (jsonObject.getString("currency").equals(coinArr.get(0).toLowerCase())&&jsonObject.getString("type").equals("trade")) {
                        firstBalance = jsonObject.getString("balance");
                    } else if (jsonObject.getString("currency").equals(coinArr.get(1).toLowerCase())&&jsonObject.getString("type").equals("trade")) {
                        lastBalance = jsonObject.getString("balance");
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }else {
                logger.info("获取余额失败"+tradesJson);
            }

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
        if (cancelRes != null && cancelRes.getString("data") != null) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_XOXOEX);
    }

    //获取账户id
    public String getAccountId() throws UnsupportedEncodingException {
        if (accountId.equals("0")) {
            String time = gmtNow();
            String uri = "/v1/account/accounts";
            String httpMethod = "GET";
            Map<String, Object> params = new TreeMap<>();
            params.put("AccessKeyId", exchange.get("apikey"));
            params.put("SignatureVersion", 2);
            params.put("SignatureMethod", "HmacSHA256");
            params.put("Timestamp", time);
            String Signature = getSignature(exchange.get("apikey"), exchange.get("tpass"), httpMethod, host, uri, params, time);
            params.put("Signature", Signature);
            String httpParams = splicing(params);
            HttpUtil httpUtil = new HttpUtil();
            String res = httpUtil.get(baseUrl + uri + "?" + httpParams);
            JSONObject tradesObj = judgeRes(res, "status", "getaccountID");
            if (tradesObj.getString("status").equals("ok")) {
                JSONArray data = tradesObj.getJSONArray("data");
                JSONObject ress = (JSONObject) data.get(0);
                accountId = ress.getString("id");
            } else {
                logger.info("accountId获取失败");
            }
        } else {
            return accountId;
        }
        return accountId;

    }


    /**
     * 交易规则获取
     */
    public void setPrecision() throws UnsupportedEncodingException {

        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }


    public String getSignature(String appKey, String appSecretKey, String method, String host,
                               String uri, Map<String, Object> params, String time) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(method.toUpperCase()).append('\n')
                .append(host.toLowerCase()).append('\n')
                .append(uri).append('\n');
        params.put("AccessKeyId", appKey);
        params.put("SignatureVersion", "2");
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", time);
        // build signature:
        SortedMap<String, Object> map = new TreeMap<String, Object>(params);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            sb.append(key).append('=').append(urlEncode(value)).append('&');
        }
        // remove last '&':
        sb.deleteCharAt(sb.length() - 1);
        // sign:
        Mac hmacSha256 = null;
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secKey =
                    new SecretKeySpec(appSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key: " + e.getMessage());
        }
        String payload = sb.toString();
        byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String actualSign = Base64.getEncoder().encodeToString(hash);
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

    //获得时间
    long epochNow() {
        return Instant.now().getEpochSecond();
    }

    String gmtNow() {
        return Instant.ofEpochSecond(epochNow()).atZone(ZONE_GMT).format(DT_FORMAT);
    }

}