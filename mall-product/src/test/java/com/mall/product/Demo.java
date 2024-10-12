package com.mall.product;

import com.mall.product.dao.AttrGroupDao;
import com.mall.product.dao.SkuSaleAttrValueDao;
import com.mall.product.vo.SkuItemSaleAttrVo;
import com.mall.product.vo.SpuItemGroupAttrVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Demo {
//    @Autowired
//    OSSClient ossClient;
    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    private SkuSaleAttrValueDao saleAttrValueDao;
    @Test
    public void test() throws Exception {
//        FileInputStream inputStream =
//                new FileInputStream("C:\\Users\\PC\\Downloads\\谷粒商城资料整理课件\\尚硅谷谷粒商城电商项目\\docs\\pics\\fe215589ed6500f4.jpg");
//
//        ossClient.putObject("mall-hello111","fe215589ed6500f4.jpg", inputStream);
//        ossClient.shutdown();
//        List<SpuItemGroupAttrVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(9L, 225L);
//        for (SpuItemGroupAttrVo groupAttrVo : attrGroupWithAttrsBySpuId) {
//            System.out.println(groupAttrVo);
//        }
        List<SkuItemSaleAttrVo> saleAttrValuesBySpuId = saleAttrValueDao.getSaleAttrValuesBySpuId(26L);
        for (SkuItemSaleAttrVo skuItemSaleAttrVo : saleAttrValuesBySpuId) {
            System.out.println(skuItemSaleAttrVo);
        }

    }
}
