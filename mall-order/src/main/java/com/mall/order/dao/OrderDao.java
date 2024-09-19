package com.mall.order.dao;

import com.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author aulen
 * @email 772049675@qq.com
 * @date 2024-09-06 01:02:53
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
