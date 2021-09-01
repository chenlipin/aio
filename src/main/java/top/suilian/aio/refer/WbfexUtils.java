package top.suilian.aio.refer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.BaseHttp;
import top.suilian.aio.model.Refer;

import java.math.BigDecimal;
import java.util.*;

public class WbfexUtils extends BaseHttp {

    /**
     * 获取最新成交
     * @param symbol
     * @return
     */
    public static Refer getTrade(String symbol){
        Refer refer = new Refer();
        String resObject =get("https://openapi.wbf.info/open/api/get_trades?symbol="+symbol);
        JSONObject resJson = JSONObject.fromObject(resObject);
        if (resJson != null && !resJson.isEmpty()) {
            JSONArray data = resJson.getJSONArray("data");
            JSONObject nearLog = data.getJSONObject(0);
            refer.setAmount(nearLog.getString("amount"));
            refer.setPrice(nearLog.getString("price"));
            refer.setId(nearLog.getString("id"));
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
    public  static HashMap<String, Map<String,String>> getDepth(String symbol) {
        HashMap<String, Map<String,String>> depth = new HashMap<>();
        Map<String,String> bids= new LinkedHashMap<>();
        Map<String,String> asks=new LinkedHashMap<>();

        String trades = get("https://openapi.wbf.info/open/api/market_dept?symbol="+symbol+ "&type=step0");
        JSONObject tradesObj = JSONObject.fromObject(trades);

        if (!"".equals(trades) && trades != null && !trades.isEmpty() && tradesObj != null) {
            JSONObject data = tradesObj.getJSONObject("data");

            JSONObject tick = data.getJSONObject("tick");

            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");

            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");

            for(int i=0;i<buyPrices.size();i++){
                bids.put(String.valueOf(buyPrices.get(i).get(0)),String.valueOf(buyPrices.get(i).get(1)));
            }
            depth.put("bids",bids);
            for(int i=0;i<sellPrices.size();i++){
                asks.put(String.valueOf(sellPrices.get(i).get(0)),String.valueOf(sellPrices.get(i).get(1)));
            }
            depth.put("asks",asks);

        }
        return depth;
    }

}
