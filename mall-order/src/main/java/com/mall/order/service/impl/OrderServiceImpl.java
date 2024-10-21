package com.mall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mall.common.exception.NoStockException;
import com.mall.common.to.OrderTo;
import com.mall.common.utils.R;
import com.mall.common.vo.MemberRespVo;
import com.mall.order.constant.OrderConstant;
import com.mall.order.entity.OrderItemEntity;
import com.mall.common.enume.OrderStatusEnum;
import com.mall.order.feign.CartFeignService;
import com.mall.order.feign.MemberFeignService;
import com.mall.order.feign.ProductFeignService;
import com.mall.order.feign.WareFeignService;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderItemService;
import com.mall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.order.dao.OrderDao;
import com.mall.order.entity.OrderEntity;
import com.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    public static final ThreadLocal<SubmitOrderVo> submitOrderVoThreadLocal = new ThreadLocal<>();
    @Autowired
    private MemberFeignService memberFeignService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private CartFeignService cartFeignService;
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认
     *
     * @return
     */
    @Override
    public OrderConfirmVo toTrade() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //异步查询地址
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            orderConfirmVo.setAddresses(address);
        }, executor);
        //异步查询购物车项目
        CompletableFuture<Void> getCurrentUserItems = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<CartItemVo> currentUserItems = cartFeignService.getCurrentUserItems();
            orderConfirmVo.setCartItems(currentUserItems);
        }, executor).thenRunAsync(() -> {
            List<CartItemVo> cartItems = orderConfirmVo.getCartItems();
            if (cartItems != null) {
                List<Long> skus = cartItems.stream().map(i -> {
                    return i.getSkuId();
                }).collect(Collectors.toList());
                R r = wareFeignService.getSkusHasStock(skus);
                List<SkuHasStockVo> data = r.getData("data", new TypeReference<List<SkuHasStockVo>>() {
                });

                if (data != null) {
                    Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(
                            SkuHasStockVo::getSkuId,
                            SkuHasStockVo::getIsHas
                    ));
                    orderConfirmVo.setStocks(map);
                }
            }
        });

        //防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.ORDER_TOKEN_PREFIX + memberRespVo.getId(), token);
        orderConfirmVo.setOrderToken(token);

        Integer integration = memberRespVo.getIntegration();
        orderConfirmVo.setIntegration(integration == null ? 0 : integration);

        CompletableFuture.allOf(getAddress, getCurrentUserItems).get();

        return orderConfirmVo;
    }

    /**
     * 提交订单
     *
     * @param vo
     * @return
     */
    @Override
    @Transactional(rollbackFor = NoStockException.class)
    public SubmitRespVo submitOrder(SubmitOrderVo vo) {
        submitOrderVoThreadLocal.set(vo);
        SubmitRespVo submitRespVo = new SubmitRespVo();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(script), orderToken);
        if (execute == 1) {
            submitRespVo.setCode(1);
            return submitRespVo;
        } else {
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            //校验价格
            if (Math.abs(payPrice.subtract(payAmount).doubleValue()) < 0.01) {
                //保存订单
                saveOrder(order);
                //锁定库存
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemEntity> orderItems = order.getOrderItems();
                List<CartItemVo> collect = orderItems.stream().map(i -> {
                    CartItemVo cartItemVo = new CartItemVo();
                    cartItemVo.setSkuId(i.getSkuId());
                    cartItemVo.setCount(i.getSkuQuantity());
                    cartItemVo.setTitle(i.getSkuName());
                    return cartItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setItems(collect);
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    submitRespVo.setOrderEntity(order.getOrder());
                    submitRespVo.setCode(0);
                    //TODO 扣减积分
                    //发送rabbit
                    rabbitTemplate.convertAndSend("order-event-exchange",
                            "order-create-order",
                            order.getOrder());
                    return submitRespVo;
                } else {
                    //无库存
                    submitRespVo.setCode(3);
                    throw new NoStockException(1L);
                }
            } else {
                //价格对比不正确
                submitRespVo.setCode(2);
                return submitRespVo;
            }
        }
    }

    /**
     * 通过oderSn查询order实体
     *
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderById(String orderSn) {
        OrderEntity orderEntity = baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    /**
     * 关闭订单
     *
     * @param order
     */
    @Override
    public void orderClose(OrderEntity order) {
        OrderEntity byId = this.getById(order.getId());
        Integer status = byId.getStatus();
        if (status == OrderStatusEnum.CREATE_NEW.getCode()) {
            byId.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(byId);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(byId, orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other.#", orderTo);
        }
    }

    /**
     * 保存订单
     *
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        String timeId = IdWorker.getTimeId();
        //创建订单
        OrderEntity orderEntity = buildOrder(timeId);
        //创建订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(timeId);
        //核验价格
        computePrice(orderEntity, orderItemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(orderItemEntities);

        return orderCreateTo;
    }

    /**
     * 核验价格
     *
     * @param orderEntity
     * @param orderItemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0");
        BigDecimal coupon = new BigDecimal("0");
        BigDecimal integration = new BigDecimal("0");
        BigDecimal promotion = new BigDecimal("0");
        Integer gift = 0;
        Integer growth = 0;
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            BigDecimal realAmount = orderItemEntity.getRealAmount();
            coupon = coupon.add(orderItemEntity.getCouponAmount());
            integration = integration.add(orderItemEntity.getIntegrationAmount());
            promotion = promotion.add(orderItemEntity.getPromotionAmount());

            total = total.add(realAmount);
            gift = orderItemEntity.getGiftIntegration();
            growth = orderItemEntity.getGiftGrowth();
        }
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegration(integration.intValue());
        orderEntity.setCouponAmount(coupon);

        orderEntity.setIntegration(gift);
        orderEntity.setGrowth(growth);
        orderEntity.setDeleteStatus(0);
    }

    private OrderEntity buildOrder(String timeId) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberRespVo.getId());
        orderEntity.setOrderSn(timeId);
        SubmitOrderVo submitOrderVo = submitOrderVoThreadLocal.get();
        //获取地址和邮费
        R r = wareFeignService.getFare(submitOrderVo.getAddrId());
        FareVo fare = r.getData(new TypeReference<FareVo>() {
        });
        orderEntity.setFreightAmount(fare.getFare());
        orderEntity.setReceiverCity(fare.getAddress().getCity());
        orderEntity.setReceiverDetailAddress(fare.getAddress().getDetailAddress());
        orderEntity.setReceiverName(fare.getAddress().getName());
        orderEntity.setReceiverPhone(fare.getAddress().getPhone());
        orderEntity.setReceiverPostCode(fare.getAddress().getPostCode());
        orderEntity.setReceiverProvince(fare.getAddress().getProvince());
        orderEntity.setReceiverRegion(fare.getAddress().getRegion());

        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

    /**
     * 构建订单项
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String timeId) {
        List<CartItemVo> currentUserItems = cartFeignService.getCurrentUserItems();
        if (currentUserItems != null && currentUserItems.size() > 0) {
            List<OrderItemEntity> collect = currentUserItems.stream().map(i -> {
                OrderItemEntity orderItemEntity = buildOrderItem(i);
                orderItemEntity.setOrderSn(timeId);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 构建具体订单项
     *
     * @param item
     * @return
     */
    private OrderItemEntity buildOrderItem(CartItemVo item) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        R r = productFeignService.getSpuInfoBySkuId(item.getSkuId());
        SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {
        });
        //spu
        orderItemEntity.setSpuId(spuInfo.getId());
        orderItemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        orderItemEntity.setSpuName(spuInfo.getSpuName());
        orderItemEntity.setCategoryId(spuInfo.getCatalogId());
        //sku
        orderItemEntity.setSkuId(item.getSkuId());
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuPrice(item.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(item.getCount());
        //积分
        orderItemEntity.setGiftIntegration(item.getPrice().intValue() * item.getCount());
        orderItemEntity.setGiftIntegration(item.getPrice().intValue() * item.getCount());
        //价格
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal orignPrice = item.getPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
        BigDecimal subtract = orignPrice.subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }
}