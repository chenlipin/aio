package top.suilian.aio.service.fubt;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

import static top.suilian.aio.Util.HMAC.sha256_HMAC;

public class FubtParentService extends BaseService {
    public String baseUrl = "https://api.fubt.co/v1";
    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = false;
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
     * 交易规则获取
     */
    public boolean setPrecision() {
        Boolean flag=false;
        String rt = null;
        if(exchange.get("isPrecision").equals("1")){
            precision.put("amountPrecision", exchange.get("amountPrescision"));
            precision.put("pricePrecision", exchange.get("pricePrecision"));
            flag=true;
        }else if(exchange.get("isPrecision").equals("0")) {
            String param = "accessKey=" + exchange.get("apikey");//传入参数
            rt = HttpUtil.sendGet(baseUrl + "/market/tradeInfo?", param);

            JSONObject rtObj = judgeRes(rt, "status", "setPrecision");
            if (rtObj != null && rtObj.getString("status").equals("success")) {
                List<Map<String, Object>> datas = (List<Map<String, Object>>) rtObj.get("data");
                for (Map<String, Object> data : datas) {
                    if (exchange.get("market").equals(data.get("symbol"))) {
                        precision.put("amountPrecision", data.get("countPrescision"));
                        precision.put("pricePrecision", data.get("pricePrecision"));
                    }
                }
                flag=true;
            }
        }
        else {
            logger.info("获取规则失败, 停止20秒回调重新获取规则");
            setTradeLog(id, "获取交易规则异常", 0, "000000");
            sleep(20000, Integer.parseInt(exchange.get("isMobileSwitch")));
            flag=false;
        }
        return flag;
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
        String typeStr = type == 1 ? "BUY" : "SELL";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(exchange.get("minTradeLimit"));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }
                Map<String, Object> map = new LinkedHashMap<String, Object>();

                String url = baseUrl + "/order/saveEntrust";
                map.put("count", num);
                map.put("matchType", "LIMIT");
                map.put("payPwd", "121212");
                map.put("price", price1);
                map.put("symbol", exchange.get("market"));
                map.put("type", typeStr);
                map.put("timestamp", System.currentTimeMillis() + "");
                map.put("accessKey", exchange.get("apikey").trim());
                JSONObject jsonObject = JSONObject.fromObject(map);
                logger.info("robotId:" + id + "robotId:" + id + "挂单参数：" + jsonObject);

                trade = HttpUtil.getJsonPost(jsonObject, url, exchange.get("tpass"));


                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId:" + id + "挂单成功结束：" + trade);
            } else {
                setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                logger.info("robotId:" + id + "挂单失败结束");
            }
        } else {
            setTradeLog(id, "交易量最小为：" + exchange.get("minTradeLimit"), 0);
            logger.info("robotId:" + id + "挂单失败结束");
        }
        valid = 1;
        return trade;
    }

    public String submitOrder(int type, BigDecimal price, BigDecimal amount){
        String typeStr = type == 1 ? "BUY" : "SELL";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        String url = baseUrl + "/order/saveEntrust";
        map.put("count", num);
        map.put("matchType", "LIMIT");
        map.put("payPwd", "121212");
        map.put("price", price1);
        map.put("symbol", exchange.get("market"));
        map.put("type", typeStr);
        map.put("timestamp", System.currentTimeMillis() + "");
        map.put("accessKey", exchange.get("apikey").trim());
        JSONObject jsonObject = JSONObject.fromObject(map);
        logger.info("robotId:" + id + "robotId:" + id + "挂单参数：" + jsonObject);

        trade = HttpUtil.getJsonPost(jsonObject, url, exchange.get("tpass"));

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
        String url = baseUrl + "/order/queryOrderById?";
        String param = "orderId=" + orderId + "&accessKey=" + exchange.get("apikey");//传入参数
        String orderInfo = null;
        orderInfo = httpUtil.sendGet(url, param);
        System.out.println(orderInfo);
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
        JSONObject jsonObject = new JSONObject();
        String url = "/order/cancelEntrust";
        jsonObject.put("id", orderId);
        jsonObject.put("timestamp", System.currentTimeMillis() + "");
        jsonObject.put("accessKey", exchange.get("apikey"));
        String res = HttpUtil.getJsonPost(jsonObject, url, exchange.get("tpass"));

        return res;
    }


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
            String url = "https://api.fubt.co/v1/personal/getUserFinanceList?";
            String param = "selectType=" + "all" + "&accessKey=" + exchange.get("apikey");//传入参数
            String firstBalance = null;
            String lastBalance = null;
            String res = httpUtil.sendGet(url, param);
            JSONObject jsonObject = JSONObject.fromObject(res);
            JSONArray data = jsonObject.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                if ((data.getJSONObject(i).getString("coinName").toLowerCase()).equals(coinArr.get(0))) {
                    firstBalance = data.getJSONObject(i).getString("total");
                } else {
                    lastBalance = data.getJSONObject(i).getString("total");
                }
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
        if (cancelRes != null && cancelRes.getString("status").equals("success")) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_FUBT);
    }
}
