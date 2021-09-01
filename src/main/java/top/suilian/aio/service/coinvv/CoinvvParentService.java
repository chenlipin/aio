package top.suilian.aio.service.coinvv;

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

public class CoinvvParentService extends BaseService {
    public String baseUrl = "https://api.coinvv.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    public String coinProid=null;

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
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws Exception {
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
                    String orderType=null;

                    params.put("coin_proid", coinProid);
                    params.put("en_type", "2");
                    params.put("en_num", num);
                    params.put("en_price", price1);

                    if (type == 1) {
                      orderType= "/entrust_buy";
                    } else {
                      orderType= "/entrust_sell";
                    }


                    String sign=toSort(params);
                    params.put("sign", sign);


                    logger.info("robotId" + id + "----" + "挂单参数：" + params);

                    trade = httpUtil.sslPost(baseUrl + orderType, params);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {

                Map<String, Object> params = new TreeMap<String, Object>();
                String orderType=null;

                params.put("coin_proid", coinProid);
                params.put("en_type", "2");
                params.put("en_num", num);
                params.put("en_price", price1);

                if (type == 1) {
                    orderType= "/entrust_buy";
                } else {
                    orderType= "/entrust_sell";
                }


                String sign=toSort(params);
                params.put("sign", sign);


                logger.info("robotId" + id + "----" + "挂单参数：" + params);

                trade = httpUtil.sslPost(baseUrl + orderType, params);


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
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws Exception {
        Map<String, Object> param = new TreeMap<>();
        int id = Integer.parseInt(orderId);
        param.put("entrust_id",id);
        String sign=toSort(param);
        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/get_entrust_status",param);
        return res;
    }


    // 产品id

    public Boolean setCoinProid() throws  Exception {
        Boolean flag = false;
        Map<String, Object> param = new TreeMap<>();
        String[] strArr = exchange.get("market").split("/");
        param.put("base_coin",strArr[1]);
        String sign=toSort(param);

        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/get_product_list",param);
        JSONObject tradesObj = judgeRes(res, "id", "getCoinProid");

        if (!"".equals(res) && res != null && !res.isEmpty() && tradesObj != null&&"ok".equals(tradesObj.getString("status"))) {
            JSONObject data = tradesObj.getJSONObject("data");
            JSONObject tick = data.getJSONObject(strArr[0]);
            coinProid =String.valueOf(tick.getInt("id"));
            flag=true;
        }

        return flag;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws Exception {

        Map<String, Object> param = new TreeMap<>();
        param.put("en_id",orderId);
        String sign=toSort(param);
        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/cancle_en",param);
        return res;
    }


    /**
     *  深度信息
     */

    public String getDepth() throws Exception{
        Map<String, Object> param = new TreeMap<>();
        String step= "step0";
        param.put("symbol",exchange.get("market"));
        param.put("step",step);
        String sign=toSort(param);
        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/get_market_depth",param);
        return res;
    }


    /**
     * 获取余额
     */

    public void setBalanceRedis() throws Exception {
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



            String res2 = getCoinBalance(coinArr.get(1));
            String res = getCoinBalance(coinArr.get(0));
            JSONObject obj2 = JSONObject.fromObject(res2);
            JSONObject obj = JSONObject.fromObject(res);
            if(obj!=null&&obj2!=null&&obj.getJSONObject("data")!=null&&obj2.getJSONObject("data")!=null){
                JSONObject data = obj.getJSONObject("data");
                JSONObject data2 = obj2.getJSONObject("data");

                String firstBalance = data.getString("coin_yue");
                String lastBalance = data2.getString("coin_yue");
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }else {
                logger.info("获取余额失败："+obj);
            }

        }
    }

    public String getCoinBalance(String coinName) throws Exception {
        Map<String, Object> param = new TreeMap<>();
        param.put("coin_en",coinName);
        param.put("mobile",exchange.get("mobileKey"));
        String sign=toSort(param);
        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/get_yonghu_yue",param);
        return  res;
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
        if (cancelRes != null && cancelRes.getInt("code")==200) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_COINVV);
    }




    /**
     * 交易规则获取
     */
    public Boolean setPrecision() throws Exception {
        Boolean flag = false;
        Map<String, Object> param = new TreeMap<>();
        String symbol = exchange.get("market").replace("/", "_");
        param.put("symbol",symbol);
        String sign=toSort(param);
        param.put("sign",sign);
        String res = httpUtil.sslPost(baseUrl + "/get_trade_rules",param);


        JSONObject rtObj = judgeRes(res, "amount_precision", "setPrecision");

        if (!res.equals("") && rtObj != null &&"ok".equals(rtObj.getString("status"))) {
            JSONObject data = rtObj.getJSONObject("data");
            precision.put("amountPrecision", data.getString("amount_precision"));
            precision.put("pricePrecision", data.getString("price_precision"));
            precision.put("minTradeLimit", data.getString("min_amount"));
            flag=true;
        }
        return flag;
    }

    private String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();
        String key=exchange.get("apikey");
        int max = map.size() - 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (entry.getValue() != null) {
                buffer.append(entry.getKey() + "=");
                buffer.append(entry.getValue().toString() + "&");
            }

        }
        buffer.append("apikey" + "=");
        buffer.append(key);
        String signature = HMAC.MD5(HMAC.MD5(buffer.toString())).toUpperCase();
        return signature;
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
}
