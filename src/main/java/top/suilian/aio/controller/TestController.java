package top.suilian.aio.controller;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.refer.BianUtils;
import top.suilian.aio.refer.DeepVo;
import top.suilian.aio.service.ExceptionMessageService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

@RestController
public class TestController extends BaseController {

    public Logger logger;
    @Autowired
    ExceptionMessageService exceptionMessageService;
    @Autowired
    HttpUtil httpUtil;


    @RequestMapping("/insertTest")
    public void inset() {
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("apikey", "82c55df624868b7e4ac3c312e154a124");

        String res = null;
        try {
            res = HttpUtil.post("https://api.bihuex.com/api-web/api/user/getAllBanlance", param);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject obj = JSONObject.fromObject(res);
            JSONObject data = obj.getJSONObject("data");
            JSONObject dylcInfo = data.getJSONObject("dylc");
            String dylc = dylcInfo.getString("xnb");

            JSONObject usdtInfo = data.getJSONObject("eth");
            String eth = usdtInfo.getString("xnb");
            System.out.println(dylc);
            System.out.println(eth);

    }

    @RequestMapping("/getFChainBalance")
    public String getBalance() {
        List<String> coinArr = new ArrayList<>();
        coinArr.add("esp");
        coinArr.add("drink");
        String timestamp = String.valueOf(new Date().getTime());
        String params = "&recvWindow = 5000 & timestamp = " + timestamp;

        String tpass = "JF1djQoFtnP9WtHA27bpV3Zhy1zVOYv1gemUlsZkbMNsgSt5l39VjdCyGFI4eyWY";
        String signs = HMAC.sha256_HMAC(params, tpass);
        String apikey = "XvJMHiRefneZxRqSf1Q6ALbM0rW0Q8FcWIltFIBlg6qwFJ91wSsHlw7QunOrveOG";

        String par = params + "&signature=" + signs;

        String baseUrl = "https://api.fchain.one";

        String res = httpUtil.doPost(baseUrl + "/openapi/v1/order", par, apikey);


        JSONObject obj = JSONObject.fromObject(res);
        JSONArray data = obj.getJSONArray("balances");
        String firstBalance = null;
        String lastBalance = null;
        for (int i = 0; i < data.size(); i++) {
            if (data.getJSONObject(i).getString("asset").equals(coinArr.get(0))) {
                firstBalance = data.getJSONObject(i).getString("free");
            } else {
                lastBalance = data.getJSONObject(i).getString("free");
            }
        }


        HashMap<String, String> balances = new HashMap<>();
        balances.put(coinArr.get(0), firstBalance);
        balances.put(coinArr.get(1), lastBalance);

        System.out.println(balances);
        return null;
    }


    public static void main(String[] args) throws UnsupportedEncodingException {


        String market = "bpay_usdt";
        String key = "ak01ee7389a92c4d73";
        String tpass = "a066b3b57a4545119131190164a85674";

        HttpUtil httpUtil = new HttpUtil();
        String timestamp = String.valueOf(new Date().getTime());
        String baseUrl = "https://api.betterex.pro/";
        int orderTtpe = 0;


        HashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("method", "getOrders");
        params.put("accesskey", key);
        params.put("tradeType", String.valueOf(orderTtpe));
        params.put("currency", market);
        params.put("pageIndex", "1");
        params.put("pageSize", "20");

        String signs = gotoSign(params, tpass);

        params.put("sign", signs);
        String tradeOrders = httpUtil.get(baseUrl + "/api/v2/getOrders?method=getOrders&accesskey=" + key + "&tradeType=" + orderTtpe + "&currency=" + market + "&pageIndex=1&pageSize=20&sign=" + signs + "&reqTime=" + timestamp);
        System.out.println(tradeOrders);

//        HashMap<String, String> params = new HashMap<String, String>();
//


        HashMap<String, String> param = new LinkedHashMap<String, String>();
        param.put("method", "getOrders");
        param.put("accesskey", key);
        param.put("tradeType", String.valueOf(orderTtpe));
        param.put("currency", market);
        param.put("pageIndex", "1");
        param.put("pageSize", "20");

        String sign = gotoSign(param, tpass);


        String a = "https://api.betterex.pro/api/v2/getOrders?method=getOrders&accesskey=" + key + "&tradeType=" + orderTtpe + "&currency=" + market + "&pageIndex=1&pageSize=20&sign=" + sign + "&reqTime=" + timestamp;
        String re = httpUtil.get(a);


        System.out.println(re);


    }


    public static String seletcOrder(String orderId) {
        String market = "ddt_usdt";
        String baseUrl = "https://api.betterex.pro/";
        String key = "ak01ee7389a92c4d73";
        String tpass = "a066b3b57a4545119131190164a85674";

        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "getOrder");
        param.put("accesskey", key);
        param.put("id", orderId);
        param.put("currencyCode", market);
        String sign = gotoSign(param, tpass);
        long timestamp = new Date().getTime();
        HttpUtil httpUtil = new HttpUtil();
        String orderInfo = httpUtil.get(baseUrl + "/api/v2/getOrder?method=getOrder&accesskey=" + key + "&id=" + orderId + "&currencyCode=" + market + "&sign=" + sign + "&reqTime=" + timestamp);
        return orderInfo;
    }


    public static String submitOrder(int type, BigDecimal price, BigDecimal num) {

        //1173796417594720256

        String timestamp = String.valueOf(new Date().getTime());

        String market = "bpay_usdt";
        String baseUrl = "https://api.betterex.pro";
        String key = "ak01ee7389a92c4d73";
        String tpass = "a066b3b57a4545119131190164a85674";

        HashMap<String, String> param = new HashMap<String, String>();
        param.put("method", "order");
        param.put("accesskey", key);
        param.put("price", String.valueOf(price));
        param.put("amount", String.valueOf(num));
        param.put("tradeType", String.valueOf(type));
        param.put("currency", market);
        String sign = gotoSign(param, tpass);
        HttpUtil httpUtil = new HttpUtil();
        String trade = httpUtil.get(baseUrl + "/api/v2/order?amount=" + num + "&method=order&accesskey=" + key + "&price=" + price + "&currency=" + market + "&tradeType=" + type + "&sign=" + sign + "&reqTime=" + timestamp);


        return trade;
    }


    public static String getLaexBalance(String apikey, String tpass) {
        HashMap<String, String> param = new HashMap<String, String>();
        String baseUrl = "https://api.betterex.pro";

        HttpUtil httpUtil = new HttpUtil();
        param.put("method", "getAccountInfo");
        param.put("accesskey", apikey);
        String sign = gotoSign(param, tpass);
        long timestamp = new Date().getTime();
        String orderInfo = httpUtil.get(baseUrl + "/api/v2/getAccountInfo?method=getAccountInfo&accesskey=" + apikey + "&sign=" + sign + "&reqTime=" + timestamp);

        return orderInfo;
    }


    public static String gotoSign(HashMap<String, String> params, String secret) {
        String sign = "";
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
            byte[] sha1Encode = sha1Digest.digest(secret.getBytes());
            String signSecret = convertByteToHexString(sha1Encode);
            String sortStr = keySortToString(params);
            SecretKeySpec sk = new SecretKeySpec(signSecret.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(sk);
            return convertByteToHexString(mac.doFinal(sortStr.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return sign;
    }


    public static String keySortToString(HashMap<String, String> params) {
        String str = "";
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            str += entry.getKey() + "=" + entry.getValue() + "&";
        }
        return str.substring(0, str.length() - 1);
    }


    public static String convertByteToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            int temp = bytes[i] & 0xff;
            String tempHex = Integer.toHexString(temp);
            if (tempHex.length() < 2) {
                result += "0" + tempHex;
            } else {
                result += tempHex;
            }
        }
        return result;
    }

//    public static void main(String[] args) {
//        List<Object> a = new ArrayList<>();
//        a.add(12);
//        a.add(13);
//        a.add(14);
//        List<Object> b = new LinkedList<>();
//        b.add(12);
//        b.add(13);
//        b.add(14);
//
//        for (int i = 0; i < a.size(); i++) {
//            if (i == 0){
//                a.remove(0);
//            }
////            System.out.println(a.get(2));
//        }
//
//
//
//        for (int i = 0; i < b.size(); i++) {
//            if (i == 0){
//                b.remove(0);
//            }
////            System.out.println(b.get(2));
//        }
//
//    }

    private static String toSort(Map<String, Object> map) {
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, Object> entry :
                map.entrySet()) {
            /** ?????????????????? */
            if (entry.getKey().equals("sign")) {
                continue;
            }

            /** ???????????????????????? */
            if (entry.getValue() != null) {
                buffer.append(entry.getKey());
                buffer.append(entry.getValue().toString());
            }

        }
        return buffer.toString();
    }

    private static String splicingMap(String url, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
            int x = 1;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (x == 1) {
                    url = url + "?";
                } else {
                    url = url + "&";
                }
                url += entry.getKey() + "=" + String.valueOf(entry.getValue());
                x++;
            }
        }
        return url;
    }


    public static String getMD5(String info) {
        try {
            //?????? MessageDigest ?????????????????? MD5 ?????????????????????????????? MD5 ????????????????????? SHA1 ???????????????
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            //update(byte[])????????????????????????
            //??????StringBuilder?????????append()?????????????????????????????????????????????????????????
            md5.update(info.getBytes("UTF-8"));

            //digest()????????????,MessageDigest???????????????????????????????????????????????????????????????????????????MD5????????????????????????reset()????????????????????????
            //digest()?????????16???????????????????????????byte[]??????
            byte[] md5Array = md5.digest();

            //byte[]???????????????????????????????????????32??????????????????????????????,??????????????????????????????????????????
            return bytesToHex(md5Array);
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] md5Array) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < md5Array.length; i++) {
            int temp = 0xff & md5Array[i];
            String hexString = Integer.toHexString(temp);
            if (hexString.length() == 1) {//????????????????????????0f??????????????????f??????????????????0
                strBuilder.append("0").append(hexString);
            } else {
                strBuilder.append(hexString);
            }
        }
        return strBuilder.toString();
    }

    @GetMapping("/testde")
    public void demo() {
        List<DeepVo> btcusdt = BianUtils.getHistory("BTCUSDT");
        String string = com.alibaba.fastjson.JSONObject.toJSONString(btcusdt);
        System.out.println(string);
    }


}