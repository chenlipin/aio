package top.suilian.aio.service.kucoin;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
@Service
@DependsOn("beanContext")
public class KucoinParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.kucoin.com";

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


    @Override
    public List<getAllOrderPonse> selectOrder() {
        return null;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    /**
     * 下单
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {

        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(String.valueOf(exchange.get("minTradeLimit")));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) > 0) {
                    num = BigDecimal.valueOf(numThreshold1);
                }
                Map<String, String> param = new TreeMap<>();
                param.put("clientOid", "KUNKUN" + String.valueOf(new Date().getTime()));
                param.put("side", type == 1 ? "buy" : "sell");
                param.put("symbol", exchange.get("market"));
                param.put("price", String.valueOf(price));
                param.put("size", String.valueOf(amount));
                param.put("type", "limit");
                String body = JSON.toJSONString(param);
                HashMap<String, String> head = new HashMap<String, String>();
                String apikey = exchange.get("apikey");
                String timestamp = System.currentTimeMillis() + "";
                String passphrase = exchange.get("passphrase");
//                 passphrase = HMAC.genHMAC(passphrase, exchange.get("tpass"));
//                 passphrase = HMAC.Base64(passphrase);
                String method = "POST";
                String requestPath = "/api/v1/orders";
                head.put("KC-API-KEY", apikey);
                head.put("KC-API-TIMESTAMP", timestamp);
                head.put("KC-API-PASSPHRASE", passphrase);
//                head.put("KC-API-KEY-VERSION","2");
                String payload = (timestamp + method + requestPath + body);
                String signstr = HMAC.genHMAC(payload, exchange.get("tpass"));
                String sign = HMAC.Base64(signstr);
                head.put("KC-API-SIGN", signstr);
                trade = httpUtil.postByPackcoin(baseUrl + "/api/v1/orders", param, head);
                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId:" + id + "挂单成功结束：" + trade);

                JSONObject jsonObject = JSONObject.fromObject(trade);
                if (200000 != jsonObject.getInt("code")) {
                    setWarmLog(id, 3, "API接口错误", jsonObject.getString("msg"));
                }
            } else {
                setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                logger.info("robotId:" + id + "挂单失败结束");
            }
        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId:" + id + "挂单失败结束");
        }

        return trade;
    }

    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(exchange.get("amountPrecision").toString()));
        Map<String, String> param = new TreeMap<>();
        param.put("clientOid", "SUIlian" + String.valueOf(new Date().getTime()));
        param.put("type", "limit");
        param.put("side", type == 1 ? "buy" : "sell");
        param.put("symbol", exchange.get("market"));
        param.put("price", String.valueOf(price));
        param.put("size", String.valueOf(amount));
        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = System.currentTimeMillis() + "";
        String passphrase = exchange.get("passphrase");
        String method = "POST";
        String requestPath = "/api/v1/orders";
        head.put("KC-API-KEY", apikey);
        head.put("KC-API-TIMESTAMP", timestamp);
        head.put("KC-API-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + body);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("KC-API-SIGN", sign);
        trade = httpUtil.postByPackcoin(baseUrl + "/api/v1/orders", param, head);
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId:" + id + "挂单成功结束：" + trade);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (200000 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msg"));
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

        String trade = null;
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = System.currentTimeMillis() + "";
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/v1/orders/" + orderId;
        head.put("KC-API-KEY", apikey);
        head.put("KC-API-TIMESTAMP", timestamp);
        head.put("KC-API-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("KC-API-SIGN", sign);
        trade = httpUtil.getAddHead(baseUrl + "/api/v1/orders/" + orderId, head);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (200000 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msg"));
        }
        return trade;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = System.currentTimeMillis() + "";
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/v1/accounts";
        head.put("KC-API-KEY", apikey);
        head.put("KC-API-TIMESTAMP", timestamp);
        head.put("KC-API-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("KC-API-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + "/api/v1/accounts", head);
        return trade;
    }

    /**
     * 通过币名获取余额
     */


    protected String getBalanceByName(String name) {
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/account/v3/wallet/" + name;
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + "/api/account/v3/wallet/" + name, head);
        return trade;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
//        Map<String, String> param = new TreeMap<>();
//        param.put("client_oid", orderId);
//        param.put("instrument_id", exchange.get("market"));
//        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = System.currentTimeMillis() + "";
        String passphrase = exchange.get("passphrase");
        String method = "DELETE";
        String requestPath = "/api/v1/orders/" + orderId;
        head.put("KC-API-KEY", apikey);
        head.put("KC-API-TIMESTAMP", timestamp);
        head.put("KC-API-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + "");
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("KC-API-SIGN", sign);
        String trade = httpUtil.delete(baseUrl + "/api/v1/orders/" + orderId, head);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if (200000 != jsonObject.getInt("code") && 400100 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("msg"));
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

            //获取余额
            JSONArray coinLists = null;
            String rt = getBalance();
            logger.info("获取余额"+rt);

            if (rt != null) {
                com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(rt);
                coinLists = JSONArray.fromObject(jsonObject1.getString("data"));
                String firstBalance = null;
                String lastBalance = null;

                for (int i = 0; i < coinLists.size(); i++) {
                    JSONObject jsonObject = coinLists.getJSONObject(i);

                    if (jsonObject.getString("currency").equals(coinArr.get(0)) && "trade".equals(jsonObject.getString("type"))) {
                        double v = jsonObject.getDouble("balance") - jsonObject.getDouble("available");
                        firstBalance = jsonObject.getString("available") + "_" + v;
                    } else if (jsonObject.getString("currency").equals(coinArr.get(1)) && "trade".equals(jsonObject.getString("type"))) {
                        double v = jsonObject.getDouble("balance") - jsonObject.getDouble("available");
                        lastBalance = jsonObject.getString("available") + "_" + v;
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            } else {
                logger.info("获取余额失败");
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
        JSONObject cancelRess = judgeRes(res, "result", "cancelTrade");
        if (cancelRes != null && "true".equals(cancelRess.getString("result"))) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_KUCOIN);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/api/spot/v3/instruments");

        JSONArray jsonArray = JSONArray.fromObject(rt);
        if (!rt.equals("") && jsonArray != null) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("instrument_id").equals(exchange.get("market"))) {
                    String amountPrecision = getPrecision(jsonObject.getString("size_increment"));
                    String pricePrecision = getPrecision(jsonObject.getString("tick_size"));
                    String minTradeLimit = jsonObject.getString("min_size");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", minTradeLimit);
                    falg = true;
                }
            }

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
            submitOrder = submitTrade(type, price, amount);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200000".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("orderId");
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
        return null;
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
            if ("200000".equals(jsonObject.getString("code"))||"400100".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }

    public String getTimestamp() {
        String time = null;
        while (time == null) {
            time = gettime();
            sleep(500, 0);
        }
        return time;
    }

    public String gettime() {
        String res = httpUtil.get(baseUrl + "/api/general/v3/time");
        JSONObject jsonObject = judgeRes(res, "iso", "gettime");
        if (jsonObject != null) {
            return jsonObject.getString("iso");
        } else {
            return null;
        }
    }

    public String getPrecision(String num) {
        int length = num.length();
        return String.valueOf(length - 2);
    }

}
