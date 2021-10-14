package top.suilian.aio.service.wbfex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class WbfexParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://openapi.wbf.info";

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

                    trade = HttpUtil.post(baseUrl + "/open/api/create_order", params);

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

                trade = HttpUtil.post(baseUrl + "/open/api/create_order", params);


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

    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {

        String timestamp = String.valueOf(new Date().getTime());


        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("symbol", exchange.get("market"));
        params.put("type", "1");
        if (type == 1) {
            params.put("side", "BUY");
        } else {
            params.put("side", "SELL");
        }



        params.put("volume", amount);
        params.put("price", price);
        params.put("api_key", exchange.get("apikey"));
        params.put("time", timestamp);

        String toSign = toSort(params) + exchange.get("tpass");

        String sign = DigestUtils.md5Hex(toSign);
        params.put("sign", sign);


        logger.info("robotId" + id + "----" + "挂单参数：" + params);

        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/open/api/create_order", params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price + ": 数量" + amount + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
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



    public String selectOrder(String orderId)   {

        String timestamp = String.valueOf(new Date().getTime());

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("symbol", exchange.get("market"));
        params.put("order_id", orderId);
        params.put("api_key", exchange.get("apikey"));
        params.put("time", timestamp);
        String toSign = toSort(params) + exchange.get("tpass");
        String sign = DigestUtils.md5Hex(toSign);
        params.put("sign", sign);
        String url = splicingMap(baseUrl + "/open/api/order_info", params);
        String trade = httpUtil.get(url);
        return trade;
    }


    protected String getTradeOrders() {
        String timestamp = String.valueOf(new Date().getTime());

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("symbol", exchange.get("market"));
        params.put("api_key", exchange.get("apikey"));
        params.put("pageSize", "100");
        params.put("time", timestamp);
        String toSign = toSort(params) + exchange.get("tpass");
        String sign = DigestUtils.md5Hex(toSign);
        params.put("sign", sign);
        String url = splicingMap(baseUrl + "/open/api/v2/new_order", params);
        String tradeOrders = httpUtil.get(url);
        return tradeOrders;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        String time = String.valueOf(new Date().getTime());
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("time", time);
        String toSign = toSort(params) + exchange.get("tpass");

        String sign = DigestUtils.md5Hex(toSign);
        params.put("sign", sign);
        String url = splicingMap(baseUrl + "/open/api/user/account", params);

        String rt = httpUtil.get(url);

        return rt;
    }

    /**
     * 撤单
     *
     */

    public String cancelTrade(String orderId)  {

        String timestamp = String.valueOf(new Date().getTime());

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("symbol", exchange.get("market"));
        params.put("order_id", orderId);
        params.put("api_key", exchange.get("apikey"));
        params.put("time", timestamp);

        String toSign = toSort(params) + exchange.get("tpass");

        String sign = DigestUtils.md5Hex(toSign);
        params.put("sign", sign);


        String res = null;
        try {
            res = HttpUtil.post(baseUrl + "/open/api/cancel_order", params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
            JSONObject obj = JSONObject.fromObject(rt);
            logger.info("getBalance====================>"+rt);
            if(obj!=null&&"0".equals(obj.getString("code"))){
                JSONObject data = obj.getJSONObject("data");
                JSONArray coinLists = data.getJSONArray("coin_list");
                String firstBalance = null;
                String lastBalance = null;


                for (int i = 0; i < coinLists.size(); i++) {
                    JSONObject jsonObject = coinLists.getJSONObject(i);

                    if (jsonObject.getString("coin").equals(coinArr.get(0))) {
                        firstBalance = jsonObject.getString("normal");
                    } else if (jsonObject.getString("coin").equals(coinArr.get(1))) {
                        lastBalance = jsonObject.getString("normal");
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }else {
                logger.info("获取余额失败:"+obj);
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
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/open/api/common/symbols");

        JSONObject rtObj = judgeRes(rt, "symbol", "setPrecision");

        if (!rt.equals("") && rtObj != null) {
            JSONArray jsonArray = rtObj.getJSONArray("data");

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("symbol").equals(exchange.get("market"))) {
                    String amountPrecision = jsonObject.getString("amount_precision");

                    String pricePrecision = jsonObject.getString("price_precision");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", exchange.get("minTradeLimit"));
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

        for (Map.Entry<String, Object> entry :
                map.entrySet()) {
            /** 去掉签名字段 */
            if (entry.getKey().equals("sign")) {
                continue;
            }

            /** 空参数不参与签名 */
            if (entry.getValue() != null) {
                buffer.append(entry.getKey());
                buffer.append(entry.getValue().toString());
            }

        }
        return buffer.toString();
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

    @Override
    public  Map<String,String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        return null;
    }

    @Override
    public Map<String,Integer> selectOrderStr(String orderId) {
        return null;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        return null;
    }
}
