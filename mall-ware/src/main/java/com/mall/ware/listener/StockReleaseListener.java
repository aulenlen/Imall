package com.mall.ware.listener;

import com.mall.common.mq.StockLockedTo;
import com.mall.ware.service.WareSkuService;
import com.mall.common.to.OrderTo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = {"stock.release.stock.queue"})
@Service
public class StockReleaseListener {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * rabbit解锁库存
     *
     * @param to
     * @param message
     * @param channel
     */
    @RabbitHandler
    private void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到库存消息");
        try {
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    private void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到订单解锁消息");
        try {
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
