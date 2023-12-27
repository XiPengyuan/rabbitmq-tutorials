# 7 Publisher Confirms

## Publisher Confirms

[Publisher confirms](https://rabbitmq.com/confirms.html#publisher-confirms)是RabbitMQ的扩展，用于实现可靠的发布。
当在channel上启用publisher confirms时，客户端发布的消息将由broker异步确认，这意味着它们已在服务器端得到处理。

## 在Channel上启用Publisher Confirms

Publisher confirms是在channel级别使用`confirmSelect`方法启用的：

```
Channel channel = connection.createChannel();
channel.confirmSelect();
```

必须在您希望使用publisher confirms的每个channel上调用此方法。Confirms应该只启用一次，而不是每条发布的消息都启用。

## 策略#1：单独发布消息

让我们从最简单的确认发布方法开始，即发布消息并同步等待其确认：

```
while (thereAreMessagesToPublish()) {
    byte[] body = ...;
    BasicProperties properties = ...;
    channel.basicPublish(exchange, queue, properties, body);
    // uses a 5 second timeout
    channel.waitForConfirmsOrDie(5_000);
}
```

在前面的示例中，我们照常发布消息并使用`Channel#waitForConfirmsOrDie(long)`方法等待其确认。一旦消息被确认，该方法就会返回。
如果消息在超时时间内没有得到确认或者被nack-ed（意味着代理由于某种原因无法处理它），该方法将抛出异常。
异常的处理通常包括记录错误消息和/或重试发送消息。

这种技术非常直接，但也有一个主要缺点：它会显着减慢发布速度，因为消息的confirmation会阻止所有后续消息的发布。
这种方法不会提供每秒超过数百条发布的消息的吞吐量。

## 策略#2：批量发布消息

为了改进之前的示例，我们可以发布一批消息并等待整批消息被确认。

与等待单个消息的确认相比，等待一批消息被确认可以极大地提高吞吐量（对于远程RabbitMQ节点最多可提高20-30倍）。
一个缺点是我们不知道发生故障时到底出了什么问题，因此我们可能必须在内存中保留一整批数据以记录有意义的内容或重新发布消息。
这个方案还是同步的，所以会阻塞消息的发布。

## 策略#3：异步处理Publisher Confirms

broker异步确认已发布的消息，只需在客户端上注册回调即可收到这些confirms的通知：

```
Channel channel = connection.createChannel();
channel.confirmSelect();
channel.addConfirmListener((sequenceNumber, multiple) -> {
    // 消息被confirmed时的代码
}, (sequenceNumber, multiple) -> {
    // 消息被nack-ed时的代码
});
```

这里有2个callback：一种用于已确认的消息，另一种用于nack-ed消息（可以被代理视为丢失的消息）。每个回调有2个参数：

* sequence number：标识confirmed或nack-ed消息的编号。
* multiple：这是一个布尔值。如果为false，则仅confirmed/nack-ed一条消息，
  如果为true，则所有具有较低或相同sequence number的消息都会被confirmed/nack-ed。

> ### 重新发布nack-ed消息？
> 从相应的callback中重新发布nack-ed消息可能很诱人，但应该避免这种情况，
> 因为confirm callbacks是在channels不应该执行操作的I/O线程中调度的。
> 更好的解决方案是将消息放入内存队列中，由发布线程轮询。
> 像`ConcurrentLinkedQueue`这样的类是在confirm回调和发布线程之间传输消息的良好候选者。
