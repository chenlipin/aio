//package top.suilian.aio.service.firstv;
//
//import net.sf.json.JSONObject;
//import org.apache.commons.codec.digest.DigestUtils;
//import top.suilian.aio.Util.Constant;
//import top.suilian.aio.Util.HMAC;
//import top.suilian.aio.service.BaseService;
//
//import java.io.UnsupportedEncodingException;
//import java.math.BigDecimal;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//public class FirstvParentService extends BaseService {
//    public String baseUrl = "https://api.firstvc.net";
//
//    //region    可配置参数
//    public String orderSum;        //褥羊毛次数
//    public String timeSlot;        //波动周期(秒)
//    public Double priceRange;      //价格区间等份
//    public String numThreshold;    //交易最大量
//    public String market;          //交易对
//    public String apikey;          //key
//    public String tpass;           //私钥
//    public int orderSumSwitch;     //防褥羊毛开关
//    public int startTime;          //暂停最小值
//    public int endTime;          //暂停最大值
//    public String mobile;      //联系电话
//    public int isMobileSwitch; //短信开关
//    public int isOpenIntervalSwitch; //刷开区间开关
//    public BigDecimal openIntervalAllAmount;     //允许刷开区间的总交易量
//    public BigDecimal openIntervalPrice;       //刷开区间
//    public BigDecimal openIntervalFromPrice;   //触发区间大小
//    public String numMinThreshold;         //交易量最小值
//
//    public String loginFiled;                 //firstv 登录账号
//    public String password;                    //firstv 登录密码
//    //endregion
//    public Map<String, Object> precision = new HashMap<String, Object>();
//    public int cnt = 0;
//    public boolean isTest = true;
//    public boolean submitCnt = true;
//    public int valid = 1;
//    public String exceptionMessage = null;
//
//
//    /**
//     * 下单
//     *
//     * @param type
//     * @param price
//     * @param amount
//     * @return
//     * @throws UnsupportedEncodingException
//     */
//    public String submitTrade(int type, BigDecimal price, BigDecimal amount) throws UnsupportedEncodingException {
//        String trade = null;
//
//
//        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
//        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));
//        String typeStr = type == 1 ? "买" : "卖";
//
//
//        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);
//
//        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
//        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
//            Double numThreshold1 = Double.valueOf(numThreshold);
//            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
//                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
//                    num = BigDecimal.valueOf(numThreshold1);
//                }
//                String timestamp = getTimeStamp();
//
//                String sign = doSign(apikey, timestamp);
//
//                String toAmount = String.valueOf(nN(amount, 4));
//
//                String toPrice = String.valueOf(nN(price, 4));
//
//                HashMap<String, String> params = new HashMap<String, String>();
//                params.put("sign", sign);
//                params.put("price", toPrice);
//
//                params.put("type", String.valueOf(type));
//
//
//                params.put("number", toAmount);
//                params.put("cur_time", timestamp);
//
//
//                String token = getToken(apikey, loginFiled, password);
//                logger.info("robotId" + id + "----" + "挂单参数：" + JSONObject.fromObject(params));
//                logger.info("robotId" + id + "----" + "签名：" + JSONObject.fromObject(token));
//                HashMap<String,String> tokes  = new HashMap<>();
//                tokes.put("Authorization",token);
//
//                try {
//                    trade = httpUtil.post("https://api.firstvc.net/api/open/trade/order/" + market.toUpperCase(), params, tokes);
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//
//
//                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
//                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
//
//
//            } else {
//                setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
//                logger.info("robotId" + id + "----" + "挂单失败结束");
//
//            }
//        } else {
//            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
//            logger.info("robotId" + id + "----" + "挂单失败结束");
//
//        }
//
//        valid = 1;
//        return trade;
//    }
//
//    /**
//     * 查询订单详情
//     *
//     * @param orderId
//     * @return
//     * @throws UnsupportedEncodingException
//     */
//
//
//    public String selectOrder(String orderId, String type) throws UnsupportedEncodingException {
//        String token = getToken(apikey, loginFiled, password);
//
//        String params = "order_id=" + orderId + "&type=" + type;
//        String url = "https://api.firstvc.net/api/open/user/orderDetail?" + params;
//        String re = httpUtil.doGet(url, token);
//        return re;
//    }
//
//
//    /**
//     * 撤单
//     * @param orderId
//     * @return
//     * @throws UnsupportedEncodingException
//     */
//    public String cancelTrade(String orderId, String type) throws UnsupportedEncodingException {
//        String timestamp = getTimeStamp();
//        String sign = doSign(apikey, timestamp);
//
//        HashMap<String, String> params = new HashMap<String, String>();
//        params.put("order_id", orderId);
//        params.put("sign", sign);
//        params.put("type", type);
//        params.put("cur_time", timestamp);
//
//
//        String re = null;
//
//        String token = getToken(apikey, loginFiled, password);
//        HashMap<String,String> tokes  = new HashMap<>();
//        tokes.put("Authorization",token);
//
//        try {
//            re = httpUtil.post("https://api.firstvc.net/api/open/trade/cancelOrder", params, tokes);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return re;
//    }
//
//    /**
//     * 存储撤单信息
//     * @param cancelRes
//     * @param res
//     * @param orderId
//     * @param type
//     */
//    public void setCancelOrder(JSONObject cancelRes, String res, String orderId, Integer type) {
//        int cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
//        if (cancelRes != null && cancelRes.getInt("error") == 0) {
//            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
//        }
//        insertCancel(id, orderId, 1, type, isMobileSwitch, cancelStatus, res);
//    }
//
//
//    public String getToken(String key, String login_filed, String password) {
//
//        String timestamp = getTimeStamp();
//
//        String sign = doSign(key, timestamp);
//        HashMap<String, String> params = new HashMap<String, String>();
//        params.put("sign", sign);
//        params.put("key", key);
//        params.put("login_field", login_filed);
//        params.put("password", password);
//        params.put("cur_time", timestamp);
//
//        String re = null;
//        try {
//            re = httpUtil.post("https://api.firstvc.net/api/open/getToken", params);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//
//
//
//        JSONObject toToken = JSONObject.fromObject(re);
//        JSONObject data = toToken.getJSONObject("data");
//        String tokenType = data.getString("token_type");
//        String accessToken = data.getString("access_token");
//        String token = tokenType + " " + accessToken;
//        return token;
//    }
//
//    public static String doSign(String key, String timestamp) {
//        return DigestUtils.md5Hex(key + timestamp);
//    }
//
//    public String getTimeStamp() {
//        Date date = new Date();
//
//        String timestamp = String.valueOf(date.getTime() / 1000);
//        return timestamp;
//    }
//
//}
