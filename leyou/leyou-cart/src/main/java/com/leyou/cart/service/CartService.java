package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix = "user:cart:";

    public void saveCart(Cart cart) {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        // 组装hash类型key
        String key = key_prefix + userInfo.getId();
        // 查询redis中有没有
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(key);

        // 记录页面传递过来的数量
        Integer num = cart.getNum();

        // 判断当前的记录，在不在
        if (hashOperations.hasKey(cart.getSkuId().toString())){
            // 在，更新数量
            String jsonData = hashOperations.get(cart.getSkuId().toString()).toString();
            // 反序列化
            cart = JsonUtils.parse(jsonData, Cart.class);
            // 更新数量
            cart.setNum(cart.getNum() + num);
        } else {
            // 不在，新增记录
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setUserId(userInfo.getId());
            cart.setPrice(sku.getPrice());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages())[0]);
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setTitle(sku.getTitle());
        }
        // 执行更新操作
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));
    }

    public List<Cart> queryCart() {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        // 组装hash类型key
        String key = key_prefix + userInfo.getId();

        if (!redisTemplate.hasKey(key)) {
            return null;
        }
        // 查询redis中有没有
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(key);
        // 获取values
        List<Object> jsonData = hashOperations.values();
        // 判断购物车记录是否为空
        if (CollectionUtils.isEmpty(jsonData)){
            return null;
        }
        // 解析购物车json集合，转化成cart集合
        List<Cart> carts = new ArrayList<>();
        jsonData.forEach(cartJson -> {
            Cart cart = JsonUtils.parse(cartJson.toString(), Cart.class);
            carts.add(cart);
        });
        return carts;
    }

    public void updateCart(Cart cart) {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        // 组装hash类型key
        String key = key_prefix + userInfo.getId();

        Integer num = cart.getNum();

        // 查询redis中有没有
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(key);

        // 获取cartjson对象
        String jsonData = hashOperations.get(cart.getSkuId().toString()).toString();
        // 反序列化
        cart = JsonUtils.parse(jsonData, Cart.class);
        // 修改数量
        cart.setNum(num);

        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));
    }

    public void deleteCart(Long skuId) {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        // 组装hash类型key
        String key = key_prefix + userInfo.getId();

        // 查询redis中有没有
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(key);

        if (hashOperations.hasKey(skuId.toString()))
            hashOperations.delete(skuId.toString());
    }
}
