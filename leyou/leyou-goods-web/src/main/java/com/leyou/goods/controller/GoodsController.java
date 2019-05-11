package com.leyou.goods.controller;

import com.leyou.goods.service.GoodsHtmlService;
import com.leyou.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping("item")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsHtmlService goodsHtmlService;

    /**
     * controller方法返回值是字符串的情况下：默认是视图名称
     * @param spuId
     * @param model
     * @return
     */
    @GetMapping("{spuId}.html")
    public String toItem(@PathVariable("spuId")Long spuId, Model model){

        Map<String, Object> map = this.goodsService.loadData(spuId);

        model.addAllAttributes(map);

        this.goodsHtmlService.getHtml(spuId);

        return "item";
    }
}
