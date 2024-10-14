package com.mall.thirdparty.controller;

import com.mall.common.utils.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        System.out.println("phone:" + phone + " " + "code:" + code);
        return R.ok();
    }
}
