package com.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.mall.common.to.es.SkuEsModel;
import com.mall.search.config.MallElasticsearchConfig;
import com.mall.search.constant.EsConstant;
import com.mall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private RestHighLevelClient client;

    /**
     * 商品上架，将商品信息写入es
     * @param skuEsModels
     * @return
     * @throws IOException
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();

        for (SkuEsModel skuEsModel : skuEsModels) {
            //es映射
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());

            String jsonString = JSON.toJSONString(skuEsModel);
            indexRequest.source(jsonString, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        BulkResponse bulk = client.bulk(bulkRequest, MallElasticsearchConfig.COMMON_OPTIONS);

        boolean b = bulk.hasFailures();

        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架成功:{}", collect);
        return !b;
    }
}
