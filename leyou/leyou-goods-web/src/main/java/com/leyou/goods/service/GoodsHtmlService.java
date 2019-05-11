package com.leyou.goods.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

@Service
public class GoodsHtmlService {

    @Autowired
    private TemplateEngine engine;

    @Autowired
    private GoodsService goodsService;

    public void getHtml(Long spuId){

        // 初始化上下文对象
        Context context = new Context();
        // 获取数据模型
        Map<String, Object> map = this.goodsService.loadData(spuId);
        // 把数据模型放入上下文对象
        context.setVariables(map);

        // 初始化本地文件流
        PrintWriter printWriter = null;
        try {
            File file = new File("C:\\hm44\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
            printWriter = new PrintWriter(file);
            // 生成静态化页面
            this.engine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }

    }

    public void delete(Long spuId) {
        File file = new File("C:\\hm44\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
        file.deleteOnExit();
    }
}
