package top.suilian.aio.service.nivex0;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.DateUtils;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.mxc.randomDepth.RunMxcDeep;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

@DependsOn("beanContext")
@Service
public class NivexParentService extends BaseService implements RobotAction {

    public String baseUrl = "https://api.oihqfjapi.online/";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    public RunMxcDeep runMxcDeep = BeanContext.getBean(RunMxcDeep.class);

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




    @Override
    public List<getAllOrderPonse> selectOrder() {
        String dealList = getDealList();
        JSONArray array = JSONObject.fromObject(dealList).getJSONObject("data").getJSONArray("list");

        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            JSONObject jsonObject = array.getJSONObject(i);
            getAllOrderPonse.setOrderId(jsonObject.getString("order_id"));
            getAllOrderPonse.setCreatedAt(jsonObject.getString("created_at"));
            getAllOrderPonse.setPrice(jsonObject.getString("side")+"-"+jsonObject.getString("price"));
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(jsonObject.getString("num"));
            getAllOrderPonses.add(getAllOrderPonse);
        }
        return getAllOrderPonses;
    }

    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    /**
     * {
     *     "status": 200,
     *     "source": "API",
     *     "msg": "OK",
     *     "data": {
     *         "order_id": 34712416,
     *         "user_id": 10100014887,
     *         "client_user_id": null,
     *         "symbol": "ppt_usdt",
     *         "side": "buy",
     *         "price": "0.10000",
     *         "total": "10.000000000000000000",
     *         "over_num": "100.00",
     *         "deal_num": 0,
     *         "deal_total": 0,
     *         "deal_avg_price": "0.10000",
     *         "fee": 0,
     *         "status": 0
     *     },
     *     "seconds": 1754318397,
     *     "microtime": 1754318397468,
     *     "curl_id": "6890c63d5dae6240920417",
     *     "host": "127.0.0.1"
     * }
     */

    //下单
    protected String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        long time = System.currentTimeMillis() / 1000;
        int number = generate6DigitNumber();
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("random_int_str", number);
        params.put("time", time);
        params.put("symbol",  exchange.get("market"));
        params.put("amount", num);
        params.put("type", type == 1?"buy":"sell");
        params.put("price",price1);
        String sort = toSort(params)+exchange.get("tpass");
        String sign = HMAC.MD5(sort);
        params.put("sign",sign);
        logger.info("下单参数：" + params);

        String trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/Deal/dealLimitOrder", params);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            logger.info("submitOrder返回参数：" + trade);
            if(jsonObject==null||200!=jsonObject.getInt("status")){
                setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }
    public static int generate6DigitNumber() {
        // 生成100000到999999之间的随机整数
        Random random = new Random();
        return random.nextInt(900000) + 100000;
    }

    /**
     * 查询订单详情
     {
     "status": 200,
     "source": "API",
     "msg": "OK",
     "data": {
     "order_id": 34712416,
     "user_id": 10100014887,
     "symbol": "ppt_usdt",
     "side": "buy",
     "price": "0.1",
     "total": "10",
     "num": "100",
     "over_num": "100",
     "deal_num": "0",
     "deal_total": "0",
     "deal_avg_price": "0.1",
     "fee": "0.00000000",
     "status": 0,
     "created_at": "2025-08-04 22:39:57.438737",
     "updated_at": "2025-08-04 22:39:57.463384"
     },
     "seconds": 1754319105,
     "microtime": 1754319105141,
     "curl_id": "6890c9011e456170344615",
     "host": "127.0.0.1"
     }
     */


    public String selectOrder(String orderId)   {

        long time = System.currentTimeMillis() / 1000;
        int number = generate6DigitNumber();
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("random_int_str", number);
        params.put("time", time);
        params.put("symbol",  exchange.get("market"));
        params.put("id",orderId);
        String sort = toSort(params)+exchange.get("tpass");
        String sign = HMAC.MD5(sort);
        params.put("sign",sign);
        logger.info("查询订单参数：" + params);


        String  trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/Deal/dealDetail", params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("status")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        return trade;
    }

    /**
     * 撤单
     {
     "status": 200,
     "msg": "ok",
     "data": {
     "num": "100.000000000000000000",
     "price": "0.100000000000000000",
     "total": "10.000000000000000000",
     "status": 3,
     "user_id": 10100014887,
     "client_user_id": "",
     "trust_id": 34712416,
     "over_num": "100.000000000000000000",
     "deal_num": "0.000000000000000000",
     "deal_total": "0.000000000000000000",
     "deal_avg_price": "0.100000000000000000",
     "fee": "0.000000000000000000",
     "symbol_id": 1213,
     "side": "buy",
     "total_fee": "0.000000000000000000",
     "command_id": "175427116300000003"
     },
     "microtime": 1754320227585,
     "extend": {
     "date": "2025-08-04 23:10:27",
     "unique": "1f616607"
     }
     }
     */
    public String cancelTrade(String orderId) {

        long time = System.currentTimeMillis() / 1000;
        int number = generate6DigitNumber();
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("random_int_str", number);
        params.put("time", time);
        params.put("symbol",  exchange.get("market"));
        params.put("id",orderId);
        String sort = toSort(params)+exchange.get("tpass");
        String sign = HMAC.MD5(sort);
        params.put("sign",sign);
        logger.info("撤销订单参数：" + params);
        String  trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/Deal/cancelDeal", params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("status")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
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
            String firstBalance = null;
            String lastBalance = null;
            String firstBalancefrozen = null;
            String lastBalancefrozen = null;
            String rt = getBalance();
            JSONObject obj = JSONObject.fromObject(rt);
            if (obj != null && "200".equals(obj.getString("status"))) {
                JSONArray data = obj.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    if (jsonObject.getString("name").equalsIgnoreCase(coinArr.get(0))){
                        firstBalance = jsonObject.getString("num");
                        firstBalancefrozen = jsonObject.getString("lock_num");
                    }
                    if (jsonObject.getString("name").equalsIgnoreCase(coinArr.get(1))){
                        lastBalance = jsonObject.getString("num");
                        lastBalancefrozen = jsonObject.getString("lock_num");
                    }
                }

            }

            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance+"_"+firstBalancefrozen);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalancefrozen);
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
        if (cancelRes != null && cancelRes.getInt("code") == 200) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_WBFEX);
    }


    public String getDepth() {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("symbol",  exchange.get("market"));

        String  trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/First/Business/depth", params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("status")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        return trade;
    }

    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
      precision.put("minTradeLimit",exchange.get("minTradeLimit"));
      return true;
    }

    /**
     * 获取余额
     */


    protected String getBalance() {
        long time = System.currentTimeMillis() / 1000;
        int number = generate6DigitNumber();
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("random_int_str", number);
        params.put("time", time);
        String sort = toSort(params)+exchange.get("tpass");
        String sign = HMAC.MD5(sort);
        params.put("sign",sign);
        logger.info("查询余额参数：" + params);


        String  trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/Property/getUserProperty", params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("status")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }

        logger.info("账户信息" + trade);
        return trade;
    }



    public static String getSecondTimestamp(Date date) {
        if (null == date) {
            return "0";
        }
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        if (length > 3) {
            return String.valueOf(timestamp.substring(0, length - 3));
        } else {
            return "0";
        }
    }


    private static String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();

        int i = 0;
        int max = map.size() - 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (entry.getValue() != null) {

                if (i == max) {
                    buffer.append(entry.getKey() + "=");
                    buffer.append(entry.getValue().toString());
                } else {
                    buffer.append(entry.getKey() + "=");
                    buffer.append(entry.getValue().toString() + "&");
                }
                i++;

            }

        }
        return buffer.toString();
    }

    @Override
    public Map<String, String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ( "200".equals(jsonObject.getString("status"))) {
                orderId  = jsonObject.getJSONObject("data").getString("order_id");
                hashMap.put("res", "true");
                hashMap.put("orderId", orderId);
            } else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res", "false");
                hashMap.put("orderId", msg);
            }
        }
        return hashMap;
    }

    @Override
    public Map<String, Integer> selectOrderStr(String orderId) {
        String s = null;
        try {
            s = selectOrder(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.fromObject(s);
        JSONArray data = jsonObject.getJSONArray("data");
        HashMap<String, Integer> map = new HashMap<>();
        for (Object datum : data) {
            JSONObject jsonObject1 = JSONObject.fromObject(datum.toString());
            String order_Id = jsonObject1.getString("id");
            String state = jsonObject1.getString("state");
            map.put(order_Id,getTradeEnum(state).getStatus());
        }
        return map;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = cancelTrade(orderId);
        com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
        if(jsonObject.getInteger("status")==200){
            return "true";
        }else {
            return "false";
        }

    }

    public TradeEnum getTradeEnum(String status) {
        switch (status) {
            case "NEW":
                return TradeEnum.NOTRADE;

            case "FILLED":
                return TradeEnum.NOTRADED;

            case "CANCELED":
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }

    public String getDealList( )   {

        long time = System.currentTimeMillis() / 1000;
        int number = generate6DigitNumber();
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", exchange.get("apikey"));
        params.put("random_int_str", number);
        params.put("time", time);
        params.put("symbol",  exchange.get("market"));

        String sort = toSort(params)+exchange.get("tpass");
        String sign = HMAC.MD5(sort);
        params.put("sign",sign);
        logger.info("查询没成交订单参数：" + params);


        String  trade = null;
        try {
            trade = HttpUtil.post(baseUrl + "/Deal/getDealList", params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(jsonObject==null||200!=jsonObject.getInt("status")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        return trade;
    }
}
