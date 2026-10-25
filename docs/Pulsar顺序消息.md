# Pulsar 顺序消息：原理、配置与示例

> 配套代码见仓库 [pulsar/](../../pulsar) 目录（Maven 工程，包 com.testmq.pulsar）。

---

## 1. 结论

**Pulsar 通过「key + 分区」保证顺序**：给消息指定相同的业务 key，Pulsar 会按 key 的 hash 把它们路由到同一个分区（bundle/partition），分区内严格有序。消费端使用 **Key_Shared** 订阅类型即可在「多消费者并行」的同时，仍然保证「相同 key 的消息被同一消费者顺序消费」。

Pulsar 还内建了消息序列号与重试机制，失败重投时不会打乱同一 key 的顺序，所以相比 Kafka 你几乎不用操心「发送端重排」的问题。

---

## 2. 为什么能保证顺序？

- Pulsar 的 topic 是分区的（partitioned topic）。
- 生产者发送带 key 的消息时，默认按 key 的 hash 选择分区；相同 key 必然落在同一分区。
- 单个分区内的消息按写入顺序被消费，因此「同一 key 有序」。
- 重试（negativeAcknowledge / 超时未 ack）时，Pulsar 以 key 维度重新投递，顺序依旧保持。

>
> 补充：不带 key 的消息默认按 round-robin 分散到各分区，跨分区无序；所以「顺序消息」必须带 key。

---

## 3. 三步做到顺序消息

### 3.1 生产者：给消息设置 key

```java
producer.newMessage()
        .key(orderId)          // 业务 key，决定落到哪个分区
        .value(value)
        .send();
```

如需最直观的顺序语义，可关闭批量：`enableBatching(false)`；开启批量时 Pulsar 仍按 key 保序，不影响正确性。

### 3.2 消费者：用 Key_Shared 订阅

```java
client.newConsumer(Schema.STRING)
      .topic(topic)
      .subscriptionName("order-sub")
      .subscriptionType(SubscriptionType.Key_Shared)  // 同 key 同消费者且有序
      .subscribe();
```

- **Key_Shared**：相同 key 的消息始终发给同一消费者，既保序又支持多消费者并行，吞吐最好。
- **Exclusive / Failover**：单消费者独占订阅，顺序最严格但无法并行扩展。
- 不要用 **Shared**（轮询分发，会破坏顺序）。

### 3.3 失败处理：用 negativeAcknowledge 重投

```java
try {
    process(msg);
    consumer.acknowledge(msg);
} catch (Exception e) {
    consumer.negativeAcknowledge(msg); // 按 key 重新投递，顺序仍保持
}
```

---

## 4. 代码演示了什么？

[pulsar/](../../pulsar) 模块模拟「3 个订单，每个订单 5 个有先后关系的步骤」：

- **OrderedProducer**：以「订单号」为 key 发送，相同订单进入同一分区。
- **OrderedConsumer**：用 Key_Shared 订阅，按 key 顺序消费，失败用 negativeAcknowledge 重投。

运行（需先有一个可达的 Pulsar，见第 5 节）：

```bash
cd pulsar
mvn clean compile

# 终端 1：消费者
mvn exec:java -Dexec.mainClass="com.testmq.pulsar.OrderedConsumer"

# 终端 2：生产者
mvn exec:java -Dexec.mainClass="com.testmq.pulsar.OrderedProducer"
```

环境变量：`PULSAR_URL`（默认 pulsar://127.0.0.1:6650）、`PULSAR_TOPIC`、`PULSAR_SUB`。

---

## 5. 怎么准备一个 Pulsar 来跑？

仓库 [k8s/streaming/pulsar.yaml](../../k8s/streaming/pulsar.yaml) 提供了 K3s 上的 Pulsar 部署清单（见 [k8s/streaming/README.md](../../k8s/streaming/README.md)）。部署好后把 `PULSAR_URL` 指向其 service 地址即可。

---

## 6. 与 Kafka / RocketMQ 的顺序消息对比

| 中间件 | 顺序粒度 | 发送端保序要点 | 消费端保序要点 |
| --- | --- | --- | --- |
| Kafka | 分区内有序 | key 路由同分区 + 幂等/ max.in.flight=1 | 单分区串行 + 手动提交 offset |
| Pulsar | 分区内（按 key）有序 | 消息带 key 即可，重投自动保序 | Key_Shared 订阅 |
| RocketMQ | 队列（MessageQueue）内有序 | MessageQueueSelector 路由同队列 | MessageListenerOrderly 顺序消费 |

三者思路一致：**把同一业务路由到同一有序单元（分区/队列），消费端按该单元串行处理**。
