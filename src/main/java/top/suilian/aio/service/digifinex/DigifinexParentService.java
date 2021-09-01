package top.suilian.aio.service.digifinex;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import javax.crypto.Mac;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

public class DigifinexParentService extends BaseService {
    public String baseUrl = "https://openapi.digifinex.vip/v3";

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

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }
                Map<String, String> param = new TreeMap<>();
                param.put("symbol", exchange.get("market"));
                param.put("type", type == 1 ? "buy" : "sell");
                param.put("price", String.valueOf(price));
                param.put("amount", String.valueOf(amount));
                String payload = HMAC.splice(param);

                HashMap<String, String> head = new HashMap<String, String>();
                String apikey = exchange.get("apikey");
                String sign = HMAC.sha256_HMAC(payload, exchange.get("tpass"));
                String timestamp = getTimestamp();
                head.put("ACCESS-KEY", apikey);
                head.put("ACCESS-TIMESTAMP", timestamp);
                head.put("ACCESS-SIGN", sign);
                trade = httpUtil.post(baseUrl + "/spot/order/new", param, head);
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


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        String trade = null;
        Map<String, String> param = new TreeMap<>();
        param.put("order_id", orderId);
        String payload = HMAC.splice(param);
        String sign = HMAC.sha256_HMAC(payload, exchange.get("tpass"));
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        head.put("ACCESS-KEY", apikey);
        head.put("ACCESS-TIMESTAMP", timestamp);
        head.put("ACCESS-SIGN", sign);
        trade = httpUtil.getAddHead(baseUrl + "/spot/order?order_id=" + orderId, head);
        return trade;
    }


    /**
     * 获取余额
     * /spot/assets
     */


    protected String getBalance() {
        Map<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String timestamp = getTimestamp();
        head.put("ACCESS-KEY", apikey);
        head.put("ACCESS-TIMESTAMP", timestamp);
        String sign = HMAC.sha256_HMAC("", exchange.get("tpass"));
        head.put("ACCESS-SIGN", sign);
        String trade = httpUtil.getAddHead(baseUrl + "/spot/assets", head);
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
        String trade = null;
        Map<String, String> param = new TreeMap<>();
        param.put("order_id", orderId);
        String payload = HMAC.splice(param);
        HashMap<String, String> head = new HashMap<String, String>();
        String apikey = exchange.get("apikey");
        String sign = HMAC.sha256_HMAC(payload, exchange.get("tpass"));
        String timestamp = getTimestamp();
        head.put("ACCESS-KEY", apikey);
        head.put("ACCESS-TIMESTAMP", timestamp);
        head.put("ACCESS-SIGN", sign);
        trade = httpUtil.post(baseUrl + "/spot/order/cancel", param, head);
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
            JSONObject jsonObject1 = judgeRes(rt, "list", "getBalance");
            if (rt != null && "0".equals(jsonObject1.getString("code"))) {
                JSONArray coinListss = jsonObject1.getJSONArray("list");
                String firstBalance = null;
                String lastBalance = null;

                for (int i = 0; i < coinListss.size(); i++) {
                    JSONObject jsonObject = JSONObject.fromObject(coinListss.get(i));

                    if (jsonObject.getString("currency").equals(coinArr.get(0))) {
                        firstBalance = jsonObject.getString("free");
                    } else if (jsonObject.getString("currency").equals(coinArr.get(1))) {
                        lastBalance = jsonObject.getString("free");
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
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_DIGIFINEX);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg = false;
        String rt = httpUtil.get(baseUrl + "/markets");
        JSONObject jsonObject1 = judgeRes(rt, "data", "setPrecision");
        if (jsonObject1 != null && "0".equals(jsonObject1.getString("code"))) {
            JSONArray jsonArray = jsonObject1.getJSONArray("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("market").equals(exchange.get("market"))) {
                    String amountPrecision = jsonObject.getString("volume_precision");
                    String pricePrecision = jsonObject.getString("price_precision");
                    String minTradeLimit = "5";//平台接口错误   写死为5
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", minTradeLimit);
                    falg = true;
                    break;
                }
            }

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }


    public String getTimestamp() {
        String res = httpUtil.get(baseUrl + "/time");
        JSONObject resJson = JSONObject.fromObject(res);
        return resJson.getString("server_time");

    }

}
