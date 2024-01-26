package top.suilian.aio.service.xt;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.DateConvertUtil;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;
import top.suilian.aio.vo.getAllOrderPonse;

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

public class XtParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://sapi.xt.com";
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


    @Override
    public List<getAllOrderPonse> selectOrder() {
        List<getAllOrderPonse> orderPonses = new ArrayList<>();
        String time = System.currentTimeMillis() + "";
        String uri = "/v4/open-order";

        Map<String, String> head = new TreeMap<>();
        head.put("validate-algorithms", "HmacSHA256");
        head.put("validate-appkey",   exchange.get("apikey"));
        head.put("validate-recvwindow", "5000");
        head.put("validate-timestamp",time);
        String headStr = splicing(head);

        String original = headStr + "#GET#" + uri+"#symbol="+exchange.get("market");

        String sign = HMAC.sha256_HMAC(original, exchange.get("tpass"));
        head.put("validate-signature", sign);
        String trade = null;
        try {
            trade = HttpUtil.getAddHead(baseUrl + uri+"?symbol="+exchange.get("market"),  head);
        } catch (Exception e) {
            e.printStackTrace();
        }
        com.alibaba.fastjson.JSONArray jsonArray = com.alibaba.fastjson.JSONObject.parseObject(trade).getJSONArray("result");
        for (int i = 0; i < jsonArray.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            com.alibaba.fastjson.JSONObject object = jsonArray.getJSONObject(i);

            getAllOrderPonse.setOrderId(object.getString("orderId"));
            getAllOrderPonse.setPrice((object.getString("side").equals("BUY")?"买-":"卖-")+object.getString("price"));
            getAllOrderPonse.setAmount(object.getString("origQty"));
            getAllOrderPonse.setType(object.getString("side").equals("BUY")?1:2);
            getAllOrderPonse.setStatus(1);
            DateConvertUtil dateConvertUtil = new DateConvertUtil();
            getAllOrderPonse.setCreatedAt(dateConvertUtil.getTimeStampString(object.getLong("time"),"yyyy-MM-dd HH:mm:ss",0));
            orderPonses.add(getAllOrderPonse);
        }
        return orderPonses;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    /**
     * {"rc":0,"mc":"SUCCESS","ma":[],"result":{"orderId":"321171312500535296","clientOrderId":null}}
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

        String uri = "/v4/order";
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", exchange.get("market"));
        params.put("side", type == 1 ? "BUY" : "SELL");
        params.put("type","LIMIT");
        params.put("timeInForce","GTC");
        params.put("bizType","SPOT");
        params.put("price", price1 + "");
        params.put("quantity", num + "");
        System.out.println("请求body："+ JSON.toJSONString(params));
        Map<String, String> head = new TreeMap<>();
        head.put("validate-algorithms", "HmacSHA256");
        head.put("validate-appkey",   exchange.get("apikey"));
        head.put("validate-recvwindow", "5000");
        head.put("validate-timestamp",time);
        String headStr = splicing(head);
        String original = headStr + "#POST#" + uri + "#" + com.alibaba.fastjson.JSONObject.toJSONString(params);
        String sign = HMAC.sha256_HMAC(original, exchange.get("tpass"));
        head.put("validate-signature", sign);
        try {
            trade = HttpUtil.doPostMart(baseUrl + uri, JSON.toJSONString(params), head);
            logger.info("挂单结束：" + trade);
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("rc")) {
            setWarmLog(id, 3, "API接口错误", object.getString("mc"));
        }

        return trade;
    }

    /**
     * 查询订单详情
     *{
     *     "rc":0,
     *     "mc":"SUCCESS",
     *     "ma":[
     *
     *     ],
     *     "result":{
     *         "symbol":"aura_usdt",
     *         "orderId":"321171312500535296",
     *         "clientOrderId":null,
     *         "baseCurrency":"aura",
     *         "quoteCurrency":"usdt",
     *         "side":"BUY",
     *         "type":"LIMIT",
     *         "timeInForce":"GTC",
     *         "price":"0.5000",
     *         "origQty":"11.0000",
     *         "origQuoteQty":"5.5000",
     *         "executedQty":"0.0000",
     *         "leavingQty":"11.0000",
     *         "tradeBase":"0.0000",
     *         "tradeQuote":"0.0000",
     *         "avgPrice":null,
     *         "fee":null,
     *         "feeCurrency":null,
     *         "nftId":null,
     *         "symbolType":"normal",
     *         "closed":false,
     *         "state":"NEW",
     *         "time":1706275207974,
     *         "updatedTime":null
     *     }
     * }
     *
     */


    public String selectOrder(String orderId) {
        String time = System.currentTimeMillis() + "";
        String uri = "/v4/order/"+orderId;

        Map<String, String> head = new TreeMap<>();
        head.put("validate-algorithms", "HmacSHA256");
        head.put("validate-appkey",   exchange.get("apikey"));
        head.put("validate-recvwindow", "5000");
        head.put("validate-timestamp",time);
        String headStr = splicing(head);
        String original = headStr + "#GET#" + uri;

        String sign = HMAC.sha256_HMAC(original, exchange.get("tpass"));
        head.put("validate-signature", sign);
        String trade = null;
        try {
            trade = HttpUtil.getAddHead(baseUrl + uri,  head);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("rc")) {
            setWarmLog(id, 3, "API接口错误", object.getString("mc"));
        }
        return trade;

    }


    public String selectOrderS() {
        String time = System.currentTimeMillis() + "";
        String str = "123456789AAAHJSGIUAI" + time + RandomUtils.nextInt(50);
        String uri = "/v2/orders_info_history.do";
        Map<String, String> params = new TreeMap<>();
        params.put("api_key", exchange.get("apikey"));
        params.put("symbol", exchange.get("market"));
        params.put("current_page", "1");
        params.put("page_length", "+199");
        params.put("status", "0");
        params.put("signature_method", "HmacSHA256");
        params.put("timestamp", time);
        params.put("echostr", str);
        HashMap<String, String> head = new HashMap<>();
        head.put("signature_method", "HmacSHA256");
        head.put("timestamp", time);
        head.put("echostr", str);
        String order = splicing(params);
        logger.info("查询订单参数" + order);
        String md5 = DigestUtils.md5Hex(order).toUpperCase();
        String sign = HMAC.sha256_HMAC(md5, exchange.get("tpass"));
        params.put("sign", sign);
        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + uri, params, head);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("error_code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("result"));
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
        return httpUtil.get("https://sapi.xt.com/v4/public/depth?limit=10&symbol=" + exchange.get("market"));
    }


    /**
     * {"rc":0,"mc":"SUCCESS","ma":[],"result":{"totalUsdtAmount":"100.0000","totalBtcAmount":"0.00243751","assets":[{"currency":"usdt","currencyId":11,"frozenAmount":"0.00000000","availableAmount":"100.00000000","totalAmount":"100.00000000","convertBtcAmount":"0.00243751","convertUsdtAmount":"100"}]}}
     */

    protected String getBalance() {

        String time = System.currentTimeMillis() + "";
        String uri = "/v4/balances";

        Map<String, String> head = new TreeMap<>();
        head.put("validate-algorithms", "HmacSHA256");
        head.put("validate-appkey",   exchange.get("apikey"));
        head.put("validate-recvwindow", "5000");
        head.put("validate-timestamp",time);
        String headStr = splicing(head);
        String replace = exchange.get("market").replace("_", ",");
        String original = headStr + "#GET#" + uri+"#currencies="+replace;

        String sign = HMAC.sha256_HMAC(original, exchange.get("tpass"));
        head.put("validate-signature", sign);
        String trade = null;
        try {
            trade = HttpUtil.getAddHead(baseUrl + uri+"?currencies="+replace,  head);
            logger.info("获取余额：" + trade);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("rc")) {
            setWarmLog(id, 3, "API接口错误", object.getString("mc"));
        }
        return trade;
    }

    /**
     * 撤单
     * <p>
     * {"rc":0,"mc":"SUCCESS","ma":[],"result":{"orderId":"321171312500535296","cancelId":"321177482640803328","clientCancelId":""}}
     */
    public String cancelTrade(String orderId) {
        String time = System.currentTimeMillis() + "";
        String uri = "/v4/order/"+orderId;

        Map<String, String> head = new TreeMap<>();
        head.put("validate-algorithms", "HmacSHA256");
        head.put("validate-appkey",   exchange.get("apikey"));
        head.put("validate-recvwindow", "5000");
        head.put("validate-timestamp",time);
        String headStr = splicing(head);
        String original = headStr + "#DELETE#" + uri;

        String sign = HMAC.sha256_HMAC(original, exchange.get("tpass"));
        head.put("validate-signature", sign);
        String trade = null;
        try {
            trade = HttpUtil.doDeletes(baseUrl + uri,  head);
            logger.info("撤单结束：" + trade+",单号"+orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("rc")) {
            setWarmLog(id, 3, "API接口错误", object.getString("mc"));
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
            List<String> coinArr = Arrays.asList(coins.split("_"));

            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null && obj.getInt("rc") == 0) {
                JSONArray json = obj.getJSONObject("result").getJSONArray("assets");
                for (int i = 0; i < json.size(); i++) {
                    JSONObject jsonObject = json.getJSONObject(i);
                    if (jsonObject.getString("currency").equals(coinArr.get(0))){
                        firstBalance = jsonObject.getString("availableAmount");
                        firstBalance1 = jsonObject.getString("frozenAmount");
                    }else if (jsonObject.getString("currency").equals(coinArr.get(1))){
                        lastBalance = jsonObject.getString("availableAmount");
                        lastBalance1 =  jsonObject.getString("frozenAmount");
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
            if ("0".equals(jsonObject.getString("rc"))) {
                orderId = jsonObject.getJSONObject("result").getString("orderId");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("mc");
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
            if ("0".equals(jsonObject.getString("rc"))) {
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


}
