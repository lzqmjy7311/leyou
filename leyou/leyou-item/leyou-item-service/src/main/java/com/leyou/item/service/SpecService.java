package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    /**
     * 根据分类id查询分组
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        return this.groupMapper.select(group);
    }

    /**
     * 根据组id查询规格参数
     * @param gid
     * @return
     */
    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching) {

        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setGeneric(generic);
        param.setSearching(searching);
        return this.paramMapper.select(param);
    }

    public List<SpecGroup> queryGroupWithParamByCid(Long cid) {
        List<SpecGroup> groups = this.queryGroupsByCid(cid);
        groups.forEach(group->{
            List<SpecParam> params = this.queryParams(group.getId(), null, null, null);
            group.setParams(params);
        });
        return groups;
    }
}