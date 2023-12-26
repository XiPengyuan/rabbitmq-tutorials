package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.xipengyuan.demo.rabbitmq.Constant.TASK_QUEUE_NAME;

/**
 * 传递参数以指定要发送的消息内容。
 * 消息中的每个点（.）代表1秒钟的“工作量”，可以参见消费者的代码
 * <p>
 * <code>java -cp .. NewTask First message.</code><br/>
 * <code>java -cp .. NewTask Second message..</code><br/>
 * <code>java -cp .. NewTask Third message...</code><br/>
 * <code>java -cp .. NewTask Fourth message....</code><br/>
 * <code>java -cp .. NewTask Fifth message.....</code>
 */
@Slf4j
public class NewTask {

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 我们需要确保queue能够在RabbitMQ节点重启后继续存续。为此，我们需要将其声明为durable的
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

            String message = String.join(" ", args);
            // 现在需要将消息标记为persistent的——设置props值为PERSISTENT_TEXT_PLAIN
            channel.basicPublish("", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ操作异常", e);
        }
    }
}
