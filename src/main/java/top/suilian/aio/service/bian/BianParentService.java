package top.suilian.aio.service.bian;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

@Service
@DependsOn("beanContext")
public class BianParentService extends BaseService implements RobotAction {


    public String baseUrl = "https://api4.binance.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];

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
     * {"symbol":"TRXUSDT","orderId":10630294817,"clientOrderId":null,"transactTime":1650200801929,"orderStatus":"INIT"}
     * @param type
     * @param price
     * @param amount
     * @return
     */
    public String   submitTrade(int type, BigDecimal price, BigDecimal amount){
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;
        long time = System.currentTimeMillis();
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision")));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision")));
        Map<String, String> param = new LinkedHashMap<>();
        param.put("symbol", exchange.get("market"));
        param.put("side", type == 1 ? "BUY" : "SELL");
        param.put("type", "LIMIT");
        param.put("timeInForce", "GTC");
        param.put("quantity", String.valueOf(amount));
        param.put("price", String.valueOf(price));
        param.put("recvWindow","10000");
        param.put("timestamp",time+"");
        param.put("newClientOrderId", "SSS" + time);
        param.put("newOrderRespType", "ACK");
        String queryString = splicingStr(param);
        logger.info("未签名参数："+queryString);
        String signature = HMAC.sha256_HMAC(queryString, exchange.get("tpass"));
        logger.info("签名："+signature);
        param.put("signature",signature);
        String queryString1 = splicingStr(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        head.put("X-MBX-APIKEY", apikey);
        try {
            trade = httpUtil.postByPackcoin2(baseUrl + "/api/v3/order?"+queryString1,  head);
            logger.info("路径:" +(baseUrl + "/api/v3/order")+queryString1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId:" + id + "挂单成功结束：" + trade);
        return trade;
    }


    public static String splicingStr(Map<String, String> params)   {
        StringBuilder httpParams = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            try {
                httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return  status:2
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        Long timestamp = getTime();
        String trade = null;
        String param = "orderId=" + orderId +"&symbol=" + exchange.get("market")+"&timestamp=" + timestamp;
        String signs = HMAC.sha256_HMAC(param, exchange.get("tpass"));
        String par = param + "&signature=" + signs;
        String orderInfo = httpUtil.doGet(baseUrl + "/api/v1/order?" + par, exchange.get("apikey"));
        return orderInfo;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {

            long time = System.currentTimeMillis();
            Map<String, String> param = new LinkedHashMap<>();
            param.put("timestamp",System.currentTimeMillis()+"");
            param.put("recvWindow", "60000");

            String queryString = splicingStr(param);
            logger.info("未签名参数："+queryString);
            String signature = HMAC.sha256_HMAC(queryString, exchange.get("tpass"));
            logger.info("签名："+signature);
            param.put("signature",signature);
            HashMap<String, String> head = new HashMap<String, String>();
            String apikey = exchange.get("apikey");
            head.put("X-MBX-APIKEY", apikey);
            String trade=null;
            try {
                trade = httpUtil.postByPackcoin(baseUrl + "/api/v3/account", param, head);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            logger.info("robotId:" + id + "查询余额成功结束：" + trade);
//        JSONObject jsonObject = JSONObject.fromObject(trade);
            return trade;

    }

    protected Long getTime() {
        String trade = httpUtil.get("https://openapi.bitrue.com/api/v1/time");
        Long serverTime = JSONObject.fromObject(trade).getLong("serverTime");
        return serverTime;
    }

    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws
     */
    public String cancelTrade(String orderId) {
        String trade = null;
        long time = System.currentTimeMillis();
        Map<String, String> param = new LinkedHashMap<>();
        param.put("symbol", exchange.get("market"));
        param.put("recvWindow","10000");
        param.put("timestamp",time+"");
        param.put("orderId",orderId);
        String queryString = splicingStr(param);
        logger.info("未签名参数："+queryString);
        String signature = HMAC.sha256_HMAC(queryString, exchange.get("tpass"));
        String queryString1 = splicingStr(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        head.put("X-MBX-APIKEY", apikey);
        try {
            trade = httpUtil.postByPackcoin2(baseUrl + "/api/v3/openOrders?"+queryString1,  head);
            logger.info("撤销订单:" + id + "参数：" + queryString1);
        } catch (Exception e) {
            setTradeLog(id, "撤销订单:"+orderId+"失败"+trade, 0,"ff6224" );
           return null;
        }
        setTradeLog(id, "撤销订单:"+orderId, 0,"05cbc8" );
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
                com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(rt);
                coinLists = JSONArray.fromObject(jsonObject1.getString("balances"));
                String firstBalance = null;
                String lastBalance = null;

                for (int i = 0; i < coinLists.size(); i++) {
                    JSONObject jsonObject = coinLists.getJSONObject(i);

                    if (jsonObject.getString("asset").equals(coinArr.get(0))) {
                        firstBalance = jsonObject.getString("free") + "_" +jsonObject.getString("locked") ;
                    } else if (jsonObject.getString("currency").equals(coinArr.get(1))) {
                        lastBalance =  jsonObject.getString("free") + "_" +jsonObject.getString("locked") ;
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
        }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));

        exchange.put("amountPrecision",  exchange.get("amountPrecision"));
        exchange.put("pricePrecision",  exchange.get("pricePrecision"));
        exchange.put("minTradeLimit", exchange.get("minTradeLimit"));
        return true;
    }



    public JSONArray openOrders(){
        String trade = null;
        long time = System.currentTimeMillis();
        Map<String, String> param = new LinkedHashMap<>();
        param.put("symbol", exchange.get("market"));
        param.put("recvWindow","10000");
        param.put("timestamp",time+"");
        String queryString = splicingStr(param);
        logger.info("未签名参数："+queryString);
        String signature = HMAC.sha256_HMAC(queryString, exchange.get("tpass"));
        logger.info("签名："+signature);
        param.put("signature",signature);
        String queryString1 = splicingStr(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        head.put("X-MBX-APIKEY", apikey);
        try {
            trade = httpUtil.postByPackcoin2(baseUrl + "/api/v3/openOrders?"+queryString1,  head);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("查询所有未成交订单:" + id + "挂单成功结束：" + trade);
        return JSONArray.fromObject(trade);
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = null;
        try {
            submitOrder = submitTrade(type, price, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if (jsonObject.getString("orderId")!=null) {
                orderId = jsonObject.getString("orderId");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                hashMap.put("res","false");
                hashMap.put("orderId",submitOrder);
            }
        }
        return hashMap;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        logger.info("一键撤单开始:" + id + ":参数:" + type + tradeType);
        ArrayList<String> strings = new ArrayList<>();
        List<getAllOrderPonse> getAllOrderPonses = selectOrder();
        for (getAllOrderPonse getAllOrderPons : getAllOrderPonses) {
            if (Objects.equals(getAllOrderPons.getMyself(), type) && Objects.equals(tradeType, getAllOrderPons.getType())) {
                logger.info("一键撤单详情:订单方向:" + getAllOrderPons.getMyself() + ":单号:" + getAllOrderPons.getOrderId());
                String s = cancelTrade(getAllOrderPons.getOrderId());
                strings.add(getAllOrderPons.getOrderId());
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return strings;
    }

    @Override
    public List<getAllOrderPonse> selectOrder() {
        List<getAllOrderPonse> orderPonses = new ArrayList<>();
        JSONArray jsonArray = openOrders();
        getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (!object.getString("clientOrderId").contains("SS")){
                getAllOrderPonse.setMyself(0);
            }else {
                getAllOrderPonse.setMyself(1);
            }
            getAllOrderPonse.setOrderId(object.getString("orderId"));
            getAllOrderPonse.setPrice(object.getString("price"));
            getAllOrderPonse.setAmount(object.getString("origQty"));
            getAllOrderPonse.setType(object.getString("side").equals("BUY")?1:2);
            getAllOrderPonse.setStatus(1);
            getAllOrderPonse.setCreatedAt(object.getString("workingTime"));
        }
        return orderPonses;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        return null;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        return null;
    }


    public String getPrecision(String num) {
        int length = num.length();
        return String.valueOf(length - 2);
    }

}
