
package top.suilian.aio.vo;



public enum ResultCode {

    /**
     * 状态码 200-299 操作成功
     */
    SUCCESS("0", "操作成功"),
    ERROR("1001", "操作失败"),
    ;


    private String errcode;

    private String message;

    ResultCode(String errcode, String message) {
        this.errcode = errcode;
        this.message = message;
    }

    public String getErrcode(){
        return this.errcode;
    }

    public String getMessage(){
        return this.message;
    }

}
