package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Worker {
    private static final String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();
        // 确保queue能够在RabbitMQ节点重启后继续存续，将其声明为durable
        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1); // 一次只接受一条未ack的消息

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            System.out.println(" [x] Received '" + body + "'");
            try {
                doWork(body);
            } finally {
                System.out.println(" [x] Done");
                // Acknowledgement必须在接收delivery的同一channel上发送
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {});
        // 使用此代码，可以确保即使在处理消息时使用CTRL+C终止worker，也不会丢失任何内容。worker终止后不久，所有未确认的消息都会被重新传递
    }

    private static void doWork(String task) {
        for (char ch : task.toCharArray()) {
            if (ch == '.') {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
