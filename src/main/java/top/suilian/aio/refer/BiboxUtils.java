package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.model.Refer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
public class BiboxUtils extends BaseHttp {
    /**
     * 获取最新成交
     * @param symbol
     * @return
     */
    public static Refer getTrade(String symbol){
        Refer refer = new Refer();
        String resObject = get("https://api.bibox.com/v1/mdata?cmd=deals&pair="+symbol.toUpperCase()+"&size=1");
        JSONObject resJson = JSONObject.fromObject(resObject);
        JSONArray result = resJson.getJSONArray("result");
        JSONObject nearResult = result.getJSONObject(0);
        if (resJson != null && nearResult.getString("time")!=null) {
            refer.setAmount(nearResult.getString("amount"));
            refer.setPrice(nearResult.getString("price"));
            refer.setId(nearResult.getString("time"));
            String type="buy";
            if(nearResult.getInt("side")==2){   ///taker成交方向，1-买，2-卖
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

        String trades = get("https://api.bibox.com/v1/mdata?cmd=depth&pair="+symbol.toUpperCase()+"&size=20");
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (trades != null &&tradesObj!=null&&tradesObj.getJSONObject("result")!=null) {
            JSONObject depthJson = tradesObj.getJSONObject("result");
            JSONArray bidsJson=depthJson.getJSONArray("bids");
            JSONArray asksJson=depthJson.getJSONArray("asks");
            for(int i=0;i<bidsJson.size();i++){
                bids.put(String.valueOf(bidsJson.getJSONObject(i).getString("price")),String.valueOf(bidsJson.getJSONObject(i).getString("volume")));
            }
            depth.put("bids",bids);
            for(int i=0;i<asksJson.size();i++){
                asks.put(String.valueOf(asksJson.getJSONObject(i).getString("price")),String.valueOf(asksJson.getJSONObject(i).getString("volume")));
            }
            depth.put("asks",asks);

        }
        return depth;
    }


}
