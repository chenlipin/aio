package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.model.Refer;

import java.util.*;

public class GateUtils extends BaseHttp {
    /**
     * 获取最新成交
     * @param symbol
     * @return
     */
    public static Refer getTrade(String symbol){
        Refer refer = new Refer();
        String resObject = get("https://data.gateio.life/api2/1/tradeHistory/"+symbol);
        JSONObject resJson = JSONObject.fromObject(resObject);
        if (resJson != null && "true".equals(resJson.getString("result"))) {
            JSONArray dataJson = resJson.getJSONArray("data");
            JSONObject nearLog = dataJson.getJSONObject(0);
            refer.setAmount(nearLog.getString("amount"));
            refer.setPrice(nearLog.getString("rate"));
            refer.setId(nearLog.getString("timestamp"));
            String type="buy";
            if(nearLog.getString("type").equals("sell")){
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
       Map<String,String> asks= new LinkedHashMap<>();

        String trades = get("https://data.gateio.life/api2/1/orderBook/" +symbol);
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {
            JSONArray bidsJson=tradesObj.getJSONArray("bids");
            JSONArray asksJson=tradesObj.getJSONArray("asks");
            for(int i=0;i<bidsJson.size();i++){
                bids.put(String.valueOf(bidsJson.getJSONArray(i).get(0)),String.valueOf(bidsJson.getJSONArray(i).get(1)));
            }
            depth.put("bids",bids);

            for(int i=asksJson.size()-1;i>-1;i--){
                asks.put(String.valueOf(asksJson.getJSONArray(i).get(0)),String.valueOf(asksJson.getJSONArray(i).get(1)));
            }
            depth.put("asks",asks);

        }
        return depth;
    }

}
