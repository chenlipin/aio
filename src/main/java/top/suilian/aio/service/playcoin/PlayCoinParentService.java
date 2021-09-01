package top.suilian.aio.service.playcoin;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlayCoinParentService extends BaseService {
    static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    static final ZoneId ZONE_GMT = ZoneId.of("Z");
    public String baseUrl = "http://api.playcoin.net/boot_exchange_ms/robot/openapi";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    String accountId = "0";
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
     * 下单
     *
     * @param type
     * @param price
     * @param amount
     * @return
     * @throws UnsupportedEncodingException
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
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
                    HashMap<String, String> params = new HashMap<>();
                    params.put("appkey", "873098148");
                    TreeMap<String, String> map = new TreeMap<>();
                    map.put("currentUserMarkKey", exchange.get("currentUserMarkKey"));
                    map.put("type", type + "");
                    map.put("currencyCodeTrade", exchange.get("currencyCodeTrade"));
                    map.put("currencyCodeBase", exchange.get("currencyCodeBase"));
                    map.put("currencyCodeTradeQuota", amount + "");
                    map.put("transactionPairsQuotaExpect", price + "");
                    map.put("tradeType", "1");
                    trade = httpUtil.post(baseUrl + "/addCoinTradeOrder.do", map, params);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put("appkey", "873098148");
                TreeMap<String, String> map = new TreeMap<>();
                map.put("type", type + "");
                map.put("currentUserMarkKey", exchange.get("currentUserMarkKey"));
                map.put("currencyCodeTrade", exchange.get("currencyCodeTrade"));
                map.put("currencyCodeBase", exchange.get("currencyCodeBase"));
                map.put("currencyCodeTradeQuota", amount + "");
                map.put("transactionPairsQuotaExpect", price + "");
                map.put("tradeType", "1");
                trade = httpUtil.post(baseUrl + "/addCoinTradeOrder.do", map, params);

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

    public String submitOrder(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;
        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        HashMap<String, String> params = new HashMap<>();
        params.put("appkey", "873098148");
        TreeMap<String, String> map = new TreeMap<>();
        map.put("currentUserMarkKey", exchange.get("currentUserMarkKey"));
        map.put("type", type + "");
        map.put("currencyCodeTrade", exchange.get("currencyCodeTrade"));
        map.put("currencyCodeBase", exchange.get("currencyCodeBase"));
        map.put("currencyCodeTradeQuota", amount + "");
        map.put("transactionPairsQuotaExpect", price + "");
        map.put("tradeType", "1");
        trade = httpUtil.post(baseUrl + "/addCoinTradeOrder.do", map, params);

        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

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
        HashMap<String, String> params = new HashMap<>();
        params.put("appkey", "873098148");
        String trade = httpUtil.getAddHead(baseUrl + "/loadUserOrders.do?currentUserMarkKey=" + exchange.get("currentUserMarkKey") + "&orderId=" + orderId + "&isCurrent=2&start=0&limit=1", params);
        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId, String type) throws UnsupportedEncodingException {
        HashMap<String, String> params = new HashMap<>();
        params.put("appkey", "873098148");
        Map<String, String> map = new TreeMap<>();
        map.put("currentUserMarkKey", exchange.get("currentUserMarkKey"));
        map.put("orderId", orderId);
        map.put("type", type);
        String trade = httpUtil.post(baseUrl + "/cancelCoinTradeOrder.do", map, params);
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
            String firstBalance = null;
            String lastBalance = null;
            firstBalance = findBalanceByCoinName(coinArr.get(0));
            lastBalance = findBalanceByCoinName(coinArr.get(1));
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance);
            balances.put(coinArr.get(1), lastBalance);
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            logger.info("setBalanceParam获取余额" + firstBalance + "&&" + lastBalance);
        } else {
            logger.info("获取余额失败");
        }

    }


    public String findBalanceByCoinName(String currencyCode) {
        String str = "0";
        HashMap<String, String> map = new HashMap<>();
        map.put("appkey", "873098148");
        Map<String, String> maps = new TreeMap<>();
        maps.put("currentUserMarkKey", exchange.get("currentUserMarkKey"));
        maps.put("currencyCode", currencyCode);

        String rt = httpUtil.getAddHead(baseUrl + "//loadUserCurrencyDetail.do?currentUserMarkKey=" + exchange.get("currentUserMarkKey") + "&currencyCode=" + currencyCode, map);
        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");
        if (rtObj != null && rtObj.getInt("code") == 0) {
            str = rtObj.getJSONObject("data").getString("quotaBalance");
        }
        return str;
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
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_XOXOEX);
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() throws UnsupportedEncodingException {
        HashMap<String, String> map = new HashMap<>();
        map.put("appkey", "873098148");
        String rt = httpUtil.getAddHead(baseUrl + "/loadTransactionPairsListByCurrencyCodeBase.do?currencyCodeBase=" + exchange.get("currencyCodeBase"), map);
        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");
        if (rtObj != null && rtObj.getInt("code") == 0) {
            JSONArray data = rtObj.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject jsonObject = data.getJSONObject(i);
                if (jsonObject.getString("currencyCodeTrade").equals(exchange.get("currencyCodeTrade")) && jsonObject.getString("currencyCodeBase").equals(exchange.get("currencyCodeBase"))) {
                    String amountPrecision = jsonObject.getString("numberDecimalPlaces");
                    String pricePrecision = jsonObject.getString("priceDecimalPlaces");
                    String minTradeLimit = jsonObject.getString("minimumTradingVolume");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", minTradeLimit);
                    break;
                }
            }
        }

    }


}