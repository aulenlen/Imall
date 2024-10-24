package com.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.to.SecOrderTo;
import com.mall.common.utils.PageUtils;
import com.mall.order.entity.OrderEntity;
import com.mall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author aulen
 * @email 772049675@qq.com
 * @date 2024-09-06 01:02:53
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo toTrade() throws ExecutionException, InterruptedException;

    SubmitRespVo submitOrder(SubmitOrderVo vo);

    OrderEntity getOrderById(String orderSn);

    void orderClose(OrderEntity order);

    PayVo getPayVo(String orderSn);

    PageUtils queryListWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void createSeckillOrder(SecOrderTo order);
}

