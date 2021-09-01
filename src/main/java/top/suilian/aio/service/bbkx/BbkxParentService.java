package top.suilian.aio.service.bbkx;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class BbkxParentService extends BaseService {
    public String baseUrl = "https://openapi.bbkx.com";

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
     */
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {

        String typeStr = type == 0 ? "买" : "卖";
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
                Map<String, Object> param = new TreeMap<String, Object>();
                param.put("side", type == 0 ? "BUY" : "SELL");
                param.put("type", "1");
                param.put("volume", String.valueOf(amount));
                param.put("price", String.valueOf(price));
                param.put("symbol", exchange.get("market"));
                param.put("api_key", exchange.get("apikey"));
                param.put("time", String.valueOf(new Date().getTime()));
                String tpass = exchange.get("tpass");
                String deploy = deploy(param) + tpass;
                String sign = HMAC.MD5(deploy);
                param.put("sign", sign);
                trade = httpUtil.post(baseUrl + "/open/api/create_order", param);
                setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
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
        String time = String.valueOf(new Date().getTime());
        String trade = null;
        Map<String, Object> map = new TreeMap<>();
        map.put("order_id",orderId);
        map.put("symbol",exchange.get("market"));
        map.put("api_key", exchange.get("apikey"));
        map.put("time", time);
        String deploy = deploy(map)+ exchange.get("tpass");
        String sign = HMAC.MD5(deploy);
        trade = httpUtil.get(baseUrl + "/open/api/order_info?order_id=" + orderId + "&symbol=" + exchange.get("market")+"&api_key="+exchange.get("apikey")+"&time="+time+"&sign="+sign);
        return trade;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        String time = String.valueOf(new Date().getTime());
        Map<String, Object> map = new TreeMap<>();
        map.put("api_key", exchange.get("apikey"));
        map.put("time", time);
        String deploy = deploy(map)+ exchange.get("tpass");
        String sign = HMAC.MD5(deploy);
        String trade = httpUtil.get(baseUrl + "/open/api/user/account?api_key="+exchange.get("apikey")+"&time="+time+"&sign="+sign);
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
        String trade;
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("order_id",orderId);
        param.put("symbol", exchange.get("market"));
        param.put("api_key", exchange.get("apikey"));
        param.put("time", String.valueOf(new Date().getTime()));
        String tpass = exchange.get("tpass");
        String deploy = deploy(param) + tpass;
        String sign = HMAC.MD5(deploy);
        param.put("sign", sign);
        trade = httpUtil.post(baseUrl + "/open/api/cancel_order", param);
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
            if (rt != null) {
                JSONObject jsonObject1 = judgeRes(rt, "data", "getBalance");
                coinLists = jsonObject1.getJSONObject("data").getJSONArray("coin_list");
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
        JSONObject cancelRess = judgeRes(res, "code", "cancelTrade");

        if (cancelRes != null && "0".equals(cancelRess.getString("code"))) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BBKX);
    }


    public String deploy(Map<String, Object> map) {
        String str = "";
        Iterator iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            str += (String) entry.getKey() + (String) entry.getValue();

        }
        return str;
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/open/api/common/symbols");
        JSONObject jsonObject1 = judgeRes(rt, "code", "getPrecision");


        if (jsonObject1 != null && "0".equals(jsonObject1.get("code"))) {
            JSONArray jsonArray = JSONArray.fromObject(jsonObject1.get("data"));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("symbol").equals(exchange.get("market"))) {
                    String amountPrecision = jsonObject.getString("amount_precision");
                    String pricePrecision = jsonObject.getString("price_precision");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    String minTradeLimit = exchange.get("minTradeLimit");
                    precision.put("minTradeLimit", minTradeLimit);
                    falg = true;
                }
            }

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }



}
