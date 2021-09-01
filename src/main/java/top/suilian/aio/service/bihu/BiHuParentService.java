package top.suilian.aio.service.bihu;


import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiHuParentService extends BaseService {
    public String baseUrl = "https://www.btb.io/";
    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = false;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];

    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        String rt = null;
        if (isTest) {
            rt = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=rule&cnt=1");
        } else {
            rt = httpUtil.get(baseUrl + "api-web/api/market");
        }
        JSONObject rtObj = JSONObject.fromObject(rt);
        logger.info("交易规则："+rtObj);
        if (rtObj != null && rtObj.getInt("error") == 0) {
            List<Map<String, Object>> datas = (List<Map<String, Object>>) rtObj.get("data");
            for (Map<String, Object> data : datas) {
                if (exchange.get("market").toUpperCase().equals(data.get("market"))) {
                    precision.put("amountPrecision", data.get("amountPrecision"));
                    precision.put("pricePrecision", data.get("pricePrecision"));
                    precision.put("exRate", data.get("exRate"));
                    precision.put("minTradeLimit", data.get("minTradeLimit"));
                }
            }
            return true;
        } else {
            logger.info("获取规则失败, 停止20秒回调重新获取规则");
            setTradeLog(id, "获取交易规则异常", 0, "000000");
            sleep(20000, Integer.parseInt(exchange.get("isMobileSwitch")));
            return false;

        }
    }


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
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
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
                HashMap<String, Object> param = new HashMap<String, Object>();
                param.put("apikey", exchange.get("apikey"));
                param.put("type", String.valueOf(type));
                param.put("market", exchange.get("market"));
                param.put("price", String.valueOf(price1));
                param.put("num", String.valueOf(num));
                param.put("tpass", exchange.get("tpass"));
                logger.info("robotId:" + id + "robotId:" + id + "挂单参数：" + JSON.toJSONString(param));
                if (isTest) {
                    int count;
                    if (submitCnt) {
                        count = cnt * 2 - 1;
                        submitCnt = false;
                    } else {
                        count = cnt * 2;
                        submitCnt = true;
                    }
                    trade = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=submit&cnt=" + count);
                } else {
                    trade = httpUtil.post(baseUrl + "api-web/api/user/trade", param);
                }
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
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("apikey", exchange.get("apikey"));
        param.put("orderId", orderId);
        String url = baseUrl + "api-web/api/user/queryOrderStatus";
        String orderInfo = null;
        if (isTest) {
            orderInfo = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=detail&cnt=" + cnt);
        } else {
            orderInfo = httpUtil.post(url, param);
        }
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
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("apikey", exchange.get("apikey"));
        param.put("id", orderId);

        String res = null;
        if (isTest) {
            res = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=cancel&cnt=" + cnt);
        } else {
            res = httpUtil.post(baseUrl + "api-web/api/user/cancelOrder", param);
        }

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
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("apikey", exchange.get("apikey"));
            String res = httpUtil.post(baseUrl + "api-web/api/user/getAllBanlance", param);
            JSONObject obj = judgeRes(res, "error", "setBalanceRedis");
            if (obj != null && obj.getInt("error") == 0) {
                JSONObject data = obj.getJSONObject("data");
                JSONObject firstCoin = data.getJSONObject(coinArr.get(0));
                String firstBalance = firstCoin.getString("xnb");

                JSONObject lastCoin = data.getJSONObject(coinArr.get(1));
                String lastBalance = lastCoin.getString("xnb");

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
        if (cancelRes != null && cancelRes.getInt("error") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BIHU);
    }
}
