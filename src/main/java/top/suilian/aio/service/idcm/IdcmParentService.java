package top.suilian.aio.service.idcm;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class IdcmParentService extends BaseService {
    public String baseUrl = "https://api.IDCM.cc:8323/api/v1/";
    public Map<String, String> precision = new HashMap<String, String>();
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
    public void setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
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
        String typeStr = type == 0 ? "买" : "卖";
        logger.info("robotId:" + id + "robotId:" + id + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        String trade = null;

        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));

        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            if (price1.compareTo(BigDecimal.ZERO) > 0) {
                HashMap<String, Object> param = new HashMap<String, Object>();
                param.put("Symbol", exchange.get("market").toUpperCase());
                param.put("Size", num);
                param.put("Price", price1);
                param.put("Side", type);
                param.put("Type", 1);
                HashMap<String, String> headers = getHeaders(param, exchange.get("tpass"), exchange.get("apikey"));
                logger.info("robotId:" + id + "robotId:" + id + "挂单参数：" + JSON.toJSONString(headers).replace("\\", ""));
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
                    trade = httpUtil.post(baseUrl + "trade", new HashMap<String, String>(), headers);
                }
                setTradeLog(id, "量化挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 0 ? "05cbc8" : "ff6224");
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
        param.put("Symbol", exchange.get("market").toUpperCase());
        param.put("OrderID", orderId);
        String orderInfo = null;
        if (isTest) {
            orderInfo = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=detail&cnt=" + cnt);
        } else {
            orderInfo = httpUtil.post(baseUrl + "getorderinfo", new HashMap<String, String>(), getHeaders(param, exchange.get("tpass"), exchange.get("apikey")));
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
    public String cancelTrade(String orderId, int type) throws UnsupportedEncodingException {
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("Symbol", exchange.get("market").toUpperCase());
        param.put("OrderID", orderId);
        param.put("Side", type);

        String res = null;
        if (isTest) {
            res = httpUtil.get("http://120.77.223.226:8017/?exchange=bihu&action=cancel&cnt=" + cnt);
        } else {
            res = httpUtil.post(baseUrl + "cancel_order", new HashMap<String, String>(), getHeaders(param, exchange.get("tpass"), exchange.get("apikey")));
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
            param.put("Symbol", exchange.get("market").toUpperCase());
            String res = httpUtil.post(baseUrl + "getuserinfo", new HashMap<String, String>(), getHeaders(param, exchange.get("tpass"), exchange.get("apikey")));
            JSONObject obj = judgeRes(res, "result", "setBalanceRedis");
            if (obj != null && obj.getInt("result") == 1) {
                JSONArray datas = obj.getJSONArray("data");
                String firstBalance = null;
                String lastBalance = null;
                for (Object data : datas) {
                    JSONObject info = JSONObject.fromObject(data.toString());
                    if (coinArr.get(0).toUpperCase().equals(info.getString("code"))) {
                        firstBalance = info.getString("free");
                        if (lastBalance != null) {
                            break;
                        }
                    } else if (coinArr.get(1).toUpperCase().equals(info.getString("code"))) {
                        lastBalance = info.getString("free");
                        if (firstBalance != null) {
                            break;
                        }
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance);
                balances.put(coinArr.get(1), lastBalance);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
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
        if (cancelRes != null && cancelRes.getInt("result") == 1) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_IDCM);
    }

    public HashMap<String, String> getHeaders(HashMap<String, Object> params, String tpass, String apikey) {
        byte[] notsign = HMAC.HmacSHA384(JSON.toJSONString(params), tpass);
        String sign = Base64.getEncoder().encodeToString(notsign);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("X-IDCM-APIKEY", apikey);
        headers.put("X-IDCM-SIGNATURE", sign);
        headers.put("X-IDCM-INPUT", JSON.toJSONString(params));
        return headers;
    }
}
