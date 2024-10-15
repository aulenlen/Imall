package com.mall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.mall.cart.feign.ProductFeignService;
import com.mall.cart.interceptor.CartInterceptor;
import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartItemVo;
import com.mall.cart.vo.CartVo;
import com.mall.cart.vo.SkuInfoEntityVo;
import com.mall.cart.vo.UserInfoTo;
import com.mall.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    private static final String CART_PREFIX = "mall:cart:";

    /**
     * 添加到购物车
     *
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = new CartItemVo();
        if (StringUtils.isEmpty(s)) {
            CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
                R r = productFeignService.info(skuId);
                SkuInfoEntityVo skuInfoEntityVo = r.getData("skuInfo", new TypeReference<SkuInfoEntityVo>() {
                });
                cartItemVo.setChecked(true);
                cartItemVo.setImage(skuInfoEntityVo.getSkuDefaultImg());
                cartItemVo.setSkuId(skuInfoEntityVo.getSkuId());
                cartItemVo.setTitle(skuInfoEntityVo.getSkuTitle());
                cartItemVo.setPrice(skuInfoEntityVo.getPrice());
                cartItemVo.setCount(num);
            }, threadPoolExecutor);

            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttr(skuSaleAttrValues);
            }, threadPoolExecutor);

            CompletableFuture.allOf(getSkuInfo, getSkuSaleAttrValues).get();
            String cartItemVoJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartItemVoJson);
            return cartItemVo;
        } else {
            CartItemVo cartItemVoUpdate = JSON.parseObject(s, CartItemVo.class);
            cartItemVoUpdate.setCount(cartItemVoUpdate.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVoUpdate));
            return cartItemVoUpdate;
        }
    }

    /**
     * 从redis获取购物车项
     *
     * @param skuId
     * @return
     */
    @Override
    public CartItemVo getCartItem(Long skuId) {
        String s = (String) getCartOps().get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(s, CartItemVo.class);
        return cartItemVo;
    }

    /**
     * 获取购物车
     *
     * @return
     */
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        if (userInfoTo.getUserId() != null) {
            //登录
            String key = CART_PREFIX + userInfoTo.getUserId();
            List<CartItemVo> cartItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
            if (cartItems != null && cartItems.size() > 0) {
                for (CartItemVo cartItem : cartItems) {
                    //合并购物车
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
                //清空购物车
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }

            List<CartItemVo> allCart = getCartItems(key);
            cartVo.setCartItemList(allCart);
        } else {
            //未登录
            String key = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> cartItems = getCartItems(key);
            cartVo.setCartItemList(cartItems);
        }

        return cartVo;
    }

    /**
     * 更改购物车项选中状态
     *
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Long check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setChecked(check == 1 ? true : false);
        String jsonString = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonString);
    }

    /**
     * 增减购物车商品数量
     *
     * @param skuId
     * @param num
     */
    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String jsonString = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonString);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 使用用户id或者临时key绑定redis操作
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //登录
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //未登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    /**
     * 获取redis购物车项
     *
     * @param key
     * @return
     */
    private List<CartItemVo> getCartItems(String key) {
        List<Object> values = redisTemplate.boundHashOps(key).values();
        List<CartItemVo> cartItemVoList = null;
        if (values.size() > 0 && values != null) {
            cartItemVoList = values.stream().map((obj) -> {
                String s = (String) obj;
                CartItemVo cartItemVo = JSON.parseObject(s, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
        }
        return cartItemVoList;
    }

    /**
     * 清空购物车
     *
     * @param key
     */
    private void clearCart(String key) {
        redisTemplate.delete(key);
    }
}
