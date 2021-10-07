package top.suilian.aio.filter;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


//@WebFilter(filterName = "aio", urlPatterns = "/service/*")
public class UrlFilter implements Filter {
    private static Logger logger = Logger.getLogger(UrlFilter.class);
    @Autowired
    HttpServletRequest request;
    @Autowired
    HttpServletResponse response;
    private static final String KEY_ACCESS_CODE = "d25925dd1fb35bd22cf0e5eba0fe7ffc";
    private static final String KEY_SALT = "~AIO~";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("创建过滤器");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String accessCode = request.getHeader("KEY_ACCESS_CODE");
        String token = request.getHeader("KEY_TOKEN");
        if (!"".equals(accessCode) && !"".equals(token) && token != null && accessCode != null) {
            if (accessCode.equals(DigestUtils.md5Hex(KEY_ACCESS_CODE + KEY_SALT + token))) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                Map<String, Object> rt = new HashMap<String, Object>();
                rt.put("status", "203");
                rt.put("msg", "Verification exception");
                renderJson(rt);
            }
        }
    }

    @Override
    public void destroy() {
        System.out.println("销毁过滤器");
    }

    private boolean renderJson(Map<String, Object> map) {
        String json = JSONObject.toJSONString(map);
        try {
            IOUtils.write(json, response.getWriter());
        } catch (IOException e) {
            logger.error("拦截器响应结果异常", e);
        }
        return false;
    }
}
