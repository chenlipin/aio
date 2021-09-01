package top.suilian.aio.service.euex;

import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class EuexParentService extends BaseService {
    public String baseUrl = "http://47.74.218.238:8080";

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
//        String time = httpUtil.get("https://api.fchain.one/openapi/v1/time");
//        String timestamp = JSONObject.fromObject(time).getString("serverTime");
        String typeStr = type == 0 ? "买" : "卖";

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

                    HashMap<String, String> param = new HashMap<String, String>();
                    param.put("method", "order");
                    param.put("accesskey", exchange.get("apikey"));
                    param.put("price", String.valueOf(price1));
                    param.put("amount", String.valueOf(num));
                    param.put("tradeType", String.valueOf(type));
                    param.put("currency", exchange.get("market"));
                    String sign = gotoSign(param, exchange.get("tpass"));
                    logger.info("挂单参数:" + param);
                    trade = httpUtil.get(baseUrl + "/api/v2/order?amount=" + num + "&method=order&accesskey=" + exchange.get("apikey") + "&price=" + price1 + "&currency=" + exchange.get("market") + "&tradeType=" + type + "&sign=" + sign + "&reqTime=" + timestamp);

                    setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                HashMap<String, String> param = new HashMap<String, String>();
                param.put("method", "order");
                param.put("accesskey", exchange.get("apikey"));
                param.put("price", String.valueOf(price1));
                param.put("amount", String.valueOf(num));
                param.put("tradeType", String.valueOf(type));
                param.put("currency", exchange.get("market"));
                String sign = gotoSign(param, exchange.get("tpass"));
                logger.info("挂单参数:" + param);
                trade = httpUtil.get(baseUrl + "/api/v2/order?amount=" + num + "&method=order&accesskey=" + exchange.get("apikey") + "&price=" + price1 + "&currency=" + exchange.get("market") + "&tradeType=" + type + "&sign=" + sign + "&reqTime=" + timestamp);


                setTradeLog(id, "挂" + (type == 0? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
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
     * 提交订单
     */
    public String submitOrder(int type, BigDecimal price, BigDecimal num) {

        String timestamp = String.valueOf(new Date().getTime());

        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "order");
        param.put("accesskey", exchange.get("apikey"));
        param.put("price", String.valueOf(price));
        param.put("amount", String.valueOf(num));
        param.put("tradeType", String.valueOf(type));
        param.put("currency", exchange.get("market"));
        String sign = gotoSign(param, exchange.get("tpass"));
        logger.info("挂单参数:" + param);
        String trade = httpUtil.get(baseUrl + "/api/v2/order?amount="+num  + "&method=order&accesskey=" + exchange.get("apikey") + "&price=" + price + "&currency=" + exchange.get("market") + "&tradeType=" + type + "&sign=" + sign + "&reqTime=" + timestamp);

        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

        return trade;
    }



    protected String getTradeOrders(int type) {
        String timestamp = String.valueOf(new Date().getTime());

        HashMap<String, String> param = new LinkedHashMap<String, String>();
        param.put("method", "getOrders");
        param.put("accesskey", exchange.get("apikey"));
        param.put("tradeType", String.valueOf(type));
        param.put("currency", exchange.get("market"));
        param.put("pageIndex", "1");
        param.put("pageSize","100");

        String sign = gotoSign(param, exchange.get("tpass"));

        param.put("sign", sign);
        String tradeOrders = httpUtil.get(baseUrl+"/api/v2/getOrders?method=getOrders&accesskey="+exchange.get("apikey")+"&tradeType="+type+"&currency="+exchange.get("market")+"&pageIndex=1&pageSize=100&sign="+sign+"&reqTime="+timestamp);

        return tradeOrders;
    }




    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

//        String time = httpUtil.get("https://api.fchain.one/openapi/v1/time");
//        String timestamp = JSONObject.fromObject(time).getString("serverTime");
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "getOrder");
        param.put("accesskey", exchange.get("apikey"));
        param.put("id", orderId);
        param.put("currencyCode", exchange.get("market"));
        String sign = gotoSign(param, exchange.get("tpass"));
        long timestamp = new Date().getTime();
        String orderInfo = httpUtil.get(baseUrl + "/api/v2/getOrder?method=getOrder&accesskey=" + exchange.get("apikey") + "&id=" + orderId + "&currencyCode=" + exchange.get("market") + "&sign=" + sign + "&reqTime=" + timestamp);
        return orderInfo;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {

        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "getAccountInfo");
        param.put("accesskey", exchange.get("apikey"));
        String sign = gotoSign(param, exchange.get("tpass"));
        long timestamp = new Date().getTime();
        String orderInfo = httpUtil.get(baseUrl + "/api/v2/getAccountInfo?method=getAccountInfo&accesskey=" + exchange.get("apikey") + "&sign=" + sign + "&reqTime=" + timestamp);
        return orderInfo;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        String res = "";
//        String time = httpUtil.get("https://api.fchain.one/openapi/v1/time");
//        String timestamp = JSONObject.fromObject(time).getString("serverTime");
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "cancel");
        param.put("accesskey", exchange.get("apikey"));
        param.put("id", orderId);
        param.put("currency", exchange.get("market"));
        String sign = gotoSign(param, exchange.get("tpass"));
        long timestamp = new Date().getTime();
        res = httpUtil.get(baseUrl + "/api/v2/cancel?method=cancel&accesskey=" + exchange.get("apikey") + "&currency=" + exchange.get("market") + "&id=" + orderId + "&sign=" + sign + "&reqTime=" + timestamp);
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
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
            //获取余额
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if("0000".equals(obj.getString("code"))&&obj.getJSONObject("data").getJSONObject("balance")!=null){
                JSONObject data = obj.getJSONObject("data");
                JSONObject userBalance = data.getJSONObject("balance");


                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), userBalance.getString(coinArr.get(0).toUpperCase()));
                balances.put(coinArr.get(1), userBalance.getString(coinArr.get(1).toUpperCase()));
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
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
        if (cancelRes != null && "0000".equals(cancelRes.getString("code"))) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_EUEX);
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }


    public String gotoSign(HashMap<String, String> params, String secret) {
        String sign = "";
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
            byte[] sha1Encode = sha1Digest.digest(secret.getBytes());
            String signSecret = convertByteToHexString(sha1Encode);
            String sortStr = keySortToString(params);
            SecretKeySpec sk = new SecretKeySpec(signSecret.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(sk);
            return convertByteToHexString(mac.doFinal(sortStr.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return sign;
    }


    public String keySortToString(HashMap<String, String> params) {
        String str = "";
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            str += entry.getKey() + "=" + entry.getValue() + "&";
        }
        return str.substring(0, str.length() - 1);
    }


    public static String convertByteToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            int temp = bytes[i] & 0xff;
            String tempHex = Integer.toHexString(temp);
            if (tempHex.length() < 2) {
                result += "0" + tempHex;
            } else {
                result += tempHex;
            }
        }
        return result;
    }


    //判断  撤单    必须撤
    public void judgeCancel(String orderId) throws UnsupportedEncodingException {
        int count=0;
        Boolean flag = true;
        String res = null;
        res = cancelTrade(orderId);
        JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
        if (cancelRes != null && "0000".equals(cancelRes.getString("code"))) {

            setTradeLog(id, "撤单成功[" + orderId + "]=>" + cancelRes, 0, "000000");
            setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
            flag = false;
        } else {
            sleep(600, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
        while (flag&&count<3) {
            String selectResult = selectOrder(orderId);
            JSONObject selectJson = JSONObject.fromObject(selectResult);
            if("0000".equals(selectJson.getString("code"))&&selectJson!=null&&selectJson.getJSONObject("data").getString("status")!=null){
                if (selectJson.getJSONObject("data").getString("status").equals("3")) {
                    logger.info(orderId + "完全成交");
                    setTradeLog(id, "对标完全成交[" + orderId + "]=>" + selectJson, 0, "000000");
                    flag = false;

                } else if (selectJson.getJSONObject("data").getString("status").equals("5")) {
                    logger.info(orderId + "已撤单");
                    setTradeLog(id, "对标已撤单[" + orderId + "]=>" + selectJson, 0, "000000");
                    flag = false;
                } else {
                    logger.info("订单：【"+orderId+"】撤单次数=====》"+count);
                    res = cancelTrade(orderId);
                    JSONObject cancelRes1 = judgeRes(res, "code", "cancelReferTrade");
                    if (cancelRes1 != null && "0000".equals(cancelRes1.getString("code"))) {
                        setTradeLog(id, "撤单成功[" + orderId + "]=>" + cancelRes1, 0, "000000");
                        setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                        flag = false;
                    } else {
                        if(count==2){
                            setTradeLog(id, "撤单失败[" + orderId + "]=>" + cancelRes1, 0, "000000");
                            logger.info("==========================撤单失败========================");
                            setCancelOrder(cancelRes, res, orderId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                        }
                        sleep(600, Integer.parseInt(exchange.get("isMobileSwitch")));
                    }
                }
            }

            count++;
            sleep(600, Integer.parseInt(exchange.get("isMobileSwitch")));
        }

    }


}
