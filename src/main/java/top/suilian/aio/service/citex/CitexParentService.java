package top.suilian.aio.service.citex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
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
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CitexParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.citex.info";
    public String host = "api.citex.io";
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



    public String submitOrder(int type, BigDecimal price, BigDecimal amount){
        String time = gmtNow();
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));

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
        map.put("amount", num);
        map.put("price", price1);
        // map.put("source", "api");
        String order = splicing(map);


        logger.info("挂单参数" + order);
        try {
            trade = HttpUtil.postes(baseUrl + uri + "?" + httpParams, map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        logger.info("挂单结束："+trade);
        return trade;
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return{"code":0,"code_num":0,"msg":"ok","message":"ok","data":{"create_at":1648047858200,"fee":"0","match_amt":"0","match_price":"0","match_qty":"0","order_id":"34489150830","order_type":1,"price":"1.2920","quantity":"4.000","side":1,"status":2,"symbol":"FTM-USDT","ticker":"FTM-USDT","trade_no":"40522245595950224093911","trades":[]}}
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId){
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market")+"&order_id="+orderId;
        String rt = httpUtil.get(baseUrl+"/open/v1/orders/detail?"+parm);
        JSONObject object = JSONObject.fromObject(rt);
        if(0!=object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return rt;

    }


    protected String getTradeOrders() {
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/v1/orders/last?"+parm);
        logger.info("查询自己的委托列表"+res);
        return res;

    }

    public String getDepth(){
        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/v1/depth?"+parm);
        return res;

    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign;
        String res = httpUtil.get(baseUrl+"/open/v1/balance?"+parm);
        return res;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return {"code":0,"code_num":0,"msg":"ok","message":"ok","data":[]}
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId,String tradeNo) {

        Map<String, String> params = new TreeMap<String, String>();
        params.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        params.put("ts",timespace);
        params.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        params.put("sign",sign);
        params.put("symbol",exchange.get("market"));
        params.put("order_id",orderId);
        params.put("trade_no",tradeNo);
        HashMap<String, String> headMap = new HashMap<String, String>();
        headMap.put("Content-Type", " application/x-www-form-urlencoded");
        String res = null;
        try {
            res = httpUtil.post(baseUrl + "/open/v1/orders/cancel",params, headMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(res);
        if(0!=object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return res;
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
            if (obj != null&& obj.getInt("code")==0) {
                JSONArray dataJson=obj.getJSONArray("data");
                for(int i=0;i<dataJson.size();i++){
                    if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(0))){
                        firstBalance = dataJson.getJSONObject(i).getString("amount");
                        firstBalance1 = dataJson.getJSONObject(i).getString("freeze");
                    } else if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(1))){
                        lastBalance = dataJson.getJSONObject(i).getString("amount");
                        lastBalance1 = dataJson.getJSONObject(i).getString("freeze");
                    }
                }
            }else {
                logger.info("获取余额失败"+obj);
            }
            if(lastBalance!=null){
                if (Double.parseDouble(lastBalance) < 10) {
                    setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance+"_"+firstBalance1);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalance1);
            logger.info("获取余额"+ com.alibaba.fastjson.JSONObject.toJSONString(balances));
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
        if(cancelRes!=null&&cancelRes.getInt("code")==0){
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }

    public String getTimespace(){
        String timespace=null;
        String rs=httpUtil.get(baseUrl+"/open/v1/timestamp");
        JSONObject jsonObject = JSONObject.fromObject(rs);
        if(jsonObject!=null&&jsonObject.getInt("code")==0){
            timespace=jsonObject.getString("data");
        }
        return timespace;
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type == 1 ? 1 : -1, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("order_id")+"_"+jsonObject.getJSONObject("data").getString("trade_no");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
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
            map.put(jsonObject.getString("order_id")+"_"+jsonObject.getString("trade_no"),0);
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
     * @return
     */
    private String getTrade() {
        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/v1/orders/last?"+parm);
        logger.info("获取委托中列表"+res);
        return res;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
            String[] split = orderId.split("_");
            cancelTrade = cancelTrade(split[0],split[1]);
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("code"))){
                return "true";
            }
        }
        return "false";
    }

    public boolean setPrecision() {
        //为client_id, ts, nonce, sign
        boolean falg = false;
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign;
        String rt = httpUtil.get(baseUrl+"/open/v1/tickers?"+parm);

        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");

        if (!rt.equals("") && rtObj != null&&rtObj.getInt("code")==0) {
            JSONArray jsonArray = rtObj.getJSONArray("data");
            for(int i=0;i<jsonArray.size();i++){
                if(jsonArray.getJSONObject(i).getString("symbol").equals(exchange.get("market"))){
                    precision.put("amountPrecision",jsonArray.getJSONObject(i).getString("qty_num") );
                    precision.put("pricePrecision", jsonArray.getJSONObject(i).getString("amt_num"));
                    precision.put("minTradeLimit",exchange.get("minTradeLimit"));
                    falg=true;
                }
            }

        }else {
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

    public static String splicing(Map<String, Object> params){
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
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

    //获取账户id
    public String getAccountId(){
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
}
