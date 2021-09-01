package top.suilian.aio.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpDelete;
import org.omg.CORBA.BAD_CONTEXT;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.service.BaseService;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static top.suilian.aio.service.BaseService.firstIndexOf;
import static top.suilian.aio.service.hwanc.HwancParentService.sign;



public class Main {
    /**
     * @param args
     */

    public static String baseUrl = "https://api.coinvv.com";
    public static String key = "BloibsSk6qyMN0hco8rwgTG7SEhao69B";
    public static String tpass = "d7a33c6b9627b87776eaa652f9fe348d";


    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;
    public String[] transactionArr = new String[24];
    public static Map<String, Object> precision = new HashMap<String, Object>();


    public static void main(String[] args) throws IOException {
        test();
    }

    public static void test(){
        String timestamp = String.valueOf(new Date().getTime());
        int length = timestamp.length();
        String time= String.valueOf(timestamp.substring(0,length-3));

        String orderNumber=getRandomString(18);
        System.out.println(orderNumber);

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("symbol","COPUSDT");
        params.put("id",44425);
        params.put("api_key", "35f0997c-4580-41d5-b1a9-ccdf169b98d0");
        params.put("timestamp", time);
        params.put("sign_type", "MD5");
        Map<String, Object> map = new TreeMap<String, Object>();
        map.put("api_key", "35f0997c-4580-41d5-b1a9-ccdf169b98d0");
        map.put("timestamp", time);
        map.put("sign_type", "MD5");
        String sign=toSort2(map,"FC39E28B9EFA4E1FEC2F486F9C883B16");

        params.put("sign", sign);
        String url = splicingMap("https://www.gwet.io/o/api/order/trades/", params);
        HttpUtil httpUtil = new HttpUtil();
        String rt = httpUtil.get(url);
        System.out.println(rt);
    }

    public static void test2(){
        String timestamp = String.valueOf(new Date().getTime());
        int length = timestamp.length();
        String time= String.valueOf(timestamp.substring(0,length-3));

        String orderNumber=getRandomString(18);
        System.out.println(orderNumber);

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("symbol","COPUSDT");
        params.put("page",1);
        params.put("page_size",20);
        params.put("status",1);
        params.put("api_key", "35f0997c-4580-41d5-b1a9-ccdf169b98d0");
        params.put("timestamp", time);
        params.put("sign_type", "MD5");
        Map<String, Object> map = new TreeMap<String, Object>();
        map.put("api_key", "35f0997c-4580-41d5-b1a9-ccdf169b98d0");
        map.put("timestamp", time);
        map.put("sign_type", "MD5");
        String sign=toSort2(map,"FC39E28B9EFA4E1FEC2F486F9C883B16");
        params.put("sign", sign);
        String url = splicingMap("https://www.gwet.io/o/api/orders", params);
        HttpUtil httpUtil = new HttpUtil();
        String rt = httpUtil.get(url);
        System.out.println(rt);
    }

     public static void text() throws IOException {




         String timestamp = String.valueOf(new Date().getTime());
         int length = timestamp.length();
         String time= String.valueOf(timestamp.substring(0,length-3));
         System.out.println(time);
         Double price= Double.valueOf(1);
         Double volume= Double.valueOf(1);
         System.out.println(price);
         String orderNumber=getRandomString(18);
         System.out.println(orderNumber);

         Map<String, Object> params = new LinkedHashMap<String, Object>();
         params.put("api_key","35f0997c-4580-41d5-b1a9-ccdf169b98d0");
         params.put("o_no",timestamp+"-"+orderNumber);
         params.put("o_price_type", "limit");
         params.put("o_type", "sell");
         params.put("price","1");
         params.put("volume", "1.0");
         params.put("symbol", "COPUSDT");
         params.put("timestamp", time);
         params.put("sign_type", "MD5");
                 String sign=toSort2(params,"FC39E28B9EFA4E1FEC2F486F9C883B16");
         params.put("sign", sign);
         JSONObject json = JSONObject.fromObject(params);



         HttpUtil httpUtil = new HttpUtil();

         String rt = httpUtil.sendPost("https://www.gwet.io/o/api/order",json);
         System.out.println(rt);
     }

    public static String getRandomString(int length){
        //1.  定义一个字符串（A-Z，a-z，0-9）即62个数字字母；
        String str="zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890.-";
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




    public static JSONObject judgeRes(String res, String code, String action) {
        System.out.println(code);
        System.out.println(res);
        if (!"".equals(res) && res != null && isjson(res)) {
            JSONObject resJson = JSONObject.fromObject(res);

            System.out.println(res.indexOf(code));
            if (res.indexOf(code) != -1) {
                return resJson;
            }
//            if (resJson.has(code)) {
//                removeSmsRedis(Constant.KEY_SMS_INTERFACE_ERROR);
//                return resJson;
//            }
        }
        return null;
    }


    private static boolean isjson(String string) {
        try {
            JSONObject jsonStr = JSONObject.fromObject(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }




    public static Boolean setPrecision() {
        Boolean flag =false;
        HttpUtil httpUtil = new HttpUtil();
         Map<String, Object> precision = new HashMap<String, Object>();


        String rt = httpUtil.get(baseUrl + "/open/api/common/symbols");

        JSONObject rtObj = JSONObject.fromObject(rt);


        if (!rt.equals("") && rtObj != null&&"0".equals(rtObj.getString("code"))) {
           JSONArray data = rtObj.getJSONArray("data");
            for (int i =0;i<data.size();i++) {
                if ("btcusdt".equals(data.getJSONObject(i).getString("symbol"))) {

                    precision.remove("price_precision");
                    precision.put("pricePrecision", data.getJSONObject(i).getString("price_precision"));
                    precision.remove("amountPrecision ");
                    precision.put("amountPrecision", data.getJSONObject(i).getString("amount_precision"));
                    precision.put("minTradeLimit", 0.1);
                    System.out.println("交易规则："+precision);
                    flag=true;
                    break;
                }
            }
        }else {
            System.out.println("获取交易规则失败");
        }
        return flag;
    }


    public static String getDepth() {
        HttpUtil httpUtil = new HttpUtil();
        String trade = null;
        String symbol = "btcusdt";
        trade = httpUtil.get(baseUrl+"/open/api/market_dept?type=step0&symbol=" + symbol);
        System.out.println("深度信息：" + trade);
        JSONObject result = JSONObject.fromObject(trade);

        if (!"".equals(trade) && trade != null &&"0".equals(result.getString("code"))) {

            JSONObject data =result.getJSONObject("data");
            JSONObject tick = data.getJSONObject("tick");


            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

            BigDecimal buyPri = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellPri = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));
            System.out.println("买一：" + buyPri + ",卖一：" + sellPri);

        }
        return trade;
    }

    
    public static String cancelTrade(String orderId) {

        HttpUtil httpUtil = new HttpUtil();
        String trade = null;
        String params = null;
        String timestamp = String.valueOf(new Date().getTime());
        HashMap<String, String> header = new HashMap<>();
        Map<String, Object> param = new TreeMap<>();
        param.put("api_key", key);
        param.put("symbol", "btcusdt");
        param.put("time",timestamp);
        param.put("order_id",orderId);
        String parms = addMD5(param,tpass);
        System.out.println("加密參數：" + parms.toString());
        String signature = HMAC.MD5(parms);
        param.put("sign",signature);

        try {
            trade = httpUtil.post(baseUrl + "/open/api/cancel_order",param);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("撤单结果：" + trade);

        return trade;
    }


    public static String selectOrder(String orderId) {

        HttpUtil httpUtil = new HttpUtil();
        String trade = null;
        String timestamp = String.valueOf(new Date().getTime());

        Map<String, Object> param = new TreeMap<>();
        param.put("api_key", key);
        param.put("time", timestamp);
        param.put("symbol","btcusdt");
        param.put("order_id",orderId);
        String params = addMD5(param,tpass);


        System.out.println("加密參數：" + param.toString());
        String signature = HMAC.MD5(params);
        param.put("sign",signature);
        String par =toSort(param,key);

        trade = httpUtil.get(baseUrl + "/open/api/order_info?"+par);
        System.out.println("订单详情" + trade);
        return trade;
    }


    protected static String submitOrder(int type, String price, String amount) {
        String trade = null;
        String parms = null;
        HttpUtil httpUtil = new HttpUtil();
        String timestape = String.valueOf(new Date().getTime());
        Map<String, Object> param = new TreeMap<>();
        String typeStr = null;
        if (type == 1) {
            param.put("side", "BUY");
        } else {
            param.put("side", "SELL");
        }
        param.put("type",1);
        param.put("api_key", key);
        param.put("volume", amount);
        param.put("symbol", "btcusdt");
        param.put("time", timestape);
        param.put("price", price);

        parms = addMD5(param,tpass);
        System.out.println("下单参数：" + parms.toString());
        String signature = HMAC.MD5(parms);
        param.put("sign",signature);

        try {
            trade = httpUtil.post(baseUrl + "/open/api/create_order?",param);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println("下单结果：" + trade);

        return trade;
    }






    private static String toSort(Map<String, Object> map,String key) {
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (entry.getValue() != null) {

                    buffer.append(entry.getKey() + "=");
                    buffer.append(entry.getValue().toString() + "&");



            }

        }
        buffer.append("apiSecret" + "=");
        buffer.append(key);
        System.out.println("加密參數：" + buffer.toString());
        String signature = HMAC.MD5(buffer.toString());
        System.out.println(signature);
        return signature;
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
        System.out.println(signature);
        return signature;
    }




    public  static String  addMD5(Map<String, Object> map, String tpass) {

        String secretKey = tpass;
        /** 拼接签名字符串，md5签名 */
        StringBuilder result = new StringBuilder();

        Set<Map.Entry<String, Object>> entrys = map.entrySet();
        for (Map.Entry<String, Object> param : entrys) {
            /** 去掉签名字段 */
            if (param.getKey().equals("sign")) {
                continue;
            }
            /** 空参数不参与签名 */
            if (param.getValue() != null) {
                result.append(param.getKey());
                result.append(param.getValue().toString());
            }
        }
        result.append(secretKey);
        String signs = result.toString();
        return signs;
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



}

