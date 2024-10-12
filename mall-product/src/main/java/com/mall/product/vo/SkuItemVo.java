package com.mall.product.vo;

import com.mall.product.entity.SkuImagesEntity;
import com.mall.product.entity.SkuInfoEntity;
import com.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //sku信息 pms_sku_info
    private SkuInfoEntity info;
    //是否有库存
    boolean hasStock = true;

    //sku图片 pms_sku_images
    private List<SkuImagesEntity> images;

    //spu销售组合
    private List<SkuItemSaleAttrVo> saleAttr;

    //spu介绍
    private SpuInfoDescEntity desp;

    //spu参数组合
    private List<SpuItemGroupAttrVo> groupAttrs;

}
