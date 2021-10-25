package top.suilian.aio.service;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.TradeLogMapper;
import top.suilian.aio.dao.WarmLogMapper;
import top.suilian.aio.model.TradeLog;
import top.suilian.aio.model.WarmLog;

@Service("tradeLog")
public class TradeLogService {
    @Autowired
    TradeLogMapper tradeLogMapper;

    @Autowired
    WarmLogMapper warmLogMapper;


    public int insert(TradeLog tradeLog) {
        return tradeLogMapper.insert(tradeLog);
    }

    public int deletedByTime(Integer robotId, String startTime) {
        return tradeLogMapper.deletedByTime(robotId, startTime);
    }

    public WarmLog selectByRobotIdAndType(Integer robotId,  Integer type){
       return warmLogMapper.selectByRobotIdAndType(robotId,type);
    }

    public int insertWarmLog(WarmLog warmLog) {
        return warmLogMapper.insert(warmLog);
    }

    public int updateWarmLog(WarmLog warmLog) {
        return warmLogMapper.updateByPrimaryKeySelective(warmLog);
    }
}
