package com.xipengyuan.demo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

@Slf4j
public class PublisherConfirms {
    private static final int MESSAGE_COUNT = 50_000;

    private static Connection createConnection() throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf.newConnection();
    }

    public static void main(String[] args) {
        publishMessagesIndividually();
        publishMessagesInBatch();
        handlePublishConfirmsAsynchronously();
    }

    private static void publishMessagesIndividually() {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        } catch (IOException | TimeoutException e) {
            log.error("创建连接异常", e);
        } catch (InterruptedException e) {
            log.error("等待PublisherConfirm时异常", e);
        }
    }

    private static void publishMessagesInBatch() {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            final int batchSize = 100;
            int outstandingMessageCount = 0;

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                outstandingMessageCount++;

                if (outstandingMessageCount == batchSize) {
                    ch.waitForConfirmsOrDie(5_000);
                    outstandingMessageCount = 0;
                }
            }

            if (outstandingMessageCount > 0) {
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        } catch (IOException | TimeoutException e) {
            log.error("创建连接异常", e);
        } catch (InterruptedException e) {
            log.error("等待PublisherConfirm时异常", e);
        }
    }

    private static void handlePublishConfirmsAsynchronously() {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            final ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
            // 当confirms到达时，我们需要清理该map，
            final ConfirmCallback cleanOutstandingConfirms = (deliveryTag, multiple) -> {
                if (multiple) {
                    ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(deliveryTag, true);
                    confirmed.clear();
                } else {
                    outstandingConfirms.remove(deliveryTag);
                }
            };
            // 并执行一些操作，例如在消息被nack时记录警告
            ch.addConfirmListener(cleanOutstandingConfirms, (deliveryTag, multiple) -> {
                String body = outstandingConfirms.get(deliveryTag);
                System.err.format("Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n", body, deliveryTag, multiple);
                // 重新使用之前的callback来清除未完成confirms的map（无论消息被confirmed还是被nack-ed，都必须删除map中相应的条目。）
                cleanOutstandingConfirms.handle(deliveryTag, multiple);
            });

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                // 使用map将publishing sequence number与消息的字符串正文关联起来
                outstandingConfirms.put(ch.getNextPublishSeqNo(), body);
                ch.basicPublish("", queue, null, body.getBytes());
            }

            if (!waitUntil(Duration.ofSeconds(60), outstandingConfirms::isEmpty)) {
                throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        } catch (IOException | TimeoutException e) {
            log.error("创建连接异常", e);
        } catch (InterruptedException e) {
            log.error("等待时被打断", e);
        }
    }

    private static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited += 100;
        }
        return condition.getAsBoolean();
    }
}
