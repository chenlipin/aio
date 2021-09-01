package top.suilian.aio.service.gwet;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class GwetParentService extends BaseService {
    public String baseUrl = "https://www.gwet.io";

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
    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
        String timestamp = String.valueOf(new Date().getTime());
        int length = timestamp.length();
        String time= String.valueOf(timestamp.substring(0,length-3));
        String typeStr = type == 1 ? "buy" : "sell";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;
        String orderNum="";
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


                    Map<String, Object> params = new TreeMap<String, Object>();
                    String orderNumber=getRandomString(18);
                    params.put("api_key", exchange.get("apikey"));
                    orderNum=timestamp+'-'+orderNumber;
                    params.put("o_no", timestamp+'-'+orderNumber);
                    params.put("o_price_type", "limit");
                    if (type == 1) {
                        params.put("o_type", "BUY");
                    } else {
                        params.put("o_type", "SELL");
                    }

                    params.put("price", price1);
                    params.put("volume", num);
                    params.put("symbol", exchange.get("market"));
                    params.put("timestamp", time);
                    params.put("sign_type", "MD5");

                    String sign = toSort2(params , exchange.get("tpass"));

                    params.put("sign", sign);



                    logger.info("robotId" + id + "----" + "挂单参数：" + params);
                    JSONObject json = JSONObject.fromObject(params);

                    trade = httpUtil.sendPost(baseUrl + "/o/api/order", json);

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                Map<String, Object> params = new TreeMap<String, Object>();
                String orderNumber=getRandomString(18);
                params.put("api_key", exchange.get("apikey"));
                orderNum=timestamp+'-'+orderNumber;
                params.put("o_no",timestamp+'-'+orderNumber);
                params.put("o_price_type", "market");
                if (type == 1) {
                    params.put("o_type", "BUY");
                } else {
                    params.put("o_type", "SELL");
                }

                params.put("price", price1);
                params.put("volume", num);
                params.put("symbol", exchange.get("market"));
                params.put("time", time);
                params.put("sign_type", "MD5");

                String sign = toSort2(params , exchange.get("tpass"));

                params.put("sign", sign);


                logger.info("robotId" + id + "----" + "挂单参数：" + params);
                JSONObject json = JSONObject.fromObject(params);

                trade = httpUtil.sendPost(baseUrl + "/o/api/order", json);


                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
            }


        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }

        valid = 1;
        String res= trade+"/"+orderNum;
        return res;

    }


    public static String getRandomString(int length){
        //1.  定义一个字符串（A-Z，a-z，0-9）即62个数字字母；
        String str="zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        //2.  由Random生成随机数
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        //3.  长度为几就循环几次
        for(int i=0; i<length; ++i){
            //从62个的数字或字母中选择
            int number=random.nextInt(62);
            //将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        //将承载的字符转换成字符串
        return sb.toString();
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {

        String timestamp = String.valueOf(new Date().getTime());
        int length = timestamp.length();
        String time= timestamp.substring(0,length-3);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("symbol",exchange.get("market"));
        params.put("id",orderId);
        params.put("api_key", exchange.get("apikey"));
        params.put("timestamp", time);
        params.put("sign_type", "MD5");
        Map<String, Object> map = new TreeMap<String, Object>();
        map.put("api_key", exchange.get("apikey"));
        map.put("timestamp", time);
        map.put("sign_type", "MD5");
        String sign=toSort2(map,exchange.get("tpass"));
        params.put("sign", sign);
        String url = splicingMap("https://www.gwet.io/o/api/order/trades", params);
        HttpUtil httpUtil = new HttpUtil();
        String rt = httpUtil.get(url);
        return rt;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId,String Ono) throws IOException {
        String timestamp = String.valueOf(new Date().getTime());
        int length = timestamp.length();
        String time= String.valueOf(timestamp.substring(0,length-3));
        Map<String, Object> map = new TreeMap<String, Object>();
        map.put("api_key",exchange.get("apikey"));
        map.put("o_no", Ono);
        map.put("id", orderId);
        map.put("symbol", exchange.get("market"));
        map.put("timestamp", time);
        map.put("sign_type", "MD5");
        String sign=toSort2(map,exchange.get("spass"));
        map.put("sign", sign);
        HttpUtil httpUtil = new HttpUtil();
        String json = JSON.toJSONString(map);
        String rt = httpUtil.delete("https://www.gwet.io/o/api/order",json);
        return rt;
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

            String timestamp = String.valueOf(new Date().getTime()/1000);

            Integer time= Integer.valueOf(timestamp);


            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("api_key", exchange.get("apikey"));
            params.put("timestamp", time);
            params.put("sign_type", "MD5");

            Map<String, Object> map = new TreeMap<String, Object>();
            map.put("api_key", exchange.get("apikey"));
            map.put("timestamp", time);
            map.put("sign_type", "MD5");
            String sign=toSort2(map,exchange.get("tpass"));
            params.put("sign", sign);



            String url = splicingMap(baseUrl + "/a/api/accounts", params);

            String rt = httpUtil.get(url);
            JSONObject obj = JSONObject.fromObject(rt);
            if(obj!=null&&obj.getJSONObject("msg").toString()=="success"){
                JSONArray coinLists = obj.getJSONArray("accounts");
                String firstBalance = null;
                String lastBalance = null;


                for (int i = 0; i < coinLists.size(); i++) {
                    JSONObject jsonObject = coinLists.getJSONObject(i);

                    if (jsonObject.getString("coin").equals(coinArr.get(0))) {
                        firstBalance = jsonObject.getString("balance");
                    } else if (jsonObject.getString("coin").equals(coinArr.get(1))) {
                        lastBalance = jsonObject.getString("balance");
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
        if (cancelRes != null && cancelRes.getInt("code")==0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_GWET);
    }


    /**
     * 交易规则获取
     */
    public boolean setPrecision() {
        boolean falg=false;
        String rt = httpUtil.get(baseUrl + "/m/symbol");

        JSONObject rtObj = judgeRes(rt, "symbols", "price_precision");

        if (!rt.equals("") && rtObj != null) {
            JSONArray jsonArray = rtObj.getJSONArray("symbols");

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("symbol").equals(exchange.get("market"))) {
                    String amountPrecision = jsonObject.getString("volume_precision");

                    String pricePrecision = jsonObject.getString("price_precision");
                    precision.put("amountPrecision", amountPrecision);
                    precision.put("pricePrecision", pricePrecision);
                    precision.put("minTradeLimit", exchange.get("numMinThreshold"));
                    falg=true;
                }
            }

        }else{
            setTradeLog(id, "获取交易规则异常", 0);
        }
        return falg;
    }

    private static String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, Object> entry :
                map.entrySet()) {
            /** 去掉签名字段 */
            if (entry.getKey().equals("sign")) {
                continue;
            }

            /** 空参数不参与签名 */
            if (entry.getValue() != null) {
                buffer.append(entry.getKey());
                buffer.append(entry.getValue().toString());
            }

        }
        return buffer.toString();
    }


    private static String toSort2(Map<String, Object> map,String key) {
        StringBuffer buffer = new StringBuffer();
        map=sortMapByKey(map);
        int max = map.size() - 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (entry.getValue() != null) {
                buffer.append(entry.getKey() + "=");
                buffer.append(entry.getValue().toString() + "&");
            }

        }
        buffer.append("apiSecret" + "=");
        buffer.append(key);
        System.out.println("加密參數：" + buffer.toString());
        String signature = HMAC.MD5(buffer.toString()).substring(0,28);
        return signature;
    }

    /**
     * 使用 Map按key进行排序
     * @param map
     * @return
     */
    public static Map<String, Object> sortMapByKey(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
//        Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());
        Map<String, Object> sortMap = new TreeMap<String, Object>(new Comparator<String>() {
            public int compare(String obj1, String obj2) {
                return obj1.compareTo(obj2);//升序排序
            }
        });
        sortMap.putAll(map);
        return sortMap;
    }



    private static String splicingMap(String url, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
            int x = 1;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                url = url + "/";
                url += String.valueOf(entry.getValue());
                x++;
            }
        }
        return url;
    }

}
