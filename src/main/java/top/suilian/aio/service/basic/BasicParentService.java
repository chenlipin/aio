package top.suilian.aio.service.basic;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@DependsOn("beanContext")
@Service
public class BasicParentService extends BaseService {
    public String baseUrl = "http://45.32.127.96:11100";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    public String submitOrder(List<OrderVO> list) {
        Map<String, String> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String apiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", apiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("orders",com.alibaba.fastjson.JSONObject.toJSONString(list));
        String post = "";
        try {
            logger.info("挂单参数" + com.alibaba.fastjson.JSONObject.toJSONString(list));
            post = httpRequest(baseUrl + "/api/v1/placeOrders", map);
            setTradeLog(id, "挂单结果" + post, 0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    public String submitOrder(OrderVO order) {
        Map<String, String> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String apiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", apiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("number",order.getNumber());
        map.put("pair",order.getPair());
        map.put("price",order.getPrice());
        map.put("type",order.getType());
        String post = "";
        try {
            post = httpRequest(baseUrl + "/api/v1/place", map);
            setTradeLog(id, "挂单结果" + post, 0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    public void cancalOrder(com.alibaba.fastjson.JSONArray list, String pair) {
        for (Object o : list) {
            cancalOrder(o.toString());
        }
    }

    public String cancalOrder(String orderId) {
        Map<String, String> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String ApiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", ApiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("orderId", orderId);
        String post = "";
        try {
            post = httpRequest(baseUrl + "/api/v1/cancel", map);
            setTradeLog(id, orderId+"撤单结果" + post, 0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sleep(500,1);
        return post;
    }

    public String selectOrder(String orderId) {
        Map<String, String> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String ApiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", ApiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("orderId", orderId);
        String post = "";
        try {
            post = httpRequest(baseUrl + "/api/v1/detailById", map);
            setTradeLog(id, orderId+"查询订单结果" + post, 0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

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

    public String submitTrade(int type, BigDecimal price, BigDecimal amount) {
        // 输出字符串
        String trade = null;
        BigDecimal price1 = nN(price, Integer.valueOf(exchange.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(exchange.get("amountPrecision").toString()));
        Map<String, String> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String apiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", apiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("number",amount+"");
        map.put("pair",exchange.get("market"));
        map.put("price",price+"");
        map.put("type",type==0?"buy":"sell");
        try {
            logger.info("挂单参数" + map);
            trade = httpRequest(baseUrl + "/api/v1/place", map);
            setTradeLog(id, "挂单结果" + trade, 0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        setTradeLog(id, "挂" + (type == 0 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }


    //获取余额
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


            Map<String, Object> map = new TreeMap<>();
            String timestamp = System.currentTimeMillis() + "";
            String ApiKey = exchange.get("apikey");
            String tpass = exchange.get("tpass");
            Map params = new HashMap();
            params.put("timestamp",System.currentTimeMillis());
            params.put("ApiKey",ApiKey);
            params.put("ApiSecret",tpass);
            String url = baseUrl+"/api/v1/balanceList";
            String string = httpRequest(url, params);
            JSONObject jsonObject = JSONObject.fromObject(string);
            if (jsonObject != null && jsonObject.getInt("status") == 0) {
                JSONArray data = jsonObject.getJSONArray("data");
                String firstBalance = "";
                String firstBalancefreeze = "";
                String lastBalance = "";
                String lastBalancefreeze = "";
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject1 = data.getJSONObject(i);
                    if (jsonObject1.getString("coinCode").equals(coinArr.get(0))) {
                        firstBalance = jsonObject1.getString("balanceAvailable");
                        firstBalancefreeze = jsonObject1.getString("balanceFrozen");
                    }
                    if (jsonObject1.getString("coinCode").equals(coinArr.get(1))) {
                        lastBalance = jsonObject1.getString("balanceAvailable");
                        lastBalancefreeze = jsonObject1.getString("balanceFrozen");
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance + "_" + firstBalancefreeze);
                balances.put(coinArr.get(1), lastBalance + "_" + lastBalancefreeze);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }
        }
    }
    private static String httpRequest(String requestUrl, Map params) {
        //buffer用于接受返回的字符
        StringBuffer buffer = new StringBuffer();
        try {
            //建立URL，把请求地址给补全，其中urlencode（）方法用于把params里的参数给取出来
            URL url = new URL(requestUrl + "?" + urlencode(params) + "sign=" + makeSign(params));
            //打开http连接
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("POST");
            httpUrlConn.connect();

            //获得输入
            InputStream inputStream = httpUrlConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            //将bufferReader的值给放到buffer里
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            System.out.println(buffer);
            //关闭bufferReader和输入流
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            inputStream = null;
            //断开连接
            httpUrlConn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回字符串
        return buffer.toString();
    }





    private static String urlencode(Map<String, Object> data) {
        //将map里的参数变成像 showapi_appid=###&showapi_sign=###&的样子
        return getString(data);
    }
    //进行url encode编码
    private static String getString(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry i : data.entrySet()) {
            try {
                sb.append(i.getKey()).append("=").append(URLEncoder.encode(i.getValue() + "", "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
//        return MD5Utils.MD5(sb.toString());

    }
    //拼接参数的字符串
    private static String getString1(Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder(120);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            stringBuilder.append(entry.getKey()).append("=").append(value).append("&");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
//        return MD5Utils.MD5(sb.toString());

    }


    public static String makeSign(Map<String, String> makeOrderParams) {
        SortedMap<String, String> paramsMaps = new TreeMap<>();//声明一个map  得到所有的参数  类型转换

        for (String field : makeOrderParams.keySet()) {//遍历map
            if (!field.equals("sign")) {//判断map里面不是sign的所有的参数
                Object value = makeOrderParams.get(field);
                if (value != null) {
                    String valueStr = null;
                    if (value instanceof BigDecimal) {
                        valueStr = ((BigDecimal) value).stripTrailingZeros().toPlainString();
                    } else {
                        valueStr = value.toString();
                    }
                    paramsMaps.put(field, valueStr);
                }
            }
        }
        String signStr = getString1(paramsMaps);//把所有的参数转换为字符串
        return Objects.requireNonNull(MD5(signStr)).toLowerCase();//通过md5加密后转换为小写
    }



    public final static String MD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }



}
