# RocketMQ 顺序消息：原理、配置与示例

> 配套代码见仓库 [java/](../../java) 工程下的 `OrderedProducer.java` / `OrderedConsumer.java`（包 com.testmq，复用现有 RocketMQ 依赖）。

---

## 1. 结论

**RocketMQ 把顺序消息做成了「一等公民」**：

- 生产者发送时用 `MessageQueueSelector` 把相同业务 key 的消息路由到**同一个 MessageQueue**；
- 消费者用 `MessageListenerOrderly`（顺序监听）消费，RocketMQ 保证**同一个 MessageQueue 内的消息被单线程、按顺序**消费。

这样就能得到「某个订单 / 某个用户」维度的严格顺序。

---

## 2. 为什么能保序？

- RocketMQ 的 topic 由多个 MessageQueue 组成（类似 Kafka 的分区）。
- 顺序消息的核心是「同一业务实体进同一个队列」——队列内 FIFO，消费端对该队列加锁单线程消费，自然有序。
- 若用普通发送（轮询到不同队列）或 `MessageListenerConcurrently`（并发消费），就不保证顺序。

---

## 3. 三步做到顺序消息

### 3.1 生产者：用 MessageQueueSelector 路由同队列

```java
SendResult res = producer.send(msg, new MessageQueueSelector() {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        String id = (String) arg;                 // arg 就是下面传入的 orderId
        int idx = Math.abs(id.hashCode()) % mqs.size();
        return mqs.get(idx);                       // 相同 orderId 永远落到同一队列
    }
}, orderId);                                       // 第三个参数 = 业务 key
```

> 也可以自己实现更精细的路由（如按地区、按用户段），只要保证「相同业务进同一队列」。

### 3.2 消费者：用 MessageListenerOrderly

```java
consumer.registerMessageListener(new MessageListenerOrderly() {
    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
        for (MessageExt msg : msgs) { process(msg); }
        return ConsumeOrderlyStatus.SUCCESS;       // 返回 SUCCESS 才会提交该队列的消费进度
    }
});
```

- `MessageListenerOrderly`：同一队列单线程顺序消费，是顺序消息的关键。
- `MessageListenerConcurrently`：并发消费，不保证顺序（普通消息用这个）。

### 3.3 注意点

- 顺序消费的队列在消费时会被加锁，因此**同一个队列无法被同组多个消费者并行消费**——吞吐受队列数限制。
- 某条消息消费失败会阻塞该队列后续消息（RocketMQ 会重试），所以要保证消费逻辑尽快成功，避免长时间阻塞。

---

## 4. 代码演示了什么？

[java/](../../java) 工程新增两个类，模拟「3 个订单，每个订单 5 个步骤」：

- **OrderedProducer**：`send(msg, selector, orderId)` 把同一订单发到同一队列。
- **OrderedConsumer**：`MessageListenerOrderly` 顺序消费并打印 queueId。

运行（需先启动 RocketMQ，见仓库 [操作指南.md](../../操作指南.md) / [README.md](../../README.md)）：

```bash
cd java
mvn clean compile

# 终端 1：顺序消费者
mvn exec:java -Dexec.mainClass="com.testmq.OrderedConsumer"

# 终端 2：顺序生产者
mvn exec:java -Dexec.mainClass="com.testmq.OrderedProducer"
```

环境变量：`ROCKETMQ_NAMESRV`（默认 127.0.0.1:9876）、`ROCKETMQ_TOPIC`（默认 OrderTopic）、`ROCKETMQ_PRODUCER_GROUP` / `ROCKETMQ_CONSUMER_GROUP`。

---

## 5. 与 Kafka / Pulsar 的顺序消息对比

| 中间件 | 顺序单元 | 发送端保序要点 | 消费端保序要点 |
| --- | --- | --- | --- |
| Kafka | 分区 | key 路由同分区 + 幂等/ max.in.flight=1 | 单分区串行 + 手动提交 offset |
| Pulsar | 分区（按 key） | 消息带 key 即可 | Key_Shared 订阅 |
| RocketMQ | MessageQueue | MessageQueueSelector 路由同队列 | MessageListenerOrderly 顺序消费 |

三者思路一致：**把同一业务路由到同一有序单元，消费端按该单元串行处理**。RocketMQ 的区别在于它把「selector + orderly listener」直接做成 API，使用上最直白。
