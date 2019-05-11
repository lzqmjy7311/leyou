package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    public Map<String, Object> loadData(Long spuId){

        Map<String, Object> modelMap = new HashMap<>();

        // 查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);

        // 查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);

        // 查询brand
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询分类名称
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }

        // 查询spu下的所有sku
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spuId);

        // 查询规格参数组及组下的规格参数
        List<SpecGroup> groups = this.specClient.queryGroupWithParamByCid(spu.getCid3());

        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), false, null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });

        modelMap.put("spu", spu);
        modelMap.put("spuDetail", spuDetail);
        modelMap.put("brand", brand);
        modelMap.put("categories", categories);
        modelMap.put("skus", skus);
        modelMap.put("groups", groups);
        // 获取特殊的规格参数：Map<paramId, paramName>
        modelMap.put("paramMap", paramMap);

        return modelMap;
    }

}
