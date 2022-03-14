package top.suilian.aio.service.basic;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HMAC;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@DependsOn("beanContext")
@Service
public class BasicParentService extends BaseService{
    public String baseUrl = "http://45.32.127.96:11100";

    public Map<String, Object> precision = new HashMap<String, Object>();
    public int cnt = 0;
    public boolean isTest = true;
    public boolean submitCnt = true;
    public int valid = 1;
    public String exceptionMessage = null;

    public String submitOrder(List<OrderVO> list) {
        Map<String, Object> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String ApiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", ApiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("orders", com.alibaba.fastjson.JSONObject.toJSONString(list));
        String splicing = "";
        String post = "";
        try {
            splicing = HMAC.splicing(map);
            String sign = HMAC.MD5(URLEncoder.encode(splicing, "UTF-8"));
            map.put("sign", sign);
            setTradeLog(id, "挂单参数" + com.alibaba.fastjson.JSONObject.toJSONString(list),0, "000000");
            post = HttpUtil.post(baseUrl + "/api/v1/placeOrders", map);
            setTradeLog(id, "挂单结果" + com.alibaba.fastjson.JSONObject.toJSONString(map),0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    public String cancalOrder(com.alibaba.fastjson.JSONArray list, String pair) {
        Map<String, Object> map = new TreeMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String ApiKey = exchange.get("apikey");
        String tpass = exchange.get("tpass");
        map.put("ApiKey", ApiKey);
        map.put("ApiSecret", tpass);
        map.put("timestamp", timestamp);
        map.put("pair",pair);
        map.put("orders", com.alibaba.fastjson.JSONObject.toJSONString(list));
        String splicing = "";
        String post = "";
        try {
            splicing = HMAC.splicing(map);
            String sign = HMAC.MD5(splicing);
            map.put("sign", sign);
            setTradeLog(id, "撤单参数" + com.alibaba.fastjson.JSONObject.toJSONString(list),0, "#67c23a");
            post = HttpUtil.post(baseUrl + "/v1/order/batchCancel", map);
            setTradeLog(id, "撤单结果" + post,0, "#67c23a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    //获取余额
    public void setBalanceRedis() throws UnsupportedEncodingException {
        String coins = redisHelper.getBalanceParam(Constant.KEY_ROBOT_COINS + id);
        if (coins == null) {
            RobotArgs robotArgs = robotArgsService.findOne(id, "market");
            coins = robotArgs.getRemark();
            redisHelper.setBalanceParam(Constant.KEY_ROBOT_COINS + id, robotArgs.getRemark());
        }
        String balance = redisHelper.getBalanceParam(Constant.KEY_ROBOT_BALANCE + id);
        boolean overdue = false;
        if (balance != null) {
            long lastTime = redisHelper.getLastTime(Constant.KEY_ROBOT_BALANCE + id);
            if (System.currentTimeMillis() - lastTime > Constant.KEY_BALACE_TIME) {
                overdue = true;
            }
        }
        if (balance == null || overdue) {
            List<String> coinArr = Arrays.asList(coins.split("_"));


            Map<String, Object> map = new TreeMap<>();
            String timestamp = System.currentTimeMillis() + "";
            String ApiKey = exchange.get("apikey");
            String tpass = exchange.get("tpass");
            map.put("ApiKey", ApiKey);
            map.put("ApiSecret", tpass);
            map.put("timestamp", timestamp);
            String splicing = "";
            String post = "";
            try {
                splicing = HMAC.splicing(map);
                String sign = HMAC.MD5(splicing);
                map.put("sign", sign);
                post = HttpUtil.post(baseUrl + "/api/v1/balanceList", map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            JSONObject jsonObject = JSONObject.fromObject(post);
            if (jsonObject !=null && jsonObject.getInt("status") == 200) {
                JSONArray data = jsonObject.getJSONArray("data");
                String firstBalance="";
                String firstBalancefreeze="";
                String lastBalance="";
                String lastBalancefreeze="";
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject1 = data.getJSONObject(i);
                    if (jsonObject1.getString("coinCode").equals(coinArr.get(0))){
                         firstBalance = jsonObject1.getString("balanceAvailable");
                         firstBalancefreeze = jsonObject1.getString("balanceFrozen");
                    }
                    if (jsonObject1.getString("coinCode").equals(coinArr.get(1))){
                         lastBalance = jsonObject1.getString("balanceFrozen");
                         lastBalancefreeze = jsonObject1.getString("freeze");
                    }
                }
                HashMap<String, String> balances = new HashMap<>();
                balances.put(coinArr.get(0), firstBalance + "_" + firstBalancefreeze);
                balances.put(coinArr.get(1), lastBalance + "_" + lastBalancefreeze);
                redisHelper.setBalanceParam(Constant.KEY_ROBOT_BALANCE + id, balances);
            }
        }
    }



}
