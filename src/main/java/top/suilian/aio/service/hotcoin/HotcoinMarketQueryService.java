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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 热币公共行情只读查询，一次获取全部交易对的最新价格及24小时成交量。 */
@Service
public class HotcoinMarketQueryService {
    /** 直接读取ticker的last和vol，vol为市场24小时成交量，不是自然日或账户成交量。 */
    public Map<String, Map<String, String>> latestTickers() {
        JSONObject response = requestTickers();
        if (response == null || !"ok".equals(response.getString("status"))) {
            throw new IllegalStateException("热币行情接口返回错误");
        }
        JSONArray tickers = response.getJSONArray("ticker");
        if (tickers == null) {
            throw new IllegalStateException("热币行情接口缺少ticker");
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        for (int i = 0; i < tickers.size(); i++) {
            JSONObject ticker = tickers.getJSONObject(i);
            String symbol = ticker.getString("symbol");
            if (StringUtils.isBlank(symbol)) {
                continue;
            }
            Map<String, String> values = new HashMap<>();
            values.put("price", decimalValue(ticker.getString("last")));
            values.put("volume24h", decimalValue(ticker.getString("vol")));
            result.put(symbol.trim().toLowerCase(Locale.ROOT), values);
        }
        return result;
    }

    /** 独立校验价格和交易量，字段缺失或异常时返回null，不影响另一字段显示。 */
    private String decimalValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(value);
            return number.signum() < 0 ? null : number.toPlainString();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 公共行情不使用账户密钥，有限超时避免长时间阻塞指令接收线程。 */
    protected JSONObject requestTickers() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.hotcoinfin.com/v1/market/ticker");
            request.setConfig(RequestConfig.custom().setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000).setSocketTimeout(15000).build());
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("热币行情HTTP状态异常");
                }
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("热币行情请求失败");
        }
    }
}
