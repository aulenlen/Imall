package com.mall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.mall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private String app_id = "9021000141644053";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCEpovWRx6QLcRtf/flwvs7v0D6zKx0cDaO9dH12qsNqvQzvCz4aRGjf4nAEkuCw9mu0XAKH3WHJm5J12vu7rJkn7RGQ5n/TP41djpafr4RelARzmMmX2OiLTHJ+tl+UzivaQ89kxFt4vUA3YzMsXa1Sz1vlzKWeFJ3hojYoHrDYsKnbKcGhuVFArQHHpamumv16NjIECv2U1zN7/O8thJ2DPPj7TaQ/5kjZw8IQVuwzrs6Ei789lYQ9L1tf/wK/Pj03NVsIdiw1DATUBD9ZokX17WMoG79tGNg3aHKSRcnk8p2ydgeLCMx/4oYrZfwe7tJr+rQ9iW1Srkn/VqYw6ELAgMBAAECggEAP9586BBYO+19bYe3EY6/LrZYkClqQ6of6tebX3gI8kzExgAt2vGkB4gKCEwGxZCNSGoZOxaInSk44x7xHhsaE+79evNju5QTtiQV1qq902aEFP69qKU3MMf3+BtrCzMA0RyRtQER3Enx05fZClnMIWB31ToEpoSKmHhTfZJPabF5nyAP2vmv8rfoixLo/vQNGMgYCyhETgsuhP5waubpQO8Kk5/+PHcZ01gcoS4/opn9LEDXuokQZt9Td4EgL72eukaXZFliESB+9YMEPUAuVwf13Jj04JLHIx9bOiWNOulGltbz4d49bhyDXHG2NAOFr1MfjFwaksMi20LVZtvmsQKBgQDSv97NZIANyGJXycJlAZ6N7YUZt2M5CYtMysUb83+Hxmu3NUCboiXVvAFevo5ZehnP19KNWbz5TYMBG80EU55RTnWWy81uUwoX8dus/7aDLDv6PGed/ylSy9rlmk3Bmu5Y72Y3m75yibuMuiG6U9G+iO9PJIwjloAk4xifcENYCQKBgQChId0ORMHiJuIYqwzE1C3WOLMuLR/tm8d3Ove4gAyKWLkRgCBaF3XuxXtqOzgHJcMYKiHPDuYqePyuXdCZIUi/GM7EU3Zt3fFsrJ9E09SElYVRHK7DylszlOKOjHpSMe7Hf907dz1ql5YJ+Ag88Jfl7s1jBp+T6KHruu3s9x2tcwKBgBA5LviWtPjapR9m660E2+THFQjuwheg8XU+4r+NFhMopiAbXFbeu5ThfSWC+8hNivEiBxZUgySZ3+zU7ApaOYS+ynSKSc0lXVCMkMGXicA9Rnyz67IQ82hpQveL0lKGD7UO8Mp1FcsRaTujFHeYfjvxpKJuCM+whTrRtJKJfNWpAoGAODKAT36phD99cY8OqAM8bU9fJsa0MQG4wpu9VArM1N92xGTEY8d06S7VCUrYEp4X+fCP1BBlNAD4V7P+kbmrDoMPcieN0WmrzvQxpCahIxCm65AuX7jOsCHIFmFmNA6YMdolhZzjva5atOVdsitTPu1Xkqb3tvVWq8mJO7FAF4MCgYA1FA17+cWip74l6yfuRSBIdDsmjVg7PIYmqENIC5OaIuwI08EC+jTxHsA3kP3Ux4dFXg9DHmXTmXpEOHvXqgkbye8/xWFUnyeWZdViBTta0SMWyJMfkh9r+LjwwHuczhip9VXFhZrQmVRh/gIIg2z8f9byaxtnVap168RmI/DxRQ==";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnjli/XDDFeses2DP1JnBnIU5g9jyubeaSNhiFFscM6IkE3qltQiXYzM0MtJNwtCuF/jAxRD5TpNiH+TKJPW9Rd2BziD2ptexnsA8s6yi33PSyFxy9EbfnF/YF6mve0Vo/5myK+uHkvvCvvCJtFocAw/RF5Zu2U5qQozD8Mu4nxEodaG+cCHbCoZjAxw0mdlBch3pdJp36Ysm5LC3jVgUEvowlD2JQX7zktex9VG93kCeS9i7wxkhcEH5Fw0aHHwY7Hslj2zR7QhrwjjQB8/bXk4qL0BaryNZiLCn++C8Q7qJmGOCWFLquVbMgQSq78Iebp8jZteCe3MeaMi1wRbyUwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private String notify_url = "http://daua8p.natappfree.cc/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private String return_url = "http://member.mall.com/memberOrder.html";
    ;

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                +"\"timeout_express\":\"" + subject + "\","
                + "\"body\":\"1m\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
//        System.out.println("支付宝的响应：" + result);

        return result;

    }
}
