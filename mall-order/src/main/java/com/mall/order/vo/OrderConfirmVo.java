package com.mall.order.vo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public class OrderConfirmVo {
    @Getter
    @Setter
    private List<MemberAddressVo> addresses;
    @Getter
    @Setter
    private List<CartItemVo> cartItems;
    @Getter
    @Setter
    private Integer integration;
    @Getter
    @Setter
    private String orderToken;
    @Getter
    @Setter
    private Map<Long,Boolean> stocks;

    public Integer getCount() {
        Integer i = 0;
        if (cartItems != null) {
            for (CartItemVo cartItem : cartItems) {
                i += cartItem.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal(0);
        if (cartItems != null) {
            for (CartItemVo cartItem : cartItems) {
                BigDecimal price = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount()));
                sum = sum.add(price);
            }
        }
        return sum;
    }

    public BigDecimal payPrice() {
        BigDecimal sum = new BigDecimal(0);
        if (cartItems != null) {
            for (CartItemVo cartItem : cartItems) {
                BigDecimal price = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount()));
                sum = sum.add(price);
            }
        }
        return sum;
    }
}
