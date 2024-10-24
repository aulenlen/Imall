package com.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class CartItemVo {
    private Long skuId;
    private String title;
    private boolean checked;
    private BigDecimal price;
    private String image;
    private List<String> skuAttr;
    private Integer count;
    private BigDecimal totalPrice;
    private boolean hasStock;
    private BigDecimal weight;
    private String skuPic;
}
