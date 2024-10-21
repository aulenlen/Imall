package com.mall.cart.controller;

import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartItemVo;
import com.mall.cart.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    /**
     * 获取当前登录用户选择的购物车项目
     * @return
     */
    @GetMapping("/currentUserItems")
    @ResponseBody
    public List<CartItemVo> getCurrentUserItems() {
        return cartService.getCurrentUserItems();
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.mall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.countItem(skuId, num);
        return "redirect:http://cart.mall.com/cart.html";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Long check) {
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.mall.com/cart.html";
    }

    @GetMapping("/cart.html")
    public String cartPage(Model model) throws ExecutionException, InterruptedException {
        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    /**
     * 添加到购物车 浏览器地址刷新不会添加数量
     *
     * @param skuId
     * @param num
     * @param ra
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.mall.com/addToCartSuccessPage.html";
    }

    /**
     * 二次跳转 防止刷新重复添加
     *
     * @param skuId
     * @param model
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/addToCartSuccessPage.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,
                                       Model model) throws ExecutionException, InterruptedException {
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItemVo);
        return "success";
    }
}
