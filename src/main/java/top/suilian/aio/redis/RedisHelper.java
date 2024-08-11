package top.suilian.aio.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;

@Component
public class RedisHelper {
    @Autowired
    RedisStringExecutor redisStringExecutor;


    public void setParam(String key, String value) {
        RedisObject arg = new RedisObject();
        arg.setExtObject(value);
        redisStringExecutor.set("ROBOT_PARAM_" + key, JSON.toJSONString(arg));
    }

    public String getParam(String key) {
        String str = redisStringExecutor.get("ROBOT_PARAM_" + key);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(str);
        String value = obj.get("extObject").toString();
        return value;
    }

    public String get(String key) {
        String str = redisStringExecutor.get( key);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return str;
    }

    public void setBalanceParam(String key, String value) {
        RedisObject arg = new RedisObject();
        arg.setExtObject(value);
        redisStringExecutor.set(key, JSON.toJSONString(arg));
    }

    public String getBalanceParam(String key) {
        String str = redisStringExecutor.get(key);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(str);
        String value = obj.get("extObject").toString();
        return value;
    }

    public String getClearLogParam(String key) {
        String str = redisStringExecutor.get(key);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(str);
        String value = obj.get("extObject").toString();
        return value;
    }




    public long getLastTime(String key) {
        String str = redisStringExecutor.get(key);
        if (StringUtils.isEmpty(str)) {
            return Long.parseLong(null);
        }
        JSONObject obj = JSON.parseObject(str);
        long value = Long.valueOf(obj.get("lastActiveDateTime").toString());
        return value;
    }

    public void setClearLogParam(String key, String value) {
        RedisObject arg = new RedisObject();
        arg.setExtObject(value);
        redisStringExecutor.set(key, JSON.toJSONString(arg));
    }


    public void setBalanceParam(String key, HashMap<String, String> value) {
        RedisObject arg = new RedisObject();
        arg.setExtObject(value);
        redisStringExecutor.set(key, JSON.toJSONString(arg));
    }

    public void remove(String... key) {
        redisStringExecutor.remove(key);
    }

    public void removeParent(String key) {
        redisStringExecutor.removeParent("ROBOT_PARAM_" + key);
    }
}
