package com.mall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.mall.common.exception.BizCodeEnum;
import com.mall.common.to.SocialUserTo;
import com.mall.member.exception.PhoneExistException;
import com.mall.member.exception.UserNameExistException;
import com.mall.member.vo.MemberLoginVo;
import com.mall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.member.entity.MemberEntity;
import com.mall.member.service.MemberService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.R;


/**
 * 会员
 *
 * @author aulen
 * @email 772049675@qq.com
 * @date 2024-09-06 00:57:30
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);
        return R.ok().put("member", member);
    }

    /**
     * 注册
     *
     * @param vo
     * @return
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo) {
        try {
            memberService.regist(vo);
        } catch (UserNameExistException u) {
            return R.error().put(BizCodeEnum.USER_EXIST_EXCEPTION.getCode() + "", BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (PhoneExistException p) {
            return R.error().put(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode() + "", BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 第三方社交账号登录
     * @param vo
     * @return
     */
    @PostMapping("/auth2/login")
    public R authLogin(@RequestBody SocialUserTo vo) {
        MemberEntity member = memberService.login(vo);
        if (member == null) {
            return R.error(BizCodeEnum.USERNAME_OR_PASSWORD_EXCEPTION.getCode(), BizCodeEnum.USERNAME_OR_PASSWORD_EXCEPTION.getMsg());
        } else {
            return R.ok().setData(member);
        }
    }

    /**
     * 账号密码登录
     * @param vo
     * @return
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo) {
        MemberEntity member = memberService.login(vo);
        if (member == null) {
            return R.error(BizCodeEnum.USERNAME_OR_PASSWORD_EXCEPTION.getCode(), BizCodeEnum.USERNAME_OR_PASSWORD_EXCEPTION.getMsg());
        } else {
            return R.ok().setData(member);
        }
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
