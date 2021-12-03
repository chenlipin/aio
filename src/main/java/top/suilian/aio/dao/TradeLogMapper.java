package top.suilian.aio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.TradeLog;

@Mapper
public interface TradeLogMapper {
    int deleteByPrimaryKey(Integer tradeLogId);

    int insert(TradeLog record);

    TradeLog selectByPrimaryKey(Integer tradeLogId);

    List<TradeLog> selectAll();

    int updateByPrimaryKey(TradeLog record);

    public int deletedByTime(@Param("robotId") Integer robotId, @Param("startTime") String startTime);

    List<TradeLog> selectByid(@Param("robotId")Integer robotId, @Param("time")String time);
}