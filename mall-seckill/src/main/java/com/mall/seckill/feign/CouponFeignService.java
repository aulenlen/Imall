package com.mall.seckill.feign;

import com.mall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mall-coupon")
public interface CouponFeignService {
    @GetMapping("/coupon/seckillsession/late3DaysSession")
    R getLate3DaysSession();
}
