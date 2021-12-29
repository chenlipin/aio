package top.suilian.aio.service.zg;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

@DependsOn("beanContext")
@Service
public class ZGParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://www.ztb.im/api/v1";

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

        String typeStr = type == 1 ? "卖" : "买";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(exchange.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }
                    Map<String, Object> param = new TreeMap<String, Object>();

                    param.put("api_key", exchange.get("apikey"));
                    param.put("amount", num);
                    param.put("market", exchange.get("market"));
                    param.put("price", price1);
                    param.put("side", type);
                    String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();

                    param.put("sign", signature);

                    logger.info("robotId" + id + "----" + "挂单参数：" + JSONObject.fromObject(param));

                    trade = httpUtil.post(baseUrl + "/private/trade/limit", (TreeMap<String, Object>) param);
                    System.out.println("量化挂dan" + trade);
                    JSONObject jsonObject = JSONObject.fromObject(trade);
                    if (0 != jsonObject.getInt("code")) {
                        setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
                    }
                    setTradeLog(id, "量化挂" + (type == 1 ? "卖" : "买") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);


                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, Object> param = new TreeMap<String, Object>();

                param.put("api_key", exchange.get("apikey"));
                param.put("amount", num);
                param.put("market", exchange.get("market"));
                param.put("price", price1);
                param.put("side", type);
                String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();

                param.put("sign", signature);

                logger.info("robotId" + id + "----" + "挂单参数：" + JSONObject.fromObject(param));

                trade = httpUtil.post(baseUrl + "/private/trade/limit", (TreeMap<String, Object>) param);
                JSONObject jsonObject = JSONObject.fromObject(trade);
                if (0 != jsonObject.getInt("code")) {
                    setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
                }
                setTradeLog(id, "深度挂" + (type == 1 ? "卖" : "买") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
            }


        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }

        valid = 1;
        return trade;
    }


    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String typeStr = type == 1 ? "卖" : "买";
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price1 + "，amount(数量)：" + num);

        // 输出字符串

        String trade = "";
        Map<String, Object> param = new TreeMap<String, Object>();

        param.put("api_key", exchange.get("apikey"));
        param.put("amount", num);
        param.put("market", exchange.get("market"));
        param.put("price", price1);
        param.put("side", type);
        String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();

        param.put("sign", signature);

        logger.info("robotId" + id + "----" + "挂单参数：" + JSONObject.fromObject(param));

        try {
            trade = httpUtil.post(baseUrl + "/private/trade/limit", (TreeMap<String, Object>) param);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if (0 != jsonObject.getInt("code")) {
                setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        setTradeLog(id, "深度挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
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
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("api_key", exchange.get("apikey"));
        param.put("order_id", orderId);
        param.put("market", exchange.get("market"));
        param.put("offset", 0);
        param.put("limit", 100);


        String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();


        param.put("sign", signature);
        String trades = httpUtil.post(baseUrl + "/private/order/deals", (TreeMap<String, Object>) param);
        JSONObject jsonObject = JSONObject.fromObject(trades);
        if (0 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
        }
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

        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("api_key", exchange.get("apikey"));
        param.put("order_id", orderId);
        param.put("market", exchange.get("market"));


        String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();
        param.put("sign", signature);
        String trades = httpUtil.post(baseUrl + "/private/trade/cancel", (TreeMap<String, Object>) param);
        JSONObject jsonObject = JSONObject.fromObject(trades);
        if (0 != jsonObject.getInt("code")&&20010 != jsonObject.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", jsonObject.getString("message"));
        }
        return trades;
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

        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_ZG);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean flag = false;
        String rt = httpUtil.get("https://www.ztb.im/api/v1/exchangeInfo");

        JSONArray array = JSONArray.fromObject(rt);

        for (int i = 0; i < array.size(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);

            if (jsonObject.getString("symbol").equals(exchange.get("market").toUpperCase())) {
                String pricePrecision = jsonObject.getString("baseAssetPrecision");
                String amountPrecision = jsonObject.getString("quoteAssetPrecision");

                precision.put("pricePrecision", exchange.get("pricePrecision"));
                precision.put("amountPrecision", exchange.get("amountPrecision"));
                /*-------------------------------*/
                precision.put("exRate", 0.002);
                precision.put("minTradeLimit", exchange.get("minTradeLimit"));
                flag = true;
            }
        }
        return flag;
    }


    private String sign(Map<String, Object> params) {
        StringBuilder param = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            param.append(entry.getKey() + "=" + entry.getValue() + "&");
        }

        param.append("secret_key=" + exchange.get("tpass"));

        return param.toString();
    }


    //查询所有未成交订单
    public String selectPendingOrder() throws UnsupportedEncodingException {
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("api_key", exchange.get("apikey"));
        param.put("market", exchange.get("market"));
        param.put("offset", 0);
        param.put("limit", 100);


        String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();


        param.put("sign", signature);
        String trades = httpUtil.post(baseUrl + "/private/order/pending", (TreeMap<String, Object>) param);

        return trades;
    }

    //查询所有成交订单

    public String selectFilledOrder() throws UnsupportedEncodingException {
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("api_key", exchange.get("apikey"));
        param.put("market", exchange.get("market"));
        param.put("offset", 0);
        param.put("limit", 100);


        String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();


        param.put("sign", signature);
        String trades = httpUtil.post(baseUrl + "/private/order/pending", (TreeMap<String, Object>) param);

        return trades;
    }


    //获取余额
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


            Map<String, Object> param = new TreeMap<String, Object>();
            param.put("api_key", exchange.get("apikey"));


            String signature = DigestUtils.md5Hex(sign(param)).toUpperCase();

            param.put("sign", signature);
            String res = httpUtil.post(baseUrl + "/private/user", (TreeMap<String, Object>) param);

            JSONObject obj = judgeRes(res, "code", "setBalanceRedis");
            if (obj != null && obj.getInt("code") == 0) {
                JSONObject data = obj.getJSONObject("result");
                JSONObject firstCoin = data.getJSONObject(coinArr.get(0).toUpperCase());
                String firstBalance = firstCoin.getString("available");
                String firstBalancefreeze = firstCoin.getString("freeze");

                JSONObject lastCoin = data.getJSONObject(coinArr.get(1).toUpperCase());
                String lastBalance = lastCoin.getString("available");
                String lastBalancefreeze = lastCoin.getString("freeze");
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance + "_" + firstBalancefreeze);
                balances.put(coinArr.get(1), lastBalance + "_" + lastBalancefreeze);
                logger.info("获取余额："+coinArr.get(0)+ ":"+firstBalance + "_" + firstBalancefreeze+"--"+coinArr.get(1)+":"+lastBalance + "_" + lastBalancefreeze);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }
        }
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type==1?2:1 ,price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("result").getString("id");
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
        HashMap<String, Integer> map = new HashMap<>();
        String[] split = orderId.split(",");
        for (String s : split) {
            map.put(s, TradeEnum.NOTRADE.getStatus());
        }
        return map;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = null;
        try {
            cancelTrade = cancelTrade(orderId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(cancelTrade);
        if (jsonObject.getInteger("code") == 0) {
            return "true";
        } else {
            return "false";
        }
    }

}
