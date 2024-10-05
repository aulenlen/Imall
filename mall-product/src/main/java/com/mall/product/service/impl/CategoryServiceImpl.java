package com.mall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.mall.product.service.CategoryBrandRelationService;
import com.mall.product.vo.Catalog2Vo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.CategoryDao;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    //查出所有商品分类
    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);

        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(getChildren(menu, entities));
                    return menu;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort())
                            - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(Long[] catIds) {
        //TODO
        baseMapper.deleteBatchIds(Arrays.asList(catIds));
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {

        List<Long> path = new ArrayList<>();
        findParentPath(catelogId, path);
        Collections.reverse(path);

        return (Long[]) path.toArray(new Long[path.size()]);
    }
    @Cacheable(value = {"category"},key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("获取一级分类......");
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }

    /**
     * 从redis返回三级分类
     *
     * @return
     */
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if (catalogJson == null || StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中......");
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();
            return catalogJsonFromDb;
        }
        System.out.println("缓存命中......");
        Map<String, List<Catalog2Vo>> object = (Map<String, List<Catalog2Vo>>) JSON.parse(catalogJson);
        return object;
    }

    /**
     * 从数据库获取三级分类本地锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDb() {
        synchronized (this) {

            String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
            if (!StringUtils.isEmpty(catalogJson)) {
                Map<String, List<Catalog2Vo>> object = (Map<String, List<Catalog2Vo>>) JSON.parse(catalogJson);
                return object;
            }
            System.out.println("查询数据库");
            List<CategoryEntity> selectList = baseMapper.selectList(null);


            //一级分类
            List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

            //数据封装成map
            Map<String, List<Catalog2Vo>> collect = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                List<Catalog2Vo> catalog2Vos = null;
                if (categoryEntities != null) {
                    catalog2Vos = categoryEntities.stream().map(l2 -> {
                        Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        List<CategoryEntity> category3EntityList = getParentCid(selectList, l2.getCatId());
                        if (category3EntityList != null) {
                            List<Catalog2Vo.Catalog3Vo> catalog3VoList = category3EntityList.stream().map(l3 -> {
                                Catalog2Vo.Catalog3Vo catalog3Vo =
                                        new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catalog3Vo;
                            }).collect(Collectors.toList());
                            catalog2Vo.setCatalog3List(catalog3VoList);
                        }
                        return catalog2Vo;
                    }).collect(Collectors.toList());
                }
                return catalog2Vos;
            }));

            String jsonString = JSON.toJSONString(collect);
            stringRedisTemplate.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);

            return collect;
        }
    }

    /**
     * 从redis获取三级分类分布式锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("分布式锁加锁成功......");
            Map<String, List<Catalog2Vo>> catalogJsonFromDb;
            try {
                catalogJsonFromDb = getCatalogJsonFromDb();
            } finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long execute = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return catalogJsonFromDb;
        } else {
            System.out.println("分布式锁加锁失败......重新获取中......");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCatalogJsonFromDbWithRedisLock();
        }
    }


    @Caching(evict = {
            @CacheEvict(value = "category",key="'getLevel1Categories'"),
            @CacheEvict(value = "category",key="'getCatalogJson'")
    })
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        //同时修改缓存中的数据
        //redis.del("catalogJSON");等待下次主动查询进行更新
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return path;
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
                    return categoryEntity.getParentCid() == root.getCatId();
                })
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildren(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort())
                            - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());
        return children;
    }

}