package com.mall.order.vo;

import com.mall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitRespVo {
    private OrderEntity orderEntity;
    private Integer code;
}
