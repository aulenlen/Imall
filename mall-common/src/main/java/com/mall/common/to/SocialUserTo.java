package com.mall.common.to;


import lombok.Data;
@Data
public class SocialUserTo {
    private String accessToken;
    private String tokenType;
    private String expiresIn;
    private String refreshToken;
    private String scope;
    private String createdAt;
    private String id;
    private String login; //用户用户名
    private String name; //用户昵称
    private String avatarUrl;
    private String bio; //用户自我介绍
    private String email;
}
