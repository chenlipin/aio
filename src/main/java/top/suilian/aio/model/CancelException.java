package top.suilian.aio.model;

import java.io.Serializable;
import java.util.Date;

public class CancelException implements Serializable {
    private Integer cancelExceptionId;

    private Integer cancelOrderId;

    private Date createdAt;

    private String remark;

    public Integer getCancelExceptionId() {
        return cancelExceptionId;
    }

    public void setCancelExceptionId(Integer cancelExceptionId) {
        this.cancelExceptionId = cancelExceptionId;
    }

    public Integer getCancelOrderId() {
        return cancelOrderId;
    }

    public void setCancelOrderId(Integer cancelOrderId) {
        this.cancelOrderId = cancelOrderId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }
}