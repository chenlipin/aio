/*
 *Copyright (C)1997-2021康成投资}(中国) 有限公司
 *
 *http://www.rt-mart.com
 *
 *版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */

package top.suilian.aio.model.response;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author ：huabo.yang
 * @description：自定义返回数据格式
 * @modified By：yulong.ao
 * @version: 1.1.1$
 */

@Data
@Slf4j
public class ResponseEntity implements Serializable {

    /**
     * 响应代码
     */

    private String errcode;

    /**
     * 响应信息
     */

    private String msg;

    /**
     * 响应结果
     */

    private Object data;

    /**
     * 响应结果
     */

    private String id;


    /**
     * 成功
     *
     * @return 标准响应体
     */
    public static ResponseEntity success() {
    	ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(ResultCode.SUCCESS.getErrcode());
        responseEntity.setMsg(ResultCode.SUCCESS.getMessage());
        return responseEntity;
    }

    /**
     * 成功
     *
     * @param data 响应数据
     * @return 标准响应体
     */

    public static ResponseEntity success(Object data) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(ResultCode.SUCCESS.getErrcode());
        responseEntity.setMsg(ResultCode.SUCCESS.getMessage());
        responseEntity.setData(data);
        return responseEntity;
    }

    /**
     * 成功自定义响应msg
     *
     * @param resultCode 响应数据msg
     * @return 标准响应体
     */

    public static ResponseEntity success(ResultCode resultCode) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(ResultCode.SUCCESS.getErrcode());
        responseEntity.setMsg(resultCode.getMessage());
        responseEntity.setData(null);
        return responseEntity;
    }

    /**
     *
     * @param resultCode
     * @return
     */
    public static ResponseEntity error(ResultCode resultCode) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(resultCode.getErrcode());
        responseEntity.setMsg(resultCode.getMessage());
        responseEntity.setData(null);
        return responseEntity;
    }



    public static ResponseEntity error(ResultCode resultCode,Object data) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(resultCode.getErrcode());
        responseEntity.setMsg(resultCode.getMessage());
        responseEntity.setData(data);
        return responseEntity;
    }
    /**
     * 失败
     *
     * @param code    响应状态码
     * @param massage 响应错误信息
     * @return 标准响应体
     */
    public static ResponseEntity error(String code, String massage) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode(code);
        responseEntity.setMsg(massage);
        responseEntity.setData(null);
        return responseEntity;
    }

    /**
     * 失败
     *
     * @param massage 响应错误信息
     * @return 标准响应体
     */
    public static ResponseEntity error(String massage) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode("-1");
        responseEntity.setMsg(massage);
        responseEntity.setData(null);
        return responseEntity;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
