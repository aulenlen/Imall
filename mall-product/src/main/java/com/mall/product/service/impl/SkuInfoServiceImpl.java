package com.mall.product.service.impl;

import com.mall.product.entity.SkuImagesEntity;
import com.mall.product.entity.SpuInfoDescEntity;
import com.mall.product.entity.SpuInfoEntity;
import com.mall.product.service.*;
import com.mall.product.vo.SkuItemSaleAttrVo;
import com.mall.product.vo.SkuItemVo;
import com.mall.product.vo.SpuItemGroupAttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.SkuInfoDao;
import com.mall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private SkuSaleAttrValueService saleAttrValueService;
    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        String key = (String) params.get("key");
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq("sku_id", key).or().like("sku_name", key);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            wrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(min)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(min);
                if (bigDecimal.compareTo(new BigDecimal("0")) == 1) {
                    wrapper.le("price", max);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> listBySpuId(Long spuId) {

        List<SkuInfoEntity> skuInfoEntities =
                this.baseMapper.selectList(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return skuInfoEntities;
    }

    /**
     * 获取商品详情页信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();
        //sku基本信息
        CompletableFuture<SkuInfoEntity> info = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfoEntity = baseMapper.selectById(skuId);
            skuItemVo.setInfo(skuInfoEntity);
            return skuInfoEntity;
        }, executor);

        //销售组合
        CompletableFuture<Void> saleAttr = info.thenAcceptAsync((res) -> {
            List<SkuItemSaleAttrVo> skuItemSaleAttrVo = saleAttrValueService.getSaleAttrValuesBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(skuItemSaleAttrVo);
        }, executor);

        //spu介绍
        CompletableFuture<Void> spuDes = info.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesp(spuInfoDesc);
        }, executor);

        //spu规格属性
        CompletableFuture<Void> baseSpu = info.thenAcceptAsync((res) -> {
            List<SpuItemGroupAttrVo> groupAttrVos =
                    attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(groupAttrVos);
        }, executor);

        //sku图片
        CompletableFuture<Void> skuImages = CompletableFuture.runAsync(() -> {
            skuItemVo.setImages(skuImagesService.getSkuImagesBySkuId(skuId));
        }, executor);

        CompletableFuture.allOf(saleAttr,spuDes,baseSpu,skuImages).get();

        return skuItemVo;
    }

}