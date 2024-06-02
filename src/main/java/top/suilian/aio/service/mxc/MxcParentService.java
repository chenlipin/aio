package top.suilian.aio.service.mxc;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.mxc.randomDepth.RunMxcDeep;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

@DependsOn("beanContext")
@Service
public class MxcParentService extends BaseService implements RobotAction {

        public String baseUrl = "https://www.mexc.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    public RunMxcDeep runMxcDeep = BeanContext.getBean(RunMxcDeep.class);

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
                    params.put("order_type", "LIMIT_ORDER");
                    if (type == 1) {
                        params.put("trade_type", "BID");
                    } else {
                        params.put("trade_type", "ASK");
                    }
                    params.put("quantity", num);
                    params.put("price", price1);
                    String toSign = "POST" + '\n' + "/open/api/v2/order/place" + '\n' + "api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp;
                    logger.info("签名参数：" + toSign);
                    String sign = HMAC.sha256_HMAC(toSign, exchange.get("tpass"));
                    logger.info("下单参数：" + params);
                    try {
                        trade = httpUtil.postes(baseUrl + "/open/api/v2/order/place?api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp + "&sign=" + sign, params);
                        logger.info("submitOrder1返回参数：" + trade);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JSONObject jsonObject = JSONObject.fromObject(trade);
                    if(jsonObject==null||200!=jsonObject.getInt("code")){
                        setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
                    }
                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, Object> params = new TreeMap<String, Object>();
                params.put("symbol", exchange.get("market"));
                params.put("order_type", "LIMIT_ORDER");
                if (type == 1) {
                    params.put("trade_type", "BID");
                } else {
                    params.put("trade_type", "ASK");
                }
                params.put("quantity", num);
                params.put("price", price1);
                String toSign = "POST" + '\n' + "/open/api/v2/order/place" + '\n' + "api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp;
                logger.info("签名参数：" + toSign);
                String sign = HMAC.sha256_HMAC(toSign, exchange.get("tpass"));
                logger.info("下单参数：" + params);

                try {
                    trade = httpUtil.postes(baseUrl + "/open/api/v2/order/place?api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp + "&sign=" + sign, params);
                    logger.info("submitOrder2返回参数：" + trade);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JSONObject jsonObject = JSONObject.fromObject(trade);
                if(jsonObject==null||200!=jsonObject.getInt("code")){
                    setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
                }
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

    @Override
    public List<getAllOrderPonse> selectOrder() {
        return null;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    //下单
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String timestamp = getSecondTimestamp(new Date());

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("symbol", exchange.get("market"));
        params.put("order_type", "LIMIT_ORDER");
        if (type == 1) {
            params.put("trade_type", "BID");
        } else {
            params.put("trade_type", "ASK");
        }
        params.put("quantity", num);
        params.put("price", price1);
        String toSign = "POST" + '\n' + "/open/api/v2/order/place" + '\n' + "api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp;
        String sign = HMAC.sha256_HMAC(toSign, exchange.get("tpass"));
        logger.info("下单参数：" + params);
        String trade = null;
        try {
            trade = httpUtil.postes(baseUrl + "/open/api/v2/order/place?api_key=" + exchange.get("apikey") + "&recv_window=20" + "&req_time=" + timestamp + "&sign=" + sign, params);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            logger.info("submitOrder返回参数：" + trade);
            if(jsonObject==null||200!=jsonObject.getInt("code")){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        String timestamp = getSecondTimestamp(new Date());
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("order_ids", orderId);
        params.put("api_key", exchange.get("apikey"));
        params.put("req_time", timestamp);
        String sort = toSort(params);
        sort = "GET" + '\n' + "/open/api/v2/order/query" + '\n' + sort;
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        params.put("sign", sign);
        String str = toSort(params);


        String trade = httpUtil.get(baseUrl + "/open/api/v2/order/query?" + str);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        return trade;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) {

        String timestamp = getSecondTimestamp(new Date());

        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("order_ids", orderId);
        params.put("api_key", exchange.get("apikey"));
        params.put("req_time", timestamp);

        String sort = toSort(params);
        sort = "DELETE" + '\n' + "/open/api/v2/order/cancel" + '\n' + sort;
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        params.put("sign", sign);
        String str = toSort(params);
        //delete请求
        String res = httpUtil.delete(baseUrl + "/open/api/v2/order/cancel?" + str);
        JSONObject jsonObject = JSONObject.fromObject(res);
        if(jsonObject==null||200!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        logger.info("cancelTrade：" + orderId);
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
            String firstBalance = null;
            String lastBalance = null;
            String firstBalancefrozen = null;
            String lastBalancefrozen = null;
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null && "200".equals(obj.getString("code"))) {
                JSONObject data = obj.getJSONObject("data");
                firstBalance = data.getJSONObject(coinArr.get(0).toUpperCase()).getString("available");
                firstBalancefrozen = data.getJSONObject(coinArr.get(0).toUpperCase()).getString("frozen");
                lastBalance = data.getJSONObject(coinArr.get(1).toUpperCase()).getString("available");
                lastBalancefrozen = data.getJSONObject(coinArr.get(1).toUpperCase()).getString("frozen");
            }

            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance+"_"+firstBalancefrozen);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalancefrozen);
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


    public String getDepth() {

        String trade = httpUtil.get(baseUrl + "/open/api/v2/market/depth?depth=20&symbol=" + exchange.get("market"));
        return trade;
    }

    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String s = baseUrl + "/open/api/v2/market/symbols";
        System.out.println("----" + s);
        String rt = httpUtil.get(s);
        JSONObject rtObj = JSONObject.fromObject(rt);
        if (rtObj != null && rtObj.getInt("code") == 200) {
            JSONArray jsonArray = rtObj.getJSONArray("data");
            for (Object o : jsonArray) {
                JSONObject jsonObjects = JSONObject.fromObject(o.toString());
                if (jsonObjects.getString("symbol").equals(exchange.get("market"))) {
                    precision.put("amountPrecision", jsonObjects.getInt("quantity_scale"));
                    precision.put("pricePrecision", jsonObjects.getInt("price_scale"));
                    precision.put("minTradeLimit", jsonObjects.getInt("min_amount"));
                    falg=true;
                    break;
                }
            }
        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }

    /**
     * 获取余额
     */


    protected String getBalance() {

        String timestamp = getSecondTimestamp(new Date());
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("req_time", timestamp);
        String sort = toSort(params);
        sort = "GET" + '\n' + "/open/api/v2/account/info" + '\n' + sort;
        System.out.println("加密参数：" + sort);
        String sign = HMAC.sha256_HMAC(sort, exchange.get("tpass"));
        params.put("sign", sign);
        String str = toSort(params);
        logger.info("请求参数：" + str);

        String rt = httpUtil.get(baseUrl + "/open/api/v2/account/info?" + str);
        logger.info("账户信息" + rt);
        return rt;
    }



    public static String getSecondTimestamp(Date date) {
        if (null == date) {
            return "0";
        }
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        if (length > 3) {
            return String.valueOf(timestamp.substring(0, length - 3));
        } else {
            return "0";
        }
    }


    private static String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();

        int i = 0;
        int max = map.size() - 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

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

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ( "200".equals(jsonObject.getString("code"))) {
                orderId  = jsonObject.getString("data");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res", "false");
                hashMap.put("orderId", msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        String s = null;
        try {
            s = selectOrder(orderId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(s);
        JSONArray data = jsonObject.getJSONArray("data");
        HashMap<String, Integer> map = new HashMap<>();
        for (Object datum : data) {
            JSONObject jsonObject1 = JSONObject.fromObject(datum.toString());
            String order_Id = jsonObject1.getString("id");
            String state = jsonObject1.getString("state");
            map.put(order_Id,getTradeEnum(state).getStatus());
        }
        return map;
    }

    @Override
    public String cancelTradeStr(String orderId) {
//        String cancelTrade = cancelTrade(orderId);
//        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(cancelTrade);
//        if(jsonObject.getInteger("code")==200){
//            return "true";
//        }else {
//            return "false";
//        }
        return "true";
    }

    public TradeEnum getTradeEnum(String status) {
        switch (status) {
            case "NEW":
                return TradeEnum.NOTRADE;

            case "FILLED":
                return TradeEnum.NOTRADED;

            case "CANCELED":
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }
}
