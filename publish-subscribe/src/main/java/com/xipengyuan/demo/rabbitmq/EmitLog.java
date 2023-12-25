package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.xipengyuan.demo.rabbitmq.Constant.EXCHANGE_NAME;

@Slf4j
public class EmitLog {

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 创建一个fanout类型的exchange，称之为logs
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

            String message = args.length < 1 ? "info: Hello World!" : String.join(" ", args);
            /*
             * 现在，可以发布到我们的具名exchange
             * 发送时我们需要提供routingKey，但对于fanout exchange，它的值将被忽略
             */
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
            // 如果还没有queue绑定到exchange，消息将会被丢弃，但对我们来说没关系；如果还没有消费者在监听，我们可以安全地丢弃消息
            System.out.println(" [x] Sent '" + message + "'");
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ操作异常", e);
        }
    }
}
