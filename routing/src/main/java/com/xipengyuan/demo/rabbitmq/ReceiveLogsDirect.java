package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.xipengyuan.demo.rabbitmq.Constant.EXCHANGE_NAME;

/**
 * 运行本消费者程序必须携带一个或多个参数以指定想要接收的log的级别，如：<br/>
 * <code>java -cp .. ReceiveLogsDirect error</code><br/>
 * <code>java -cp .. ReceiveLogsDirect info warning error</code>
 */
public class ReceiveLogsDirect {

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        String queueName = channel.queueDeclare().getQueue();

        if (args.length < 1) {
            System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
            System.exit(1);
        }

        for (String severity : args) {
            // 我们将为我们感兴趣的每个severity创建一个新的binding
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
        }
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message.getEnvelope().getRoutingKey() + "':'" + body + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
