package com.leyou.goods.listener;

import com.leyou.goods.service.GoodsHtmlService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsListener {

    @Autowired
    private GoodsHtmlService htmlService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.HTML.SAVE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.insert"}
            )
    )
    public void save(Long spuId) {
        if (spuId == null){
            return;
        }
        this.htmlService.getHtml(spuId);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.HTML.UPDATE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.update"}
            )
    )
    public void update(Long spuId) {
        if (spuId == null){
            return;
        }
        this.htmlService.getHtml(spuId);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.HTML.DELETE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.delete"}
            )
    )
    public void delete(Long spuId) {
        if (spuId == null){
            return;
        }
        this.htmlService.delete(spuId);
    }
}
