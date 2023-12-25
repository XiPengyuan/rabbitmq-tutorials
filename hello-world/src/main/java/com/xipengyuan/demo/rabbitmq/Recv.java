package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.xipengyuan.demo.rabbitmq.Constant.QUEUE_NAME;

/**
 * 消费者监听来自RabbitMQ的消息，
 * 因此与发布单个消息的发布者不同，我们将保持消费者运行以监听消息并将它们打印出来
 */
public class Recv {

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        /*
         * 我们没有使用try-with-resource来自动关闭Channel和Connection
         * 因为我们希望在消费者异步侦听消息到达的同时使进程保持活动状态
         */
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 我们也在这里声明了队列。因为我们可能会在发布者之前启动消费者，所以我们希望在尝试消费队列中的消息之前确保队列存在
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 异步向我们推送消息，我们提供回调，该回调将缓冲消息，直到我们准备好使用它们
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + body + "'"); // 消费者将打印通过RabbitMQ从发布者那里获得的消息
        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        // 消费者将持续运行，等待消息
    }
}
