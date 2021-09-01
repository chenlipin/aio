package top.suilian.aio.model;

import java.io.Serializable;
import java.util.Date;

public class ExceptionMessage implements Serializable {
    private Integer exceptionMessageId;

    private Integer robotId;

    private int isMobile;

    private Date createdAt;

    private Date updatedAt;

    private String message;

    public Integer getExceptionMessageId() {
        return exceptionMessageId;
    }

    public void setExceptionMessageId(Integer exceptionMessageId) {
        this.exceptionMessageId = exceptionMessageId;
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    public int getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(int isMobile) {
        this.isMobile = isMobile;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? null : message.trim();
    }
}