package com.mall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class CartItemVo {
    private Long skuId;
    private String title;
    private boolean checked;
    private BigDecimal price;
    private String image;
    private List<String> skuAttr;
    private Integer count;

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSkuAttr() {
        return skuAttr;
    }

    public void setSkuAttr(List<String> skuAttr) {
        this.skuAttr = skuAttr;
    }

    public Integer getCount() {
        return count;
    }


    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(count));
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    private BigDecimal totalPrice;
}
