package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.TradeLogMapper;
import top.suilian.aio.model.TradeLog;

@Service("tradeLog")
public class TradeLogService {
    @Autowired
    TradeLogMapper tradeLogMapper;


    public int insert(TradeLog tradeLog) {
        return tradeLogMapper.insert(tradeLog);
    }

    public int deletedByTime(Integer robotId, String startTime) {
        return tradeLogMapper.deletedByTime(robotId, startTime);
    }
}
