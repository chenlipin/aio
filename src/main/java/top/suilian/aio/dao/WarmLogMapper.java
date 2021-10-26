package top.suilian.aio.dao;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.WarmLog;

/**
 * @Entity generator.domain.WarmLog
 */
public interface WarmLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(WarmLog record);

    int insertSelective(WarmLog record);

    WarmLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(WarmLog record);

    int updateByPrimaryKey(WarmLog record);

    WarmLog selectByRobotIdAndType(@Param("robotId") Integer robotId, @Param("type") Integer type);


}
