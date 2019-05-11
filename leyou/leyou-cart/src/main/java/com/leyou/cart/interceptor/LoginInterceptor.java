package com.leyou.cart.interceptor;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 在Handler方法执行之前执行
     * true-放行
     * false-被拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取jwt类型的token
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        // 判断token是否为null
        if (StringUtils.isBlank(token)){
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=http://www.leyou.com/cart.html");
            return false;
        }
        // 解析jwt
        UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
        // 如果解析结果为null，重定向到登录页面
        if (userInfo == null) {
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=http://www.leyou.com/cart.html");
            return false;
        }

        // 把用户信息保存到线程变量中
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    public static UserInfo get(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须清楚线程变量。因为使用的是tomcat线程池，一个请求处理完成之后，并没有销毁线程，而是把该线程还回到线程池
        THREAD_LOCAL.remove();
    }
}
