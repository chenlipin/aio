package top.suilian.aio.service.whitebit;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
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
import top.suilian.aio.vo.getAllOrderPonse;

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
public class WhitebitParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://whitebit.com";
    public String host = "";
    public RunHotcoinRandomDepth runHotcoinRandomDepth = BeanContext.getBean(RunHotcoinRandomDepth.class);

    public Map<String, Object> precision = new HashMap<String, Object>();
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


    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        Map<String, String> params = new TreeMap<>();
        String time = System.currentTimeMillis() + "";
        params.put("request", "/api/v4/order/new");
        params.put("nonce", time);
        params.put("price", price1 + "");
        params.put("amount", amount + "");
        params.put("side", type==1? "buy":"sell");
        params.put("market", exchange.get("market"));
        String play = Base64.getEncoder().encodeToString(JSON.toJSONString(params).getBytes());
        HashMap<String, String> head = new HashMap<>();
        head.put("X-TXC-PAYLOAD", play);
        head.put("X-TXC-APIKEY",  exchange.get("apikey"));
        String sign = HMAC.Hmac_SHA512(play, exchange.get("tpass"));
        head.put("X-TXC-SIGNATURE", sign);
        head.put("Content-Type", "application/json");
        logger.info("挂单参数" + params);
        String trade = null;
        try {
            trade = httpUtil.doPostMart(baseUrl + "/api/v4/order/new", JSON.toJSONString(params), head);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if (null== jsonObject.getString("orderId")) {
                setWarmLog(id, 3, "API接口错误", trade);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setWarmLog(id, 3, "API接口错误", trade);
        }

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return https://api.coinone.co.kr/v2/order/query_order/
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId){
        String trade = null;
        Map<String, String> params = new TreeMap<>();
        String time = System.currentTimeMillis() + "";
        params.put("request", "/api/v4/trade-account/order");
        params.put("nonce", time);
        params.put("orderId", orderId);
        String play = Base64.getEncoder().encodeToString(JSON.toJSONString(params).getBytes());
        HashMap<String, String> head = new HashMap<>();
        head.put("X-TXC-PAYLOAD", play);
        head.put("X-TXC-APIKEY",  exchange.get("apikey"));
        String sign = HMAC.Hmac_SHA512(play, exchange.get("tpass"));
        head.put("X-TXC-SIGNATURE", sign);
        head.put("Content-Type", "application/json");
        logger.info("查询订单参数" + params);
        try {
            trade = httpUtil.doPostMart(baseUrl + "/api/v4/trade-account/order", JSON.toJSONString(params), head);
        } catch (Exception e) {
            logger.info("查询订单结果" + trade);
            e.printStackTrace();
        }
        logger.info("查询订单结果" + trade);
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return https://api.coinone.co.kr/v2/order/cancel/
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId){
        String trade = null;
        Map<String, String> params = new TreeMap<>();
        String time = System.currentTimeMillis() + "";
        params.put("request", "/api/v4/order/cancel");
        params.put("nonce", time);
        params.put("orderId", orderId);
        params.put("market", exchange.get("market"));
        String play = Base64.getEncoder().encodeToString(JSON.toJSONString(params).getBytes());
        HashMap<String, String> head = new HashMap<>();
        head.put("X-TXC-PAYLOAD", play);
        head.put("X-TXC-APIKEY",  exchange.get("apikey"));
        String sign = HMAC.Hmac_SHA512(play, exchange.get("tpass"));
        head.put("X-TXC-SIGNATURE", sign);
        head.put("Content-Type", "application/json");
        logger.info("撤单参数" + params);
        try {
            trade = httpUtil.doPostMart(baseUrl + "/api/v4/order/cancel", JSON.toJSONString(params), head);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;
    }

    /**
     * 获取余额
     */

    public void setBalanceRedis()  {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

            Map<String, String> params = new TreeMap<>();
            String time = System.currentTimeMillis() + "";
            params.put("request", "/api/v4/trade-account/balance");
            params.put("nonce", time);
            String play = Base64.getEncoder().encodeToString(JSON.toJSONString(params).getBytes());
            HashMap<String, String> head = new HashMap<>();
            head.put("X-TXC-PAYLOAD", play);
            head.put("X-TXC-APIKEY",  exchange.get("apikey"));
            String sign = HMAC.Hmac_SHA512(play, exchange.get("tpass"));
            head.put("X-TXC-SIGNATURE", sign);
            head.put("Content-Type", "application/json");
            logger.info("挂单参数" + params);
            String trade = null;
            try {
                trade = httpUtil.doPostMart(baseUrl + "/api/v4/trade-account/balance", JSON.toJSONString(params), head);
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject tradesJson = JSONObject.fromObject(trade);
            Double firstBalance = null;
            Double lastBalance = null;
            Double firstBalance1 = null;
            Double lastBalance1 = null;

            JSONObject jsonObject = tradesJson.getJSONObject(coinArr.get(0));
            firstBalance = jsonObject.getDouble("available");
            firstBalance1 = jsonObject.getDouble("freeze") ;

            JSONObject jsonObject1 = tradesJson.getJSONObject(coinArr.get(1));
            lastBalance = jsonObject1.getDouble("available");
            lastBalance1 = jsonObject1.getDouble("freeze");
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

    /**
     * 存储撤单信息
     *
     * @param cancelRes
     * @param res
     * @param orderId
     * @param type
     */
    public void setCancelOrder(JSONObject cancelRes, String res, String orderId, Integer type) {
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
            if (jsonObject.getString("orderId")!=null) {
                orderId = jsonObject.getString("orderId");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = submitOrder;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("code"))) {
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

    private static String calcSignature(String data,String tpass) {

        final String HMAC_SHA512 = "HmacSHA512";
        SecretKeySpec secretKeySpec = new SecretKeySpec(tpass.getBytes(), HMAC_SHA512);
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA512);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            mac.init(secretKeySpec);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        byte[] bytes = mac.doFinal(data.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }
}
