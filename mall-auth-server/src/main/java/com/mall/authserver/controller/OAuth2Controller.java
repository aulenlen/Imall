package com.mall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.mall.authserver.feign.MemberFeignService;
import com.mall.common.vo.MemberRespVo;
import com.mall.common.to.SocialUserTo;
import com.mall.common.utils.HttpUtils;
import com.mall.common.utils.R;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

@Controller
public class OAuth2Controller {
    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * OAuth2 gitee认证
     *
     * @param code
     * @return
     * @throws Exception
     */
    @GetMapping("/oauth2/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession session) throws Exception {
        HashMap<String, String> header = new HashMap<>();
        HashMap<String, String> query = new HashMap<>();
        HashMap<String, String> map = new HashMap<>();

        map.put("client_id", "2ab9cb8b0835aeaf1379429758fb8db062b1f38a33be1871b19d33a0aaa35a9d");
        map.put("client_secret", "5b50b1cdbcfa47ae29dfc6d174622176d0930f85948f7056e1b2aa1599cac806");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.mall.com/oauth2/gitee/success");
        map.put("code", code);
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "POST", header, query, map);

        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            if (socialUserTo != null && StringUtils.hasLength(socialUserTo.getAccessToken())) {
                query.clear();
                query.put("access_token", socialUserTo.getAccessToken());
                HttpResponse responseGet = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "GET", header, query);
                if (responseGet.getStatusLine().getStatusCode() == 200) {
                    json = EntityUtils.toString(responseGet.getEntity());
                    SocialUserTo user = JSON.parseObject(json, SocialUserTo.class);
                    BeanUtils.copyProperties(user, socialUserTo, "accessToken", "tokenType", "expiresIn", "refreshToken", "scope", "createdAt");
                    R r = memberFeignService.authLogin(socialUserTo);
                    if (r.getCode() == 0) {
                        json = JSON.toJSONString(r.get("data"));
                        MemberRespVo vo = JSON.parseObject(json, MemberRespVo.class);
                        session.setAttribute("loginUser",vo);
                        return "redirect:http://mall.com";
                    } else {
                        return "redirect:http://auth.mall.com/login.html";
                    }
                } else {
                    return "redirect:http://auth.mall.com/login.html";
                }
            } else {
                return "redirect:http://auth.mall.com/login.html";
            }
        } else {
            return "redirect:http://auth.mall.com/login.html";
        }
    }
}
