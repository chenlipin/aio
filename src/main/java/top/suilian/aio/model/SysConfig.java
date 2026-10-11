package top.suilian.aio.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置表实体，用来保存全局配置，例如 Telegram Bot Token。
 */
public class SysConfig implements Serializable {
    /** 主键ID */
    private Integer id;
    /** 配置键，例如 telegram.bot.token */
    private String configKey;
    /** 配置值 */
    private String configValue;
    /** 配置备注 */
    private String remark;
    /** 是否启用：1启用，0停用 */
    private Integer enabled;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey == null ? null : configKey.trim();
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue == null ? null : configValue.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
