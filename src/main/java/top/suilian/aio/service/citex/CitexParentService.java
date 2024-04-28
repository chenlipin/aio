package top.suilian.aio.service.citex;

import com.alibaba.fastjson.JSON;
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

public class CitexParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://openapi.citex.io/spot";
    public String host = "api.citex.io";
    public RunHooRandomDepth runHooRandomDepth = BeanContext.getBean(RunHooRandomDepth.class);
    public Map<String, Object> precision = new HashMap<String, Object>();
    static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    static final ZoneId ZONE_GMT = ZoneId.of("Z");
    public int cnt = 0;

    @Override
    public List<getAllOrderPonse> selectOrder() {
        return null;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

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



    public String submitTrade(int type, BigDecimal price, BigDecimal amount)   {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;
        String apikey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        long time = System.currentTimeMillis();
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        // 签名过程
        String pattern = "ts=%s,apiKey=%s,apiSecret=%s";
        // ts有时效性，超过10秒则无效
        String data = String.format(pattern, time, apikey, tpass);
        // 使用MD5工具对数据进行加密，生成32位的十六进制字符串（不区分大小写）
        String sign =HMAC.MD5(data);

        Map<String, String> param = new TreeMap<>();
        param.put("side", type == 1 ? "0" : "1");
        param.put("orderType", "0");
        param.put("priceType", "1");
        param.put("symbol", exchange.get("market"));
        param.put("price", String.valueOf(price1));
        param.put("orderQty", String.valueOf(num));
        HashMap<String, String> head = new HashMap<String, String>();
        String requestPath = "/order/place";
        head.put("apiKey", apikey);
        head.put("ts", time+"");
        head.put("sign", sign);
        try {
            trade = httpUtil.postByPackcoin(baseUrl + requestPath, param, head);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }






    public String getDepth(){
        return httpUtil.get("https://www.citex.io/prod-api/market/depth?businessType=spot&depth=20&step=step0&symbol="+exchange.get("market"));
    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        String trade = null;
        String apikey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        long time = System.currentTimeMillis();
        // 签名过程
        String pattern = "ts=%s,apiKey=%s,apiSecret=%s";
        // ts有时效性，超过10秒则无效
        String data = String.format(pattern, time, apikey, tpass);
        // 使用MD5工具对数据进行加密，生成32位的十六进制字符串（不区分大小写）
        String sign = HMAC.MD5(data);


        HashMap<String, String> head = new HashMap<String, String>();
        String requestPath = "/wallet/assets";
        head.put("apiKey", apikey);
        head.put("ts", time+"");
        head.put("sign", sign);
        try {
            trade = httpUtil.getAddHead(baseUrl + requestPath, head);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return trade;
    }

    /**
     * 撤单
     *
     * {"code":0,"msg":"success","data":null}
     */
    public String cancelTrade(String orderId) {

        String trade = null;
        String apikey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        long time = System.currentTimeMillis();
        // 签名过程
        String pattern = "ts=%s,apiKey=%s,apiSecret=%s";
        // ts有时效性，超过10秒则无效
        String data = String.format(pattern, time, apikey, tpass);
        // 使用MD5工具对数据进行加密，生成32位的十六进制字符串（不区分大小写）
        String sign =HMAC.MD5(data);
        Map<String, String> param = new TreeMap<>();
        param.put("orderId", orderId);
        HashMap<String, String> head = new HashMap<String, String>();
        String requestPath = "/order/cancel";
        head.put("apiKey", apikey);
        head.put("ts", time+"");
        head.put("sign", sign);
        try {
            trade = httpUtil.postByPackcoin(baseUrl + requestPath, param, head);
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
            List<String> coinArr = Arrays.asList(coins.split("_"));

            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
                JSONArray dataJson=obj.getJSONArray("data");
                for(int i=0;i<dataJson.size();i++){
                    if(dataJson.getJSONObject(i).getString("coinId").equals(coinArr.get(0))){
                        firstBalance = dataJson.getJSONObject(i).getString("balance");
                        firstBalance1 = dataJson.getJSONObject(i).getString("frozenBalance");
                    } else if(dataJson.getJSONObject(i).getString("coinId").equals(coinArr.get(1))){
                        lastBalance = dataJson.getJSONObject(i).getString("balance");
                        lastBalance1 = dataJson.getJSONObject(i).getString("frozenBalance");
                    }
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
        String submitOrder = submitTrade(type == 1 ? 1 : -1, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("orderId");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("message");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
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
            cancelTrade = cancelTrade(orderId);
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
