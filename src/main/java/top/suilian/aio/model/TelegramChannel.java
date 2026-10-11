package top.suilian.aio.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Telegram接收目标实体，对应一个群组、频道或用户会话。
 */
public class TelegramChannel implements Serializable {
    /** 主键ID */
    private Integer id;
    /** 接收目标名称，便于后台识别 */
    private String name;
    /** Telegram chat_id，群组一般为负数 */
    private String chatId;
    /** 是否启用：1启用，0停用 */
    private Integer enabled;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
    /** 机器人绑定的通知类型，来自关联表 robot_telegram_channel.notice_types */
    private String noticeTypes;
    /** 当前群绑定的逻辑机器人标识，仅在群绑定查询中返回 */
    private String typename;
    /** 群绑定的主动通知开关，不影响查询权限。 */
    private Integer noticeEnabled;

    /** 获取主动通知开关。 */
    public Integer getNoticeEnabled() {
        return noticeEnabled;
    }

    /** 设置主动通知开关。 */
    public void setNoticeEnabled(Integer noticeEnabled) {
        this.noticeEnabled = noticeEnabled;
    }

    /** 获取群绑定的typename。 */
    public String getTypename() {
        return typename;
    }

    /** 设置群绑定的typename。 */
    public void setTypename(String typename) {
        this.typename = typename == null ? null : typename.trim();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId == null ? null : chatId.trim();
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

    public String getNoticeTypes() {
        return noticeTypes;
    }

    public void setNoticeTypes(String noticeTypes) {
        this.noticeTypes = noticeTypes == null ? null : noticeTypes.trim();
    }
}
