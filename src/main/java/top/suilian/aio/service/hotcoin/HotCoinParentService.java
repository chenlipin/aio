package top.suilian.aio.service.hotcoin;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.BeanContext;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.BaseService;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinDeep;
import top.suilian.aio.vo.getAllOrderPonse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@DependsOn("beanContext")
public class HotCoinParentService extends BaseService implements RobotAction {
    @Override
    public List<String> cancelAllOrder(Integer type, Integer tradeType) {
        return null;
    }

    public String baseUrl = "https://api.hotcoinfin.com";
    public String host = "api.hotcoinfin.com";
    public RunHotcoinDeep runHotcoinDeep = BeanContext.getBean(RunHotcoinDeep.class);

    public Map<String, Object> precision = new HashMap<String, Object>();
    /** 官方规则仅覆盖运行时参数，另存原始配置用于查询失败后的回退。 */
    private final Map<String, String> configuredRules = new HashMap<>();

    /** 重载数据库参数时清除旧规则，下一次使用重新查询官方接口。 */
    @Override
    public void setParam() {
        super.setParam();
        configuredRules.clear();
        precision.clear();
    }

    /** 指定机器人加载参数也采用同样的精度重置规则。 */
    @Override
    public void setParam(Integer robotId) {
        super.setParam(robotId);
        configuredRules.clear();
        precision.clear();
    }
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
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


        ensurePrecision();
        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));

        Double minTradeLimit = Double.valueOf(String.valueOf(precision.get("minTradeLimit")));
        if (num.compareTo(BigDecimal.valueOf(minTradeLimit)) >= 0) {

            boolean flag = exchange.containsKey("numThreshold");
            if (flag) {
                Double numThreshold1 = Double.valueOf(exchange.get("numThreshold"));
                if (price1.compareTo(BigDecimal.ZERO) > 0 && num.compareTo(BigDecimal.valueOf(numThreshold1)) < 1) {
                    if (num.compareTo(BigDecimal.valueOf(numThreshold1)) == 1) {
                        num = BigDecimal.valueOf(numThreshold1);
                    }


                    String uri = "/v1/order/place";
                    String httpMethod = "GET";
                    Map<String, Object> params = new TreeMap<>();
                    params.put("AccessKeyId", exchange.get("apikey"));
                    params.put("SignatureVersion", 2);
                    params.put("SignatureMethod", "HmacSHA256");
                    params.put("Timestamp", new Date().getTime());
                    params.put("symbol", exchange.get("market"));
                    if (type == 1) {
                        params.put("type", "buy");
                    } else {
                        params.put("type", "sell");
                    }
                    params.put("tradeAmount", num);
                    params.put("tradePrice", price1);
                    String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
                    params.put("Signature", Signature);
                    String httpParams = splicing(params);

                    logger.info("挂单参数" + httpParams);


                    trade = httpUtil.get("https://" + host + uri + "?" + httpParams);

                    JSONObject jsonObject = JSONObject.fromObject(trade);
                    if(200!=jsonObject.getInt("code")){
                       setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
                    }

                    setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                    logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);

                } else {
                    setTradeLog(id, "price[" + price1 + "] num[" + num + "]", 1);
                    logger.info("robotId" + id + "----" + "挂单失败结束");
                }
            } else {
                String uri = "/v1/order/place";
                String httpMethod = "GET";
                Map<String, Object> params = new TreeMap<>();
                params.put("AccessKeyId", exchange.get("apiley"));
                params.put("SignatureVersion", 2);
                params.put("SignatureMethod", "HmacSHA256");
                params.put("Timestamp", new Date().getTime());
                params.put("symbol", exchange.get("market"));
                if (type == 1) {
                    params.put("type", "buy");
                } else {
                    params.put("type", "sell");
                }
                params.put("tradeAmount", num);
                params.put("tradePrice", price1);
                String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
                params.put("Signature", Signature);
                String httpParams = splicing(params);

                logger.info("挂单参数" + httpParams);
                trade = httpUtil.get("https://" + host + uri + "?" + httpParams);

                JSONObject jsonObject = JSONObject.fromObject(trade);
                if(200!=jsonObject.getInt("code")){
                    setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
                }
                setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
                logger.info("robotId" + id + "----" + "挂单成功结束：" + trade);
            }


        } else {
            setTradeLog(id, "交易量最小为：" + precision.get("minTradeLimit"), 0);
            logger.info("robotId" + id + "----" + "挂单失败结束");
        }

        valid = 1;
        return trade;

    }

    //对标下单
    public String submitOrder(int type, BigDecimal price, BigDecimal amount) {
        String timestamp = String.valueOf(new Date().getTime());
        String typeStr = type == 1 ? "买" : "卖";

        logger.info("robotId" + id + "----" + "开始挂单：type(交易类型)：" + typeStr + "，price(价格)：" + price + "，amount(数量)：" + amount);

        // 输出字符串
        String trade = null;
        ensurePrecision();
        BigDecimal price1 = nN(price, Integer.parseInt(precision.get("pricePrecision").toString()));
        BigDecimal num = nN(amount, Integer.parseInt(precision.get("amountPrecision").toString()));
        String uri = "/v1/order/place";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        if (type == 1) {
            params.put("type", "buy");
        } else {
            params.put("type", "sell");
        }
        params.put("tradeAmount", num);
        params.put("tradePrice", price1);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        logger.info("挂单参数" + params);
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        trade = httpUtil.get("https://" + host + uri + "?" + httpParams);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(200!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        setTradeLog(id, "挂" + (type == 1 ? "买" : "卖") + "单[价格：" + price1 + ": 数量" + num + "]=>" + trade, 0, type == 1 ? "05cbc8" : "ff6224");
        return trade;
    }

    //获取委托单
    public String getTradeOrders(int type) {
        String trade = null;
        String uri = "/v1/order/entrust";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        params.put("type", type);
        params.put("count", 100);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String trades = httpUtil.get("https://" + host + uri + "?" + httpParams);
        JSONObject jsonObject = JSONObject.fromObject(trade);
        if(200!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        return trades;

    }


    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */


    public String selectOrder(String orderId) throws UnsupportedEncodingException {


        String uri = "/v1/order/detailById";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("id", orderId);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = splicing(params);
        String trades = httpUtil.get("https://" + host + uri + "?" + httpParams);
        JSONObject jsonObject = JSONObject.fromObject(trades);
        if(200!=jsonObject.getInt("code")){
            setWarmLog(id,3,"API接口错误",jsonObject.getString("msg"));
        }
        logger.info("查询订单："+orderId+"  结果"+trades);
        return trades;
    }

    /** 查询成交对手，供新旧K线核对对敲订单是否成交给外部订单。 */
    public String selectOrderCounterparties(String orderId) throws UnsupportedEncodingException {
        String uri = "/v1/order/counterpartiesById";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("id", orderId);
        params.put("Signature", getSignature(exchange.get("tpass"), host, uri, "GET", params));
        return httpUtil.get("https://" + host + uri + "?" + splicing(params));
    }

    /** 返回外部成交明细；查询失败单独告警，不当作已确认的正常对敲。 */
    protected String getExternalOrderTradeMessage(String orderId) throws UnsupportedEncodingException {
        try {
            JSONObject response = JSONObject.fromObject(selectOrderCounterparties(orderId));
            if (response.getInt("code") != 200) {
                throw new IllegalStateException("成交对手接口返回失败");
            }
            JSONArray trades = response.getJSONObject("data").getJSONArray("entrusts");
            JSONArray externalTrades = new JSONArray();
            for (int i = 0; i < trades.size(); i++) {
                JSONObject trade = trades.getJSONObject(i);
                int isSelfTrade = trade.getInt("isSelfTrade");
                if (isSelfTrade == 0) {
                    externalTrades.add(trade);
                } else if (isSelfTrade != 1) {
                    throw new IllegalStateException("成交对手标记异常");
                }
            }
            if (!externalTrades.isEmpty()) {
                return "订单{" + orderId + "}外部成交撞单，成交明细：" + externalTrades;
            }
        } catch (RuntimeException e) {
            setWarmLog(id, 3, "订单{" + orderId + "}成交对手查询失败，未完成撞单检查", "");
        }
        return null;
    }


    /**
     * 撤单
     *
     * @param orderId
     * @return
     * @throws UnsupportedEncodingException
     */
    public String cancelTrade(String orderId) throws UnsupportedEncodingException {

        String uri = "/v1/order/cancel";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("id", orderId);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = splicing(params);

        System.out.println("cancel"+orderId);
        HttpUtil httpUtil = new HttpUtil();
        String res = httpUtil.get("https://" + host + uri + "?" + httpParams);
        JSONObject jsonObject = JSONObject.fromObject(res);
        return res;
    }


    /**
     * 获取所有尾单
     *
     * @return
     */
    public String getTrade() {
        return getTrade(1, 100);
    }

    public String getTrade(int page, int count) {
        String uri = "/v1/order/entrust";
        String httpMethod = "GET";
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", exchange.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", new Date().getTime());
        params.put("symbol", exchange.get("market"));
        params.put("type", 1);
        params.put("page", page);
        params.put("count", count);
        String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
        params.put("Signature", Signature);
        String httpParams = null;
        try {
            httpParams = splicing(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpUtil httpUtil = new HttpUtil();
        String res = httpUtil.get("https://" + host + uri + "?" + httpParams);
        System.out.println(res);
        return res;
    }


    /**
     * 获取余额
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

            String uri = "/v1/balance";
            String httpMethod = "GET";
            Map<String, Object> params = new TreeMap<>();
            params.put("AccessKeyId", exchange.get("apikey"));
            params.put("SignatureVersion", 2);
            params.put("SignatureMethod", "HmacSHA256");
            params.put("Timestamp", new Date().getTime());
            String Signature = getSignature(exchange.get("tpass"), host, uri, httpMethod, params);
            params.put("Signature", Signature);
            String httpParams = splicing(params);
            String trades = httpUtil.get(baseUrl + uri + "?" + httpParams);
            JSONObject jsonObjectss = JSONObject.fromObject(trades);
            if(200!=jsonObjectss.getInt("code")){
                setWarmLog(id,3,"API接口错误",jsonObjectss.getString("msg"));
            }
            JSONObject tradesJson = JSONObject.fromObject(trades);
            JSONObject data = tradesJson.getJSONObject("data");

            JSONArray wallet = data.getJSONArray("wallet");

            String firstBalance = null;
            String lastBalance = null;
            String firstBalancefrozen = null;
            String lastBalancefrozen = null;

            for (int i = 0; i < wallet.size(); i++) {
                JSONObject jsonObject = wallet.getJSONObject(i);
                if (jsonObject.getString("shortName").equals(coinArr.get(0).toUpperCase())) {
                    firstBalance = jsonObject.getString("total");
                    firstBalancefrozen = jsonObject.getString("frozen");
                } else if (jsonObject.getString("shortName").equals(coinArr.get(1).toUpperCase())) {
                    lastBalance = jsonObject.getString("total");
                    lastBalancefrozen=jsonObject.getString("frozen");
                    if(Double.parseDouble(lastBalance)<10){
                        setWarmLog(id,0,"余额不足",coinArr.get(1).toUpperCase()+"余额为:"+lastBalance);
                        // Hotcoin计价币余额不足时，同时推送Telegram余额通知。
                        telegramNotifyService.sendHotcoinBalanceNotice(id, exchange.get("market"), coinArr.get(1).toUpperCase(), lastBalance);
                    }else{
                        // 余额恢复后清除冷却Key，下次再次不足可以立即推送。
                        telegramNotifyService.clearHotcoinBalanceNoticeCooldown(id, coinArr.get(1).toUpperCase());
                    }
                }
            }
            HashMap<String, String> balances = new HashMap<>();
            balances.put(coinArr.get(0),  firstBalance+"_"+firstBalancefrozen);
            balances.put(coinArr.get(1), lastBalance+"_"+lastBalancefrozen);
            logger.info("获取余额"+balances);
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
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
        // 首次解析前保存配置，避免exchange被官方值覆盖后失去回退依据。
        for (String key : Arrays.asList("amountPrecision", "pricePrecision", "minOrderAmount")) {
            if (!configuredRules.containsKey(key)) {
                configuredRules.put(key, exchange.get(key));
            }
        }
        JSONObject symbolRule = getHotcoinSymbolRule();
        // 各字段独立回退，一个字段缺失不能丢弃其他有效官方规则。
        Map<String, Object> resolved = new HashMap<>();
        resolved.put("amountPrecision", getHotcoinRuleValue(symbolRule, "amountPrecision", "amountPrecision"));
        resolved.put("pricePrecision", getHotcoinRuleValue(symbolRule, "pricePrecision", "pricePrecision"));
        // 数据库参数名为minOrderAmount，单位仍是币数量；minTradeLimit保留为策略内部别名。
        resolved.put("minTradeLimit", getHotcoinRuleValue(symbolRule, "minOrderCount", "minOrderAmount"));
        precision.putAll(resolved);
        exchange.put("amountPrecision", precision.get("amountPrecision").toString());
        exchange.put("pricePrecision", precision.get("pricePrecision").toString());
        exchange.put("minTradeLimit", precision.get("minTradeLimit").toString());
    }

    private void ensurePrecision() {
        if (!precision.containsKey("amountPrecision") || !precision.containsKey("pricePrecision") || !precision.containsKey("minTradeLimit")) {
            setPrecision();
        }
    }

    private String getHotcoinRuleValue(JSONObject symbolRule, String apiKey, String configKey) {
        String value = normalizeHotcoinRule(symbolRule == null ? null : symbolRule.opt(apiKey), apiKey);
        if (value != null) {
            return value;
        }
        logger.info("Hotcoin交易规则字段不可用，使用配置：market=" + exchange.get("market") + ", field=" + apiKey);
        value = normalizeHotcoinRule(configuredRules.get(configKey), apiKey);
        if (value == null) {
            // 两边都无有效值时禁止继续下单，不猜测精度或将未知值当作0。
            throw new IllegalStateException("Hotcoin官方规则和配置均无有效值：" + configKey);
        }
        return value;
    }

    /** 精度必须为非负整数，最小数量可为非负小数；0小数位也是有效值。 */
    private String normalizeHotcoinRule(Object raw, String apiKey) {
        if (raw == null) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.toString().trim());
            if (value.signum() < 0) {
                return null;
            }
            if ("pricePrecision".equals(apiKey) || "amountPrecision".equals(apiKey)) {
                return Integer.toString(value.intValueExact());
            }
            return value.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    private JSONObject getHotcoinSymbolRule() {
        try {
            String market = exchange.get("market");
            if (StringUtils.isBlank(market)) {
                return null;
            }
            market = market.trim();
            String res = requestHotcoinSymbols();
            JSONObject jsonObject = JSONObject.fromObject(res);
            if (jsonObject == null || !"200".equals(jsonObject.getString("code")) || !jsonObject.has("data")) {
                logger.info("Hotcoin交易规则查询失败，使用配置精度：market=" + market);
                return null;
            }
            JSONArray data = jsonObject.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                if (!(data.get(i) instanceof JSONObject)) {
                    continue;
                }
                JSONObject item = data.getJSONObject(i);
                if (market.equalsIgnoreCase(item.optString("symbol"))) {
                    logger.info("Hotcoin交易规则查询成功：market=" + market);
                    return item;
                }
            }
            logger.info("Hotcoin交易规则未匹配到交易对：" + exchange.get("market"));
        } catch (Exception e) {
            logger.info("Hotcoin交易规则查询异常，使用配置精度：" + e.getMessage());
        }
        return null;
    }


    /** 公共规则不需要账户签名；单独入口便于测试替换，避免访问真实交易所。 */
    protected String requestHotcoinSymbols() {
        return HttpUtil.get(baseUrl + "/v1/common/symbols");
    }

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


    @Override
    public Map<String,String> submitOrderStr(int type, BigDecimal price, BigDecimal amount) {
        String orderId = "";
        HashMap<String, String> hashMap = new HashMap<>();
        String submitOrder = submitOrder(type, price, amount);
        if (StringUtils.isNotEmpty(submitOrder)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(submitOrder);
            if ("200".equals(jsonObject.getString("code"))) {
                orderId = jsonObject.getJSONObject("data").getString("ID");
                hashMap.put("res","true");
                hashMap.put("orderId",orderId);
            }else {
                String msg = jsonObject.getString("msg");
                hashMap.put("res","false");
                hashMap.put("orderId",msg);
            }
        }
        return hashMap;
    }

    @Override
    public List<getAllOrderPonse> selectOrder() {
        ArrayList<getAllOrderPonse> getAllOrderPonses = new ArrayList<>();
        int page = 1;
        int pageSize = 100;
        int maxPage = 50;
        while (page <= maxPage) {
            String trade = getTrade(page, pageSize);
            JSONObject jsonObject = JSONObject.fromObject(trade);
            if (jsonObject == null || !jsonObject.has("data") || jsonObject.get("data") == null) {
                break;
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (!data.has("entrutsCur") || data.get("entrutsCur") == null) {
                break;
            }
            JSONArray jsonArray = data.getJSONArray("entrutsCur");
            if (jsonArray == null || jsonArray.size() == 0) {
                break;
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                getAllOrderPonse getAllOrderPonse = new getAllOrderPonse();
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                getAllOrderPonse.setOrderId(jsonObject1.getString("id"));
                getAllOrderPonse.setCreatedAt(jsonObject1.getString("time"));
                getAllOrderPonse.setPrice(jsonObject1.getString("types")+"-"+jsonObject1.getString("price"));
                getAllOrderPonse.setStatus(0);
                getAllOrderPonse.setAmount(jsonObject1.getString("count"));
                getAllOrderPonses.add(getAllOrderPonse);
            }
            if (jsonArray.size() < pageSize) {
                break;
            }
            page++;
        }
        return getAllOrderPonses;
    }

    @Override
    //TradeEnum   1 未成交 2 部分成交 3 完全成交 4 撤单处理中 5 已撤销
    public Map<String, Integer> selectOrderStr(String orderId) {
        String trade = getTrade();
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(trade);
        com.alibaba.fastjson.JSONArray entrutsHis = jsonObject1.getJSONObject("data").getJSONArray("entrutsHis");
        HashMap<String, Integer> map = new HashMap<>();
        for (Object entrutsHi : entrutsHis) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(entrutsHi.toString());
            map.put(jsonObject.getString("id"), jsonObject.getInteger("statusCode"));
        }

        List<String> orders = Arrays.asList(orderId.split(","));
        HashMap<String, Integer> hashMap = new HashMap<>();

        for (String order : orders) {
            Integer integer = map.get(order);
            if (integer != null) {
                hashMap.put(order, getTradeEnum(integer).getStatus());
            } else {
                String result = "";
                try {
                    result = selectOrder(order);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(result);
                if ("200".equals(jsonObject.getString("code"))) {
                    Integer statusCode = jsonObject.getJSONObject("data").getInteger("statusCode");
                    hashMap.put(order, getTradeEnum(statusCode).getStatus());
                }

            }
        }
        return hashMap;
    }

    @Override
    public String cancelTradeStr(String orderId) {
        String cancelTrade = "";
        try {
            cancelTrade = cancelTrade(orderId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtils.isNotEmpty(cancelTrade)) {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(cancelTrade);
            if ("200".equals(jsonObject.getString("code"))||"300".equals(jsonObject.getString("code"))) {
                return "true";
            }
        }
        return "false";
    }




    public TradeEnum getTradeEnum(Integer integer) {
        switch (integer) {
            case 1:
                return TradeEnum.NOTRADE;

            case 2:
                return TradeEnum.TRADEING;

            case 3:
                return TradeEnum.NOTRADED;

            case 4:
                return TradeEnum.CANCEL;

            case 5:
                return TradeEnum.CANCEL;

            default:
                return TradeEnum.CANCEL;

        }
    }
}
