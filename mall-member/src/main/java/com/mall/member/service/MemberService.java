package com.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.to.SocialUserTo;
import com.mall.common.utils.PageUtils;
import com.mall.member.entity.MemberEntity;
import com.mall.member.exception.PhoneExistException;
import com.mall.member.exception.UserNameExistException;
import com.mall.member.vo.MemberLoginVo;
import com.mall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author aulen
 * @email 772039675@qq.com
 * @date 2024-09-06 00:57:30
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UserNameExistException;


    MemberEntity login(MemberLoginVo vo);
    MemberEntity login(SocialUserTo vo);
}

