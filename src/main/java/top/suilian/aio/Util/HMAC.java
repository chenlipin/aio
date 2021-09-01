package top.suilian.aio.Util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class HMAC {
    /**
     * 将加密后的字节数组转换成字符串
     *
     * @param b 字节数组
     * @return 字符串
     */
    public static String byteArrayToHexString(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1)
                hs.append('0');
            hs.append(stmp);
        }
        return hs.toString().toLowerCase();
    }


    /**
     * sha256_HMAC加密
     *
     * @param message 消息
     * @param secret  秘钥
     * @return 加密后字符串
     */
    public static String sha256_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes());
            hash = byteArrayToHexString(bytes);
        } catch (Exception e) {
            System.out.println("Error HmacSHA256 ===========" + e.getMessage());
        }
        return hash;
    }


    public static byte[] HmacSHA384(String message, String secret) {
        String hash = "";
        byte[] bytes = new byte[0];
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA384");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA384");
            sha256_HMAC.init(secret_key);
            bytes = sha256_HMAC.doFinal(message.getBytes("UTF-8"));
        } catch (Exception e) {
            System.out.println("Error HmacSHA384 ===========" + e.getMessage());
        }
        return bytes;
    }


    /**
     * md5_HMAC
     *
     * @param message 消息
     * @param secret  秘钥
     * @return 加密后字符串
     */
    public static String md5_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac md5_HMAC = Mac.getInstance("HmacMD5");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacMD5");
            md5_HMAC.init(secret_key);
            byte[] bytes = md5_HMAC.doFinal(message.getBytes());
            hash = byteArrayToHexString(bytes);
        } catch (Exception e) {
            System.out.println("Error HmacMD5 ===========" + e.getMessage());
        }
        return hash;
    }


    public static String genHMAC(String data, String key) {
        byte[] result = null;
        try {
            //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
            SecretKeySpec signinKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            //生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance("HmacSHA256");
            //用给定密钥初始化 Mac 对象
            mac.init(signinKey);
            //完成 Mac 操作
            byte[] rawHmac = mac.doFinal(data.getBytes());
            result = Base64.encodeBase64(rawHmac);

        } catch (NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println(e.getMessage());
        }
        if (null != result) {
            return new String(result);
        } else {
            return null;
        }
    }


    public static String jsonToString(String json){
        JSONObject jsonObject = JSON.parseObject(json);
        Map<String, Object> treeMap = new TreeMap<>();
        jsonObject.forEach((s, o) -> treeMap.put(s, o));
        StringBuilder result = new StringBuilder();
        treeMap.forEach((s, o) -> {
            StringBuilder stringBuilder = new StringBuilder();
            if (o instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) o;
                jsonArray.forEach(o1 -> {
                    stringBuilder.append(o1).append(",");
                });
                stringBuilder.deleteCharAt(stringBuilder.length() -1);
            } else if (o instanceof JSONObject) {
                stringBuilder.append(jsonToString(((JSONObject) o).toJSONString()));
            } else {
                stringBuilder.append(o);
            }
            result.append(s).append("=").append(stringBuilder.toString()).append("&");
        });
        result.deleteCharAt(result.length() -1);
        return result.toString();
    }

    /**
     * sha256_HMAC加密
     *
     * @param json 参数信息
     * @param secret 秘钥
     * @return 加密后字符串
     */
    public static String sha256_HMAC1(String json, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(json.getBytes());
            hash = java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.out.println("Error HmacSHA256 ===========" + e.getMessage());
        }
        return hash;
    }


    public static String Hmac_SHA1(String encryptText, String encryptKey)
        {
            byte[] data= new byte[0];
            String hash="";
            try {
                data = encryptKey.getBytes("UTF-8");
                SecretKey secretKey = new SecretKeySpec(data, "HmacSHA1");
                //生成一个指定 Mac 算法 的 Mac 对象
                Mac mac = Mac.getInstance("HmacSHA1");
                //用给定密钥初始化 Mac 对象
                mac.init(secretKey);
                byte[] text = encryptText.getBytes("UTF-8");
                byte[] bytes = mac.doFinal(text);
                hash = java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                System.out.println("Error HmacSHA1 ==========="+e.getMessage());
            }


        return hash;
    }



    public static String Hmac_SHA512(String encryptText, String encryptKey)
    {
        byte[] data= new byte[0];
        String hash="";
        try {
            data = encryptKey.getBytes("UTF-8");
            SecretKey secretKey = new SecretKeySpec(data, "HmacSHA512");
            //生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance("HmacSHA512");
            //用给定密钥初始化 Mac 对象
            mac.init(secretKey);
            byte[] text = encryptText.getBytes("UTF-8");
            byte[] bytes = mac.doFinal(text);
            hash = byteArrayToHexString(bytes);
        } catch (Exception e) {
            System.out.println("Error HmacSHA512 ==========="+e.getMessage());
        }


        return hash;
    }


    public static String MD5(String str){

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //使用指定的字节数组对摘要进行最后更新，然后完成摘要计算
        byte[] results = md5.digest(str.getBytes());
        //String  hash = java.util.Base64.getEncoder().encodeToString(results);
        //将得到的字节数组变成字符串返回
        String result = byteArrayToHexString(results);

        return result;
    }


    public static String encryption(String plainText) {
        String re_md5 = new String();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }

            re_md5 = buf.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return re_md5;
    }

    public static String Base64(String str){
        Base64 base64 = new Base64();
        String encodedText=null;
        try {
             encodedText = base64.encodeToString(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedText;
    }


    /**
     * 拼接字符串
     * 格式a=1&b=2&c=3
     * @param params
     * @return
     */

    public static String splice(Map<String, String> params) {

        StringBuffer httpParams = new StringBuffer();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            httpParams.append(key).append("=").append(value).append("&");
        }
        if (httpParams.length() > 0) {
            httpParams.deleteCharAt(httpParams.length() - 1);
        }
        return httpParams.toString();
    }


}
