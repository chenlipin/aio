
package top.suilian.aio.model;

public enum TradeEnum {

    /**
     * 0:未成交
     * 1:半成交
     * 2:已成交
     * -1:已撤单
     */
    NOTRADE(0,"未成交"),

    TRADEING(1,"半成交"),

    NOTRADED(2,"已成交"),

    CANCEL(-1,"已撤单");

    private Integer status;
    private String statusStr;

    TradeEnum(Integer status, String statusStr) {
        this.status = status;
        this.statusStr = statusStr;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusStr() {
        return statusStr;
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
    }

    public static String getMessage(Integer value) {
        TradeEnum[] businessModeEnums = values();
        for (TradeEnum businessModeEnum : businessModeEnums) {
            if (businessModeEnum.getStatus().equals(value)) {
                return businessModeEnum.getStatusStr();
            }
        }
        return null;
    }
}
