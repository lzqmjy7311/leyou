package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand>,SelectByIdListMapper<Brand, Long> {

    @Insert("insert into tb_category_brand(category_id, brand_id) values (#{cid}, #{bid})")
    public void insertCategoryAndBrand(@Param("cid") Long cid, @Param("bid") Long bid);

    @Select("select a.* from tb_brand a INNER JOIN tb_category_brand b on a.id=b.brand_id where b.category_id=#{cid}")
    public List<Brand> selectBrandByCid(Long cid);
}
