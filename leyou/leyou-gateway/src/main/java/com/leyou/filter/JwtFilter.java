package com.leyou.filter;

import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.config.FilterProperties;
import com.leyou.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
@Component
public class JwtFilter extends ZuulFilter {

    @Autowired
    private JwtProperties properties;

    @Autowired
    private FilterProperties filterProperties;

    /**
     * 过滤器的类型：pre post route error
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 执行顺序，返回值越小优先级越高
     * @return
     */
    @Override
    public int filterOrder() {
        return 10;
    }

    /**
     * 是否执行run方法，true-执行
     * false-不执行
     * @return
     */
    @Override
    public boolean shouldFilter() {
        // 获取zuul的上下文对象
        RequestContext currentContext = RequestContext.getCurrentContext();
        // 获取request对象
        HttpServletRequest request = currentContext.getRequest();
        // 获取请求路径
        String url = request.getRequestURL().toString();
        for (String path : this.filterProperties.getAllowPaths()) {
            if (StringUtils.contains(url, path)){
                return false;
            }
        }
        return true;
    }

    /**
     * 过滤器的拦截逻辑
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {

        // 获取zuul的上下文对象
        RequestContext currentContext = RequestContext.getCurrentContext();
        // 获取request对象
        HttpServletRequest request = currentContext.getRequest();
        // 通过cookie的工具类，获取token字符串
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());

        try {
            // 解析jwt的token
            JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            // 不转发请求
            currentContext.setSendZuulResponse(false);
            // 直接响应登陆未认证
            currentContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        }

        // 什么都不干，放行
        return null;
    }
}
