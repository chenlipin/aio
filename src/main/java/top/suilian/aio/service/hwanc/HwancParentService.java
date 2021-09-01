package top.suilian.aio.service.hwanc;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class HwancParentService extends BaseService {

    public String baseUrl = "https://api.hwanc.com";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;


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

        String timestamp = String.valueOf(new Date().getTime());
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

                    Map<String, String> params = new HashMap<>();
                    String coin1 = exchange.get("market").split("_")[1];
                    String coin2 = exchange.get("market").split("_")[0];
                    params.put("coin_code1", coin1);
                    params.put("coin_code2", coin2);
                    if (type == 1) {
                        params.put("type", "buy");
                    } else {
                        params.put("type", "sell");
                    }
                    params.put("price", String.valueOf(price1));
                    params.put("nums", String.valueOf(num));
                    params.put("ot", "1");

                    HashMap<String, String> headers = new HashMap<>();
                    String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

                    headers.put("Authorization", sign);


                    logger.info("robotId" + id + "----" + "挂单参数：" + params);

                    trade = httpUtil.post("https://api.hwanc.com/v1/bot/Market/Order", params, headers);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, String> params = new HashMap<>();
                String coin1 = exchange.get("market").split("_")[1];
                String coin2 = exchange.get("market").split("_")[0];
                params.put("coin_code1", coin1);
                params.put("coin_code2", coin2);
                if (type == 1) {
                    params.put("type", "buy");
                } else {
                    params.put("type", "sell");
                }
                params.put("price", String.valueOf(price1));
                params.put("nums", String.valueOf(num));
                params.put("ot", "1");

                HashMap<String, String> headers = new HashMap<>();
                String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

                headers.put("Authorization", sign);


                logger.info("robotId" + id + "----" + "挂单参数：" + params);

                trade = httpUtil.post("https://api.hwanc.com/v1/bot/Market/Order", params, headers);

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
        Map<String, String> params = new HashMap<>();

        params.put("id", orderId);

        HashMap<String, String> headers = new HashMap<>();
        String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

        headers.put("Authorization", sign);

        String orderInfo = httpUtil.post("https://api.hwanc.com/v1/bot/Market/OrderDetail", params, headers);

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

        Map<String, String> params = new HashMap<>();

        params.put("id", orderId);

        HashMap<String, String> headers = new HashMap<>();
        String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

        headers.put("Authorization", sign);

        String res = httpUtil.post("https://api.hwanc.com/v1/bot/Market/CancelOrder", params, headers);

        return res;
    }


    /**
     * 获取余额
     */

    public String getBalance(String coin_code1) {
        Map<String, String> params = new HashMap<>();

        params.put("coin_code1", coin_code1);
        params.put("wallet_name","market");

        HashMap<String, String> headers = new HashMap<>();
        String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

        headers.put("Authorization", sign);

        String balacne = null;
        try {
            balacne = httpUtil.post("https://api.hwanc.com/v1/bot/Account/CoinBalance", params, headers);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return balacne;
    }


    /**
     * 将余额存入redis
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

            String firstResult = getBalance(coinArr.get(0));
            JSONObject firstJson = JSONObject.fromObject(firstResult);
            String firstBalance = "";

            if (firstJson.getInt("code") == 200) {
                JSONObject datas = firstJson.getJSONObject("datas");
                firstBalance = datas.getString("Balance");
            }
            String lastResult = getBalance(coinArr.get(1));
            JSONObject lastJson = JSONObject.fromObject(lastResult);
            String lastBalance = "";
            System.out.println(firstJson);
            System.out.println(lastJson);


            if (lastJson.getInt("code") == 200) {
                JSONObject datas = lastJson.getJSONObject("datas");
                lastBalance = datas.getString("Balance");
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
        if (cancelRes != null && cancelRes.getString("code").equals("200")) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_HWANC);
    }


    /**
     * 获取深度
     */

    public String getDepth() {
        HttpUtil httpUtil = new HttpUtil();


        Map<String, String> params = new HashMap<>();

        String[] split = exchange.get("market").split("_");

        params.put("coin_code1", split[1]);
        params.put("coin_code2", split[0]);

        HashMap<String, String> headers = new HashMap<>();
        String sign = sign(exchange.get("apikey"), exchange.get("tpass"));

        headers.put("Authorization", sign);

        String depth = null;
        try {
            depth = httpUtil.post("https://api.hwanc.com/v1/bot/Market/Deep", params, headers);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return depth;
    }

    /**
     * 交易规则获取
     */
    public void setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }


    public static String sign(String appid, String tpass) {
        String sign = appid + " " + DigestUtils.md5Hex(tpass);
        return sign;
    }
}
