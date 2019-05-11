package com.leyou.search.test;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticSearchTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GoodsRepository goodsRepository;

    @Test
    public void testImport(){
        this.elasticsearchTemplate.createIndex(Goods.class);
        this.elasticsearchTemplate.putMapping(Goods.class);

        Integer page = 1;
        Integer rows = 100;
        do {
            // 分批查询spu
            PageResult<SpuBo> result = this.goodsClient.querySpuBoByPage(null, null, page, rows);
            // 定义集合，接收构造好的goods对象
            List<Goods> goodsList = new ArrayList<>();
            // 遍历当前页中的所有spu，构造成goods对象
            result.getItems().forEach(spuBo -> {
                try {
                    Goods goods = this.searchService.buildGoods(spuBo);
                    goodsList.add(goods);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            this.goodsRepository.saveAll(goodsList);

            // 重置rows
            rows = result.getItems().size();
            // 导入下一页数据
            page++;

        } while (rows == 100);
    }
}
