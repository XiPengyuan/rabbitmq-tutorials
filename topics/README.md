# 5 Topics

## Topics

尽管使用`direct` exchange改进了我们的系统，但它仍有局限性——它不能基于多个criteria进行路由。

在我们的日志系统中，我们可能不仅希望根据severity订阅日志，还希望根据发出日志的源订阅日志。

为了在我们的日志系统中实现这一点，我们需要了解更复杂的`topic` exchange。

## Topic exchange

发送到`topic` exchange的消息不能有任意的`routing_key`——它必须是一个由点分隔的单词列表。
routing key中可以有任意多个单词，最多255字节。

`topic` exchange背后的逻辑与`direct` exchange类似——使用特定routing key发送的消息将被传递到与匹配的binding key绑定的所有队列。
然而，binding key有两种重要的特殊情况：

* `*`（星号）可以恰好替代一个单词。
* `#`（hash）可以替代零个或多个单词。

通过一个例子来解释这一点是最简单的：

![](https://rabbitmq.com/img/tutorials/python-five.png)

在本例中，我们将发送描述动物的消息。消息将使用由三个单词（两个点）组成的routing key发送。
routing key中的第一个单词将描述速度，第二个单词描述颜色，第三个单词描述物种："`<speed>.<colour>.<species>`"。

我们创建了三个binding：Q1与binding key "`*.orange.*`"绑定，Q2与"`*.*.rabbit`"和"`lazy.#`"绑定。

routing key设置为"`quick.orange.rabbit`"的消息将被传递到两个queue。消息"`lazy.orange.elephant`"也将发送给他们两个。
另一方面，"`quick.orange.fox`"只会进入第一个queue，而"`lazy.brown.fox`"只会进入第二个queue。
"`lazy.pink.rabbit`"只会被传递到第二个queue一次，即使它匹配两个binding。"`quick.brown.fox`"与任何binding都不匹配，因此它将被丢弃。

如果我们违反约定并发送包含1或4个单词（如"`orange`"或"`quick.orange.new.rabbit`"）的消息，会发生什么呢？
那么，这些消息不会与任何binding匹配，并且将会被丢弃。

另一方面，"`lazy.orange.new.rabbit`"，即使它有四个单词，也会匹配最后一个绑定，并将被投递到第二个queue。

> ### Topic exchange
> 当队列与"`#`"（hash）binding key绑定时——它将接收所有消息，无论routing key是什么——就像在`fanout` exchange中一样。
>
> 当binding中未使用特殊字符"`*`"（星号）和"`#`"（hash）时，topic exchange的行为就像`direct` exchange一样。
