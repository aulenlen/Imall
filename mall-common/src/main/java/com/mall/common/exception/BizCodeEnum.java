package com.mall.common.exception;

public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统位置异常"),
    VAILD_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码获取频率过高"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户已存在异常"),
    PHONE_EXIST_EXCEPTION(15002, "手机号码已存在异常"),
    USERNAME_OR_PASSWORD_EXCEPTION(15003, "用户名或密码错误");

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
