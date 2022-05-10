package top.suilian.aio.service.asproex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class AsproexParentService extends BaseService {
    public String baseUrl = "https://api.asproex.com";

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
     *
     * @param type
     * @param price
     * @param amount
     * @return
     * @throws UnsupportedEncodingException
     */
    public String submitTrade(int type, BigDecimal price, BigDecimal amount)   {

        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }
                    String params = "symbol=" + exchange.get("market") + "&side=" + (type == 1 ? "BUY" : "SELL") + "&type=LIMIT&timeInForce=GTC&quantity=" + String.valueOf(num) + "&price=" + String.valueOf(price1) + "&recvWindow=5000&timestamp=" + timestamp;

                    String signs = HMAC.sha256_HMAC(params, exchange.get("tpass"));


                    String par = params + "&signature=" + signs;

                    logger.info("robotId" + id + "----" + "挂单参数：" + par);

                    trade = httpUtil.doPost(baseUrl + "/api/v1/order", par, exchange.get("apikey"));

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                String params = "symbol=" + exchange.get("market") + "&side=" + (type == 1 ? "BUY" : "SELL") + "&type=LIMIT&timeInForce=GTC&quantity=" + String.valueOf(num) + "&price=" + String.valueOf(price1) + "&recvWindow=5000&timestamp=" + timestamp;

                String signs = HMAC.sha256_HMAC(params, exchange.get("tpass"));

                String par = params + "&signature=" + signs;

                logger.info("robotId" + id + "----" + "挂单参数：" + par);

                trade = httpUtil.doPost(baseUrl + "/openapi/v1/order", par, exchange.get("apikey"));

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

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        String param = "orderId=" + orderId + "&timestamp=" + new Date().getTime();
        String signs = HMAC.sha256_HMAC(param, exchange.get("tpass"));
        String par = param + "&signature=" + signs;
        String orderInfo = httpUtil.doGet(baseUrl + "/openapi/v1/order?" + par, exchange.get("apikey"));
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

        String timestamp = String.valueOf(new Date().getTime());
        String param = "orderId=" + orderId + "&timestamp=" + timestamp;
        String signs = HMAC.sha256_HMAC(param, exchange.get("tpass"));
        String par = param + "&signature=" + signs;
        String res = httpUtil.doDelete(baseUrl + "/openapi/v1/order?" + par, "", exchange.get("apikey"));
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

            String timestamp = String.valueOf(new Date().getTime());
            String params = "recvWindow=5000&timestamp=" + timestamp;

            String signs = HMAC.sha256_HMAC(params, exchange.get("tpass"));


            String par = params + "&signature=" + signs;

            logger.info("robotId" + id + "----" + "挂单参数：" + par);

            String res = httpUtil.doGet(baseUrl + "/openapi/v1/account?" + par, exchange.get("apikey"));


            JSONObject obj = JSONObject.fromObject(res);
            if(obj!=null&&obj.getJSONArray("balances")!=null){
                JSONArray data = obj.getJSONArray("balances");
                String firstBalance = null;
                String lastBalance = null;
                for (int i = 0; i < data.size(); i++) {
                    if (data.getJSONObject(i).getString("asset").equals(coinArr.get(0).toUpperCase())) {
                        firstBalance = data.getJSONObject(i).getString("free");
                    } else if (data.getJSONObject(i).getString("asset").equals(coinArr.get(1).toUpperCase())) {
                        lastBalance = data.getJSONObject(i).getString("free");
                    }
                }


                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
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
        if (cancelRes != null && cancelRes.getString("status").equals("CANCELED")) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BITAI);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        String rt = httpUtil.get(baseUrl + "/openapi/v1/brokerInfo");

        JSONObject rtObj = judgeRes(rt, "symbols", "setPrecision");

        if (!rt.equals("") && rtObj != null) {
            List<Map<String, Object>> symbols = (List<Map<String, Object>>) rtObj.get("symbols");
            for (Map<String, Object> data : symbols) {
                if (exchange.get("market").toUpperCase().equals(data.get("symbol"))) {

                    JSONArray filtersArr = (JSONArray) data.get("filters");

                    String minQty = filtersArr.getJSONObject(1).getString("minQty");
                    String amountPrecision=String.valueOf(data.get("baseAssetPrecision"));
                    int amountIndex=amountPrecision.indexOf(".");
                    String pricePrecision=String.valueOf(data.get("quotePrecision"));
                    int priceIndex=pricePrecision.indexOf(".");


                    precision.put("pricePrecision", pricePrecision.length()-priceIndex-1 );
                    precision.put("amountPrecision", amountPrecision.length()-amountIndex-1);
                    precision.put("minTradeLimit", minQty);
                    logger.info("交易规则："+precision);
                }
            }
            return true;

        } else {
            setTradeLog(id, "获取交易规则异常", 0);
            return false;
        }
    }
}
