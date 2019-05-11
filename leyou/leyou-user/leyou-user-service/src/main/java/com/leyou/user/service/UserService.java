package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:";

    private static final Long TIMEOUT = 5l;

    /**
     * 校验用户名或者手机号是否可用
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUser(String data, Integer type) {

        User record = new User();
        if (type == 1) {
            record.setUsername(data);
        } else if (type == 2) {
            record.setPhone(data);
        } else {
            return null;
        }
        return this.userMapper.selectCount(record) == 0;
    }

    public void sendCode(String phone) {
        // 判断手机号是否为null
        if (StringUtils.isBlank(phone)) {
            return;
        }

        // 生成验证码
        String code = NumberUtils.generateCode(6);

        // 发送消息给sms
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        this.amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE", "sms.verify", msg);

        // 保存验证码到redis中
        this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, TIMEOUT, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {

        // 1.校验验证码
        String cacheCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(code, cacheCode)) {
            return;
        }

        // 2.生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        // 3.加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        // 4.新增用户
        user.setCreated(new Date());
        user.setId(null);
        Boolean flag = this.userMapper.insertSelective(user) == 1;

        // 5.删除redis中的验证码
        if (flag) {
            this.redisTemplate.delete(KEY_PREFIX + user.getPhone());
        }
    }

    public User queryUser(String username, String password) {
        // 根据用户名查询用户信息
        User record = new User();
        record.setUsername(username);
        User u = this.userMapper.selectOne(record);

        // 判断用户是否为null
        if (u == null) {
            return null;
        }

        // 对用户输入的密码进行加密（user）
        password = CodecUtils.md5Hex(password, u.getSalt());

        // 和u里面的password进行比较
        if (StringUtils.equals(password, u.getPassword())) {
            return u;
        }

        return null;
    }
}
