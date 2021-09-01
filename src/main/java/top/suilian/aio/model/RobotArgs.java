package top.suilian.aio.model;

import java.io.Serializable;

public class RobotArgs implements Serializable {
    private Integer robotArgsId;

    private Integer robotId;

    private String variable;

    private String describe;

    private String remark;

    private String value;

    public Integer getRobotArgsId() {
        return robotArgsId;
    }

    public void setRobotArgsId(Integer robotArgsId) {
        this.robotArgsId = robotArgsId;
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable == null ? null : variable.trim();
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe == null ? null : describe.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? null : value.trim();
    }
}