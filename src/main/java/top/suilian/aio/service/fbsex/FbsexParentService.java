package top.suilian.aio.service.fbsex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class FbsexParentService extends BaseService {
    public String baseUrl = "https://api.fbsex.co";

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
     * 下单
     *
     * @param type
     * @param price
     * @param amount
     * @return
     * @throws UnsupportedEncodingException
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {

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

                    Map<String,String> param =new TreeMap<>();
                    HashMap<String,String> header = new HashMap<>();
                    param.put("pair",exchange.get("market"));
                    param.put("direction",type==1?"BID":"ASK");
                    param.put("price",String.valueOf(price1));
                    param.put("amount",String.valueOf(num));
                    String sort=toSort(param);
                    String sign= HMAC.sha256_HMAC(sort,exchange.get("tpass"));
                    header.put("X_ACCESS_KEY",exchange.get("apikey"));
                    header.put("X_SIGNATURE",sign);
                    header.put("Cache-Control","no-cache");
                    try {
                        trade=httpUtil.post(baseUrl+"/v1/u/trade/order/create",param,header);
                        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {

                Map<String, String> param = new TreeMap<>();
                HashMap<String, String> header = new HashMap<>();
                param.put("pair", exchange.get("market"));
                param.put("direction", type == 1 ? "BID" : "ASK");
                param.put("price", String.valueOf(price1));
                param.put("amount", String.valueOf(num));
                String sort = toSort(param);
                String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
                header.put("X_ACCESS_KEY", exchange.get("apikey"));
                header.put("X_SIGNATURE", sign);
                header.put("Cache-Control", "no-cache");
                try {
                    trade= httpUtil.post(baseUrl + "/v1/u/trade/order/create", param, header);
                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

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


    public String selectOrder(String orderId, String direction) throws UnsupportedEncodingException {
        String rs = null;
        String updateTime = String.valueOf(new Date().getTime());
        Map<String, String> param = new TreeMap<>();
        HashMap<String, String> header = new HashMap<>();
        param.put("updateTime", updateTime);
        param.put("orderNo", orderId);
        param.put("direction", direction);
        param.put("pair", exchange.get("market"));
        String sort = toSort(param);
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        header.put("X_ACCESS_KEY", exchange.get("apikey"));
        header.put("X_SIGNATURE", sign);
        header.put("Cache-Control", "no-cache");
        rs = httpUtil.getAddHead(baseUrl + "/v1/u/trade/order/finished/detail?direction=" + direction + "&pair=" + exchange.get("market") + "&orderNo=" + orderId + "&updateTime=" + updateTime, header);
        logger.info("查询订单完成：" + rs);
        return rs;

    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId, String direction) throws UnsupportedEncodingException {

        Map<String, String> param = new TreeMap<>();
        HashMap<String, String> header = new HashMap<>();
        param.put("pair", exchange.get("market"));
        param.put("orderNo", orderId);
        param.put("direction", direction);
        String sort = toSort(param);
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        header.put("X_ACCESS_KEY", exchange.get("apikey"));
        header.put("X_SIGNATURE", sign);
        header.put("Cache-Control", "no-cache");
        String rs = httpUtil.post(baseUrl + "/v1/u/trade/order/cancel", param, header);
        logger.info("撤单完成：" + rs);
        return rs;
    }


    /**
     * 获取余额
     */

    public String getBalance(String coinTypes) {
        Map<String, String> param = new TreeMap<>();
        HashMap<String, String> header = new HashMap<>();
        param.put("coinTypes", coinTypes);
        String sort = toSort(param);
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        header.put("X_ACCESS_KEY", exchange.get("apikey"));
        header.put("X_SIGNATURE", sign);
        header.put("Cache-Control", "no-cache");
        String rs = httpUtil.getAddHead(baseUrl + "/v1/u/wallet/balance?coinTypes=" + coinTypes, header);
        logger.info("查询余额完成：" + rs);
        return rs;

    }

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
            String firstBalance = null;
            String lastBalance = null;
            String res1 = getBalance(coinArr.get(0));
            JSONObject obj1 = JSONObject.fromObject(res1);
            if (obj1 != null && obj1.getInt("code") == 0) {
                JSONArray data = obj1.getJSONArray("data");
                firstBalance = data.getJSONObject(0).get("available").toString();
            }
            String res2 = getBalance(coinArr.get(1));
            JSONObject obj2 = JSONObject.fromObject(res2);
            if (obj2 != null && obj2.getInt("code") == 0) {
                JSONArray data = obj2.getJSONArray("data");
                lastBalance = data.getJSONObject(0).get("available").toString();
            }
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
        if (cancelRes != null && cancelRes.getInt("code") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_FCHAIN);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        Boolean flog = false;
        String symbols = exchange.get("market");
        String rs = httpUtil.get(baseUrl + "/v1/exchangeInfo");
        JSONObject jsonObject = JSONObject.fromObject(rs);
        if (jsonObject != null && jsonObject.getInt("code") == 0) {
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray pairs = data.getJSONArray("pairs");
            for (int i = 0; i < pairs.size(); i++) {
                if (pairs.getJSONObject(i).getString("pair").equals(symbols)) {
                    precision.put("amountPrecision", exchange.get("amountPrecision"));
                    precision.put("pricePrecision", pairs.getJSONObject(i).getString("pricePrecision"));
                    precision.put("minTradeLimit", pairs.getJSONObject(i).getString("minimumTradeAmount"));
                    flog = true;
                    break;
                }
            }
        } else {
            System.out.println("交易规则接口异常");
            flog = false;
        }
        return flog;
    }

    //获取深度
    public  String getDepth(){
        String symbol=exchange.get("market");
        String depth=httpUtil.get(baseUrl+"/v1/q/depth?pair="+symbol);
        System.out.println(depth);
        return depth;
    }


    private static String toSort(Map<String, String> map) {
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
