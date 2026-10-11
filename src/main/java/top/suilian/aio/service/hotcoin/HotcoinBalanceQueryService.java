package top.suilian.aio.service.hotcoin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 群指令使用的热币实时只读查询，不修改交易服务参数、余额缓存或触发余额告警。 */
@Service
public class HotcoinBalanceQueryService {
    /** 复用交易服务的签名协议，一次返回该API账户的所有币种余额。 */
    public List<Map<String, String>> query(Map<String, String> account) {
        if (StringUtils.isBlank(account.get("apikey")) || StringUtils.isBlank(account.get("tpass"))) {
            throw new IllegalArgumentException("账户API配置不完整");
        }
        Map<String, Object> params = new TreeMap<>();
        params.put("AccessKeyId", account.get("apikey"));
        params.put("SignatureVersion", 2);
        params.put("SignatureMethod", "HmacSHA256");
        params.put("Timestamp", System.currentTimeMillis());
        String signature = HotCoinParentService.getSignature(account.get("tpass"),
                "api.hotcoinfin.com", "/v1/balance", "GET", params);
        if (signature == null) {
            throw new IllegalArgumentException("账户签名失败");
        }
        params.put("Signature", signature);
        JSONObject response = requestBalance(params);
        if (response == null || response.getIntValue("code") != 200 || response.getJSONObject("data") == null) {
            throw new IllegalStateException("热币余额接口返回错误");
        }
        JSONArray wallet = response.getJSONObject("data").getJSONArray("wallet");
        if (wallet == null) {
            throw new IllegalStateException("热币余额接口缺少wallet");
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < wallet.size(); i++) {
            JSONObject coin = wallet.getJSONObject(i);
            // 不把缺失数据当成0；完整校验后再格式化显示余额。
            BigDecimal total = new BigDecimal(coin.getString("total"));
            BigDecimal frozen = new BigDecimal(coin.getString("frozen"));
            if (total.signum() == 0 && frozen.signum() == 0) {
                continue;
            }
            if (StringUtils.isBlank(coin.getString("shortName"))) {
                throw new IllegalStateException("热币币种数据不完整");
            }
            Map<String, String> item = new HashMap<>();
            item.put("coinName", coin.getString("shortName"));
            item.put("total", total.toPlainString());
            item.put("freeze", frozen.toPlainString());
            result.add(item);
        }
        return result;
    }

    /** 有限超时的只读HTTP请求；失败信息不包含API Key、签名或账户密钥。 */
    protected JSONObject requestBalance(Map<String, Object> params) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.hotcoinfin.com/v1/balance?" + HotCoinParentService.splicing(params));
            request.setConfig(RequestConfig.custom().setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000).setSocketTimeout(15000).build());
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("热币接口HTTP状态异常");
                }
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("热币余额请求失败");
        }
    }
}
