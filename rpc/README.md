# 6 RPC

## Remote procedure call (RPC)

在本教程中，我们将使用RabbitMQ构建一个RPC系统：一个客户端和一个可扩展的RPC server。
由于我们没有任何值得分发的耗时任务，因此我们将创建一个返回斐波那契数的虚拟RPC服务。

## Callback queue

为了接收响应，我们需要随请求发送“callback”队列地址。我们可以使用default queue（Java客户端独有）。

> ### Message properties
> AMQP 0-9-1协议预定义了随消息附带的一组14个属性。大多数属性很少使用，但下面几个除外：
> * `deliveryMode`：将消息标记为持久的（值为`2`）或瞬态的（任何其他值）。
> * `contentType`：用于描述编码的mime-type。例如，对于经常使用的JSON编码，最好将此属性设置为：application/json。
> * `replyTo`：通常用于命名callback queue。
> * `correlationId`：对于将RPC响应与请求关联起来很有用。

## Correlation Id

在上面介绍的方法中，我们建议为每个RPC请求创建一个callback queue。
这相当低效，但幸运的是有更好的方法——让我们为每个client创建一个callback queue。

这引发了一个新问题，在队列中收到响应后，不清楚该响应属于哪个请求。这就是使用`correlationId`属性的时候。我们将为每个请求将其设置为唯一值。
稍后，当我们在callback queue中收到消息时，我们将查看此属性，并基于此我们能够将响应与请求进行匹配。
如果我们看到未知的`correlationId`值，我们可以安全地丢弃该消息——它不属于我们的请求。

你可能会问，为什么我们要忽略callback queue中的未知消息，而不是附带error失败？
虽然不太可能，但RPC server有可能在向我们发送答案之后，但在发送该请求的acknowledgment消息之前就挂掉了。
如果发生这种情况，重新启动的RPC server将再次处理该请求。这就是为什么在客户端我们必须优雅地处理重复响应，并且RPC理想情况下应该是幂等的。

## 总结

![](https://rabbitmq.com/img/tutorials/python-six.png)

我们的RPC将像这样工作：
* 对于RPC请求，Client发送具有两个属性的消息：`replyTo`，它被设置为专门为该请求创建的匿名独占队列，
  以及`correlationId`，它被设置为对每个请求的唯一值。
* 请求被发送到`rpc_queue`队列。
* RPC worker（aka: server）等待该队列上的请求。当请求出现时，它会执行作业并使用来自`replyTo`字段的队列将带有结果的消息发送回客户端。
* 客户端等待reply queue上的数据。当出现消息时，它会检查`correlationId`属性。如果它与请求中的值匹配，它将向应用返回响应。
