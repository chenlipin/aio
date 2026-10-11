package top.suilian.aio.service.hotcoin;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** 隔离HTTP请求，验证热币余额格式和错误响应处理。 */
public class HotcoinBalanceQueryServiceTest {
    /** 精确保留小数余额，并忽略总额及冻结余额均为0的币种。 */
    @Test
    public void formatsAllNonZeroAssetsWithoutRounding() {
        HotcoinBalanceQueryService query = response("{\"code\":200,\"data\":{\"wallet\":["
                + "{\"shortName\":\"BTC\",\"total\":\"0.000012345\",\"frozen\":\"0.000000001\"},"
                + "{\"shortName\":\"USDC\",\"total\":\"15.12\",\"frozen\":\"2.00\"},"
                + "{\"shortName\":\"ETH\",\"total\":\"0\",\"frozen\":\"0\"}]}}");
        List<Map<String, String>> result = query.query(account());
        assertEquals(2, result.size());
        assertEquals("0.000012345", result.get(0).get("total"));
        assertEquals("0.000000001", result.get(0).get("freeze"));
    }

    /** 错误响应不能伪装为余额为0。 */
    @Test(expected = IllegalStateException.class)
    public void exchangeErrorFailsQuery() {
        response("{\"code\":401}").query(account());
    }

    /** wallet缺失不能伪装为余额为0。 */
    @Test(expected = IllegalStateException.class)
    public void missingWalletFailsQuery() {
        response("{\"code\":200,\"data\":{}}").query(account());
    }

    /** 创建不包含真实凭证的账户配置。 */
    private Map<String, String> account() {
        Map<String, String> account = new HashMap<>();
        account.put("apikey", "test-key");
        account.put("tpass", "test-secret");
        return account;
    }

    /** 覆盖网络边界，返回固定响应。 */
    private HotcoinBalanceQueryService response(String json) {
        return new HotcoinBalanceQueryService() {
            @Override
            protected JSONObject requestBalance(Map<String, Object> params) {
                return JSONObject.parseObject(json);
            }
        };
    }
}
