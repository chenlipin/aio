package top.suilian.aio.service.hotcoin;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

/** 公共行情解析测试，不访问真实热币接口。 */
public class HotcoinMarketQueryServiceTest {
    /** 规范化交易对，并精确保留小数及科学计数法表示的价格。 */
    @Test
    public void normalizesSymbolsAndPreservesPricePrecision() {
        Map<String, Map<String, String>> tickers = response("{\"status\":\"ok\",\"ticker\":["
                + "{\"symbol\":\"BTC_USDC\",\"last\":\"123.456000\",\"vol\":\"9876.543000\"},"
                + "{\"symbol\":\"eth_usdc\",\"last\":\"1E-8\",\"vol\":\"0\"},"
                + "{\"symbol\":\"bad_usdc\",\"last\":\"invalid\",\"vol\":\"5\"}]}").latestTickers();
        assertEquals("123.456000", tickers.get("btc_usdc").get("price"));
        assertEquals("9876.543000", tickers.get("btc_usdc").get("volume24h"));
        assertEquals("0.00000001", tickers.get("eth_usdc").get("price"));
        assertEquals("0", tickers.get("eth_usdc").get("volume24h"));
        assertNull(tickers.get("bad_usdc").get("price"));
        assertEquals("5", tickers.get("bad_usdc").get("volume24h"));
    }

    /** 错误状态不能伪装成最新价为0。 */
    @Test(expected = IllegalStateException.class)
    public void marketErrorFailsQuery() {
        response("{\"status\":\"error\"}").latestTickers();
    }

    /** 成交量缺失时保持未知，不能影响有效价格或显示伪造的0成交量。 */
    @Test
    public void missingVolumeKeepsValidPrice() {
        Map<String, String> ticker = response("{\"status\":\"ok\",\"ticker\":["
                + "{\"symbol\":\"btc_usdc\",\"last\":\"123\"}]}")
                .latestTickers().get("btc_usdc");
        assertEquals("123", ticker.get("price"));
        assertNull(ticker.get("volume24h"));
    }

    /** 用固定响应替代网络请求。 */
    private HotcoinMarketQueryService response(String json) {
        return new HotcoinMarketQueryService() {
            @Override
            protected JSONObject requestTickers() {
                return JSONObject.parseObject(json);
            }
        };
    }
}
