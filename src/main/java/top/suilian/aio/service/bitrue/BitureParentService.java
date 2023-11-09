package top.suilian.aio.service.bitrue;

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
import java.util.*;

@Service
@DependsOn("beanContext")
public class BitureParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://openapi.bitrue.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;

    @Override
    public List<getAllOrderPonse> selectOrder() {
        return null;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

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
        String timestamp = String.valueOf(getTime());
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String params = "symbol=" + exchange.get("market") + "&side=" + (type == 1 ? "BUY" : "SELL") + "&type=LIMIT&timeInForce=GTC&quantity=" + String.valueOf(num) + "&price=" + String.valueOf(price1) + "&recvWindow=5000&timestamp=" + timestamp;

        String signs = HMAC.sha256_HMAC(params, exchange.get("tpass"));

        String par = params + "&signature=" + signs;

        logger.info("robotId" + id + "----" + "挂单参数：" + par);

        trade = httpUtil.doPost(baseUrl + "/api/v1/order", par, exchange.get("apikey"));

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
        return trade;
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
        Map<String, String> param = new TreeMap<>();
        String requestPath = "/api/v1/account";
        String timestamp = String.valueOf(getTime());
        String params = "recvWindow=10000&timestamp=" + timestamp;
        String sign = HMAC.sha256_HMAC(params, exchange.get("tpass"));
        String par = params + "&signature=" + sign;
        String trade = httpUtil.doGet(baseUrl + requestPath+"?"+par, exchange.get("apikey"));
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
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
        String timestamp = String.valueOf(getTime());
        String param = "orderId=" + orderId + "&timestamp=" + timestamp;
        String signs = HMAC.sha256_HMAC(param, exchange.get("tpass"));
        String par = param + "&signature=" + signs;
        String res = httpUtil.doDelete(baseUrl + "/api/v1/order?" + par, "", exchange.get("apikey"));
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
    public Map<String, Integer> selectOrderStr(String orderId) {
        return null;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        try {
            cancelTrade = cancelTrade(orderId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("200000".equals(jsonObject.getString("code"))||"400100".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }

    public String getTimestamp() {
        String time = null;
        while (time == null) {
            time = gettime();
            sleep(500, 0);
        }
        return time;
    }

    public String gettime() {
        String res = httpUtil.get(baseUrl + "/api/general/v3/time");
        JSONObject jsonObject = judgeRes(res, "iso", "gettime");
        if (jsonObject != null) {
            return jsonObject.getString("iso");
        } else {
            return null;
        }
    }

    public String getPrecision(String num) {
        int length = num.length();
        return String.valueOf(length - 2);
    }

}
