package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.model.Refer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
