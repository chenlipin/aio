package top.suilian.aio.service.bision;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
@DependsOn("beanContext")
@Service
public class BisionParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.xt.com";

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
     * 下单 与判断
     *
     * @param type
     * @param price
     * @param amount
     * @return
     * @throws UnsupportedEncodingException
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 0 ? "卖" : "买";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(exchange.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(exchange.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
                if (price1.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> param = new TreeMap<String, Object>();
                    param.put("nonce", timestamp);
                    param.put("accesskey", exchange.get("apikey"));
                    param.put("entrustType", "0");
                    param.put("price", String.valueOf(price1));
                    param.put("number", String.valueOf(num));
                    param.put("type", type == 0 ? "0" : "1");
                    param.put("market", exchange.get("market"));
                    String peload = toSort(param);
                    String signature = HMAC.sha256_HMAC(peload, exchange.get("tpass"));
                    param.put("signature", signature);
                    logger.info("挂单参数:" + peload);
                    trade = HttpUtil.post(baseUrl + "/trade/api/v1/order", param);
                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
                    JSONObject jsonObject = JSONObject.fromObject(trade);
                    if (200 != jsonObject.getInt("code")) {
                        setWarmLog(id, 3, "API接口错误", jsonObject.getString("msgInfo"));
                    }

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束11");
                }
            }

        valid = 1;
        return trade;

    }


    protected String getTradeOrders(int type) {
        String timestamp = String.valueOf(new Date().getTime());

        HashMap<String, String> param = new LinkedHashMap<String, String>();
        param.put("method", "getOrders");
        param.put("accesskey", exchange.get("apikey"));
        param.put("tradeType", String.valueOf(type));
        param.put("currency", exchange.get("market"));
        param.put("pageIndex", "1");
        param.put("pageSize", "20");

        String sign = gotoSign(param, exchange.get("tpass"));

        param.put("sign", sign);
        String trade = httpUtil.get(baseUrl + "/api/v2/getOrders?method=getOrders&accesskey=" + exchange.get("apikey") + "&tradeType=" + type + "&currency=" + exchange.get("market") + "&pageIndex=1&pageSize=20&sign=" + sign + "&reqTime=" + timestamp);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (200 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msgInfo"));
        }
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
        long timestamp = new Date().getTime();
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("nonce", timestamp + "");
        param.put("accesskey", exchange.get("apikey"));
        param.put("id", orderId);
        param.put("market", exchange.get("market"));
        String peload = toSort(param);
        String signature = HMAC.sha256_HMAC(peload, exchange.get("tpass"));
        String orderInfo = httpUtil.get(baseUrl + "/trade/api/v1/getOrder?accesskey=" + exchange.get("apikey") + "&id=" + orderId + "&market=" + exchange.get("market") + "&signature=" + signature + "&nonce=" + timestamp);
        JSONObject jsonObject = JSONObject.fromObject(orderInfo);
        if (200 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msgInfo"));
        }
        return orderInfo;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        long timestamp = new Date().getTime();
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("nonce", timestamp);
        param.put("accesskey", exchange.get("apikey"));
        String peload = toSort(param);
        String signature = HMAC.sha256_HMAC(peload, exchange.get("tpass"));
        String orderInfo = httpUtil.get(baseUrl + "/trade/api/v1/getBalance?accesskey=" + exchange.get("apikey") + "&signature=" + signature + "&nonce=" + timestamp);
        JSONObject jsonObject = JSONObject.fromObject(orderInfo);
        if (200 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msgInfo"));
        }
        return orderInfo;
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
        String res = "";
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("nonce", timestamp + "");
        param.put("accesskey", exchange.get("apikey"));
        param.put("id", orderId);
        param.put("market", exchange.get("market"));
        String peload = toSort(param);
        String signature = HMAC.sha256_HMAC(peload, exchange.get("tpass"));
        param.put("signature", signature);
        res = HttpUtil.post(baseUrl + "/trade/api/v1/cancel", param);
        JSONObject jsonObject = JSONObject.fromObject(res);
        if (200 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msgInfo"));
        }
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

            //获取余额
            String rt = getBalance();
          logger.info("获取余额"+rt);
            JSONObject obj = JSONObject.fromObject(rt);

            if ("200".equals(obj.getString("code")) && obj.getJSONObject("data") != null) {
                JSONObject data = obj.getJSONObject("data");
                HashMap<String, String> balances = new HashMap<String, String>();
                balances.put(coinArr.get(0), data.getJSONObject(coinArr.get(0)).getString("available")+"_"+data.getJSONObject(coinArr.get(0)).getString("freeze"));
                if (data.getJSONObject(coinArr.get(1)) == null) {
                    balances.put(coinArr.get(1), "0");
                } else {
                    balances.put(coinArr.get(1), data.getJSONObject(coinArr.get(1)).getString("available")+"_"+data.getJSONObject(coinArr.get(1)).getString("freeze"));
                }
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            } else {
                logger.info("获取余额失败：" + obj);
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
        if (cancelRes != null && cancelRes.getInt("code") == 200) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BiSION);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/data/api/v1/getMarketConfig");
        JSONObject rtObj = judgeRes(rt, exchange.get("market"), "setPrecision");
        if (!rt.equals("") && rtObj != null) {
            JSONObject object = rtObj.getJSONObject(exchange.get("market"));
            precision.put("amountPrecision", object.getString("coinPoint"));
            precision.put("pricePrecision", object.getString("pricePoint"));
            precision.put("minTradeLimit", object.getString("minAmount"));
            return true;
        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }


    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = null;
        try {
            submitOrder = submitTrade(type==1?1:0, price, amount);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("id");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("msgInfo");
                hashMap.put("res", "false");
                hashMap.put("orderId", msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> hashMap = new HashMap<>();
        orders.forEach(e -> {
            hashMap.put(e, 2);
        });
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
            if ("200".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }

    public String gotoSign(HashMap<String, String> params, String secret) {
        String sign = "";
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
            byte[] sha1Encode = sha1Digest.digest(secret.getBytes());
            String signSecret = convertByteToHexString(sha1Encode);
            String sortStr = keySortToString(params);
            SecretKeySpec sk = new SecretKeySpec(signSecret.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(sk);
            return convertByteToHexString(mac.doFinal(sortStr.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return sign;
    }


    public String keySortToString(HashMap<String, String> params) {
        String str = "";
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            str += entry.getKey() + "=" + entry.getValue() + "&";
        }
        return str.substring(0, str.length() - 1);
    }


    public static String convertByteToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            int temp = bytes[i] & 0xff;
            String tempHex = Integer.toHexString(temp);
            if (tempHex.length() < 2) {
                result += "0" + tempHex;
            } else {
                result += tempHex;
            }
        }
        return result;

    }

    private static String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();

        int i = 0;
        int max = map.size() - 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (entry.getValue() != null) {

                if (i == max) {
                    buffer.append(entry.getKey() + "=");
                    buffer.append(entry.getValue().toString());
                } else {
                    buffer.append(entry.getKey() + "=");
                    buffer.append(entry.getValue().toString() + "&");
                }
                i++;

            }

        }
        return buffer.toString();
    }

}
