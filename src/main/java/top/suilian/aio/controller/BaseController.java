package top.suilian.aio.controller;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component
public class BaseController {
    @Resource
    protected HttpServletRequest request;



    //endregion


}
