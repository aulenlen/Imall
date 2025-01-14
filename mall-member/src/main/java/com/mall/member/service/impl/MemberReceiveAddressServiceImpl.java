package com.mall.member.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.member.dao.MemberReceiveAddressDao;
import com.mall.member.entity.MemberReceiveAddressEntity;
import com.mall.member.service.MemberReceiveAddressService;


@Service("memberReceiveAddressService")
public class MemberReceiveAddressServiceImpl extends ServiceImpl<MemberReceiveAddressDao, MemberReceiveAddressEntity> implements MemberReceiveAddressService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberReceiveAddressEntity> page = this.page(
                new Query<MemberReceiveAddressEntity>().getPage(params),
                new QueryWrapper<MemberReceiveAddressEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取用户对应的地址
     *
     * @param memberId
     * @return
     */
    @Override
    public List<MemberReceiveAddressEntity> getAddress(Long memberId) {
        List<MemberReceiveAddressEntity> address =
                baseMapper.selectList(new QueryWrapper<MemberReceiveAddressEntity>().eq("member_id", memberId));
        return address;
    }

}