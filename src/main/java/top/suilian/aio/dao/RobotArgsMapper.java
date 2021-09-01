package top.suilian.aio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.RobotArgs;

public interface RobotArgsMapper {
    int deleteByPrimaryKey(Integer robotArgsId);

    int insert(RobotArgs record);

    RobotArgs selectByPrimaryKey(Integer robotArgsId);

    List<RobotArgs> selectAll(Integer robotId);

    int updateByPrimaryKey(RobotArgs record);

    RobotArgs findOne(@Param("robotId") Integer robotId, @Param("variable") String variable);

    int update(@Param("robotId") Integer robotId,@Param("variable") String variable, @Param("value") String value);
}