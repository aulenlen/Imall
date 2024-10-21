package com.mall.common.exception;

public class NoStockException extends RuntimeException {
    private Long skuId;

    public NoStockException(Long skuId) {
        super(skuId + "无库存");
    }
}
