package top.suilian.aio.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.Member;
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.RobotArgsService;
import top.suilian.aio.service.RobotService;

import java.util.HashMap;
import java.util.List;

@Component
public class RedisHelper {
    @Autowired
    RedisStringExecutor redisStringExecutor;
    @Autowired
    RobotArgsService robotArgsService;
    @Autowired
    RobotService robotService;

    /**
     * 设置机器人
     *
     * @param id
     */
    public void initRobot(Integer id) {
        Robot robot = robotService.findById(id);
        RedisObject arg = new RedisObject();
        arg.setExtObject(robot);
        redisStringExecutor.set(Constant.KEY_ROBOT + id, JSON.toJSONString(arg));
    }

    /**
     * 获得机器人
     *
     * @param id
     */
    public Robot getRobot(Integer id) {
        String str = redisStringExecutor.get(Constant.KEY_ROBOT + id);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(str);
        Robot value = JSON.parseObject(obj.get("extObject").toString(), Robot.class);
        return value;
    }

    /**
     * 设置机器人参数
     *
     * @param id
     */
    public void initRobotArgs(Integer id) {
        List<RobotArgs> robotArgs = robotArgsService.findAll(id);
        for (RobotArgs robotArg : robotArgs) {
            RedisObject arg = new RedisObject();
            arg.setExtObject(robotArg);
            redisStringExecutor.set(Constant.KEY_ROBOT_ARG + id + "_" + robotArg.getVariable(), JSON.toJSONString(arg));
        }
    }

    /**
     * 获取机器人参数
     *
     * @param id
     */
    public String getArgsValue(Integer id, String variable) {
        String str = redisStringExecutor.get(Constant.KEY_ROBOT_ARG + id + "_" + variable);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(str);
        RobotArgs value = JSON.parseObject(obj.get("extObject").toString(), RobotArgs.class);
        return value.getValue();
    }

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

    public Member getUser(String token) {
        String str = redisStringExecutor.get(token);
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        Member user = JSON.parseObject(str, Member.class);
        return user;
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
