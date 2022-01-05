package top.suilian.aio.service.bthex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

public class BthexParentService extends BaseService {
    public String baseUrl = "http://www.bthex.pro/appApi.json";

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
        String timestamp = getSecondTimestamp(new Date());

        String typeStr = type == 0 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));


        Double minTradeLimit = Double.valueOf(String.valueOf("0.0001"));
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
                    params.put("type", type);
                    params.put("api_key", exchange.get("apikey"));
                    params.put("amount", num);
                    params.put("price", price1);

                    String toSign = toSort(params) + "&secret_key=" + exchange.get("tpass");

                    String sign = DigestUtils.md5Hex(toSign).toUpperCase();
                    params.put("sign", sign);


                    logger.info("robotId" + id + "----" + "挂单参数：" + params);

                    trade = httpUtil.post(baseUrl + "?action=trade", params);

                  //  setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 0 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, Object> params = new TreeMap<String, Object>();
                params.put("api_key", exchange.get("apikey"));
                params.put("symbol", exchange.get("market"));
                params.put("type", type);

                params.put("amount", num);
                params.put("price", price1);

                String toSign = toSort(params) + "&secret_key=" + exchange.get("tpass");

                String sign = DigestUtils.md5Hex(toSign);
                params.put("sign", sign);


                logger.info("robotId" + id + "----" + "挂单参数：" + params);

                trade = httpUtil.post(baseUrl + "?action=trade", params);


                setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 0 ? "05cbc8" : "ff6224");
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
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */

    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("id", orderId);
        String toSign = toSort(params) + "&secret_key=" + exchange.get("tpass");
        String sign = DigestUtils.md5Hex(toSign).toUpperCase();
        params.put("sign", sign);
        return httpUtil.post(baseUrl + "?action=order", params);
    }


    /**
     * 获取余额
     */


    protected String getBalance() throws UnsupportedEncodingException {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        String toSign = toSort(params) + "&secret_key=" + exchange.get("tpass");
        String sign = DigestUtils.md5Hex(toSign).toUpperCase();
        params.put("sign", sign);
        return httpUtil.post(baseUrl + "?action=userinfo", params);
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("id", orderId);
        String toSign = toSort(params) + "&secret_key=" + exchange.get("tpass");
        String sign = DigestUtils.md5Hex(toSign).toUpperCase();
        params.put("sign", sign);
        return httpUtil.post(baseUrl + "?action=cancel_entrust", params);
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
            logger.info("getBalance:" + rt);
            JSONObject obj = JSONObject.fromObject(rt);
            JSONObject coin = obj.getJSONObject("data").getJSONObject("free");
            String firstBalance = null;
            String lastBalance = null;


            firstBalance = coin.getString(coinArr.get(0).toUpperCase());

            lastBalance = coin.getString(coinArr.get(1).toUpperCase());


            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance);
            balances.put(coinArr.get(1), lastBalance);
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
        if (cancelRes != null && cancelRes.getInt("code") == 200) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));

//        precision.put("amountPrecision","4" );
//        precision.put("pricePrecision", "4");
//        precision.put("minTradeLimit", "0.1");

    }

    public static String toSort(Map<String, Object> params) {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(value).append("&");
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }

    private static String splicingMap(String url, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
            int x = 1;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (x == 1) {
                    url = url + "?";
                } else {
                    url = url + "&";
                }
                url += entry.getKey() + "=" + String.valueOf(entry.getValue());
                x++;
            }
        }
        return url;
    }

    /**
     * 获取精确到秒的时间戳
     *
     * @return
     */
    public String getSecondTimestamp(Date date) {
        if (null == date) {
            return "";
        }
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        if (length > 3) {
            return String.valueOf(timestamp.substring(0, length - 3));
        } else {
            return "";
        }
    }

}
