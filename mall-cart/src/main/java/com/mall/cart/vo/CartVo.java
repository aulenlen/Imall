package com.mall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CartVo {
    private List<CartItemVo> cartItemList;
    private Integer countNum;
    private Integer countType;

    private BigDecimal totalAmount;
    private BigDecimal reduce = new BigDecimal("0.00");

    public List<CartItemVo> getCartItemList() {
        return cartItemList;
    }

    public void setCartItemList(List<CartItemVo> cartItemList) {
        this.cartItemList = cartItemList;
    }

    public Integer getCountNum() {
        countNum = 0;
        if (cartItemList != null && cartItemList.size() > 0) {
            for (CartItemVo item : cartItemList) {
                countNum += item.getCount();
            }
        }
        return countNum;
    }

    public Integer getCountType() {
        countType = 0;
        if (cartItemList != null && cartItemList.size() > 0) {
            for (CartItemVo item : cartItemList) {
                countType += 1;
            }
        }
        return countType;
    }

    public void setCountType(Integer countType) {
        this.countType = countType;
    }

    public BigDecimal getTotalAmount() {
        totalAmount = new BigDecimal(0);

        if (cartItemList != null && cartItemList.size() > 0) {
            for (CartItemVo item : cartItemList) {
                if(item.isChecked() == true){
                    BigDecimal totalPrice = item.getTotalPrice();
                    totalAmount = totalAmount.add(totalPrice);
                }
            }
        }
        totalAmount.subtract(reduce);
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
