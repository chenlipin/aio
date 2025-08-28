package top.suilian.aio.service.superex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.DateUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;
import top.suilian.aio.vo.getAllOrderPonse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

@Service
@DependsOn("beanContext")
public class SuperexParentService extends BaseService implements RobotAction {
    public String baseUrl = "https://api.superexchang.com/api";
    public RunHotcoinRandomDepth runHotcoinRandomDepth = BeanContext.getBean(RunHotcoinRandomDepth.class);

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;

    @Override
    public List<getAllOrderPonse> selectOrder() {
        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();
        String uri = "https://api.superex.com/spot/spot/order?days=30&status=1";
        Map<String, String> params = new TreeMap<>();
        params.put("currencyPair", exchange.get("market"));
        HashMap<String, String> head = new HashMap<>();
        head.put("x-api-key",exchange.get("apikey"));
        head.put("accept-language","zh-TC");

        String trade=null;
        try {
            trade = HttpUtil.getAddHead(uri,head);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JSONObject jsonObject1 = JSONObject.fromObject(trade);
        if(!"200".equals(jsonObject1.getString("code"))){
            setWarmLog(id,3,"API接口错误",jsonObject1.getString("msg"));
        }
        JSONArray array = jsonObject1.getJSONObject("data").getJSONArray("items");
        for (int i = 0; i < array.size(); i++) {
            getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
            JSONObject jsonObject = array.getJSONObject(i);

            getAllOrderPonse.setOrderId(jsonObject.getString("id"));
            getAllOrderPonse.setCreatedAt(DateUtils.convertTimestampToString(jsonObject.getLong("updateTime")));
            getAllOrderPonse.setPrice((jsonObject.getInt("tradeType")==1?"BUY":"SELL")+"-"+new BigDecimal(jsonObject.getString("price")).stripTrailingZeros().toPlainString());
            getAllOrderPonse.setStatus(0);
            getAllOrderPonse.setAmount(new BigDecimal(jsonObject.getString("orderNumber")).stripTrailingZeros().toPlainString());
            getAllOrderPonses.add(getAllOrderPonse);
        }

        return getAllOrderPonses;
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



    public  String getDepth(){
        String trades = httpUtil.get("https://data.gateapi.io/api2/1/orderBook/"+exchange.get("market") );
        return trades;
    }

    /**
     *{
     * 	"code":200,
     * 	"data":479427268484018176,
     * 	"msg":"委託成功"
     * }
     * @param type
     * @param price
     * @param amount
     * @return
     */
    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String typeStr = type == 1 ? "买" : "卖";
        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
        // 输出字符串
        String trade = null;
        String time = System.currentTimeMillis() + "";
        BigDecimal price1 = nN(price, Integer.parseInt(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(exchange.get("amountPrecision").toString()));
        String uri = "https://api.superex.com/spot/spot/order?days=7&status=0";
        Map<String, String> params = new TreeMap<>();
        params.put("orderNumber", num+"");
        params.put("orderPriceType", "1");
        params.put("symbol", exchange.get("market"));
        params.put("price", price1+"");
        params.put("tradeType",type+"");

        HashMap<String, String> head = new HashMap<>();
        head.put("x-api-key",exchange.get("apikey"));
        head.put("accept-language","zh-TC");
        try {
            trade = HttpUtil.postByPackcoin( uri ,params,head);
            System.out.println(trade);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);

        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }


    /**
     * 查询订单详情
     *{
     * 	"code":200,
     * 	"data":{
     * 		"averagePrice":0,
     * 		"createTime":1732205612000,
     * 		"deductionFee":0,
     * 		"deductionFeeCurrency":"",
     * 		"fee":0,
     * 		"feeCurrency":"",
     * 		"id":479442552821399552,
     * 		"isStopOrder":0,
     * 		"orderNumber":"15",
     * 		"orderPriceType":1,
     * 		"orderRecordList":[],
     * 		"price":"0.8",
     * 		"status":1,
     * 		"symbol":"usdc_usdt",
     * 		"tradeNumber":"0",
     * 		"tradeTotal":"0",
     * 		"tradeType":1,
     * 		"updateTime":1732205612000,
     * 		"userId":28345004
     *        },
     * 	"msg":"查詢成功"
     * }
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) {
        String uri = "https://api.superex.com/spot/spot/order/records/detail?orderId="+orderId;

        HashMap<String, String> head = new HashMap<>();
        head.put("x-api-key",exchange.get("apikey"));
        head.put("accept-language","zh-TC");
        String trade=null;
        try {
            trade = HttpUtil.getAddHead(uri,head);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);

        return trade;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return {
     * 	"code":200,
     * 	"data":null,
     * 	"msg":"撤單成功"
     * }
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        String uri = "https://api.superex.com/spot/spot/order/repeal?id="+orderId;
        Map<String, String> params = new TreeMap<>();
        params.put("id",orderId);

        HashMap<String, String> head = new HashMap<>();
        head.put("x-api-key",exchange.get("apikey"));
        head.put("accept-language","zh-TC");
        String trade=null;
        try {
            trade = HttpUtil.post( uri ,params,head);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);

        logger.info("撤销订单："+orderId+"  结果"+trade);
        return trade;
    }

    public String getDeep(){
        String uri = "https://api.superex.com/spot/spot/assets";
        HashMap<String, String> head = new HashMap<>();
        head.put("x-api-key",exchange.get("apikey"));
        head.put("accept-language","zh-TC");

        String trade=null;
        try {
            trade = HttpUtil.getAddHead(uri,head);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = JSONObject.fromObject(trade);
        return trade;
    }




    public static String splicing(Map<String, Object> params)  {
        StringBuilder httpParams = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(value).append("&");
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }


    /**
     * 获取余额
     */

    public void setBalanceRedis()  {
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
            List<String> coinArr = Arrays.asList(coins.toUpperCase().split("_"));

            String deep = getDeep();


            String firstBalance = null;
            String lastBalance = null;
            String firstBalance1 = null;
            String lastBalance1 = null;
            com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(deep);
            com.alibaba.fastjson.JSONArray array = object.getJSONObject("data").getJSONArray("assetList");

            for (int i = 0; i < array.size(); i++) {
                com.alibaba.fastjson.JSONObject jsonObject = array.getJSONObject(i);
                if (coinArr.get(0).equalsIgnoreCase(jsonObject.getString("currency"))) {
                    firstBalance = jsonObject.getString("available");
                    firstBalance1 = jsonObject.getString("frozen");
                }
                if (coinArr.get(1).equalsIgnoreCase(jsonObject.getString("currency"))) {
                    lastBalance = jsonObject.getString("available");
                    lastBalance1 = jsonObject.getString("frozen");
                }
            }

            if (lastBalance != null) {
                if (Double.parseDouble(lastBalance) < 10) {
                    setWarmLog(id, 0, "余额不足", coinArr.get(1).toUpperCase() + "余额为:" + lastBalance);
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0), firstBalance + "_" + firstBalance1);
            balances.put(coinArr.get(1), lastBalance + "_" + lastBalance1);
            logger.info("获取余额" + com.alibaba.fastjson.JSONObject.toJSONString(balances));
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
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_HOTCOIN);
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {

        precision.put("amountPrecision", exchange.get("amountPrecision"));
        precision.put("pricePrecision", exchange.get("pricePrecision"));
        precision.put("minTradeLimit", exchange.get("minTradeLimit"));
    }





    @Override
    public Map<String,String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getString("data");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
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
            if ("200".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }


    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 1:
                return TradeEnum.NOTRADE;

            case 2:
                return TradeEnum.TRADEING;

            case 3:
                return TradeEnum.NOTRADED;

            case 4:
                return TradeEnum.CANCEL;

            case 5:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }
}
