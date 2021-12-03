package top.suilian.aio.dao;
import org.apache.ibatis.annotations.Param;


import org.springframework.stereotype.Repository;
import top.suilian.aio.model.ApitradeLog;
import top.suilian.aio.vo.getAllOrderPonse;

import java.util.List;

@Repository
public interface ApitradeLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ApitradeLog record);

    int insertSelective(ApitradeLog record);

    ApitradeLog selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ApitradeLog record);

    int updateByPrimaryKey(ApitradeLog record);

    List<getAllOrderPonse> selectByRobotId(Integer robotId);

    List<ApitradeLog> selectByRobotIdNOTrade(Integer robotId);

   ApitradeLog selectByRobotIdAndOrderId(@Param("robotId") Integer robotId, @Param("orderId") String orderId);

    List<ApitradeLog> selectByRobotIdAndTime(@Param("robotId")Integer robotId,@Param("time") String time);
}