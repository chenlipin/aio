package top.suilian.aio.service.biki;

import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class BikiParentService extends BaseService {

    public String baseUrl = "https://api.biki.com";

    //region    可配置参数
    public String orderSum;        //褥羊毛次数
    public String timeSlot;        //波动周期(秒)
    public Double priceRange;      //价格区间等份
    public String numThreshold;    //交易最大量
    public String market;          //交易对
    public String apikey;          //key
    public String tpass;           //私钥
    public int orderSumSwitch;     //防褥羊毛开关
    public int startTime;          //暂停最小值
    public int endTime;          //暂停最大值
    public String mobile;      //联系电话
    public int isMobileSwitch; //短信开关
    public int isOpenIntervalSwitch; //刷开区间开关
    public BigDecimal openIntervalAllAmount;     //允许刷开区间的总交易量
    public BigDecimal openIntervalPrice;       //刷开区间
    public BigDecimal openIntervalFromPrice;   //触发区间大小
    public String numMinThreshold;         //交易量最小值
    public String minTradeLimit;           //平台交易最小量
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

        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串

        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(numThreshold);
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }

                Map<String, Object> params = new TreeMap<>();
                params.put("side", type == 1 ? "BUY" : "SELL");
                params.put("type", 1);
                params.put("volume", num);
                params.put("price", price1);
                params.put("symbol", market);
                params.put("api_key", apikey);
                params.put("time", timestamp);
                StringBuilder sb = sign(params).get(0);
                StringBuilder param = sign(params).get(1);
                sb.append(tpass);


//                String params = "symbol=" + market + "&side=" + (type == 1 ? "BUY" : "SELL") + "&type=LIMIT&timeInForce=GTC&quantity=" + String.valueOf(num) + "&price=" + String.valueOf(price1) + "&recvWindow=5000&timestamp=" + timestamp;

                String signs = DigestUtils.md5Hex(String.valueOf(sb));

                String par = param + "sign=" + signs;

                logger.info("robotId" + id + "----" + "挂单参数：" + par);

                trade = httpUtil.post(baseUrl + "/open/api/create_order?", par);

                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);


            } else {
                setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                logger.info("robotId" + id + "----" + "挂单失败结束");
            }
        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
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
        String timestamp = String.valueOf(new Date().getTime());
        Map<String, Object> params = new TreeMap<>();
        params.put("symbol", market);
        params.put("api_key", apikey);
        params.put("time", timestamp);
        params.put("order_id", orderId);
        StringBuilder sb = sign(params).get(0);
        StringBuilder param = sign(params).get(1);

        sb.append(tpass);

        String signs = DigestUtils.md5Hex(String.valueOf(sb));

        String par = param + "sign=" + signs;

        String res = httpUtil.get(baseUrl + "/open/api/order_info?"+par);

        return res;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {
        String timestamp = String.valueOf(new Date().getTime());
        Map<String, Object> params = new TreeMap<>();
        params.put("symbol", market);
        params.put("api_key", apikey);
        params.put("time", timestamp);
        params.put("order_id", orderId);
        StringBuilder sb = sign(params).get(0);
        StringBuilder param = sign(params).get(1);

        sb.append(tpass);

        String signs = DigestUtils.md5Hex(String.valueOf(sb));

        String par = param + "sign=" + signs;

        String res = httpUtil.post(baseUrl + "/open/api/cancel_order?", par);

        return res;
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
        if (cancelRes != null && cancelRes.getInt("code") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, isMobileSwitch, cancelStatus, res, Constant.KEY_EXCHANGE_BIKI);
    }

    private List<StringBuilder> sign(Map<String, Object> params) {
        List<StringBuilder> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        StringBuilder param = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sb.append(entry.getKey() + entry.getValue());
            param.append(entry.getKey() + "=" + entry.getValue() + "&");
        }

        list.add(sb);
        list.add(param);

        return list;
    }
}


