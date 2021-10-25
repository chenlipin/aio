package top.suilian.aio.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 警告信息
 *
 * @TableName warm_log
 */
@Data
public class WarmLog implements Serializable {
    /**
     *
     */
    private Integer id;

    /**
     *
     */
    private Integer robotId;

    /**
     * 0.余额不足
     * 2.撞单
     * 3.api错误
     * 99.其他
     */
    private Integer type;

    /**
     * 警告信息
     */
    private String warmMsg;

    /**
     * 详细信息
     */
    private String warmDetailMsg;

    /**
     *
     */
    private Date creatTime;

    /**
     *
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        WarmLog other = (WarmLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getRobotId() == null ? other.getRobotId() == null : this.getRobotId().equals(other.getRobotId()))
                && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
                && (this.getWarmMsg() == null ? other.getWarmMsg() == null : this.getWarmMsg().equals(other.getWarmMsg()))
                && (this.getWarmDetailMsg() == null ? other.getWarmDetailMsg() == null : this.getWarmDetailMsg().equals(other.getWarmDetailMsg()))
                && (this.getCreatTime() == null ? other.getCreatTime() == null : this.getCreatTime().equals(other.getCreatTime()))
                && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getRobotId() == null) ? 0 : getRobotId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getWarmMsg() == null) ? 0 : getWarmMsg().hashCode());
        result = prime * result + ((getWarmDetailMsg() == null) ? 0 : getWarmDetailMsg().hashCode());
        result = prime * result + ((getCreatTime() == null) ? 0 : getCreatTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", robotId=").append(robotId);
        sb.append(", type=").append(type);
        sb.append(", warmMsg=").append(warmMsg);
        sb.append(", warmDetailMsg=").append(warmDetailMsg);
        sb.append(", creatTime=").append(creatTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}