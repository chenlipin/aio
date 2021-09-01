package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.Refer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
public class HuoBiUtils extends BaseHttp {
    /**
     * 获取最新成交
     * @param symbol
     * @return
     */
    public static Refer getTrade(String symbol){
        Refer refer = new Refer();
        String referSymbol=symbol.replaceAll("_","");
        String resObject = get("https://api.huobi.pro/market/trade?symbol=" +referSymbol);
        JSONObject resJson = JSONObject.fromObject(resObject);
        if (resJson != null && "ok".equals(resJson.getString("status"))) {
            JSONObject tickJson = resJson.getJSONObject("tick");
            JSONArray dataJson = tickJson.getJSONArray("data");
            JSONObject nearLog = dataJson.getJSONObject(0);
            refer.setAmount(nearLog.getString("amount"));
            refer.setPrice(nearLog.getString("price"));
            refer.setId(nearLog.getString("ts"));
            String type="buy";
            if(nearLog.getString("direction").equals("sell")){
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
        String referSymbol=symbol.replaceAll("_","");

        String trades = get("https://api.huobi.pro/market/depth?symbol=" + referSymbol + "&depth=20&type=step0");
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {
            JSONObject depthJson = tradesObj.getJSONObject("tick");
            JSONArray bidsJson=depthJson.getJSONArray("bids");
            JSONArray asksJson=depthJson.getJSONArray("asks");
            for(int i=0;i<bidsJson.size();i++){
                bids.put(String.valueOf(bidsJson.getJSONArray(i).get(0)),String.valueOf(bidsJson.getJSONArray(i).get(1)));
            }
            depth.put("bids",bids);
            for(int i=0;i<asksJson.size();i++){
                asks.put(String.valueOf(asksJson.getJSONArray(i).get(0)),String.valueOf(asksJson.getJSONArray(i).get(1)));
            }
            depth.put("asks",asks);

        }
        return depth;
    }


}
