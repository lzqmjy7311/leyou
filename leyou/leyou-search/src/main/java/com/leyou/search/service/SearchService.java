package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResult search(SearchRequest request) {

        // 判断查询条件是否为null
        if (StringUtils.isBlank(request.getKey())) {
            return null;
        }

        // 初始化自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加查询条件
        BoolQueryBuilder basicQuery = buildBasicQueryWithFilter(request);
        queryBuilder.withQuery(basicQuery);
        // 添加分页
        Integer page = request.getPage() - 1;
        Integer size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));

        // 添加聚合
        String categoryAggName = "categoryAgg";
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 执行查询，获取分页结果集
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // 解析聚合结果集
        List<Map<String, Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));

        // 判断用户的查询结果集中的分类是否是一个
        List<Map<String, Object>> specs = null;
        if (categories.size() == 1) {
            // 根据分类id确定哪些规格参数要聚合，基于基本的查询条件聚合出可选值，因为查询条件会影响聚合出的具体内容：如：小米手机 华为手机
            specs = getSpecAggResult((Long) categories.get(0).get("id"), basicQuery);
        }

        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(), categories, brands, specs);
    }

    /**
     * 构建基本查询条件
     *
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBasicQueryWithFilter(SearchRequest request) {
        // 构建bool查询，组合查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 基本查询条件
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("all", request.getKey());

        // 把基本查询条件放入组合查询
        boolQueryBuilder.must(matchQuery);

        // 把过滤条件放入组合查询
        Map<String, String> filter = request.getFilter();
        // 遍历所有的过滤条件
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();

            if (StringUtils.equals("品牌", key)) {
                // 如果是品牌，过滤字段BrandId
                key = "brandId";
            } else if (StringUtils.equals("分类", key)) {
                // 如果过滤条件是分类，过滤字段Cid3
                key = "cid3";
            } else {
                // 如果是普通的规格参数，specs.key.keyword
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 规格参数的聚合
     *
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getSpecAggResult(Long cid, BoolQueryBuilder basicQuery) {
        // 查询要聚合的规格参数
        List<SpecParam> params = this.specClient.queryParams(null, cid, null, true);

        // 初始化自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 基于基本的查询条件聚合
        queryBuilder.withQuery(basicQuery);
        // 添加结果集过滤，不需要普通查询结果集
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));
        // 遍历要聚合的规格参数，添加聚合
        params.forEach(param -> {
            // 添加规格参数聚合，以规格参数名为聚合名称，以specs.paramName.keyword为字段名
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        // 执行查询获取聚合结果集
        AggregatedPage<Goods> paramAggPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // key-聚合名称：规格参数名，value-对应的聚合结果
        Map<String, Aggregation> paramAggMap = paramAggPage.getAggregations().asMap();

        // 初始化返回结果集
        List<Map<String, Object>> specs = new ArrayList<>();
        // 遍历所有的聚合结果集
        for (Map.Entry<String, Aggregation> aggregationEntry : paramAggMap.entrySet()) {
            // 每一个聚合都要转化成一个map，放入返回结果集中
            Map<String, Object> map = new HashMap<>();

            map.put("k", aggregationEntry.getKey());

            // 初始化options可选值集合
            List<String> options = new ArrayList<>();
            // 解析具体的聚合结果集
            StringTerms paramAgg = (StringTerms) aggregationEntry.getValue();
            paramAgg.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options", options);

            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析品牌聚合的结果集
     *
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;

        List<Long> bids = new ArrayList<>();
        terms.getBuckets().forEach(bucket -> {
            bids.add(bucket.getKeyAsNumber().longValue());
        });
        return this.brandClient.queryBrandsByIds(bids);
    }

    /**
     * 解析分类的聚合结果集
     *
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;

        List<Long> cids = new ArrayList<>();
        terms.getBuckets().forEach(bucket -> {
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        List<String> names = this.categoryClient.queryNamesByIds(cids);

        // 初始化categories集合，搜集所有的分类对象Map
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        return categories;
    }

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // 根据cid1， cid2， cid3 查询对应的名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 根据brandId查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 根据spuId查询该spu下的所有sku
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());
        // 收集sku的集合
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        // 收集价格
        List<Long> prices = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());

            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            map.put("title", sku.getTitle());
            skuMapList.add(map);
        });

        // 根据cid3查询搜索的规格参数
        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), null, true);
        // 根据spuid查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        // 通用的规格参数Map<paramId, paramValue>
        Map<String, Object> genericMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // 特殊的规格参数Map<paramId, ParamValue>
        Map<String, List<Object>> specialMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });

        // 收集规格参数：Map<param.getName, 参数值>
        Map<String, Object> specs = new HashMap<>();
        params.forEach(param -> {
            if (param.getGeneric()) {
                // 如果是通用字段
                String value = genericMap.get(param.getId().toString()).toString();
                // 判断是否是数值类型的数据
                if (param.getNumeric()) {
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            } else {
                // 特殊的规格字段
                List<Object> value = specialMap.get(param.getId().toString());
                specs.put(param.getName(), value);
            }
        });

        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        // 设置搜索字段：spu的title 分类名称 品牌名称
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + brand.getName());
        // 设置价格集合，搜索sku中所有价格List<Long>
        goods.setPrice(prices);
        // 设置skus，搜集所有的List<sku>，序列化为字符串
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        // 设置规格参数，Map<参数名，参数值>
        goods.setSpecs(specs);

        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }


    public void save(Long spuId) throws IOException {
        // 根据spuId查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void delete(Long spuId) {
        this.goodsRepository.deleteById(spuId);
    }
}
