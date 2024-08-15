/*
 * Copyright (C) 1997-2022 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.controller;

import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <B>Description:</B> <br>
 * <B>Create on:</B> 2024/8/15 15:00 <br>
 * bg2hyw2dfg-33781639-187ea5d3-77d94
 *
 * 48e691ab-6dfe18bc-7ee6dfb2-43c8c
 * @author dong.wan
 * @version 1.0
 */
public class Test {
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    private static final ZoneId ZONE_GMT = ZoneId.of("Z");
    public static void main(String[] args) {
//        String strSign = "POST" + "\n" +
//                "api.huobi.pro" + "\n" +
//                "/v1/account/accounts" + "\n";
//        Map<String, String> params = new TreeMap<String, String>();
//        params.put("AccessKeyId","bg2hyw2dfg-33781639-187ea5d3-77d94");
//        params.put("SignatureMethod","HmacSHA256");
//        params.put("SignatureVersion","2");
//        params.put("Timestamp",gmtNow());
//        String splicingStr = splicingStr(params);
//
//        strSign=strSign+splicingStr;
//        System.out.println(strSign);
        getbalanceInfo();

    }


    public static void getbalanceInfo(){
        String strSign = "POST" + "\n" +
                "api.huobi.pro" + "\n" +
                "/v1/order/orders/place" + "\n";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("AccessKeyId","bg2hyw2dfg-33781639-187ea5d3-77d94");
        params.put("SignatureMethod","HmacSHA256");
        params.put("SignatureVersion","2");
        params.put("Timestamp",gmtNow());
        String splicingStr = splicingStr(params);
        strSign=strSign+splicingStr;
        System.out.println(strSign);
        Map<String, Object> reqparam = new TreeMap<String, Object>();
        reqparam.put("account-id", "62914536");
        reqparam.put("symbol", "trxusdt");
        reqparam.put("type", "buy-limit");
        reqparam.put("amount", "10");
        reqparam.put("price", "0.1");

        String sign = HMAC.sha256_HMACAndBase(strSign, "48e691ab-6dfe18bc-7ee6dfb2-43c8c");
        sign = urlEncode(sign);
        System.out.println(sign);

        System.out.println("https://api.huobi.pro/v1/order/orders/place?" + splicingStr + "&Signature=" + sign);

        String s = null;
        try {
            s = HttpUtil.postes("https://api.huobi.pro/v1/order/orders/place?" + splicingStr + "&Signature=" + sign,reqparam);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        System.out.println(s);
    }


    static String gmtNow() {
        return Instant.ofEpochSecond(epochNow()).atZone(ZONE_GMT).format(DT_FORMAT);
    }
    private static long epochNow() {
        return Instant.now().getEpochSecond();
    }
    public static String splicingStr(Map<String, String> params)   {
        StringBuilder httpParams = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            try {
                httpParams.append(key).append("=").append(urlEncode(value)).append("&");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


}
