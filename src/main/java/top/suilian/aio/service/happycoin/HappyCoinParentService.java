package top.suilian.aio.service.happycoin;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class HappyCoinParentService extends BaseService {
    public String baseUrl = "https://openapi.happycoin.pro/open/api/";


    //endregion
    public int orderNum = 0;
    public BigDecimal buyPrice = BigDecimal.ZERO;
    public BigDecimal sellPrice = BigDecimal.ZERO;
    public BigDecimal intervalAmount = BigDecimal.ZERO;
    public int runTime = 0;
    public int randomNum;
    public String orderIdOne = "";
    public String orderIdTwo = "";

    public int valid = 1;
    public int balanceValid = 1;

    public Map<String, Object> precision = new HashMap<String, Object>();
    public Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();

    public HttpUtil httpUtil;
    public String msg;

    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public String exceptionMessage = null;

    public String[] transactionArr = new String[24];

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
     * 设置交易对规则
     */

    public void setPrecision() {


        String rt = httpUtil.get(baseUrl + "/common/symbols");

        JSONObject rtObj = judgeRes(rt, "code", "setPrecision");

        if (!rt.equals("") && rtObj != null) {
            JSONArray data = rtObj.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                if (exchange.get("market").equals(data.getJSONObject(i).getString("symbol"))) {

                    precision.remove("pricePrecision");
                    precision.put("pricePrecision", data.getJSONObject(i).getString("price_precision"));
                    precision.remove("amountPrecision");
                    precision.put("amountPrecision", data.getJSONObject(i).getString("amount_precision"));
                    precision.put("minTradeLimit", exchange.get("numMinThreshold"));
                    break;
                }
            }

        } else {
            logger.info("获取交易规则失败");
        }
    }


    public String submitTrade(int type, BigDecimal price, BigDecimal amount) {

        String typeStr = type == 1 ? "BUY" : "SELL";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串
        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));//获取最小交易数量
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {
            Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
            if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                    num = BigDecimal.valueOf(numThreshold1);
                }

                TreeMap<String, String> map = new TreeMap<String, String>();
                map.put("side", typeStr);//买卖方向BUY、SELL
                map.put("type", "1");//挂单类型，1:限价委托、2:市价委托
                map.put("volume", num.toString());//购买数量（多义，复用字段）type=1:表示买卖数量type=2:买则表示总价格，卖表示总个数买卖限制
                map.put("price", String.valueOf(price1));//委托单价：type=2：不需要此参数
                map.put("symbol", exchange.get("market"));//市场标记，
                map.put("fee_is_user_exchange_coin", "0");//0，当交易所有平台币时，此参数表示是否使用用平台币支付手续费，0否，1是
                map.put("api_key", exchange.get("apikey"));//
                map.put("time", new Date().getTime() + "");
                String time1 = map.get("time");
                String sign = addMD5(map, exchange.get("tpass"));
                String params = appendString(map, sign);

                logger.info("robotId" + id + "----" + "挂单参数：" + params);

                trade = httpUtil.doPost(baseUrl + "/create_order", params, exchange.get("apikey"));
                // trade = "{'code':'0','msg':'成功','data':{'order_id':10479},'message':null}";
                setTradeLog(id, "量化挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
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


    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {

        String typeStr = type == 1 ? "BUY" : "SELL";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串
        String trade = null;


        BigDecimal price1 = nN(price, Integer.valueOf(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.valueOf(precision.get("amountPrecision").toString()));

        TreeMap<String, String> map = new TreeMap<String, String>();
        map.put("side", typeStr);//买卖方向BUY、SELL
        map.put("type", "1");//挂单类型，1:限价委托、2:市价委托
        map.put("volume", num.toString());//购买数量（多义，复用字段）type=1:表示买卖数量type=2:买则表示总价格，卖表示总个数买卖限制
        map.put("price", String.valueOf(price1));//委托单价：type=2：不需要此参数
        map.put("symbol", exchange.get("market"));//市场标记，
        map.put("fee_is_user_exchange_coin", "0");//0，当交易所有平台币时，此参数表示是否使用用平台币支付手续费，0否，1是
        map.put("api_key", exchange.get("apikey"));//
        map.put("time", new Date().getTime() + "");
        String time1 = map.get("time");
        String sign = addMD5(map, exchange.get("tpass"));
        String params = appendString(map, sign);

        logger.info("robotId" + id + "----" + "挂单参数：" + params);

        trade = httpUtil.doPost(baseUrl + "/create_order", params, exchange.get("apikey"));
        // trade = "{'code':'0','msg':'成功','data':{'order_id':10479},'message':null}";
        setTradeLog(id, "量化挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

        return trade;
    }


    /**
     * 查询订单祥情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String selectOrder(String orderId) throws UnsupportedEncodingException {
        TreeMap<String, String> map = new TreeMap<String, String>();

        map.put("order_id", orderId);//挂单类型，1:限价委托、2:市价委托
        map.put("symbol", exchange.get("market"));//市场标记，
        map.put("api_key", exchange.get("apikey"));//
        map.put("time", new Date().getTime() + "");
        String signs = addMD5(map, exchange.get("tpass"));
        String par = appendString(map, signs);

        String orderInfo = httpUtil.get(baseUrl + "/order_info?" + par);
        return orderInfo;
    }

    /**
     * 取消订单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) {
        TreeMap<String, String> map = new TreeMap<String, String>();

        map.put("order_id", orderId);//挂单类型，1:限价委托、2:市价委托
        map.put("symbol", exchange.get("market"));//市场标记，
        map.put("api_key", exchange.get("apikey"));//
        map.put("time", new Date().getTime() + "");
        String signs = addMD5(map, exchange.get("tpass"));
        String par = appendString(map, signs);
        String res = httpUtil.post(baseUrl + "/cancel_order", par);
        return res;
    }


    /**
     * 设置余额
     *
     * @throws UnsupportedEncodingException
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
            TreeMap<String, String> param = new TreeMap<String, String>();
            param.put("api_key", exchange.get("apikey"));
            param.put("time", new Date().getTime() + "");
            String signs = addMD5(param, exchange.get("tpass"));
            String par = appendString(param, signs);
            String res = httpUtil.get(baseUrl + "/user/account?" + par);
            JSONObject obj = judgeRes(res, "code", "setBalanceRedis");
            if (obj != null && obj.getInt("code") == 0) {
                JSONObject data = obj.getJSONObject("data");
                //获取账户余额的集合
                JSONArray coin_list = data.getJSONArray("coin_list");
                //将json对象集合转换成字符串
                //创建存储相应货币及其余额的集合
                HashMap<String, String> balances = new HashMap<>();
                String firstBalance = null;
                String lastBalance = null;
                //遍历集合，查找对用币种相关信息,存入集合
                for (int i = 0; i < coin_list.size(); i++) {
                    JSONObject coinlist = coin_list.getJSONObject(i);
                    if (coinlist.getString("coin").equals(coinArr.get(0))) {
                        firstBalance = coinlist.getString("normal");
                        if (lastBalance != null) {
                            break;
                        }
                    } else if (coinlist.getString("coin").equals(coinArr.get(1))) {
                        lastBalance = coinlist.getString("normal");
                        if (firstBalance != null) {
                            break;
                        }
                    }
                    balances.put(coinArr.get(0), firstBalance);
                    balances.put(coinArr.get(1), lastBalance);


                }
                //将余额存入redis
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);

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
        if (cancelRes != null && cancelRes.getInt("code") == 0) {
            cancelStatus = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
        }
        insertCancel(id, orderId, 1, type, Integer.parseInt(exchange.get("isMobileSwitch")), cancelStatus, res, Constant.KEY_EXCHANGE_BIHU);
    }


    public String addMD5(TreeMap<String, String> map, String tpass) {

        String secretKey = tpass;
        /** 拼接签名字符串，md5签名 */
        StringBuilder result = new StringBuilder();

        Set<Map.Entry<String, String>> entrys = map.entrySet();
        for (Map.Entry<String, String> param : entrys) {
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
        String sign = getMD5String(signs);//签名

        return sign;
    }

    /**
     * MD5加密
     */
    public String getMD5String(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 拼接字符串
     */
    public String appendString(TreeMap<String, String> map, String sign) {

        StringBuilder str = new StringBuilder();
        Set<Map.Entry<String, String>> entrys = map.entrySet();
        for (Map.Entry<String, String> param : entrys) {
            if (param.getKey().equals("sign")) {
                continue;
            }
            if (param.getValue() != null) {
                str.append(param.getKey());
                str.append("=");
                str.append(param.getValue().toString());
                str.append("&");
            }
        }
        str.append("sign=" + sign);
        return str.toString();

    }
}
