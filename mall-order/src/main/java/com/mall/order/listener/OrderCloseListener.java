package com.mall.order.listener;

import com.mall.order.entity.OrderEntity;
import com.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderCloseListener {
    @Autowired
    private OrderService orderService;
    @RabbitHandler
    public void listener(OrderEntity order, Message message, Channel channel) throws IOException {
        System.out.println("收到过期的订单信息：准备关闭订单"+order.getOrderSn()+"==>"+order.getId());
        try {
            orderService.orderClose(order);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
