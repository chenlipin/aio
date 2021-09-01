package top.suilian.aio.dao;

import java.util.List;
import top.suilian.aio.model.RobotLog;

public interface RobotLogMapper {
    int deleteByPrimaryKey(Integer robotLogId);

    int insert(RobotLog record);

    RobotLog selectByPrimaryKey(Integer robotLogId);

    List<RobotLog> selectAll();

    int updateByPrimaryKey(RobotLog record);
}