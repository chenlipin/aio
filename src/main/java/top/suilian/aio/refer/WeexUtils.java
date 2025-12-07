package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.Util.HttpUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeexUtils extends BaseHttp {
    public static String uri = "https://api-spot.weex.com";

    public static Map<String, List<DeepVo>> getdeep(String symbol) {
        Map<String, List<DeepVo>> map = new HashMap<>();
        List<DeepVo> buy = new ArrayList<>();
        List<DeepVo> sell = new ArrayList<>();
        String trades = HttpUtil.get("https://api-spot.weex.com/api/v2/market/depth?symbol="+symbol);
        System.out.println("https://api-spot.weex.com/api/v2/market/depth?symbol="+symbol+"&type=step0&limit=10");
        JSONObject tradesObj = JSONObject.fromObject(trades).getJSONObject("data");
        //买单
        JSONArray bidsJson = tradesObj.getJSONArray("bids");
        //卖单
        JSONArray asksJson = tradesObj.getJSONArray("asks");

        for (int i = 0; i < bidsJson.size(); i+=1) {
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
        for (int i = 0; i < asksJson.size(); i+=1) {
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
        String trades = get("https://api-spot.weex.com/api/v2/market/fills?symbol=" + symbol.toUpperCase()+"&limit=10");
        JSONArray jsonArray = JSONObject.fromObject(trades).getJSONArray("data");
        for (int i = 0; i < jsonArray.size(); i+=2) {
            DeepVo deepVo = new DeepVo();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            BigDecimal price = new BigDecimal(jsonObject.getString("fillPrice")).setScale(9, RoundingMode.HALF_UP);
            boolean b = deepVos.stream().anyMatch(e -> e.getPrice().compareTo(price) == 0);
            if (b){
                continue;
            }
            deepVo.setType(RandomUtils.nextBoolean() ? 1 : 2);
            deepVo.setAmount(new BigDecimal(jsonObject.getString("fillQuantity")));
            deepVo.setPrice(price);
            deepVos.add(deepVo);
        }
        return deepVos;
    }
}
