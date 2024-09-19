package com.mall.member.dao;

import com.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author aulen
 * @email 772039675@qq.com
 * @date 2024-09-06 00:57:30
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
