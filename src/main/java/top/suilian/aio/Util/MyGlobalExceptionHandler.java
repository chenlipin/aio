/*
 * Copyright (C) 1997-2020 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import top.suilian.aio.vo.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @date ：Created in 2020/9/23 0023 下午 4:30
 * @description：全局异常处理
 * @version: 1.1.1$
 */
@ControllerAdvice
public class MyGlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyGlobalExceptionHandler.class);

    /**
     * create by: huabo.yang
     * description: 处理绑定错误
     * create time: 2020/9/23 0023 下午 4:58
     *
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity validationBodyException(MethodArgumentNotValidException e) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode("-1");
        //json.setMsg(e.getMessage());
        responseEntity.setMsg(Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage());
        return responseEntity;
    }


    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity runtimeException(RuntimeException e, HttpServletRequest request) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setMsg(e.getMessage());
        responseEntity.setErrcode("-1");
        logger.info("exception url is {},detail :", request.getRequestURL(), e);
        return responseEntity;
    }

    /**
     * 参数校验异常处理
     *
     * @param e 捕捉BindException异常类型
     * @return ResponseEntity
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResponseEntity validationBodyException(BindException e) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setErrcode("-1");
        //json.setMsg(e.getMessage());
        responseEntity.setMsg(Objects.requireNonNull(e.getFieldError().getDefaultMessage()));
        return responseEntity;
    }


}
