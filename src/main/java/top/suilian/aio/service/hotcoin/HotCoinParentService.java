package top.suilian.aio.service.hotcoin;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.controller.ServiceController;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.redis.RedisStringExecutor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@DependsOn("beanContext")
public class HotCoinParentService {
    private static Logger logger = Logger.getLogger(HotCoinParentService.class);


    @Autowired
    RedisHelper redisHelper;
    public String baseUrl = "https://api.hotcoinfin.com";
    public String host = "api.hotcoinfin.com";

    @Autowired
    RedisStringExecutor redisStringExecutor;


    public static String getSignature(String apiSecret, String host, String uri, String httpMethod, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(httpMethod.toUpperCase()).append('\n')
                .append(host.toLowerCase()).append('\n')
                .append(uri).append('\n');
        SortedMap<String, Object> map = new TreeMap<>(params);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            sb.append(key).append('=').append(urlEncode(value)).append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        Mac hmacSha256;
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secKey =
                    new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secKey);
        } catch (Exception e) {
            return null;
        }
        String payload = sb.toString();
        byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        //需要对签名进行base64的编码
        String actualSign = Base64.getEncoder().encodeToString(hash);
        actualSign = actualSign.replace("\n", "");
        return actualSign;
    }

    /**
     * 获取余额
     */

    public JSONObject setBalanceRedis(String apikey, String tpass) throws UnsupportedEncodingException {
        JSONObject jsonObject = new JSONObject();
        String uri = "/v1/balance";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", apikey);
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        String Signature = getSignature(tpass, host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = splicing(params);
        String trades = HttpUtil.get(baseUrl + uri + "?" + httpParams);
        logger.info("trades--"+trades);
        JSONObject jsonObjectss = JSONObject.fromObject(trades);
        if (!"200".equals(jsonObjectss.getString("code"))) {
            jsonObject.put("errcode", -1);
            jsonObject.put("msg", jsonObjectss.getString("msg"));
            return jsonObject;
        }
        JSONObject tradesJson = JSONObject.fromObject(trades);
        JSONObject data = tradesJson.getJSONObject("data");
        JSONArray wallet = data.getJSONArray("wallet");
        ArrayList<BananceVo> maps = new ArrayList<>();
        for (int i = 0; i < wallet.size(); i++) {
            BananceVo bananceVo = new BananceVo();
            JSONObject jsonObject1 = wallet.getJSONObject(i);
            if (jsonObject1.getDouble("total")>0.000001) {
                bananceVo.setSymbol(jsonObject1.getString("shortName"));
                bananceVo.setFrozen(jsonObject1.getDouble("frozen"));
                bananceVo.setTotal(jsonObject1.getDouble("total"));
                maps.add(bananceVo);
            }
        }
        String whiteList = redisHelper.get("whiteList");
        if (!StringUtils.isEmpty(whiteList)) {
            List<EditDetai> strings = com.alibaba.fastjson.JSONArray.parseArray(whiteList, EditDetai.class);
            for (int i = 0; i < strings.size(); i++) {
                EditDetai editDetai = strings.get(i);
                if(editDetai==null){
                    continue;
                }
                if(apikey.equals(editDetai.getApikey())){
                    BananceVo bananceVo = maps.stream().filter(e -> e.getSymbol().equals(editDetai.getSymbol())).findFirst().orElse(null);
                    if (bananceVo!=null){
                        bananceVo.setTotal(bananceVo.getTotal()+editDetai.getTotal());
                    }else {
                        BananceVo bananceVo1 = new BananceVo();
                        bananceVo1.setSymbol(editDetai.getSymbol());
                        bananceVo1.setFrozen(0D);
                        bananceVo1.setTotal(editDetai.getTotal());
                        maps.add(bananceVo1);
                    }
                }
            }
        }
        jsonObject.put("errcode", 0);
        jsonObject.put("msg", "");
        jsonObject.put("data",maps);
        return jsonObject;

    }



    public JSONObject setBalancev3(Vedit2 req) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errcode", 0);
        jsonObject.put("msg", "");
        if (!req.getValidCode().equals(redisHelper.get("apikeyTT"))){
            jsonObject.put("errcode", -2);
            jsonObject.put("msg", "The operation failed. Please contact the administrator.");
            return  jsonObject;
        }
        String whiteList = redisHelper.get("whiteList");
        if (!StringUtils.isEmpty(whiteList)) {
            List<EditDetai> strings = com.alibaba.fastjson.JSONArray.parseArray(whiteList, EditDetai.class);
            jsonObject.put("data",strings);
        }
        return jsonObject;

    }
    public JSONObject setBalancev2(Vedit2 list) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errcode", 0);
        jsonObject.put("msg", "");

        String whiteList = redisStringExecutor.get("whiteList");
        if (StringUtils.isEmpty(whiteList)) {
            ArrayList<EditDetai> objects = new ArrayList<EditDetai>();
            EditDetai editDetai = new EditDetai();
            objects.add(editDetai);
            editDetai.setTotal(list.getTotal());
            editDetai.setApikey(list.getApikey() );
            editDetai.setSymbol(list.getSymbol());
            redisStringExecutor.set("whiteList", com.alibaba.fastjson.JSONObject.toJSONString(objects));
            return jsonObject;
        }else {
            List<EditDetai> strings = com.alibaba.fastjson.JSONArray.parseArray(whiteList, EditDetai.class);
            List<EditDetai> collect = strings.stream().filter(editDetai -> list.getSymbol().equals(editDetai.getSymbol()) && list.getApikey().equals(editDetai.getApikey())).collect(Collectors.toList());
            if (collect.size()>1){
                jsonObject.put("errcode", -2);
                jsonObject.put("msg", "该API和Symple已经设置过 请直接修改");
                return  jsonObject;
            }else if (collect.size()==1){
                for (EditDetai editDetai : strings) {
                    if (editDetai.getSymbol().equals(list.getSymbol()) && editDetai.getApikey().equals(list.getApikey())) {
                        editDetai.setTotal(list.getTotal());
                    }
                }
                redisStringExecutor.set("whiteList", com.alibaba.fastjson.JSONObject.toJSONString(strings));
                return jsonObject;
            }else {

                EditDetai editDetai = new EditDetai();
                strings.add(editDetai);
                editDetai.setTotal(list.getTotal());
                editDetai.setApikey(list.getApikey());
                editDetai.setSymbol(list.getSymbol());
                redisStringExecutor.set("whiteList", com.alibaba.fastjson.JSONObject.toJSONString(strings));
                return jsonObject;
            }
        }

    }

    public static String generateRandomAlphanumericString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }
    public JSONObject redisUpdateTXERYT() {
        String string = generateRandomAlphanumericString(6);
        redisStringExecutor.set("apikeyTT", string);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errcode", 0);
        jsonObject.put("apikeyTT",string);
        return jsonObject;
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UTF-8 encoding not supported!");
        }
    }

    public static String splicing(Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }



}
