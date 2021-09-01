package top.suilian.aio.service.qb;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class QbParentService extends BaseService {
    public String baseUrl = "https://api2.qb.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];

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


                    Map<String, Object> params = new TreeMap<String, Object>();
                    params.put("symbol", exchange.get("market"));
                    params.put("type", "1");
                    if (type == 1) {
                        params.put("side", "BUY");
                    } else {
                        params.put("side", "SELL");
                    }
                    params.put("volume", num);
                    params.put("price", price1);
                    params.put("api_key", exchange.get("apikey"));
                    params.put("time", timestamp);

                    String toSign = toSort(params) + exchange.get("tpass");

                    String sign = DigestUtils.md5Hex(toSign);
                    params.put("sign", sign);


                    logger.info("robotId" + id + "----" + "挂单参数：" + params);

                    trade = httpUtil.post(baseUrl + "/open/api/create_order", params);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, Object> params = new TreeMap<String, Object>();
                params.put("symbol", exchange.get("market"));
                params.put("type", "1");
                if (type == 1) {
                    params.put("side", "BUY");
                } else {
                    params.put("side", "SELL");
                }
                params.put("volume", num);
                params.put("price", price1);
                params.put("api_key", exchange.get("apikey"));
                params.put("time", timestamp);

                String toSign = toSort(params) + exchange.get("tpass");

                String sign = DigestUtils.md5Hex(toSign);
                params.put("sign", sign);


                logger.info("robotId" + id + "----" + "挂单参数：" + params);

                trade = httpUtil.post(baseUrl + "/open/api/create_order", params);


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


    /**
     * 下单
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {

        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }
                Map<String, Object> param = new TreeMap<>();
                param.put("signV", "1");
                param.put("ts", String.valueOf((new Date().getTime()) / 1000));
                param.put("accessKey", exchange.get("apikey"));
                param.put("symbol", exchange.get("market"));
                param.put("price", price);
                param.put("amount", amount);
                param.put("type", "limit");
                param.put("side", type == 1 ? "buy" : "sell");
                String signs = toSort(param);
                String sign = HMAC.sha256_HMAC1(signs, exchange.get("tpass"));
                param.put("sign", sign);

                trade = httpUtil.post(baseUrl + "/api/v1/order/place", param);
                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId:" + id + "挂单成功结束：" + trade);
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


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        String trade = null;

        Map<String, Object> param = new TreeMap<>();
        param.put("signV", "1");
        param.put("ts", String.valueOf((new Date().getTime()) / 1000));
        param.put("accessKey", exchange.get("apikey"));
        param.put("symbol", exchange.get("market"));
        param.put("orderId", orderId);
        String signs = toSort(param);
        String sign = HMAC.sha256_HMAC1(signs, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/v1/order/detail?" + signs + "&sign=" + sign);
        return trade;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        String trade = null;

        Map<String, Object> param = new TreeMap<>();
        param.put("signV", "1");
        param.put("ts", String.valueOf((new Date().getTime()) / 1000));
        param.put("accessKey", exchange.get("apikey"));
        String signs = toSort(param);
        String sign = HMAC.sha256_HMAC1(signs, exchange.get("tpass"));
        trade = httpUtil.get(baseUrl + "/api/v1/account/assets?" + signs + "&sign=" + sign);
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
        Map<String, Object> param = new TreeMap<>();
        param.put("signV", "1");
        param.put("ts", String.valueOf((new Date().getTime()) / 1000));
        param.put("accessKey", exchange.get("apikey"));
        param.put("symbol", exchange.get("market"));
        param.put("orderId", orderId);
        String signs = toSort(param);
        String sign = HMAC.sha256_HMAC1(signs, exchange.get("tpass"));
        param.put("sign", sign);

        try {
            trade = httpUtil.post(baseUrl + "/api/v1/order/cancel", param);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
            JSONObject obj = JSONObject.fromObject(rt);
            if(obj!=null&&obj.getJSONObject("result")!=null){
                JSONObject result = obj.getJSONObject("result");
                if ("ok".equals(obj.getString("status"))) {
                    coinLists = result.getJSONArray("data");

                    String firstBalance = null;
                    String lastBalance = null;


                    for (int i = 0; i < coinLists.size(); i++) {
                        JSONObject jsonObject = coinLists.getJSONObject(i);

                        if (jsonObject.getString("currency").equals(coinArr.get(0))) {
                            firstBalance = jsonObject.getString("avail");
                        } else if (jsonObject.getString("currency").equals(coinArr.get(1))) {
                            lastBalance = jsonObject.getString("avail");
                        }
                    }
                    HashMap<String, String> balances = new HashMap<>();
                    balances.put(coinArr.get(0), firstBalance);
                    balances.put(coinArr.get(1), lastBalance);
                    redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
                }
            }else {
                logger.info("获取余额失败："+obj);
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
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_QB);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/api/v1/common/symbols");

        JSONObject rtObj = judgeRes(rt, "result", "setPrecision");

        if (!rt.equals("") && rtObj != null) {
            JSONObject result = rtObj.getJSONObject("result");
            JSONArray jsonArray = result.getJSONArray("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("symbol").equals(exchange.get("market"))) {
                    String amountPrecision = jsonObject.getString("amountPrecision");
                    String pricePrecision = jsonObject.getString("pricePrecision");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", exchange.get("numMinThreshold"));
                    falg = true;
                }
            }

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
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
