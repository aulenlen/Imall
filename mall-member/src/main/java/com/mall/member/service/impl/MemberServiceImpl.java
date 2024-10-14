package com.mall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.mall.common.to.SocialUserTo;
import com.mall.common.utils.HttpUtils;
import com.mall.common.utils.R;
import com.mall.member.dao.MemberLevelDao;
import com.mall.member.entity.MemberLevelEntity;
import com.mall.member.exception.PhoneExistException;
import com.mall.member.exception.UserNameExistException;
import com.mall.member.service.MemberLevelService;
import com.mall.member.vo.MemberLoginVo;
import com.mall.member.vo.MemberRegistVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.member.dao.MemberDao;
import com.mall.member.entity.MemberEntity;
import com.mall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelService levelService;
    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册
     *
     * @param vo
     */
    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置默认会员等级
        MemberLevelEntity memberLevel = levelService.getDefaultLevel();
        //检查手机是否唯一
        checkPhoneUnique(vo.getPhone());
        memberEntity.setMobile(vo.getPhone());
        //检查用户名是否唯一
        checkUserNameUnique(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        memberEntity.setLevelId(memberLevel.getId());
        baseMapper.insert(memberEntity);
    }

    /**
     * 检查手机号码是否唯一
     *
     * @param phone
     * @throws PhoneExistException
     */
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    /**
     * 检查用户名是否唯一
     *
     * @param userName
     * @throws UserNameExistException
     */
    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UserNameExistException();
        }
    }

    /**
     * 登录
     *
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        MemberEntity member = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", vo.getLoginacct()).or().eq("mobile", vo.getLoginacct()));
        if (member == null) {
            return null;
        }
        String passwordDb = member.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matches = passwordEncoder.matches(vo.getPassword(), passwordDb);
        return matches == true ? member : null;
    }

    /**
     * 查询社交账号的登录，未注册则注册
     *
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(SocialUserTo vo) {
        String uid = vo.getId();
        MemberEntity member = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (member != null) {
            MemberEntity update = new MemberEntity();
            update.setExpiresIn(vo.getExpiresIn());
            update.setId(member.getId());
            update.setAccessToken(vo.getAccessToken());
            baseMapper.updateById(update);
            member.setAccessToken(vo.getAccessToken());
            member.setExpiresIn(vo.getExpiresIn());
            return member;
        } else {
            MemberEntity save = new MemberEntity();
            save.setUsername(vo.getLogin());
            save.setNickname(vo.getName());
            save.setEmail(vo.getEmail());
            save.setHeader(vo.getAvatarUrl());
            save.setSign(vo.getBio());
            save.setSocialUid(uid);
            MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
            save.setLevelId(memberLevelEntity.getId());
            this.baseMapper.insert(save);
            return save;
        }
    }
}