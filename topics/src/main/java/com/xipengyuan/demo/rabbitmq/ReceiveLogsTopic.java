package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.xipengyuan.demo.rabbitmq.Constant.EXCHANGE_NAME;

/**
 * 接收所有log：<br/>
 * <code>java -cp .. ReceiveLogsTopic "#"</code>
 * <p>
 * 接收所有来自设备"kern"的日志：<br/>
 * <code>java -cp .. ReceiveLogsTopic "kern.*"</code>
 * <p>
 * 只监听"critical"的日志：<br/>
 * <code>java -cp .. ReceiveLogsTopic "*.critical"</code>
 * <p>
 * 也可以创建多个binding：<br/>
 * <code>java -cp .. ReceiveLogsTopic "kern.*" "*.critical"</code>
 */
public class ReceiveLogsTopic {

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();

        if (args.length < 1) {
            System.err.println("Usage: ReceiveLogsTopic [binding_key]...");
            System.exit(1);
        }

        for (String bindingKey : args) {
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
        }

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message.getEnvelope().getRoutingKey() + "':'" + body + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
