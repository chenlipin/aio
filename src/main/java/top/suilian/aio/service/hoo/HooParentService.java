package top.suilian.aio.service.hoo;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class HooParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.hoolgd.com";
    public RunHooRandomDepth runHooRandomDepth = BeanContext.getBean(RunHooRandomDepth.class);
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
     * 下单   {"code":0,"code_num":0,"msg":"ok","message":"ok","data":{"order_id":"34489150830","trade_no":"40522245595950224093911"}}
     */
    protected String submitTrade(int type, BigDecimal price, BigDecimal amount)   {

        String typeStr = type == 1 ? "1" : "-1";//买/卖
        // 输出字符串
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        Map<String, String> params = new TreeMap<String, String>();
        HashMap<String,String> hader = new HashMap<>();
        hader.put("Content-Type","application/x-www-form-urlencoded");
        params.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        params.put("ts",timespace);
        params.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        params.put("sign",sign);
        params.put("symbol",exchange.get("market"));
        params.put("price",String.valueOf(price1));
        params.put("quantity",String.valueOf(num));
        params.put("side",typeStr);
        logger.info("robotId" + id + "----" + "挂单参数：" + params);
        try {
            trade = httpUtil.post(baseUrl + "/open/innovate/v1/orders/place",params,hader);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject rt = JSONObject.fromObject(trade);
        if(0!=rt.getInt("code")){
            setWarmLog(id,3,"API接口错误",rt.getString("msg"));
            setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }else
       {
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
            logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
        }
        return trade;
    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return{"code":0,"code_num":0,"msg":"ok","message":"ok","data":{"create_at":1648047858200,"fee":"0","match_amt":"0","match_price":"0","match_qty":"0","order_id":"34489150830","order_type":1,"price":"1.2920","quantity":"4.000","side":1,"status":2,"symbol":"FTM-USDT","ticker":"FTM-USDT","trade_no":"40522245595950224093911","trades":[]}}
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId){
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market")+"&order_id="+orderId;
        String rt = httpUtil.get(baseUrl+"/open/innovate/v1/orders/detail?"+parm);
        JSONObject object = JSONObject.fromObject(rt);
        if(0!=object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return rt;

    }


    protected String getTradeOrders() {
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/v1/orders/last?"+parm);
        logger.info("查询自己的委托列表"+res);
        return res;

    }

    public String getDepth(){
        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/innovate/v1/depth?"+parm);
        logger.info("查询深度列表"+res);
        return res;

    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign;
        String res = httpUtil.get(baseUrl+"/open/innovate/v1/balance?"+parm);
        return res;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return {"code":0,"code_num":0,"msg":"ok","message":"ok","data":[]}
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId,String tradeNo) {

        Map<String, String> params = new TreeMap<String, String>();
        params.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        params.put("ts",timespace);
        params.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicingStr(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        params.put("sign",sign);
        params.put("symbol",exchange.get("market"));
        params.put("order_id",orderId);
        params.put("trade_no",tradeNo);
        HashMap<String, String> headMap = new HashMap<String, String>();
        headMap.put("Content-Type", " application/x-www-form-urlencoded");
        String res = null;
        try {
            res = httpUtil.post(baseUrl + "/open/innovate/v1/orders/cancel",params, headMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject object = JSONObject.fromObject(res);
        if(0!=object.getInt("code")) {
            setWarmLog(id, 3, "API接口错误", object.getString("msg"));
        }
        return res;
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
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null&& obj.getInt("code")==0) {
                JSONArray dataJson=obj.getJSONArray("data");
                for(int i=0;i<dataJson.size();i++){
                    if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(0))){
                        firstBalance = dataJson.getJSONObject(i).getString("amount");
                        firstBalance1 = dataJson.getJSONObject(i).getString("freeze");
                    } else if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(1))){
                        lastBalance = dataJson.getJSONObject(i).getString("amount");
                        lastBalance1 = dataJson.getJSONObject(i).getString("freeze");
                    }
                }
            }else {
                logger.info("获取余额失败"+obj);
            }
            if(lastBalance!=null){
                if (Double.parseDouble(lastBalance) < 10) {
                    setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance+"_"+firstBalance1);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalance1);
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
        if(cancelRes!=null&&cancelRes.getInt("code")==0){
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }

    public String getTimespace(){
        String timespace=null;
        String rs=httpUtil.get(baseUrl+"/open/v1/timestamp");
        JSONObject jsonObject = JSONObject.fromObject(rs);
        if(jsonObject!=null&&jsonObject.getInt("code")==0){
            timespace=jsonObject.getString("data");
        }
        return timespace;
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitTrade(type == 1 ? 1 : -1, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("0".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("order_id")+"_"+jsonObject.getJSONObject("data").getString("trade_no");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        String trade = getTrade();
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(trade);
        com.alibaba.fastjson.JSONArray entrutsHis = jsonObject1.getJSONArray("data");
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < entrutsHis.size(); i++) {
            com.alibaba.fastjson.JSONObject jsonObject = entrutsHis.getJSONObject(i);
            map.put(jsonObject.getString("order_id")+"_"+jsonObject.getString("trade_no"),0);
        }
        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> hashMap = new HashMap<>();

        for (String order : orders) {
            Integer integer = map.get(order);
            if (integer != null) {
                hashMap.put(order, 0);
            } else {
                String result = "";
                try {
                    result = selectOrder(order);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(result);
                if ("0".equals(jsonObject.getString("code"))) {
                    Integer statusCode = jsonObject.getJSONObject("data").getInteger("status");
                    hashMap.put(order, getTradeEnum(statusCode).getStatus());
                }
            }
        }
        return hashMap;
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
     * @return
     */
    private String getTrade() {
        Map<String,Object> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= null;
        try {
            signs = HMAC.splicing(parms);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/innovate/v1/orders/last?"+parm);
        logger.info("获取委托中列表"+res);
        return res;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
            String[] split = orderId.split("_");
            cancelTrade = cancelTrade(split[0],split[1]);
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("0".equals(jsonObject.getString("code"))){
                return "true";
            }
        }
        return "false";
    }
}
