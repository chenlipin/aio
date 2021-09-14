/*
 * Copyright (C) 1997-2020 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.vo;


/**
 * @author huabo.yang
 * @create 2021-04-09 16:04
 * @description 常见错误枚举
 */
public enum ResultCode {

    /**
     * 状态码 200-299 操作成功
     */
    SUCCESS("0", "操作成功"),
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
