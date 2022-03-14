package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.BaseHttp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BiMartUtils extends BaseHttp {
    public static String uri = "https://api-cloud.bitmart.com";

    public static Map<String, List<DeepVo>> getdeep(String symbol) {
        Map<String, List<DeepVo>> map = new HashMap<>();
        List<DeepVo> buy = new ArrayList<>();
        List<DeepVo> sell = new ArrayList<>();
        String trades = get(uri + "/spot/v1/symbols/book?size=15&symbol="+symbol);
        JSONObject tradesObj = JSONObject.fromObject(trades);
        JSONObject data = tradesObj.getJSONObject("data");
        //买单
        JSONArray bidsJson = data.getJSONArray("buys");
        //卖单
        JSONArray asksJson = data.getJSONArray("sells");

        for (int i = 0; i < bidsJson.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = bidsJson.getJSONObject(i);
            BigDecimal price = new BigDecimal(jsonObject.getString("price"));
            BigDecimal amount = new BigDecimal(jsonObject.getString("amount"));
            deepVo.setType(1);
            deepVo.setAmount(amount);
            deepVo.setPrice(price);
            buy.add(deepVo);
        }
        for (int i = 0; i < asksJson.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = bidsJson.getJSONObject(i);
            BigDecimal price = new BigDecimal(jsonObject.getString("price"));
            BigDecimal amount = new BigDecimal(jsonObject.getString("amount"));
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
        String trades = get(uri + "/spot/v1/symbols/trades?N=10&symbol="+symbol);
        JSONObject jsonObject1 = JSONObject.fromObject(trades);

        JSONArray jsonArray = jsonObject1. getJSONObject("data").getJSONArray("trades");
        for (int i = 0; i < jsonArray.size(); i++) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            deepVo.setType(RandomUtils.nextBoolean() ? 1 : 2);
            deepVo.setAmount(new BigDecimal(jsonObject.getString("amount")));
            deepVo.setPrice(new BigDecimal(jsonObject.getString("price")));
            deepVos.add(deepVo);
        }
        return deepVos;
    }
}
