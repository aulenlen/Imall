package com.mall.order.web;

import com.mall.order.service.OrderService;
import com.mall.order.vo.OrderConfirmVo;
import com.mall.order.vo.SubmitOrderVo;
import com.mall.order.vo.SubmitRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;


@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest servletRequest) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.toTrade();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(SubmitOrderVo vo, Model model, RedirectAttributes redirectAttributes) {
        SubmitRespVo submitRespVo = orderService.submitOrder(vo);
        if (submitRespVo.getCode() == 0) {
            model.addAttribute("submitRespVo", submitRespVo);
            return "pay";
        } else {
            String msg = "下单失败；";
            switch (submitRespVo.getCode()) {
                case 1:
                    msg += "订单信息过期，请刷新再次提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化，请确认后再次提交";
                    break;
                default:
                    msg += "库存锁定失败，商品库存不足";
            }
            redirectAttributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.mall.com/toTrade";
        }
    }
}
