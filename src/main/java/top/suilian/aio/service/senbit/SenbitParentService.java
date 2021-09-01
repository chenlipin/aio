package top.suilian.aio.service.senbit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

public class SenbitParentService extends BaseService {

    public String baseUrl = "https://www.senbit.com";

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
        String timestape =getTime();
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


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

                    Map<String, String> param = new TreeMap<>();
                    Map<String, String> param1 = new TreeMap<>();
                    HashMap<String, String> header = new HashMap();

                    if (type == 1) {
                        param1.put("type", "buy");
                    } else {
                        param1.put("type", "sell");
                    }
                    try {
                        param.put("access", exchange.get("apikey"));
                        param.put("_", timestape);
                        param.put("path", URLEncoder.encode("/api/x/v1/order/order", "UTF-8"));
                        param.put("method", "POST");
                        param1.put("symbol", exchange.get("market"));
                        param1.put("price", String.valueOf(price1));

                        param1.put("amount", String.valueOf(num));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    String signs = toSort1(param);
                    String signature = HMAC.sha256_HMAC(signs, exchange.get("tpass"));

                    try {
                        trade = httpUtil.post(baseUrl + "/api/x/v1/order/order?access=" + exchange.get("apikey") + "&_=" + timestape + "&sign=" + signature, param1, header);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId:" + id + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {

                Map<String, String> param = new TreeMap<>();
                Map<String, String> param1 = new TreeMap<>();
                HashMap<String, String> header = new HashMap();

                if (type == 1) {
                    param1.put("type", "buy");
                } else {
                    param1.put("type", "sell");
                }
                try {
                    param.put("access", exchange.get("apikey"));
                    param.put("_", timestape);
                    param.put("path", URLEncoder.encode("/api/x/v1/order/order", "UTF-8"));
                    param.put("method", "POST");
                    param1.put("symbol", exchange.get("market"));
                    param1.put("price", String.valueOf(price1));

                    param1.put("amount", String.valueOf(num));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String signs = toSort1(param);
                String signature = HMAC.sha256_HMAC(signs, exchange.get("tpass"));

                try {
                    trade = httpUtil.post(baseUrl + "/api/x/v1/order/order?access=" + exchange.get("apikey") + "&_=" + timestape + "&sign=" + signature, param1, header);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId:" + id + "挂单成功结束：" + trade);
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
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));


        String timestape =getTime();
        Map<String, String> param = new TreeMap<>();
        Map<String, String> param1 = new TreeMap<>();
        HashMap<String, String> header = new HashMap();
        if (type == 1) {
            param1.put("type", "buy");
        } else {
            param1.put("type", "sell");
        }
        try {
            param.put("access", exchange.get("apikey"));
            param.put("_", timestape);
            param.put("path", URLEncoder.encode("/api/x/v1/order/order", "UTF-8"));
            param.put("method", "POST");
            param1.put("symbol", exchange.get("market"));
            param1.put("price", String.valueOf(price1));
            param1.put("amount", String.valueOf(num));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String signs = toSort1(param);
        String signature = HMAC.sha256_HMAC(signs, exchange.get("tpass"));

        try {
            trade = httpUtil.post(baseUrl + "/api/x/v1/order/order?access=" + exchange.get("apikey") + "&_=" + timestape + "&sign=" + signature, param1, header);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        //logger.info("robotId:" + id + "挂单成功结束：" + trade);


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
        String timestamp = getTime();

        Map<String, Object> param = new TreeMap<>();
        param.put("access", exchange.get("apikey"));
        param.put("_", timestamp);
        String params=toSort(param);
        param.put("method","GET");
        try {
            param.put("path",URLEncoder.encode("/api/x/v1/order/order/"+orderId,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = toSort(param);
        String signature = HMAC.sha256_HMAC(sign, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/x/v1/order/order/"+orderId+"?"+params+"&sign="+signature);

        return trade;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {

        String trade = null;
        String timestamp = getTime();
        Map<String, Object> param = new TreeMap<>();
        param.put("access", exchange.get("apikey"));
        param.put("_", timestamp);
        String params = toSort(param);
        param.put("method", "GET");
        try {
            param.put("path", URLEncoder.encode("/api/x/v1/account/balance", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = toSort(param);
        String signature = HMAC.sha256_HMAC(sign, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/x/v1/account/balance?" + params + "&sign=" + signature);

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
        String params=null;
        String timestamp = getTime();
        HashMap<String,String> header = new HashMap<>();
        Map<String, Object> param = new TreeMap<>();
        param.put("access", exchange.get("apikey"));
        param.put("_", timestamp);

        try {
            param.put("symbol",URLEncoder.encode(exchange.get("market"),"UTF-8"));
            params=toSort(param);
            param.put("path",URLEncoder.encode("/api/x/v1/order/order/"+orderId,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        param.put("method","DELETE");
        String sign = toSort(param);
        System.out.println("加密參數："+sign);
        String signature = HMAC.sha256_HMAC(sign, exchange.get("tpass"));

        header.put("Content-Type","application/x-www-form-urlencoded");
        try {
            trade = httpUtil.delete(baseUrl + "/api/x/v1/order/order/"+orderId+"?"+params+"&sign="+signature,header);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("撤单结果："+trade);
        if(trade==null||"".equals(trade)){
            System.out.println("撤单成功");
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
            String firstBalance = null;
            String lastBalance = null;
            String rt = getBalance();
            JSONArray obj = JSONArray.fromObject(rt);
            if (obj != null && obj.size() > 0) {
                for (int i = 0; i < obj.size(); i++) {
                    if (coinArr.get(0).equals(obj.getJSONObject(i).getString("currency"))) {
                        firstBalance = obj.getJSONObject(i).getString("available");
                    } else if (coinArr.get(1).equals(obj.getJSONObject(i).getString("currency"))) {
                        lastBalance = obj.getJSONObject(i).getString("available");
                    }
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
        String params = null;
        String timestamp = getTime();

        Map<String, Object> param = new TreeMap<>();
        param.put("access", exchange.get("apikey"));
        param.put("_", timestamp);
        try {
            param.put("symbol", URLEncoder.encode(exchange.get("market"), "UTF-8"));
            params = toSort(param);
            param.put("method", "GET");
            param.put("path", URLEncoder.encode("/api/x/v1/market/depth", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = toSort(param);
        String signature = HMAC.sha256_HMAC(sign, exchange.get("tpass"));

        trade = httpUtil.get(baseUrl + "/api/x/v1/market/depth?" + params + "&sign=" + signature);
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
        if (cancelRes != null && cancelRes.getInt("code") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_QB);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {

        boolean falg = false;
        String params = null;
        String timestamp = getTime();

        Map<String, Object> param = new TreeMap<>();
        param.put("access", exchange.get("apikey"));
        param.put("_", timestamp);
        param.put("_t",5000);
        params = toSort(param);
        param.put("method", "GET");
        try {
            param.put("path", URLEncoder.encode("/api/x/v1/common/symbols", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign = toSort(param);
        System.out.println("加密參數：" + sign);
        String signature = HMAC.sha256_HMAC(sign, exchange.get("tpass"));
        String rt = httpUtil.get(baseUrl + "/api/x/v1/common/symbols?" + params + "&sign=" + signature);

        JSONArray rtObj = JSONArray.fromObject(rt);
        System.out.println("交易对规则" + rtObj);

        if (!rt.equals("") && rtObj != null && rtObj.size() > 0) {

            for (int i = 0; i < rtObj.size(); i++) {
                JSONObject jsonObject = rtObj.getJSONObject(i);
                if (jsonObject.getString("symbol").equals(exchange.get("market"))) {
                    String amountPrecision = String.valueOf(jsonObject.getInt("amountDecimal"));
                    String pricePrecision = String.valueOf(jsonObject.getInt("priceDecimal"));
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", exchange.get("numMinThreshold"));
                    logger.info("交易规则：" + precision);
                    falg = true;
                    logger.info("交易规则状态码："+falg);
                    break;
                }
            }

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        logger.info("交易规则状态码："+falg);
        return falg;
    }

    public String getTime() {
        String time = httpUtil.get(baseUrl + "/api/x/v1/common/timestamp");
        JSONObject timeJson = JSONObject.fromObject(time);
        return timeJson.getString("ms");
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

    private static String toSort1(Map<String, String> map) {
        StringBuffer buffer = new StringBuffer();

        int i = 0;
        int max = map.size() - 1;
        for (Map.Entry<String, String> entry : map.entrySet()) {

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
