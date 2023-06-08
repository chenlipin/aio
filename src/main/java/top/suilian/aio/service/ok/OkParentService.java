package top.suilian.aio.service.ok;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
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
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OkParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://www.okx.com";
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

//{"code":"0","data":[{"clOrdId":"SUIlian1686234726772","ordId":"587046240282865670","sCode":"0","sMsg":"Order placed","tag":""}],"msg":""}
    public String submitTrade(int type, BigDecimal price, BigDecimal amount)   {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));

        Map<String, String> param = new TreeMap<>();
        param.put("instId", exchange.get("market"));
        param.put("tdMode", "cash");
        param.put("clOrdId", "SUIlian" + String.valueOf(new Date().getTime()));
        param.put("side", type == 1 ? "buy" : "sell");
        param.put("ordType", "limit");
        param.put("sz", String.valueOf(amount));
        param.put("px", String.valueOf(price));

        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "POST";
        String requestPath = "/api/v5/trade/order";
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + body);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        try {
            trade = httpUtil.postByPackcoin(baseUrl +requestPath, param, head);
            JSONObject object = JSONObject.fromObject(trade);
            if (0 != object.getInt("code")) {
                setWarmLog(id, 3, "API接口错误", object.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId:" + id + "挂单成功结束：" + trade);
        return trade;
    }


    /**
     * {"code":"0","data":[{"accFillSz":"0","algoClOrdId":"","algoId":"","avgPx":"","cTime":"1686234730475","cancelSource":"","cancelSourceReason":"","category":"normal","ccy":"","clOrdId":"SUIlian1686234726772","fee":"0","feeCcy":"GOAL","fillPx":"","fillSz":"0","fillTime":"","instId":"GOAL-USDT","instType":"SPOT","lever":"","ordId":"587046240282865670","ordType":"limit","pnl":"0","posSide":"net","px":"0.101","quickMgnType":"","rebate":"0","rebateCcy":"USDT","reduceOnly":"false","side":"buy","slOrdPx":"","slTriggerPx":"","slTriggerPxType":"","source":"","state":"live","stpId":"","stpMode":"","sz":"50","tag":"","tdMode":"cash","tgtCcy":"","tpOrdPx":"","tpTriggerPx":"","tpTriggerPxType":"","tradeId":"","uTime":"1686234730475"}],"msg":""}
     * @param orderId
     * @return
     */
    public String selectOrder(String orderId) {
        String trade = null;
        String market = exchange.get("market");
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/v5/trade/order?instId="+market+"&ordId=" + orderId;
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        trade = httpUtil.getAddHead(baseUrl + requestPath, head);
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return trade;

    }


    public String getDepth() {
        return httpUtil.get(baseUrl+"/api/v5/market/books?sz=5&instId=" + exchange.get("market"));
    }


    /**
     * 获取余额
     */

    protected String getBalance() {
        Map<String, String> head = new HashMap<String, String>();
        String market = exchange.get("market");
        String robotArgs = robotArgsService.findOne(id, "market").getRemark();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/v5/account/balance?ccy="+robotArgs;
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + requestPath,head);
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return trade;
    }

    /**
     * 撤单
     * <p>
     * {"code":"0","data":[{"clOrdId":"SUIlian1686234726772","ordId":"587046240282865670","sCode":"0","sMsg":""}],"msg":""}
     */
    public String cancelTrade(String orderId) {
        Map<String, String> param = new TreeMap<>();
        param.put("ordId", orderId);
        param.put("instId", exchange.get("market"));
        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "POST";
        String requestPath = "/api/v5/trade/cancel-order";
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + body);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = null;
        try {
            trade = httpUtil.postByPackcoin(baseUrl + requestPath, param, head);
            JSONObject object = JSONObject.fromObject(trade);
            if (0 != object.getInt("code")) {
                setWarmLog(id, 3, "API接口错误", object.getString("msg"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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
        if (balance == null || overdue) {
            List<String> coinArr = Arrays.asList(coins.split(","));

            String firstBalance = "0.00";
            String lastBalance ="0.00";
            String firstBalance1 = "0.00";
            String lastBalance1 = "0.00";
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            JSONArray array = obj.getJSONArray("data").getJSONObject(0).getJSONArray("details");
            if ( obj.getInt("code") == 0 && array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    if (coinArr.get(0).equals(jsonObject.getString("ccy"))){
                        firstBalance = jsonObject.getString("availBal");
                        firstBalance1 = jsonObject.getString("frozenBal");
                    }
                    if (coinArr.get(1).equals(jsonObject.getString("ccy"))){
                        lastBalance =jsonObject.getString("availBal");
                        lastBalance1 = jsonObject.getString("frozenBal");
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

//    public String getTimespace() {
//        String timespace = null;
//        String rs = httpUtil.get(baseUrl + "/open/v1/timestamp");
//        JSONObject jsonObject = JSONObject.fromObject(rs);
//        if (jsonObject != null && jsonObject.getInt("code") == 0) {
//            timespace = jsonObject.getString("data");
//        }
//        return timespace;
//    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount)  {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = null;
        submitOrder = submitTrade(type == 1 ? 1 : 2, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONArray("data").getJSONObject(0).getString("ordId");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("result");
                hashMap.put("res", "false");
                hashMap.put("orderId", msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {

        return null;
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
//    private String getTrade() {
//        Map<String, Object> parms = new TreeMap<>();
//        parms.put("client_id", exchange.get("apikey"));
//        String timespace = getTimespace();
//        parms.put("ts", timespace);
//        parms.put("nonce", timespace);
//        String signs = null;
//        try {
//            signs = HMAC.splicing(parms);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        String sign = HMAC.sha256_HMAC(signs, exchange.get("tpass"));
//        String parm = signs + "&sign=" + sign + "&symbol=" + exchange.get("market");
//        String res = httpUtil.get(baseUrl + "/open/v1/orders/last?" + parm);
//        logger.info("获取委托中列表" + res);
//        return res;
//    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        cancelTrade = cancelTrade(orderId);
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
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

    public String getTimestamp() {
        String time=null;
        while(time==null){
            time = gettime();
            sleep(200,0);
        }
        return time;
    }
    public String gettime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, -8);
        String format = dateFormat.format(calendar.getTime());
        return dateFormat.format(calendar.getTime());

    }

    public static void main(String[] args) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 0);
        String format = dateFormat.format(calendar.getTime());
        System.out.println(format);
    }
}
