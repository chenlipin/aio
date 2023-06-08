package top.suilian.aio.service.pickcoin;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PickcoinParentService extends BaseService {
    public String baseUrl = "https://www.pickcoin.pro";

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

        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }
                Map<String, String> param = new TreeMap<>();
                param.put("client_oid", "SUIlian" + String.valueOf(new Date().getTime()));
                param.put("type", "limit");
                param.put("side", type == 1 ? "buy" : "sell");
                param.put("instrument_id", exchange.get("market"));
                param.put("price", String.valueOf(price));
                param.put("size", String.valueOf(amount));
                param.put("type", "limit");
                String body = JSON.toJSONString(param);
                HashMap<String, String> head = new HashMap<String, String>();
                String apikey = exchange.get("apikey");
                String timestamp = getTimestamp();
                String passphrase = exchange.get("passphrase");
                String method = "POST";
                String requestPath = "/api/spot/v3/orders";
                head.put("OK-ACCESS-KEY", apikey);
                head.put("OK-ACCESS-TIMESTAMP", timestamp);
                head.put("OK-ACCESS-PASSPHRASE", passphrase);
                String payload = (timestamp + method + requestPath + body);
                String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
                head.put("OK-ACCESS-SIGN", sign);
                trade = httpUtil.postByPackcoin(baseUrl + "/api/spot/v3/orders", param, head);
                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
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

    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        Map<String, String> param = new TreeMap<>();
        param.put("client_oid", "SUIlian" + String.valueOf(new Date().getTime()));
        param.put("type", "limit");
        param.put("side", type == 1 ? "buy" : "sell");
        param.put("instrument_id", exchange.get("market"));
        param.put("price", String.valueOf(price));
        param.put("size", String.valueOf(amount));
        param.put("type", "limit");
        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "POST";
        String requestPath = "/api/spot/v3/orders";
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + body);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        trade = httpUtil.postByPackcoin(baseUrl + "/api/spot/v3/orders", param, head);
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId:" + id + "挂单成功结束：" + trade);
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

        String trade = null;
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/spot/v3/orders/" + orderId + "?instrument_id=" + exchange.get("market");
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        trade = httpUtil.getAddHead(baseUrl + "/api/spot/v3/orders/" + orderId + "?instrument_id=" + exchange.get("market"), head);
        return trade;
    }


    /**
     * 获取余额
     */


    protected String getBalance() {
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/spot/v3/accounts";
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + "/api/spot/v3/accounts", head);
        return trade;
    }

    /**
     * 通过币名获取余额
     */


    protected String getBalanceByName(String name) {
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "GET";
        String requestPath = "/api/account/v3/wallet/" + name;
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + "/api/account/v3/wallet/" + name, head);
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
        Map<String, String> param = new TreeMap<>();
        param.put("client_oid", orderId);
        param.put("instrument_id", exchange.get("market"));
        String body = JSON.toJSONString(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        String passphrase = exchange.get("passphrase");
        String method = "POST";
        String requestPath = "/api/spot/v3/cancel_orders/" + orderId;
        head.put("OK-ACCESS-KEY", apikey);
        head.put("OK-ACCESS-TIMESTAMP", timestamp);
        head.put("OK-ACCESS-PASSPHRASE", passphrase);
        String payload = (timestamp + method + requestPath + body);
        String sign = HMAC.genHMAC(payload, exchange.get("tpass"));
        head.put("OK-ACCESS-SIGN", sign);
        String trade = httpUtil.postByPackcoin(baseUrl + "/api/spot/v3/cancel_orders/" + orderId, param, head);
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
                coinLists = JSONArray.fromObject(rt);
                String firstBalance = null;
                String lastBalance = null;

                for (int i = 0; i < coinLists.size(); i++) {
                    JSONObject jsonObject = coinLists.getJSONObject(i);

                    if (jsonObject.getString("currency").equals(coinArr.get(0))) {
                        firstBalance = jsonObject.getString("available");
                    } else if (jsonObject.getString("currency").equals(coinArr.get(1))) {
                        lastBalance = jsonObject.getString("available");
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
        JSONObject cancelRess = judgeRes(res, "result", "cancelTrade");
        if (cancelRes != null && "true".equals(cancelRess.getString("result"))) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_PICKCOIN);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/api/spot/v3/instruments");

        JSONArray jsonArray = JSONArray.fromObject(rt);
        if (!rt.equals("") && jsonArray != null) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("instrument_id").equals(exchange.get("market"))) {
                    String amountPrecision = getPrecision(jsonObject.getString("size_increment"));
                    String pricePrecision = getPrecision(jsonObject.getString("tick_size"));
                    String minTradeLimit = jsonObject.getString("min_size");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", minTradeLimit);
                    falg = true;
                }
            }

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }


    public String getTimestamp() {
        String time=null;
        while(time==null){
             time = gettime();
             sleep(500,0);
        }
        return time;
    }
    public String gettime(){
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    public String getPrecision(String num) {
        int length = num.length();
        return String.valueOf(length - 2);
    }

}
