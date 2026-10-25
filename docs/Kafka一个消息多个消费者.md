# Kafka：一个消息被多个消费者消费（发布/订阅）

> 配套代码见仓库 [kafka/](../../kafka) 工程下的 `PubSubProducer.java` / `PubSubConsumer.java`（包 com.testmq.kafka）。

---

## 1. 先分清两种模型

Kafka 里「消费者」和「消费组（consumer group）」是两回事，这决定了消息是「一人吃」还是「人人有份」：

| 场景 | 怎么部署消费者 | 一条消息被谁拿到 |
| --- | --- | --- |
| **竞争消费（负载均衡）** | 同一 group 内起多个消费者 | 组内**某一个**消费者（按分区分配） |
| **发布/订阅（广播）** | 多个消费者分属**不同 group** | **每个 group 各拿到一份完整消息** |

所以「一个消息多个消费者」的本质是：**让这些消费者属于不同的消费组**，每个组独立维护自己的 offset，于是都会收到该 topic 的全部消息。

---

## 2. 代码怎么演示

[kafka/](../../kafka) 模块新增两个类：

- **PubSubProducer**：往 `BroadcastTopic` 发 5 条消息（key 传 null，顺序不重要的广播场景）。
- **PubSubConsumer**：消费时读取环境变量 `KAFKA_GROUP` 作为组名。

启动方式（开两个终端，用**不同的 group**）：

```bash
cd kafka
mvn clean compile

# 终端 1：组 A
KAFKA_GROUP=broadcast-group-A \
  mvn exec:java -Dexec.mainClass="com.testmq.kafka.PubSubConsumer"

# 终端 2：组 B（与 A 不同组，因此也会收到全部 5 条）
KAFKA_GROUP=broadcast-group-B \
  mvn exec:java -Dexec.mainClass="com.testmq.kafka.PubSubConsumer"

# 终端 3：生产者
mvn exec:java -Dexec.mainClass="com.testmq.kafka.PubSubProducer"
```

观察：组 A 和组 B 各自打印出「广播消息-1 ... 广播消息-5」，说明一条消息被多个消费者（组）都消费了。

---

## 3. 常见误区

- **误区**：在「同一个 group」里起多个消费者，以为能广播。错——同组多消费者是分摊分区、竞争消费，一条消息只会被组内一个人拿到。
- **广播的正确姿势**：不同业务方用不同 group 订阅同一 topic，各自维护 offset，互不影响。
- **清理**：不再需要的 group 要显式处理，否则它的 offset 会一直保留，重启后从旧位置继续消费（可能重复拉历史消息）。

---

## 4. 和其它中间件的对应关系

- **RocketMQ**：同样是「不同 consumer group 各自收一份」，广播语义一致；它还额外提供 `MessageModel.BROADCASTING` 广播模式（组内每人一份）。
- **Pulsar**：通过「多个订阅（subscription）」实现，每个订阅独立维护游标，等价于 Kafka 的多个消费组。

---

## 5. 与本仓库其它内容的关系

- [Kafka顺序消息.md](./Kafka顺序消息.md) 讲的是「同一业务内有先后」（顺序消息）；本文讲的是「一份消息分给多方」（发布/订阅）。二者是正交的两个维度。
- 顺序消息要求「单分区串行」，而广播要求「多组各收一份」，两者可以叠加：例如每个 group 内部仍按 key 保序消费。
