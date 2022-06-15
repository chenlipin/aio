package top.suilian.aio.service.iex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class IexParentService extends BaseService implements RobotAction {
    private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D",
            "E", "F"};
    public String baseUrl = "https://api.iex.asia/spot-api-robot";
    public RunHooRandomDepth runHooRandomDepth = BeanContext.getBean(RunHooRandomDepth.class);
    public Map<String, Object> precision = new HashMap<String, Object>();
    static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    static final ZoneId ZONE_GMT = ZoneId.of("Z");
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    String accountId = "0";

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
     *{"code":200,"message":"操作成功","data":{"orderId":"E18331601548564023386","memberId":"1100640","memberName":"","email":"","mobilePhone":"","symbol":"STD/USDT","side":0,"priceType":1,"orderQty":1.2,"price":2.0,"transactTime":"1653988464846","status":1,"remark":"","orderType":0,"tradedAmount":0.0,"actualTradedAmount":0.0,"fee":0.0,"turnover":0.0,"completedTime":"0","canceledTime":"0","coinSymbol":"STD","baseSymbol":"USDT","server":"2","details":[],"tablePre":"robot"},"count":null,"responseString":"200~SUCCESS","url":null,"cid":null}
     *
     * @param type
     * @param price
     * @param amount
     * @return
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) {
        String time = System.currentTimeMillis() + "";
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        JSONObject params = new JSONObject();
        params.put("apiKey", exchange.get("apikey"));
        params.put("priceType", "1");
        params.put("symbol", exchange.get("market"));
        params.put("side", type == 1 ? "0" : "1");
        params.put("price", price1 + "");
        params.put("orderQty", num + "");
        params.put("ts", time);
        String content = "ts=" + time + ",apiKey=" + exchange.get("apikey") + ",apiSecret=" + exchange.get("tpass");
        String sign = null;
        try {
            sign = md5Digest(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        params.put("sign", sign);
        logger.info("挂单参数" + params);
        try {
            trade = HttpUtil.sendPost("https://api.iex.asia/spot-api-robot/v1/order/place", params);
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (200 != object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("message"));
        }
        logger.info("挂单结束：" + trade);
        return trade;
    }

    /**
     * 查询订单详情
     * <p>
     * {"result":true,"data":[{"symbol":"trx_usdt","amount":100,"create_time":1653745632172,"price":0.08203,"avg_price":0.081887,"type":"buy","order_id":"cbaa1888-d7d5-4b50-88cb-19851cd329d2","deal_amount":100,"status":2}],"error_code":0,"ts":1653746264068}
     */


    public String selectOrder(String orderId) {
        String time = System.currentTimeMillis() + "";
        String trade = null;
        String content = "ts=" + time + ",apiKey=" + exchange.get("apikey") + ",apiSecret=" + exchange.get("tpass");
        String sign = null;
        try {
            sign = md5Digest(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            trade = httpUtil.get("https://api.iex.asia/spot-api-robot/v1/order/query/detail/"+orderId+"/"+exchange.get("apikey")+"/"+sign+"/"+time+"?symbol="+exchange.get("market"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;

    }


    protected String getTradeOrders() {
        Map<String, String> parms = new TreeMap<>();
        parms.put("client_id", exchange.get("apikey"));
        String timespace = getTimespace();
        parms.put("ts", timespace);
        parms.put("nonce", timespace);
        String signs = HMAC.splice(parms);
        String sign = HMAC.sha256_HMAC(signs, exchange.get("tpass"));
        String parm = signs + "&sign=" + sign + "&symbol=" + exchange.get("market");
        String res = httpUtil.get(baseUrl + "/open/v1/orders/last?" + parm);
        logger.info("查询自己的委托列表" + res);
        return res;

    }

    public String getDepth() {
        return httpUtil.get("https://www.iex.asia/prod-api/spotMarket/v1/market/spot/depth/STD-USDT/1");
    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        String time = System.currentTimeMillis() + "";
        String trade = null;
        String content = "ts=" + time + ",apiKey=" + exchange.get("apikey") + ",apiSecret=" + exchange.get("tpass");
        String sign = null;
        try {
            sign = md5Digest(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            trade = httpUtil.get(baseUrl+"/v1/order/query/wallet?apiKey="+exchange.get("apikey")+"&sign="+sign+"&ts="+time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;
    }

    /**
     * 撤单
     * <p>
     * {"code":0,"msg":"success","data":null}
     */
    public String cancelTrade(String orderId) {
        String time = System.currentTimeMillis() + "";
        String trade = null;
        String content = "ts=" + time + ",apiKey=" + exchange.get("apikey") + ",apiSecret=" + exchange.get("tpass");
        String sign = null;
        try {
            sign = md5Digest(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            trade = httpUtil.get(baseUrl+"/v1/order/"+orderId+"/submitcancel?apiKey="+exchange.get("apikey")+"&sign="+sign+"&ts="+time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;

    }

    /**
     * 获取余额
     */

    public void setBalanceRedis() {

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
        if (balance == null ) {
            List<String> coinArr = Arrays.asList(coins.split("_"));

            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null && obj.getInt("code") == 200) {
                JSONArray data = obj.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    if (jsonObject.getString("coinId").equals(coinArr.get(0))){
                        firstBalance = jsonObject.getString("balance");
                        firstBalance1 = jsonObject.getString("frozenBalance");
                    }
                    if (jsonObject.getString("coinId").equals(coinArr.get(1))){
                        lastBalance = jsonObject.getString("balance");
                        lastBalance1 = jsonObject.getString("frozenBalance");
                    }
                }

                if (lastBalance != null) {
                    if (Double.parseDouble(lastBalance) < 10) {
                        setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
                balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
                logger.info("获取余额" + com.alibaba.fastjson.JSONObject.toJSONString(balances));
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
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
        if (cancelRes != null && cancelRes.getInt("code") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }

    public String getTimespace() {
        String timespace = null;
        String rs = httpUtil.get(baseUrl + "/open/v1/timestamp");
        JSONObject jsonObject = JSONObject.fromObject(rs);
        if (jsonObject != null && jsonObject.getInt("code") == 0) {
            timespace = jsonObject.getString("data");
        }
        return timespace;
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitTrade(type == 1 ? 1 : 2, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("orderId");
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
    public Map<String, Integer> selectOrderStr(String orderId) {
        String trade = getTrade();
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(trade);
        com.alibaba.fastjson.JSONArray entrutsHis = jsonObject1.getJSONArray("data");
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < entrutsHis.size(); i++) {
            com.alibaba.fastjson.JSONObject jsonObject = entrutsHis.getJSONObject(i);
            map.put(jsonObject.getString("order_id") + "_" + jsonObject.getString("trade_no"), 0);
        }
        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> hashMap = new HashMap<>();

        for (String order : orders) {
            Integer integer = map.get(order);
            if (integer != null) {
                hashMap.put(order, 0);
            } else {
                String result = "";
                try {
                    result = selectOrder(order);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(result);
                if ("0".equals(jsonObject.getString("code"))) {
                    Integer statusCode = jsonObject.getJSONObject("data").getInteger("status");
                    hashMap.put(order, getTradeEnum(statusCode).getStatus());
                }
            }
        }
        return hashMap;
    }

    //2 委托中，3部分成交，4全部成交，5部分成交后撤消，6全部撤消
    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 2:
                return TradeEnum.NOTRADE;

            case 3:
                return TradeEnum.TRADEING;

            case 4:
                return TradeEnum.NOTRADED;

            case 5:
                return TradeEnum.CANCEL;

            case 6:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }

    /**
     * 获取委托中列表
     *
     * @return
     */
    private String getTrade() {
        Map<String, Object> parms = new TreeMap<>();
        parms.put("client_id", exchange.get("apikey"));
        String timespace = getTimespace();
        parms.put("ts", timespace);
        parms.put("nonce", timespace);
        String signs = null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = HMAC.sha256_HMAC(signs, exchange.get("tpass"));
        String parm = signs + "&sign=" + sign + "&symbol=" + exchange.get("market");
        String res = httpUtil.get(baseUrl + "/open/v1/orders/last?" + parm);
        logger.info("获取委托中列表" + res);
        return res;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        cancelTrade = cancelTrade(orderId);
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("error_code"))) {
                return "true";
            }
        }
        return "false";
    }

    public boolean setPrecision() {
        //为client_id, ts, nonce, sign
        boolean falg = false;
        Map<String, String> parms = new TreeMap<>();
        parms.put("client_id", exchange.get("apikey"));
        String timespace = getTimespace();
        parms.put("ts", timespace);
        parms.put("nonce", timespace);
        String signs = null;
        try {
            signs = HMAC.splicingStr(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = HMAC.sha256_HMAC(signs, exchange.get("tpass"));
        String parm = signs + "&sign=" + sign;
        String rt = httpUtil.get(baseUrl + "/open/v1/tickers?" + parm);

        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");

        if (!rt.equals("") && rtObj != null && rtObj.getInt("code") == 0) {
            JSONArray jsonArray = rtObj.getJSONArray("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                if (jsonArray.getJSONObject(i).getString("symbol").equals(exchange.get("market"))) {
                    precision.put("amountPrecision", jsonArray.getJSONObject(i).getString("qty_num"));
                    precision.put("pricePrecision", jsonArray.getJSONObject(i).getString("amt_num"));
                    precision.put("minTradeLimit", exchange.get("minTradeLimit"));
                    falg = true;
                }
            }

        } else {
            setTradeLog(id, "精度接口异常：" + rt, 0, "000000");
        }
        return falg;
    }

    long epochNow() {
        return Instant.now().getEpochSecond();
    }

    String gmtNow() {
        return Instant.ofEpochSecond(epochNow()).atZone(ZONE_GMT).format(DT_FORMAT);
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

    public static String splicing(Map<String, String> params) {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            try {
                httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }

    public static String MD5(String s) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest(s.getBytes("UTF-8"));
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换字节数组为16进制字串
     *
     * @param b 字节数组
     * @return 16进制字串
     */

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
     * MD5 摘要计算(byte[]).
     *
     * @param src byte[]
     * @return byte[] 16 bit digest
     * @throws Exception
     */
    public static byte[] md5Digest(byte[] src) throws Exception {
        MessageDigest alg = MessageDigest.getInstance("MD5"); // MD5
        return alg.digest(src);
    }

    /**
     * MD5 摘要计算(String).
     *
     * @param src String
     * @return String
     * @throws Exception
     */
    public static String md5Digest(String src) throws Exception {
        return byteArrayToHexString(md5Digest(src.getBytes("UTF-8")));
    }

}

