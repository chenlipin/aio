package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.BaseHttp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BianUtils extends BaseHttp {
    public static String uri = "https://api.binance.com";

    public static Map<String, List<DeepVo>> getdeep(String symbol) {
        Map<String, List<DeepVo>> map = new HashMap<>();
        List<DeepVo> buy = new ArrayList<>();
        List<DeepVo> sell = new ArrayList<>();
        String trades = get(uri + "/api/v3/depth?limit=200&symbol=" + symbol.toUpperCase());
        JSONObject tradesObj = JSONObject.fromObject(trades);
        //买单
        JSONArray bidsJson = tradesObj.getJSONArray("bids");
        //卖单
        JSONArray asksJson = tradesObj.getJSONArray("asks");

        for (int i = 0; i < bidsJson.size(); i+=5) {
            DeepVo deepVo = new DeepVo();
            JSONArray jsonArray = bidsJson.getJSONArray(i);
            BigDecimal amount = new BigDecimal(jsonArray.getString(1));
            BigDecimal price = new BigDecimal(jsonArray.getString(0)).setScale(9, RoundingMode.HALF_UP);
            boolean b = buy.stream().anyMatch(e -> e.getPrice().compareTo(price) == 0);
            if (b){
                continue;
            }
            deepVo.setType(1);
            deepVo.setAmount(amount);
            deepVo.setPrice(price);
            buy.add(deepVo);
        }
        for (int i = 0; i < asksJson.size(); i+=5) {
            DeepVo deepVo = new DeepVo();
            JSONArray jsonArray = asksJson.getJSONArray(i);
            BigDecimal amount = new BigDecimal(jsonArray.getString(1));
            BigDecimal price = new BigDecimal(jsonArray.getString(0)).setScale(9, RoundingMode.HALF_UP);
            boolean b = buy.stream().anyMatch(e -> e.getPrice().compareTo(price) == 0);
            if (b){
                continue;
            }
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
        String trades = get(uri + "/api/v3/trades?limit=30&symbol=" + symbol.toUpperCase());
        JSONArray jsonArray = JSONArray.fromObject(trades);
        for (int i = 0; i < jsonArray.size(); i+=3) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            BigDecimal price = new BigDecimal(jsonObject.getString("price")).setScale(9, RoundingMode.HALF_UP);
            boolean b = deepVos.stream().anyMatch(e -> e.getPrice().compareTo(price) == 0);
            if (b){
                continue;
            }
            deepVo.setType(RandomUtils.nextBoolean() ? 1 : 2);
            deepVo.setAmount(new BigDecimal(jsonObject.getString("qty")));
            deepVo.setPrice(price);
            deepVos.add(deepVo);
        }
        return deepVos;
    }

    public static void main(String[] args) {
        int a=100;
        for (int i = 0; i < a; i++) {
            System.out.println( RandomUtils.nextInt(3));
        }
    }
}
