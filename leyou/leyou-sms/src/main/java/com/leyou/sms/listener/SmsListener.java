package com.leyou.sms.listener;

import com.aliyuncs.exceptions.ClientException;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

@Component
public class SmsListener {

    @Autowired
    private SmsUtils smsUtils;

    @Autowired
    private SmsProperties properties;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU.SMS.QUEUE", durable = "true"),
            exchange = @Exchange(value = "LEYOU.SMS.EXCHANGE", ignoreDeclarationExceptions = "true"),
            key = {"sms.verify"}
    ))
    public void send(Map<String, String> msg) throws ClientException {
        // 判断消息是否为null
        if (CollectionUtils.isEmpty(msg)){
            return;
        }
        // 解析消息内容
        String phone = msg.get("phone");
        String code = msg.get("code");
        // 判断消息的内容是否为null
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)){
            return;
        }
        // 发送短信
        this.smsUtils.sendSms(phone, code, this.properties.getSignName(), this.properties.getVerifyCodeTemplate());
    }
}
