package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties properties;

    public String accredit(String username, String password) throws Exception {

        // 查询用户信息
        User user = this.userClient.queryUser(username, password);

        // 判断用户信息是否为空
        if (user == null) {
            return null;
        }

        // 初始化载荷：userInfo
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername());
        // 生成jwt
        String token = JwtUtils.generateToken(userInfo, this.properties.getPrivateKey(), this.properties.getExpire());

        return token;
    }
}
