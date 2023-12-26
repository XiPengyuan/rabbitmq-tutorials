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

/**
 * 生产者接受两个参数：routingKey和log内容。
 * <p>
 * routingKey由两部分组成：&lt;facility&gt;.&lt;severity&gt;。
 * <p>
 * 比如我们要发出带有routing key "kern.critical"的日志：<br/>
 * <code>java -cp .. EmitLogTopic "kern.critical" "A critical kernel error"</code>
 */
@Slf4j
public class EmitLogTopic {

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            String routingKey = getRouting(args);
            String message = getMessage(args);

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ操作异常", e);
        }
    }

    /**
     * 我们首先假设log的routing key有两个单词："&lt;facility&gt;.&lt;severity&gt;"
     */
    private static String getRouting(String[] strings) {
        if (strings.length < 1) {
            return "anonymous.info";
        }
        return strings[0];
    }

    private static String getMessage(String[] strings) {
        if (strings.length < 2) {
            return "Hello World!";
        }
        return joinStrings(strings, " ", 1);
    }

    private static String joinStrings(String[] strings, String delimiter, int startIndex) {
        int length = strings.length;
        if (length == 0) {
            return "";
        }
        if (length < startIndex) {
            return "";
        }
        StringBuilder words = new StringBuilder(strings[startIndex]);
        for (int i = startIndex + 1; i < length; i++) {
            words.append(delimiter).append(strings[i]);
        }
        return words.toString();
    }
}
