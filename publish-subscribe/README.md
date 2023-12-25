# 3 Publish/Subscribe


## Publish/Subscribe
Work queue背后的假设是每个任务都恰好交付给一个worker。
在这一部分中，我们将做一些完全不同的事情——我们将投递一个消息给多个消费者。这种模式被称为“发布/订阅”。

为了说明该模式，我们将构建一个简单的日志系统。

在我们的日志系统中，receiver程序的每个正在运行的副本都会收到消息。


## Exchange
在教程的前面部分中，我们向队列发送消息和从队列接收消息。现在是时候介绍Rabbit中完整的messaging模型了。

让我们快速回顾一下之前教程中介绍的内容：
* 生产者（_producer_）是发送消息的user application。
* 队列（_queue_）是存储消息的缓冲区（buffer）。
* 消费者（_consumer_）是接收消息的user application。

在RabbitMQ中，messaging模型的核心思想是生产者从不直接向队列发送任何消息。实际上，生产者通常根本不知道消息是否会被传递到任何队列。

相反，生产者只能将消息发送到 _exchange_。exchange是一个非常简单的东西。一方面，它接收来自生产者的消息，另一方面，它将消息推送到队列。
exchange必须确切地知道如何处理它收到的消息。是否应该将其附加到特定队列？是否应该将其附加到许多队列中？或者应该将其丢弃。
其规则由 _exchange类型_ 定义。

![exchanges](https://www.rabbitmq.com/img/tutorials/exchanges.png)

有几种可用的exchange类型：`direct`、`topic`、`headers`和`fanout`。我们将重点关注最后一个——fanout。

fanout exchange非常简单。正如您可能从名称中猜到的那样，它只是将收到的所有消息广播到它知道的所有队列。

> ### Nameless exchange
> 在本教程的前面部分中，我们对exchange一无所知，但仍然能够将消息发送到队列。
> 这是可能的，因为我们使用了一个默认exchange，我们通过空字符串来标识它（`""`）。
>
> 回想一下我们之前怎样发布一条消息：
> ```
> channel.basicPublish("", "hello", null, message.getBytes());
> ```
> 第一个参数是exchange的名称。空字符串表示 default 或 _nameless_ exchange：消息将路由到由`routingKey`指定的名称的队列，如果存在的话。


## 临时队列
当您想要在生产者和消费者之间共享队列时，为队列命名非常重要。

但对我们的logger而言并非如此。
我们希望听到所有log消息，而不仅仅是其中的一部分。我们也只对当前流动的消息感兴趣，而非旧消息。
为了解决这些，我们需要两件事。

首先，每当我们连接到Rabbit时，我们都需要一个新的、空的队列。
为此，我们可以创建一个具有随机名称的队列，或者更好——让服务器为我们选择一个随机的队列名称。

其次，一旦我们断开消费者，队列应该被自动删除。

在Java客户端中，当我们不向`queueDeclare()`提供任何参数时，我们会创建一个非持久的、独占的、自动删除的队列，具有生成的名称

你可以在[队列指南](https://www.rabbitmq.com/queues.html)中了解有关`exclusive`标志和其他队列属性的更多信息。


## Binding
![binding](https://www.rabbitmq.com/img/tutorials/bindings.png)

exchange和queue之间的关系被称为 _binding_。
