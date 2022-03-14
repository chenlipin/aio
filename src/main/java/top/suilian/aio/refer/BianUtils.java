package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.BaseHttp;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BianUtils extends BaseHttp {
    public static String uri = "https://api.binance.com";

    public static Map<String, List<DeepVo>> getdeep(String symbol) {
        Map<String, List<DeepVo>> map = new HashMap<>();
        List<DeepVo> buy = new ArrayList<>();
        List<DeepVo> sell = new ArrayList<>();
        String trades = get(uri + "/api/v3/depth?limit=50&symbol=" + symbol.toUpperCase());
        JSONObject tradesObj = JSONObject.fromObject(trades);
        //买单
        JSONArray bidsJson = tradesObj.getJSONArray("bids");
        //卖单
        JSONArray asksJson = tradesObj.getJSONArray("asks");

        for (int i = 0; i < bidsJson.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONArray jsonArray = bidsJson.getJSONArray(i);
            BigDecimal price = new BigDecimal(jsonArray.getString(0));
            BigDecimal amount = new BigDecimal(jsonArray.getString(1));
            deepVo.setType(1);
            deepVo.setAmount(amount);
            deepVo.setPrice(price);
            buy.add(deepVo);
        }
        for (int i = 0; i < asksJson.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONArray jsonArray = asksJson.getJSONArray(i);
            BigDecimal price = new BigDecimal(jsonArray.getString(0));
            BigDecimal amount = new BigDecimal(jsonArray.getString(1));
            deepVo.setType(2);
            deepVo.setAmount(amount);
            deepVo.setPrice(price);
            sell.add(deepVo);
        }
        map.put("deepBuyList", buy);
        map.put("deepSellList", sell);
        return map;
    }

    public static List<DeepVo> getHistory(String symbol) {
        List<DeepVo> deepVos = new ArrayList<>();
        String trades = get(uri + "/api/v3/trades?limit=3&symbol=" + symbol.toUpperCase());
        JSONArray jsonArray = JSONArray.fromObject(trades);
        for (int i = 0; i < jsonArray.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            deepVo.setType(RandomUtils.nextBoolean() ? 1 : 2);
            deepVo.setAmount(new BigDecimal(jsonObject.getString("qty")));
            deepVo.setPrice(new BigDecimal(jsonObject.getString("price")));
            deepVos.add(deepVo);
        }
        return deepVos;
    }
}
