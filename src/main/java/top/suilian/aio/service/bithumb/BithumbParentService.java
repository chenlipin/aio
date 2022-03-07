package top.suilian.aio.service.bithumb;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


@Service
@DependsOn("beanContext")
public class BithumbParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.bithumb.com";
    public String host = "";
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
        long time = System.currentTimeMillis() + 800 * 10;
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String uri = "/trade/place";
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
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) > 0) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }
                    HashMap<String, String> params = new HashMap<>();
                    params.put("endpoint", uri);
                    params.put("order_currency", exchange.get("market"));
                    params.put("payment_currency", "KRW");
                    params.put("units", num + "");
                    params.put("price", price1+"");
                    params.put("type",type==1?"bid":"ask");
                    HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
                    logger.info("挂单参数" + params);
                    trade = httpUtil.post(baseUrl + uri, params, map);
                    JSONObject jsonObject = JSONObject.fromObject(trade);
                    if (!"0000".equals(jsonObject.getString("status"))) {
                        setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
                    }

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put("endpoint", uri);
                params.put("order_currency", exchange.get("market"));
                params.put("payment_currency", "KRW");
                params.put("units", num + "");
                params.put("price", price1+"");
                params.put("type",type==1?"bid":"ask");
                HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
                logger.info("挂单参数" + params);
                trade = httpUtil.post(baseUrl + uri, params, map);
                JSONObject jsonObject = JSONObject.fromObject(trade);
                if (!"0000".equals(jsonObject.getString("status"))) {
                    setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
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
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String trade = null;
        String typeStr = type == 0 ? "买" : "卖";
        String uri = "/trade/place";
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        // 输出字符串
        HashMap<String, String> params = new HashMap<>();
        params.put("endpoint", uri);
        params.put("order_currency", exchange.get("market"));
        params.put("payment_currency", "KRW");
        params.put("units", num + "");
        params.put("price", price1+"");
        params.put("type",type==1?"bid":"ask");
        HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
        logger.info("挂单参数" + params);
        try {
            trade = httpUtil.post(baseUrl + uri, params, map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (!"0000".equals(jsonObject.getString("status"))) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
        }
        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return https://api.coinone.co.kr/v2/order/query_order/
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        String trade = null;
        String uri = "/info/order_detail";
        HashMap<String, String> params = new HashMap<>();
        params.put("endpoint", uri);
        params.put("order_currency", exchange.get("market"));
        params.put("payment_currency", "KRW");
        params.put("order_id", orderId);
        HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
        logger.info("查询订单" + params);
        try {
            trade = httpUtil.post(baseUrl + uri, params, map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (!"0000".equals(jsonObject.getString("status"))) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
        }
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return https://api.coinone.co.kr/v2/order/cancel/
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId, String type) throws UnsupportedEncodingException {

        String trade = null;
        String uri = "/trade/cancel";
        HashMap<String, String> params = new HashMap<>();
        params.put("endpoint", uri);
        params.put("type", type);
        params.put("order_currency", exchange.get("market"));
        params.put("payment_currency", "KRW");
        params.put("order_id", orderId);
        HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
        logger.info("撤单参数" + params);
        try {
            trade = httpUtil.post(baseUrl + uri, params, map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (!"0000".equals(jsonObject.getString("status"))) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
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

            Double firstBalance = null;
            Double lastBalance = null;
            Double firstBalance1 = null;
            Double lastBalance1 = null;
            JSONObject balence = getBalence(coinArr.get(0));
            JSONObject data = balence.getJSONObject("data");

            firstBalance = data.getDouble("total_"+coinArr.get(0).toLowerCase());
            firstBalance1 = data.getDouble("in_use_"+coinArr.get(0).toLowerCase());


            lastBalance = data.getDouble("total_"+coinArr.get(1).toLowerCase());
            lastBalance1 = data.getDouble("in_use_"+coinArr.get(1).toLowerCase());
            if (lastBalance < 10) {
                setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), new BigDecimal(firstBalance).setScale(2,BigDecimal.ROUND_HALF_DOWN).toPlainString() + "_" +
                    new BigDecimal(firstBalance1).setScale(2,BigDecimal.ROUND_HALF_DOWN).toPlainString());
            balances.put(coinArr.get(1), new BigDecimal(lastBalance).setScale(2,BigDecimal.ROUND_HALF_DOWN).toPlainString() + "_" +
                    new BigDecimal(lastBalance1).setScale(2,BigDecimal.ROUND_HALF_DOWN).toPlainString() );
            logger.info("获取余额" + com.alibaba.fastjson.JSONObject.toJSONString(balances));
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
        }

    }

    public JSONObject getBalence(String sym){
        String trade = null;
        String uri = "/info/balance";
        HashMap<String, String> params = new HashMap<>();
        params.put("endpoint", uri);
        params.put("order_currency", exchange.get("market"));
        params.put("currency", sym);
        HashMap<String, String> map = getHttpHeaders(uri, params, exchange.get("apikey"), exchange.get("tpass"));
        try {
            trade = httpUtil.post(baseUrl + uri, params, map);
            System.out.println(trade);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        return jsonObject;
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
        if (cancelRes != null && cancelRes.getString("status").equals("0000")) {
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
//            sb.append(key).append('=').append(u(value)).append('&');
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


    public static String encodeURIComponent(String s)
    {
        String result = null;

        try
        {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%26", "&")
                    .replaceAll("\\%3D", "=")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e)
        {
            result = s;
        }

        return result;
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
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0000".equals(jsonObject.getString("status"))) {
                orderId = jsonObject.getString("order_id");
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
            cancelTrade = cancelTrade(orderId, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("00000".equals(jsonObject.getString("status"))) {
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

    public static String asHex(byte[] bytes){
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(bytes));
    }

    public static byte[] hmacSha512(String value, String key){
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512");

            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(keySpec);

            final byte[] macData = mac.doFinal( value.getBytes( ) );
            byte[] hex = new Hex().encode( macData );

            //return mac.doFinal(value.getBytes(DEFAULT_ENCODING));
            return hex;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private HashMap<String, String> getHttpHeaders(String endpoint, HashMap<String, String> rgData, String apiKey, String apiSecret) {

        String strData = mapToQueryString(rgData).replace("?", "");
        String nNonce = String.valueOf(System.currentTimeMillis() + 1000 * 10);

        strData = strData.substring(0, strData.length()-1);


        System.out.println("1 : " + strData);

        strData = encodeURIComponent(strData);

        HashMap<String, String> array = new HashMap<String, String>();


        String str = endpoint + ";"	+ strData + ";" + nNonce;

        String encoded = asHex(hmacSha512(str, apiSecret));

        System.out.println("strData was: " + str);
        System.out.println("apiSecret was: " + apiSecret);
        array.put("Api-Key", apiKey);
        array.put("Api-Sign", encoded);
        array.put("Api-Nonce", nNonce);
        array.put("api-client-type","2");
        return array;

    }

    public static String mapToQueryString(Map<String, String> map) {
        StringBuilder string = new StringBuilder();

        if (map.size() > 0) {
            string.append("?");
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            string.append(entry.getKey());
            string.append("=");
            string.append(entry.getValue());
            string.append("&");
        }

        return string.toString();
    }
}
