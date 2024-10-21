package com.mall.order.interceptor;

import com.mall.common.constant.AuthConstant;
import com.mall.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        if (match) {
            return true;
        }

        MemberRespVo memberRespVo = (MemberRespVo) request.getSession().getAttribute(AuthConstant.LOGIN_USER);
        if (memberRespVo != null) {
            loginUser.set(memberRespVo);
            return true;
        } else {
            request.setAttribute("msg", "请登录");
            response.sendRedirect("http://auth.mall.com/login.html");
            return false;
        }
    }
}
