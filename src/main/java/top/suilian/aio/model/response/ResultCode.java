
package top.suilian.aio.model.response;



public enum ResultCode {

    /**
     *
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
