package com.mall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mall.common.to.SecOrderTo;
import com.mall.common.utils.R;
import com.mall.common.vo.MemberRespVo;
import com.mall.seckill.feign.CouponFeignService;
import com.mall.seckill.feign.ProductFeignService;
import com.mall.seckill.interceptor.LoginUserInterceptor;
import com.mall.seckill.service.SeckillService;
import com.mall.seckill.to.SeckillSkuRedisTo;
import com.mall.seckill.vo.SeckillSessionVo;
import com.mall.seckill.vo.SeckillSkuRelationVo;
import com.mall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    private static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    /**
     * 上架3天内的秒杀商品
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        R r = couponFeignService.getLate3DaysSession();
        if (r.getCode() == 0) {
            List<SeckillSessionVo> sessions = r.getData(new TypeReference<List<SeckillSessionVo>>() {
            });
            if (sessions != null && sessions.size() > 0) {
                saveSessionInfo(sessions);
                saveSessionSkus(sessions);
            }
        }
    }

    /**
     * 获取当前时间段的秒杀商品
     *
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");

        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);

            if (time >= start && time <= end) {
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

                List<String> objects = ops.multiGet(range);
                if (objects != null && objects.size() > 0) {
                    List<SeckillSkuRedisTo> collect = objects.stream().map(i -> {
                        SeckillSkuRedisTo skuRedisTo = JSON.parseObject(i.toString(), SeckillSkuRedisTo.class);
                        return skuRedisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }

        }

        return null;
    }

    /**
     * 获取skuId对应的秒杀信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = ops.keys();
        String reg = "\\d_" + skuId;
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                if (Pattern.matches(reg, key)) {
                    String json = ops.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    long time = new Date().getTime();
                    if (time >= skuRedisTo.getStartTime() && time <= skuRedisTo.getEndTime()) {

                    } else {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    /**
     * 购买秒杀
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String seckill(String killId, String key, Integer num) {
        MemberRespVo user = LoginUserInterceptor.loginUser.get();
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = ops.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

            Long startTime = skuRedisTo.getStartTime();
            Long endTime = skuRedisTo.getEndTime();
            long now = new Date().getTime();
            if (startTime <= now && endTime >= now) {

                String randomCode = skuRedisTo.getRandomCode();
                String skuId = skuRedisTo.getPromotionSessionId() + "_" + skuRedisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    if (num.intValue() <= skuRedisTo.getSeckillLimit().intValue()) {
                        String id = user.getId().toString() + "_" + skuRedisTo.getPromotionSessionId().toString() + "_" + skuId.toString();
                        Long expires = endTime - now;
                        Boolean absent = redisTemplate.opsForValue().setIfAbsent(id, num.toString(), expires, TimeUnit.MICROSECONDS);
                        if (absent) {
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            try {
                                boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MICROSECONDS);
                                if (b) {
                                    String timeId = IdWorker.getTimeId();

                                    SecOrderTo secOrderTo = new SecOrderTo();
                                    secOrderTo.setOrderSn(timeId);
                                    secOrderTo.setNum(num);
                                    secOrderTo.setSkuId(skuRedisTo.getSkuId());
                                    secOrderTo.setPromotionSessionId(skuRedisTo.getPromotionSessionId());
                                    secOrderTo.setMemberId(user.getId());
                                    secOrderTo.setSeckillPrice(skuRedisTo.getSeckillPrice());

                                    rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", secOrderTo);
                                    return timeId;

                                } else {
                                    return null;
                                }

                            } catch (InterruptedException e) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * 秒杀活动场次信息
     *
     * @param sessions
     */
    private void saveSessionInfo(List<SeckillSessionVo> sessions) {
        sessions.stream().forEach(session -> {
            long start = session.getStartTime().getTime();
            long end = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + start + "_" + end;
            //幂等
            if (!redisTemplate.hasKey(key)) {
                List<String> collect = session.getRelationSkus().stream().map(sku -> {
                    return sku.getPromotionId() + "_" + sku.getSkuId().toString();
                }).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    /**
     * 秒杀商品信息
     *
     * @param sessions
     */
    private void saveSessionSkus(List<SeckillSessionVo> sessions) {
        sessions.stream().forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            List<SeckillSkuRelationVo> skus = session.getRelationSkus();
            skus.stream().forEach(sku -> {
                String random = UUID.randomUUID().toString().replace("-", "");
                if (!ops.hasKey(sku.getPromotionId() + "_" + sku.getSkuId().toString())) {
                    SeckillSkuRedisTo seckillSkuRedisTo = new SeckillSkuRedisTo();

                    R r = productFeignService.info(sku.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        seckillSkuRedisTo.setSkuInfoVo(skuInfo);
                    }

                    BeanUtils.copyProperties(sku, seckillSkuRedisTo);

                    seckillSkuRedisTo.setStartTime(session.getStartTime().getTime());
                    seckillSkuRedisTo.setEndTime(session.getEndTime().getTime());
                    //随机码
                    seckillSkuRedisTo.setRandomCode(random);

                    String skuJsonString = JSON.toJSONString(seckillSkuRedisTo);
                    ops.put(session.getId() + "_" + sku.getSkuId().toString(), skuJsonString);
                    //信号量 限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + random);
                    //秒杀数量作为信号量
                    semaphore.trySetPermits(sku.getSeckillCount().intValue());
                }
            });
        });
    }
}
