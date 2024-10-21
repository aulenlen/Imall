package com.mall.order.feign;

import com.mall.order.vo.CartItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("mall-cart")
public interface CartFeignService {
    @GetMapping("/currentUserItems")
    List<CartItemVo> getCurrentUserItems();
}
