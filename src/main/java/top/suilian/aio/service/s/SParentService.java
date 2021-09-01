package top.suilian.aio.service.s;

import net.sf.json.JSONObject;
import org.apache.catalina.manager.Constants;
import org.apache.commons.lang.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static top.suilian.aio.Util.HMAC.genHMAC;

public class SParentService extends BaseService {
    public String baseUrl = "https://exapi.s.top";

    //region    可配置参数
    //endregion
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
        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }

                String toSign = getToSign(exchange.get("apikey"));

                String notSign = "POST\nexapi.s.top\n/v1/order/create\n" + toSign;

                String sign = genHMAC(notSign, exchange.get("tpass"));


                String url = baseUrl + "/v1/order/create?" + toSign + "&Signature=" +
                        sign;


                HashMap<String, Object> params = new HashMap();
                params.put("symbol", exchange.get("market"));
                params.put("price", price.toString());
                params.put("amount", amount.toString());
                if (type == 1) {
                    params.put("type", "buy-limit");
                } else if (type == 2) {
                    params.put("type", "sell-limit");

                }

                logger.info("robotId" + id + "----" + "挂单参数：" + JSONObject.fromObject(params));


                trade = httpUtil.post(url, params);

                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");


            } else {
                setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
            }
        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
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
        String toSign = getToSign(exchange.get("apikey"));

        String sign = "GET\nexapi.s.top\n/v1/order/detail/" + orderId + "\n" + toSign;
        String signE = genHMAC(sign, exchange.get("tpass"));
        String url = "https://exapi.s.top/v1/order/detail/" + orderId + "?" + toSign +
                "&Signature=" +
                signE;

        String re = httpUtil.get(url);
        return re;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
        String toSign = getToSign(exchange.get("apikey"));

        String sign = "GET\nexapi.s.top\n/v1/order/cancel/" + orderId + "\n" + toSign;
        String signE = genHMAC(sign, exchange.get("tpass"));
        String url = "https://exapi.s.top/v1/order/cancel/" + orderId + "?" + toSign +
                "&Signature=" +
                signE;

        String re = httpUtil.get(url);
        return re;
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
        if (cancelRes != null && cancelRes.getString("status").equals("ok")) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.valueOf(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_S);
    }


    public static String getToSign(String apikey) {
        Map<String, String> par = new HashMap();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String timeStamp = dateFormat.format(date);
        par.put("AccessKeyId", apikey);
        par.put("SignatureMethod", "HmacSHA256");
        par.put("SignatureVersion", "2");
        par.put("Timestamp", gmtNow());
        String toSign = formatParamMap(par, true, false);

        return toSign;

    }

    static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    static final ZoneId ZONE_GMT = ZoneId.of("Z");

    static String gmtNow() {
        return Instant.ofEpochSecond(epochNow()).atZone(ZONE_GMT).format(DT_FORMAT);
    }


    static long epochNow() {
        return Instant.now().getEpochSecond();
    }

    public static String formatParamMap(Map<String, String> paraMap, boolean urlEncode,
                                        boolean keyToLower) {
        String buff = "";
        Map<String, String> tmpMap = paraMap;
        try {
            List<Map.Entry<String, String>> infoIds =
                    new ArrayList<Map.Entry<String, String>>(tmpMap.entrySet());
            // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
            Collections.sort(infoIds, new Comparator<Map.Entry<String, String>>() {


                @Override
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                    return (o1.getKey()).toString().compareTo(o2.getKey());
                }
            });
            // 构造URL 键值对的格式
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> item : infoIds) {
                if (StringUtils.isNotBlank(item.getKey())) {
                    String key = item.getKey();
                    String val = item.getValue();
                    if (urlEncode) {
                        val = URLEncoder.encode(val, Constants.CHARSET);
                    }
                    if (keyToLower) {
                        buf.append(key.toLowerCase() + "=" + val);
                    } else {
                        buf.append(key + "=" + val);
                    }
                    buf.append("&");
                }


            }
            buff = buf.toString();
            if (buff.isEmpty() == false) {
                buff = buff.substring(0, buff.length() - 1);
            }
        } catch (Exception e) {
            return null;
        }
        return buff;
    }


    /**
     * 交易规则获取
     */
    public void setPrecision() {
        String rt = httpUtil.get(baseUrl + "/v1/common/symbols");
        JSONObject rtObj = JSONObject.fromObject(rt);
        List<Map<String, Object>> symbols = (List<Map<String, Object>>) rtObj.get("data");
        for (Map<String, Object> data : symbols) {
            String[] baseCurrency = exchange.get("market").split("_");
            if (baseCurrency[0].equals(data.get("baseCurrency"))) {


                precision.put("pricePrecision", data.get("pricePrecision"));

                precision.put("amountPrecision", data.get("amountPrecision"));
                int minAmount = (int) data.get("amountPrecision");
                BigDecimal minQty = BigDecimal.ONE.divide(BigDecimal.valueOf(Math.pow(10, minAmount)), minAmount, BigDecimal.ROUND_UP);

                precision.put("minTradeLimit", minQty);
            }
        }
    }


}
