package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.redis.RedisHelper;

import java.util.List;

@Service
public class RobotService {
    @Autowired
    RobotMapper robotMapper;
    @Autowired
    RedisHelper redisHelper;
    public Robot findById(Integer id){
        return  robotMapper.selectByPrimaryKey(id);
    }

    public List<Robot> findByMemberId(Integer id){
        return  robotMapper.findByMemberId(id);
    }

    public int update(Robot robot) {
        int rt = robotMapper.updateByPrimaryKey(robot);
        redisHelper.initRobot(robot.getRobotId());
        return rt;
    }

    public int setRobotStatus(Integer id, Integer status){
        Robot robot = findById(id);
        robot.setStatus(status);
        return update(robot);
    }

    public int stopRobot(Integer id){
        Robot robot = findById(id);
        robot.setStatus(Constant.KEY_ROBOT_STATUS_FREE);
        robot.setServiceIp(robot.getNewIp());
        robot.setServicePort(robot.getNewPort());
        return update(robot);
    }
}
