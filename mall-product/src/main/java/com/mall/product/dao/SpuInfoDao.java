package com.mall.product.dao;

import com.mall.common.constant.ProductConstant;
import com.mall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 *
 * @author aulen
 * @email 772039675@qq.com
 * @date 2024-09-05 22:55:03
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("statusEnum") int statusEnum);

}
