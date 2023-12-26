# 4 Routing

## 路由

在上一篇教程中，我们构建了一个简单的日志系统。我们能够向许多接收者广播日志消息。

在本教程中，我们将向其添加一个功能——我们将使其能够仅订阅消息的子集。
例如，我们将能够仅将关键错误消息定向到日志文件（以节省磁盘空间），同时仍然能够在控制台上打印所有日志消息。

## Bindings

binding是exchange和queue之间的关系。

Binding可以采用额外的`routingKey`参数。为了避免与`basic_publish`参数混淆，我们将其称为`binding key`。

binding key的含义取决于exchange类型。我们之前使用的`fanout` exchange完全忽略了它的值。

## Direct exchange

我们使用过`fanout` exchange，这并没有给我们带来太大的灵活性——它只能进行无意识的广播。

我们将改用`direct` exchange。`direct` exchange背后的路由算法很简单——消息进入其`binding key`与消息的`routing key`完全匹配的队列。

![direct exchange](https://rabbitmq.com/img/tutorials/direct-exchange.png)

## Multiple bindings

![](https://rabbitmq.com/img/tutorials/direct-exchange-multiple.png)

使用相同的binding key绑定多个队列是完全合法的。

## Putting it all together

![](https://rabbitmq.com/img/tutorials/python-four.png)
