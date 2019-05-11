package com.leyou.search.listener;

import com.leyou.search.service.SearchService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoodsListener {

    @Autowired
    private SearchService searchService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.GOODS.SAVE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.insert"}
            )
    )
    public void save(Long spuId) throws IOException {
        if (spuId == null) {
            return;
        }
        this.searchService.save(spuId);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.GOODS.UPDATE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.update"}
            )
    )
    public void update(Long spuId) throws IOException {
        if (spuId == null){
            return;
        }
        this.searchService.save(spuId);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "LEYOU.ITEM.GOODS.DELETE", durable = "true"),
                    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
                    key = {"item.delete"}
            )
    )
    public void delete(Long spuId){
        if (spuId == null){
            return;
        }
        this.searchService.delete(spuId);
    }
}
