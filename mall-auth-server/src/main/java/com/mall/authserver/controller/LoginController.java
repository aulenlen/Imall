package com.mall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.mall.authserver.feign.MemberFeignService;
import com.mall.authserver.feign.ThirdPartFeignService;
import com.mall.authserver.vo.UserLoginVo;
import com.mall.authserver.vo.UserRegisterVo;
import com.mall.common.constant.AuthConstant;
import com.mall.common.exception.BizCodeEnum;
import com.mall.common.utils.R;
import com.mall.common.vo.MemberRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * 验证码
     *
     * @param phone
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long time = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - time < 600000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5) + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthConstant.SMS_CODE_CACHE_PREFIX + phone, code, 10, TimeUnit.MINUTES);
        System.out.println(code.split("_")[0]);

        thirdPartFeignService.sendCode(phone, code.split("_")[0]);

        return R.ok();
    }

    /**
     * 注册
     *
     * @param registerVo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String register(@Valid UserRegisterVo registerVo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> collect = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
//            model.addAttribute("error", collect);
            redirectAttributes.addFlashAttribute("error", collect);
            return "redirect:http://auth.mall.com/reg.html";
        }
        //注册
        String codeVo = registerVo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            String subCode = redisCode.split("_")[0];
            if (codeVo.equals(subCode)) {
                //删除redis验证码
                redisTemplate.delete(AuthConstant.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());
                //验证码通过注册用户
                R r = memberFeignService.regist(registerVo);
                if (r.getCode() == 0) {
                    //注册成功
                    return "redirect:http://auth.mall.com/login.html";
                } else {
                    Map<String, String> collect = new HashMap<>();
                    collect.put("msg", r.getData(new TypeReference<String>() {
                    }));
                    redirectAttributes.addFlashAttribute("error", collect);
                    return "redirect:http://auth.mall.com/reg.html";
                }
            } else {
                Map<String, String> collect = new HashMap<>();
                collect.put("code", "验证码过期");
                redirectAttributes.addFlashAttribute("error", collect);
                return "redirect:http://auth.mall.com/reg.html";
            }
        } else {
            Map<String, String> collect = new HashMap<>();
            collect.put("code", "验证码过期");
            redirectAttributes.addFlashAttribute("error", collect);
            return "redirect:http://auth.mall.com/reg.html";
        }
    }

    /**
     * 用户登录
     *
     * @param vo
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthConstant.LOGIN_USER, data);
            return "redirect:http://mall.com";
        } else {
            Map<String, String> collect = new HashMap<>();
            collect.put("msg", r.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("error", collect);
            return "redirect:http://auth.mall.com/login.html";
        }
    }

    @GetMapping("/login.html")
    public String login(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            return "redirect:http://mall.com";
        } else {
            return "login";
        }
    }
}
