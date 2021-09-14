package top.suilian.aio.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import top.suilian.aio.model.Robot;
@Repository
public interface RobotMapper {
    int deleteByPrimaryKey(Integer robotId);

    int insert(Robot record);

    Robot selectByPrimaryKey(Integer robotId);

    List<Robot> findByMemberId(Integer memberId);

    List<Robot> selectAll();

    int updateByPrimaryKey(Robot record);
}