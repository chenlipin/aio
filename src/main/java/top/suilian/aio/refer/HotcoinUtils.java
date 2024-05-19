package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.model.Refer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class HotcoinUtils extends BaseHttp {
    /**
     * 获取最新成交
     * @param symbol
     * @return
     */
    public static Refer getTrade(String symbol){
        Refer refer = new Refer();
        String resObject = get("https://api.hotcoinfin.com/v1/trade?count=1&symbol=" +symbol.toLowerCase());
        JSONObject resJson = JSONObject.fromObject(resObject);
        if (resJson != null && "200".equals(resJson.getString("code"))) {
            JSONObject tickJson = resJson.getJSONObject("data");
            JSONArray dataJson = tickJson.getJSONArray("trades");
            JSONObject nearLog = dataJson.getJSONObject(0);
            refer.setAmount(nearLog.getString("amount"));
            refer.setPrice(nearLog.getString("price"));
            refer.setId(nearLog.getString("ts"));
            String type="buy";
            if(nearLog.getString("en_type").equals("ask")){
                type="sell";
            }
            refer.setIsSell(type);
        }
        return refer;
    }


    public static List<DeepVo> getHistory(String symbol) {
        List<DeepVo> deepVos = new ArrayList<>();
        String resObject = get("https://api.hotcoinfin.com/v1/trade?count=10&symbol=" + symbol.toLowerCase());
        JSONObject resJson = JSONObject.fromObject(resObject);
        if (resJson != null && "200".equals(resJson.getString("code"))) {
            JSONObject tickJson = resJson.getJSONObject("data");
            JSONArray dataJson = tickJson.getJSONArray("trades");


            for (int i = 0; i < dataJson.size(); i += 3) {
                DeepVo deepVo = new DeepVo();
                JSONObject jsonObject = dataJson.getJSONObject(i);
                BigDecimal price = new BigDecimal(jsonObject.getString("price")).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros();
                boolean b = deepVos.stream().anyMatch(e -> e.getPrice().compareTo(price) == 0);
                if (b) {
                    continue;
                }
                deepVo.setType(RandomUtils.nextBoolean() ? 1 : 2);
                deepVo.setAmount(new BigDecimal(jsonObject.getString("amount")));
                deepVo.setPrice(price);
                deepVos.add(deepVo);
            }
            return deepVos;

        }else {
            return null;
        }
    }



    /**
     * 获取深度
     * @param symbol
     * @return
     */

    public static HashMap<String, Map<String,String>> getDepth(String symbol) {
        HashMap<String, Map<String,String>> depth = new HashMap<>();
        Map<String,String> bids= new LinkedHashMap<>();
        Map<String,String> asks=new LinkedHashMap<>();

        String trades = get("https://api.hotcoinfin.com/v1/depth?step=20&symbol=" +symbol.toLowerCase());
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (trades != null && !trades.isEmpty() && tradesObj != null) {
            JSONObject depthJson = tradesObj.getJSONObject("data").getJSONObject("depth");
            JSONArray bidsJson=depthJson.getJSONArray("bids");
            JSONArray asksJson=depthJson.getJSONArray("asks");

            for(int i=0;i<bidsJson.size();i++){
                JSONArray jsonArray = bidsJson.getJSONArray(i);
                bids.put(new BigDecimal(jsonArray.getString(0)).stripTrailingZeros().toPlainString(),jsonArray.getString(1));
            }
            depth.put("bids",bids);
            for(int i=0;i<asksJson.size();i++){
                JSONArray jsonArray = asksJson.getJSONArray(i);
                asks.put(new BigDecimal(jsonArray.getString(0)).stripTrailingZeros().toPlainString(),jsonArray.getString(1));
            }
            depth.put("asks",asks);

        }
        return depth;
    }




    public static Map<String, List<DeepVo>> getdeep(String symbol) {
        Map<String, List<DeepVo>> map = new HashMap<>();
        List<DeepVo> buy = new ArrayList<>();
        List<DeepVo> sell = new ArrayList<>();
        String trades = get("https://api.hotcoinfin.com/v1/depth?step=20&symbol=" +symbol.toLowerCase());
        JSONObject tradesObj = JSONObject.fromObject(trades);
        JSONObject depthJson = tradesObj.getJSONObject("data").getJSONObject("depth");
        JSONArray bidsJson=depthJson.getJSONArray("bids");
        JSONArray asksJson=depthJson.getJSONArray("asks");


        for (int i = 0; i < bidsJson.size(); i+=1) {
            DeepVo deepVo = new DeepVo();
            JSONArray jsonArray = bidsJson.getJSONArray(i);
            BigDecimal amount = new BigDecimal(jsonArray.getString(1));
            BigDecimal price = new BigDecimal(jsonArray.getString(0)).setScale(9, RoundingMode.HALF_UP).stripTrailingZeros();
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
            BigDecimal price = new BigDecimal(jsonArray.getString(0)).setScale(9, RoundingMode.HALF_UP).stripTrailingZeros();
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


    public static Map<String, BigDecimal> getDepth1(String symbol) {
        HashMap<String,BigDecimal> depth = new HashMap<>();

        String referSymbol=symbol.replaceAll("_","");

        String trades = get("https://api.huobi.pro/market/depth?symbol=" + referSymbol + "&depth=5&type=step0");
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (trades != null && !trades.isEmpty() && tradesObj != null) {
            JSONObject depthJson = tradesObj.getJSONObject("tick");
            JSONArray bidsJson=depthJson.getJSONArray("bids");
            JSONArray asksJson=depthJson.getJSONArray("asks");
            depth.put("bids",  new BigDecimal(String.valueOf(bidsJson.getJSONArray(0).get(0))));
            depth.put("asks", new BigDecimal(String.valueOf(asksJson.getJSONArray(0).get(0))));
        }
        return depth;
    }


}
