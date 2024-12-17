package top.suilian.aio.service.arbisoo;

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

public class ArbisooParentService extends BaseService implements RobotAction {
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

    public String url = "https://ttsjys.laikas.shop/api/app/other";

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
     * 下单  {
     *     "code": 200,
     *     "message": "下单成功",
     *     "data": {
     *         "order_no": "EB1734449480248706"
     *     }
     * }
     */
    protected String submitTrade(int type, BigDecimal price, BigDecimal amount) {
        // 输出字符串
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision"))).stripTrailingZeros();
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision"))).stripTrailingZeros();
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + (type==1?"buy":"sell") + "，price(价格)：" + price + "，amount(数量)：" + num);

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("direction", type==1?"buy":"sell");
        params.put("symbol", exchange.get("market"));
        params.put("type","1");
        params.put("password", exchange.get("tpass"));
        params.put("amount", String.valueOf(num));
        params.put("entrust_price", String.valueOf(price1));



        try {
             trade = HttpUtil.post("https://ttsjys.laikas.shop/api/app/other/storeEntrust", params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        if (200 != rt.getInt("code")) {
            logger.info("robotId" + id + "----" + "挂单失败结束");
        } else {
            setTradeLog(id, "挂单成功结束-- type:" + (type == 1 ? "买" : "卖") + "[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
            logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
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
     * @return{"code":0,"code_num":0,"msg":"ok","message":"ok","data":{"create_at":1648047858200,"fee":"0","match_amt":"0","match_price":"0","match_qty":"0","order_id":"34489150830","order_type":1,"price":"1.2920","quantity":"4.000","side":1,"status":2,"symbol":"FTM-USDT","ticker":"FTM-USDT","trade_no":"40522245595950224093911","trades":[]}}
     */


    public String selectOrder(String orderId) {
        String trade = null;
        try {
            trade = HttpUtil.get("https://ttsjys.laikas.shop/api/app/other/getCurrentEntrustByorder?order_id="+orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        if (200 != rt.getInt("code")) {
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
            trade = HttpUtil.get("https://ttsjys.laikas.shop/api/app/other/getCurrentEntrust?page=1");

        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade).getJSONObject("data");

        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();

        com.alibaba.fastjson.JSONArray array = com.alibaba.fastjson.JSONArray.parseArray(rt.getString("data"));

        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            com.alibaba.fastjson.JSONObject jsonObject = array.getJSONObject(i);

            getAllOrderPonse.setOrderId(jsonObject.getString("order_no"));
            getAllOrderPonse.setCreatedAt(jsonObject.getString("created_at"));
            getAllOrderPonse.setPrice(jsonObject.getString("entrust_type_text")+"-"+jsonObject.getString("entrust_price"));
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(jsonObject.getString("amount"));
            getAllOrderPonses.add(getAllOrderPonse);
        }
        return getAllOrderPonses;
    }




    public String getDepth() {
        String trades = httpUtil.get("https://ttsjys.laikas.shop/api/app/other/getDepth?symbol=" + exchange.get("market"));
        return trades;

    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        String trade = null;
        Map<String, String> params = new TreeMap<String, String>();
        params.put("api_key", exchange.get("apikey"));
        String sign = sign(params);
        params.put("sign", sign);
        logger.info("robotId" + id + "----" + "挂单参数：" + params);
        try {
            trade = HttpUtil.get("https://ttsjys.laikas.shop/api/app/other/getrobotbalance");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trade;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return {"code":1,"message":"成功","data":null,"extra":null,"success":true}
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) {

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("entrust_id", orderId);
        params.put("symbol", exchange.get("market"));
        params.put("entrust_type", "1");
        params.put("password", exchange.get("tpass"));


        String trade = "";

        try {
            trade = HttpUtil.post("https://ttsjys.laikas.shop/api/app/other/cancelEntrust", params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(trade);
        if (200 != object.getInt("code")) {
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
            JSONArray wallet = obj.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < wallet.size(); i++) {
                JSONObject jsonObject = wallet.getJSONObject(i);
                if (jsonObject.getString("symbol").equalsIgnoreCase(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("usable_balance");
                    firstBalance1 = jsonObject.getString("freeze_balance");
                } else if (jsonObject.getString("symbol").equalsIgnoreCase(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("usable_balance");
                    lastBalance1=jsonObject.getString("freeze_balance");
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
        if (cancelRes != null && cancelRes.getInt("code") == 200) {
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
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("order_no");
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
            if ("200".equals(jsonObject.getString("code"))) {
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
