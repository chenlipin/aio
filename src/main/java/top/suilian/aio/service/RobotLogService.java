package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.RobotLogMapper;
import top.suilian.aio.model.RobotLog;

@Service("robotLogService")
public class RobotLogService {
    @Autowired
    RobotLogMapper robotLogMapper;

    public int insert(RobotLog robotLog){
        return robotLogMapper.insert(robotLog);
    }

}
