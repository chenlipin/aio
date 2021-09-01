package top.suilian.aio.service.hoo;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class HooParentService extends BaseService {
    public String baseUrl = "https://api.hoo.com";

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

        String typeStr = type == 1 ? "1" : "-1";//买/卖
        // 输出字符串
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        Map<String, String> params = new TreeMap<String, String>();
        HashMap<String,String> hader = new HashMap<>();
        hader.put("Content-Type","application/x-www-form-urlencoded");
        params.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        params.put("ts",timespace);
        params.put("nonce",timespace);
        String signs= HMAC.splice(params);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        params.put("sign",sign);
        params.put("symbol",exchange.get("market"));
        params.put("price",String.valueOf(price1));
        params.put("quantity",String.valueOf(num));
        params.put("side",typeStr);
        logger.info("robotId" + id + "----" + "挂单参数：" + params);
        trade = httpUtil.post(baseUrl + "/open/v1/orders/place",params,hader);
        JSONObject rt = JSONObject.fromObject(trade);
        if (rt != null && rt.getInt("code")==0) {
            setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
            logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
        } else {
            setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
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
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market")+"&order_id="+orderId;
        String rt = httpUtil.get(baseUrl+"/open/v1/orders/detail?"+parm);
        logger.info("订单详情："+rt);
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
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign+"&symbol="+exchange.get("market");
        String res = httpUtil.get(baseUrl+"/open/v1/depth?"+parm);
        logger.info("查询深度列表"+res);
        return res;

    }


    /**
     * 获取余额
     */

    protected String getBalance() {

        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign;
        String res = httpUtil.get(baseUrl+"/open/v1/balance?"+parm);
        logger.info("查询余额"+res);
        return res;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId,String tradeNo) throws UnsupportedEncodingException {

        Map<String, String> params = new TreeMap<String, String>();
        params.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        params.put("ts",timespace);
        params.put("nonce",timespace);
        String signs= HMAC.splice(params);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        params.put("sign",sign);
        params.put("symbol",exchange.get("market"));
        params.put("order_id",orderId);
        params.put("trade_no",tradeNo);
        HashMap<String, String> headMap = new HashMap<String, String>();
        headMap.put("Content-Type", " application/x-www-form-urlencoded");
        String res = httpUtil.post(baseUrl + "/open/v1/orders/cancel",params, headMap);
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
            List<String> coinArr = Arrays.asList(coins.split("-"));

            String firstBalance = null;
            String lastBalance = null;
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null&& obj.getInt("code")==0) {
                JSONArray dataJson=obj.getJSONArray("data");
                for(int i=0;i<dataJson.size();i++){
                    if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(0))){
                        firstBalance = dataJson.getJSONObject(i).getString("amount");
                    } else if(dataJson.getJSONObject(i).getString("symbol").equals(coinArr.get(1))){
                        lastBalance = dataJson.getJSONObject(i).getString("amount");
                    }
                }
            }else {
                logger.info("获取余额失败"+obj);
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
        if(cancelRes!=null&&cancelRes.getInt("code")==0){
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }


    /**C
     * 交易规则获取
     */
    public boolean setPrecision() {
        //为client_id, ts, nonce, sign
        boolean falg = false;
        Map<String,String> parms= new TreeMap<>();
        parms.put("client_id",exchange.get("apikey"));
        String timespace=getTimespace();
        parms.put("ts",timespace);
        parms.put("nonce",timespace);
        String signs= HMAC.splice(parms);
        String sign=HMAC.sha256_HMAC(signs,exchange.get("tpass"));
        String parm=signs+"&sign="+sign;
        String rt = httpUtil.get(baseUrl+"/open/v1/tickers?"+parm);

        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");

        if (!rt.equals("") && rtObj != null&&rtObj.getInt("code")==0) {
            JSONArray jsonArray = rtObj.getJSONArray("data");
            for(int i=0;i<jsonArray.size();i++){
                if(jsonArray.getJSONObject(i).getString("symbol").equals(exchange.get("market"))){
                    precision.put("amountPrecision",jsonArray.getJSONObject(i).getString("qty_num") );
                    precision.put("pricePrecision", jsonArray.getJSONObject(i).getString("amt_num"));
                    precision.put("minTradeLimit",exchange.get("minTradeLimit"));
                    falg=true;
                }
            }

        }else {
            setTradeLog(id, "精度接口异常：" + rt, 0, "000000");
        }
        return falg;
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

}
