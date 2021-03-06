package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.RobotLogMapper;
import top.suilian.aio.dao.WarmLogMapper;
import top.suilian.aio.model.RobotLog;
import top.suilian.aio.model.WarmLog;

@Service("robotLogService")
public class RobotLogService {
    @Autowired
    RobotLogMapper robotLogMapper;
    @Autowired
    WarmLogMapper warmLogMapper;

    public int insert(RobotLog robotLog){
        return robotLogMapper.insert(robotLog);
    }


    public int insertWarmLog(WarmLog warmLog){
        return warmLogMapper.insert(warmLog);
    }

}
