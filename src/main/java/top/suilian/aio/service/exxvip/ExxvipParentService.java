package top.suilian.aio.service.exxvip;


import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import java.util.*;

public class ExxvipParentService extends BaseService {

    public String baseUrl = "https://trade.exxvip.com";

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
        String timestape = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;
        String parms = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }

                    Map<String, Object> param = new TreeMap<>();

                    if (type == 1) {
                        param.put("type", "buy");
                    } else {
                        param.put("type", "sell");
                    }
                    param.put("accesskey", exchange.get("apikey"));
                    param.put("amount", num);
                    param.put("currency", exchange.get("market"));
                    param.put("nonce", timestape);
                    param.put("price", price1);

                    parms = toSort(param);
                    String signature = HMAC.Hmac_SHA512(parms, exchange.get("tpass"));

                    trade = httpUtil.post(baseUrl + "/api/order?",parms+"&signature="+signature);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");


                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {

                Map<String, Object> param = new TreeMap<>();

                if (type == 1) {
                    param.put("type", "buy");
                } else {
                    param.put("type", "sell");
                }
                param.put("accesskey", exchange.get("apikey"));
                param.put("amount", num);
                param.put("currency", exchange.get("market"));
                param.put("nonce", timestape);
                param.put("price", price1);

                parms = toSort(param);
                String signature = HMAC.Hmac_SHA512(parms, exchange.get("tpass"));

                trade = httpUtil.post(baseUrl + "/api/order?",parms+"&signature="+signature);

                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");

            }


        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }

        valid = 1;
        return trade;

    }


    /**
     * 下单
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {

        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        String trade = null;
        String parms = null;
        String timestape = String.valueOf(new Date().getTime());
        Map<String, Object> param = new TreeMap<>();

        if (type == 1) {
            param.put("type", "buy");
        } else {
            param.put("type", "sell");
        }
        param.put("accesskey", exchange.get("apikey"));
        param.put("amount", num);
        param.put("currency", exchange.get("market"));
        param.put("nonce", timestape);
        param.put("price", price1);
        parms = toSort(param);

        String signature = HMAC.Hmac_SHA512(parms, exchange.get("tpass"));
        trade = httpUtil.post(baseUrl + "/api/order?",parms+"&signature="+signature);

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");


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
        String timestamp = String.valueOf(new Date().getTime());
        Map<String, Object> param = new TreeMap<>();
        param.put("accesskey", exchange.get("apikey"));
        param.put("nonce", timestamp);
        param.put("currency",exchange.get("market"));
        param.put("id",orderId);
        String params = toSort(param);
        String sign = toSort(param);

        String signature = HMAC.Hmac_SHA512(sign, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/getOrder?" + params + "&signature=" + signature);

        return trade;
    }


    /**
     * 获取余额
     */


    public  String getBalance() {
        String trade = null;
        String timestamp = String.valueOf(new Date().getTime());
        Map<String, Object> param = new TreeMap<>();
        param.put("accesskey",exchange.get("apikey"));
        param.put("nonce", timestamp);
        String params = toSort(param);

        String signature = HMAC.Hmac_SHA512(params, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/getBalance?" + params + "&signature=" + signature);
       logger.info("资产详情" + trade);

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

        String trade = null;
        String timestamp = String.valueOf(new Date().getTime());
        HashMap<String, String> header = new HashMap<>();
        Map<String, Object> param = new TreeMap<>();
        param.put("accesskey", exchange.get("apikey"));
        param.put("currency", exchange.get("market"));
        param.put("nonce",timestamp);
        param.put("id",orderId);
        String parms = toSort(param);

        String signature = HMAC.Hmac_SHA512(parms, exchange.get("tpass"));

        trade = httpUtil.get(baseUrl + "/api/cancel?" + parms + "&signature="+ signature);

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
            String firstBalance = null;
            String lastBalance = null;
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null && obj.getJSONObject("funds")!=null) {
                JSONObject funds = obj.getJSONObject("funds");
               if(funds.getJSONObject(coinArr.get(0).toUpperCase())!=null){
                   firstBalance =funds.getJSONObject(coinArr.get(0).toUpperCase()).getString("balance");
               }
               if (funds.getJSONObject(coinArr.get(1).toUpperCase())!=null){
                   lastBalance =funds.getJSONObject(coinArr.get(1).toUpperCase()).getString("balance");
               }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            } else {
                logger.info("获取余额失败：" + obj);
            }

        }
    }

    public String getDepth() {
        String trade = null;
        trade = httpUtil.get("https://api.exxvip.com/data/v1/depth?currency=" + exchange.get("market"));
        return trade;
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
        if (cancelRes != null && cancelRes.getInt("code") == 100) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_QB);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        Boolean flag = false;
        precision.put("pricePrecision",exchange.get("pricePrecision"));
        precision.put("amountPrecision",exchange.get("amountPrecision"));
        precision.put("minTradeLimit",exchange.get("numMinThreshold"));
        if(precision.size()>0){
            flag=true;
        }
        return flag;
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
