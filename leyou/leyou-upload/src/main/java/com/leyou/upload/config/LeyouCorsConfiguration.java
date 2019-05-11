package com.leyou.upload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class LeyouCorsConfiguration {

    /**
     * 统一配置跨域
     * @return
     */
    @Bean
    public CorsFilter corsFilter(){
        // 初始化跨域配置对象
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许跨域访问的域名，可以写*：代表所有域名，但是不能携带cookie
        configuration.addAllowedOrigin("http://manage.leyou.com");
        // 是否允许携带cookie，true-允许携带cookie
        configuration.setAllowCredentials(true);
        // 允许那些方法跨域访问
        configuration.addAllowedMethod("*");
        // 允许跨域访问携带的头信息
        configuration.addAllowedHeader("*");

        // 初始化配置源对象
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        // 所有路径适用改配置
        configurationSource.registerCorsConfiguration("/**", configuration);

        return new CorsFilter(configurationSource);
    }
}
