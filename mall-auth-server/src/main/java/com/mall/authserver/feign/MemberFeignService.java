package com.mall.authserver.feign;

import com.mall.authserver.vo.UserLoginVo;
import com.mall.authserver.vo.UserRegisterVo;
import com.mall.common.to.SocialUserTo;
import com.mall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegisterVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/auth2/login")
    R authLogin(@RequestBody SocialUserTo vo);
}
