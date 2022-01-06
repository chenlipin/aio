
package top.suilian.aio.vo;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;


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

    public String getErrcode() {
        return errcode;
    }

    public void setErrcode(String errcode) {
        this.errcode = errcode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
