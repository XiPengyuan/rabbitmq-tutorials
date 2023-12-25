# 2 Work queues


## Work Queues
![work queues](https://rabbitmq.com/img/tutorials/python-two.png)

在本教程中，我们将创建一个工作队列（_Work Queue_），用于在多个worker之间分配耗时的任务。


## 准备
现在我们将发送代表复杂任务的字符串。
我们没有现实世界的任务，比如要调整图像大小或要渲染pdf文件，所以让我们假装很忙来伪造它——使用`Thread.sleep()`函数。
我们将字符串中点数作为其复杂度；每个点将占一秒钟的“工作”。例如，`Hello...`描述的一个假任务将需要三秒钟。


## Round-robin dispatching
默认情况下，RabbitMQ会将每条消息按顺序发送给下一个消费者。平均而言，每个消费者都会收到相同数量的消息。这种分发消息的方式称为round-robin。


## 消息确认
为了确保消息永远不会丢失，RabbitMQ支持[message _acknowledgments_](https://rabbitmq.com/confirms.html)。
acknowledgement由消费者发回，来告诉RabbitMQ已收到并处理特定消息，且RabbitMQ可以自由删除该消息。

如果消费者死亡（其Channel被关闭、Connection被关闭，或TCP连接丢失）而没有发送ack，RabbitMQ将了解消息未完全处理并将其re-queue。
如果同时有其他消费者在线，那么它会快速将其redeliver给另一个消费者。

消费者delivery acknowledgement上有一个强制执行的超时（默认为30分钟）。
您可以按照[Delivery Acknowledgement Timeout](https://www.rabbitmq.com/consumers.html#acknowledgement-timeout)中的说明增加此超时。

默认情况下，手动消息确认处于打开状态。


## 消息持久性
我们已经学会了如何确保即使消费者死亡，任务也不会丢失。但是如果RabbitMQ server停机，我们的任务仍然会丢失。

当RabbitMQ退出或崩溃时，它会忘记queues和messages除非您告诉它不要这样做。要确保消息不丢需要做两件事：我们需要将queue和消息都标记为durable。

> ### 关于消息持久化的注意事项
> 将消息标记为persistent并不能完全保证消息不会丢失。
> 尽管它告诉RabbitMQ将消息保存到磁盘，但仍有一个RabbitMQ已接受消息但尚未保存它的很短的时间窗口。
> 此外，RabbitMQ也不会对每条消息都执行`fsync(2)`——它可能只是保存到cache中而不是真正写入磁盘。
> 持久性的保证并不强，但对于我们的simple task queue来说已经足够了。如果你需要更强的保证，那么你可以使用[publisher confirms](https://www.rabbitmq.com/confirms.html)。


## Fair dispatch
您可能已注意到，调度仍然没有完全按照我们想要的方式工作。
例如，在有两个worker的情况下，当所有奇数消息都很重而偶数消息都很轻时，一个worker将一直忙碌，而另一个几乎不会做任何工作。

发生这种情况是因为RabbitMQ只是在消息进入队列时才调度该消息。它不会查看消费者的未确认消息的数量。

为了解决这个问题，我们可以使用`basicQos`方法设置`prefetchCount = 1`。
这告诉RabbitMQ不要一次给一个worker多于一条消息。或者，换句话说，在worker处理并确认前一条消息之前，不要向其发送新消息。
