package top.suilian.aio.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Telegram发送日志实体，用来记录每次通知的发送结果。
 */
public class TelegramSendLog implements Serializable {
    /** 主键ID */
    private Long id;
    /** 触发通知的机器人ID */
    private Integer robotId;
    /** 触发通知的逻辑机器人标识 */
    private String typename;
    /** Telegram接收目标ID */
    private Integer channelId;
    /** 通知类型，例如 balance_low */
    private String noticeType;
    /** 实际发送的消息内容 */
    private String message;
    /** 是否发送成功：1成功，0失败 */
    private Integer success;
    /** Telegram接口响应或异常信息 */
    private String response;
    /** 创建时间 */
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    /** 获取通知所属的typename。 */
    public String getTypename() {
        return typename;
    }

    /** 保存通知所属的typename。 */
    public void setTypename(String typename) {
        this.typename = typename == null ? null : typename.trim();
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType == null ? null : noticeType.trim();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? null : message.trim();
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response == null ? null : response.trim();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
