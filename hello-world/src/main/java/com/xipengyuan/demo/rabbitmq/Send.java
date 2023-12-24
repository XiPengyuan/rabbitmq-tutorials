package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 连接到RabbitMQ，发送一条消息，然后退出
 */
@Slf4j
public class Send {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // 连接到本地的RabbitMQ节点
        // Connection对socket连接进行了抽象，并为我们处理协议版本协商、认证等工作
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) { // 创建一个Channel，这是大多数用于完成任务的API之所在
            // queue的声明是幂等的——仅当队列尚不存在时才会创建它
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ操作异常", e);
        }
    }
}
