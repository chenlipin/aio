package top.suilian.aio.dao;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.SysConfig;

/**
 * 系统配置表查询接口。
 */
public interface SysConfigMapper {
    /**
     * 按配置键查询启用状态的配置。
     */
    SysConfig selectByConfigKey(@Param("configKey") String configKey);
}
