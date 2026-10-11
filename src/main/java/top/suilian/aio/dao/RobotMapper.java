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

    /** 查询群绑定typename下未删除的记录，用于只读余额和状态指令。 */
    List<Robot> selectByTypename(@org.apache.ibatis.annotations.Param("typename") String typename);

    int updateByPrimaryKey(Robot record);
}
