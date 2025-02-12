package top.suilian.aio.service.arbisooNew;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.DateUtils;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class ArbisooNewParentService extends BaseService implements RobotAction {
    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];



    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    public String url = "https://api.huibit.com";

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
     {"data":"E173788581054944","code":0,"message":"success","totalPage":null,"totalElement":null}
     */
    protected String submitTrade(int type, BigDecimal price, BigDecimal amount) {
        // 输出字符串
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision"))).stripTrailingZeros();
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision"))).stripTrailingZeros();
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + (type==1?"buy":"sell") + "，price(价格)：" + price + "，amount(数量)：" + num);

        Map<String, String> params = new TreeMap<String, String>();
        params.put("direction", type==1?"BUY":"SELL");
        params.put("symbol", exchange.get("market"));
        params.put("type","LIMIT_PRICE");
        params.put("sign", HMAC.MD5("api_key=" + exchange.get("apikey") + "&api_name=" + exchange.get("tpass")));
        params.put("amount", String.valueOf(num));
        params.put("price", String.valueOf(price1));
        params.put("uid", exchange.get("tpass"));

        try {
             trade = HttpUtil.get("https://api.huibit.com/exchange/openApi/order/mockaddydhdnskd?"+sign(params));
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        System.out.println(rt);
        if (0 != rt.getInt("code")) {
            logger.info("robotId" + id + "----" + "挂单失败结束");
        } else {
            setTradeLog(id, "挂单成功结束-- type:" + (type == 1 ? "买" : "卖") + "[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");

        }
        return trade;
    }


    public String noOreder() {
        String trade = null;
        Map<String, String> params = new TreeMap<String, String>();
        params.put("api_key", exchange.get("apikey"));
        params.put("currencyPair", exchange.get("market"));
        String sign = sign(params);
        params.put("sign", sign);
        logger.info("robotId" + id + "----" + "撤去所有订单响应：" + params);
        try {
            trade = HttpUtil.doPostFormData(url + "private?command=cancelAllOrder", params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        if (200 != rt.getInt("code")) {
            setWarmLog(id, 3, "API ERROR", rt.getString("msg"));
            logger.info("robotId" + id + "----" + "撤去所有订单响应：" + trade);
        }
        return trade;
    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) {
        String trade = null;
        try {

            Map<String, String> params = new TreeMap<String, String>();

            params.put("orderId",orderId);
            params.put("sign", HMAC.MD5("api_key=" + exchange.get("apikey") + "&api_name=" + exchange.get("tpass")));
            params.put("uid", exchange.get("tpass"));
            trade = HttpUtil.get("https://api.huibit.com/exchange/openApi/order/details?"+sign(params));
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        if (0 != rt.getInt("code")) {
            setWarmLog(id, 3, "API ERROR", rt.getString("message"));
            logger.info("robotId" + id + "----" + "查询订单错误响应：" + trade);
        }
        return trade;
    }

    //查询所有订单详情
    @Override
    public List<getAllOrderPonse> selectOrder() {
        String trade = null;
        try {

            Map<String, String> params = new TreeMap<String, String>();

            params.put("symbol",exchange.get("market"));
            params.put("sign", HMAC.MD5("api_key=" + exchange.get("apikey") + "&api_name=" + exchange.get("tpass")));
            params.put("uid", exchange.get("tpass"));
            params.put("pageNo", "1");
            params.put("pageSize", "100");
            params.put("type","LIMIT_PRICE");
            trade = HttpUtil.get("https://api.huibit.com/exchange/openApi/order/personal/current?"+sign(params));
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade).getJSONObject("data");

        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();

        com.alibaba.fastjson.JSONArray array = com.alibaba.fastjson.JSONArray.parseArray(rt.getString("content"));

        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            com.alibaba.fastjson.JSONObject jsonObject = array.getJSONObject(i);
            getAllOrderPonse.setOrderId(jsonObject.getString("orderId"));
            getAllOrderPonse.setCreatedAt(DateUtils.convertTimestampToString(jsonObject.getLong("time")));
            getAllOrderPonse.setPrice(jsonObject.getString("direction")+"-"+jsonObject.getString("price"));
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(jsonObject.getBigDecimal("amount").stripTrailingZeros().toPlainString());
            getAllOrderPonses.add(getAllOrderPonse);
        }
        return getAllOrderPonses;
    }




    public String getDepth() {
        String trades = httpUtil.get("https://api.huibit.com/market/asset/handicap?symbol=" + exchange.get("market"));
        return trades;

    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        String trade = null;
        Map<String, String> params = new TreeMap<String, String>();
        params.put("api_key", exchange.get("apikey"));
        String sign = HMAC.MD5("api_key=" + exchange.get("apikey") + "&api_name=" + exchange.get("tpass"));
        try {
            trade = HttpUtil.get("https://api.huibit.com/market/asset/OpenList?uid="+exchange.get("tpass")+"&sign="+sign);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;
    }

    /**
     * 撤单
     *
     {
     "data": null,
     "code": 0,
     "message": "success",
     "totalPage": null,
     "totalElement": null
     }
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) {

        Map<String, String> params = new TreeMap<String, String>();

        params.put("orderId",orderId);
        params.put("sign", HMAC.MD5("api_key=" + exchange.get("apikey") + "&api_name=" + exchange.get("tpass")));
        params.put("uid", exchange.get("tpass"));
        String trade = HttpUtil.get("https://api.huibit.com/exchange/openApi/order/details?"+sign(params));
        JSONObject object = JSONObject.fromObject(trade);
        if (0 != object.getInt("code")) {
            setWarmLog(id, 3, "API ERROR", object.getString("message"));
        }
        return trade;
    }

    /**
     * 获取余额
     */

    public void setBalanceRedis() {

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
            String firstBalance1 = null;
            String lastBalance1 = null;
            //获取余额
            String rt = getBalance();
            logger.info("获取余额" + rt);
            JSONObject obj = JSONObject.fromObject(rt);
            JSONArray wallet = obj.getJSONArray("data");
            for (int i = 0; i < wallet.size(); i++) {
                JSONObject jsonObject = wallet.getJSONObject(i);
                if (jsonObject.getString("name").equalsIgnoreCase(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("balance");
                    firstBalance1 = jsonObject.getString("frozenBalance");
                } else if (jsonObject.getString("name").equalsIgnoreCase(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("balance");
                    lastBalance1=jsonObject.getString("frozenBalance");
                    if(Double.parseDouble(lastBalance)<10){
                        setWarmLog(id,0,"余额不足",coinArr.get(1).toUpperCase()+"余额为:"+lastBalance);
                    }
                }
            }
            if (lastBalance != null) {
                if (Double.parseDouble(lastBalance) < 10) {
                    setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
            balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
            logger.info("获取余额" + com.alibaba.fastjson.JSONObject.toJSONString(balances));
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
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitTrade(type == 1 ? 1 : 2, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getString("data");
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

        return null;
    }

    //2 委托中，3部分成交，4全部成交，5部分成交后撤消，6全部撤消
    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 2:
                return TradeEnum.NOTRADE;

            case 3:
                return TradeEnum.TRADEING;

            case 4:
                return TradeEnum.NOTRADED;

            case 5:
                return TradeEnum.CANCEL;

            case 6:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }

    /**
     * 获取委托中列表
     *
     * @return
     */
    private String getTrade() {
        return "";
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        cancelTrade = cancelTrade(orderId);
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }

    private String sign(Map<String, String> params) {
        StringBuilder param = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            param.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
       return param.toString();
    }

    private String sign1(Map<String, Object> params) {
        StringBuilder param = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            param.append(entry.getKey() + "=" + entry.getValue() + "&");
        }

        param.append("appSecretKey=" + exchange.get("tpass"));
        return HMAC.MD5(param.toString());
    }

}
