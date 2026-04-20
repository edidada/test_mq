# RabbitMQ 消息积压与故障处理演示 - 运行结果

## 📊 演示概述

本演示展示了在 RabbitMQ 消息队列系统中，当遇到**消息积压**、**MQ 服务宕机**、**MySQL 宕机**等场景时的处理策略。

---

## 🚀 运行环境

- **Java 版本**: 17
- **构建工具**: Maven
- **项目位置**: `D:\develops\git\github\cpp\test_mq\rabbitmq\java`

---

## 📝 运行步骤

### 1. 编译项目

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\java
mvn clean compile
```

**编译结果**: ✅ 成功

---

### 2. 运行消息发送者演示

```bash
java -cp target/classes com.testmq.rabbitmq.SimpleProducer
```

#### 运行输出：

```
=== RabbitMQ 消息发送者演示 ===

[场景 1] MQ 服务正常 - 发送消息到队列

[producer] send to queue: msg-001 (normal)
[producer] send to queue: msg-002 (normal)
[producer] send to queue: msg-003 (critical)
[producer] send to queue: msg-004 (normal)
[producer] send to queue: msg-005 (normal)

状态：队列中的消息数 = 5

[场景 2] MQ 服务宕机 - 消息暂存到 outbox

[producer] mq down, spool to outbox: msg-006
[producer] mq down, spool to outbox: msg-007
[producer] mq down, spool to outbox: msg-008

状态：outbox 中的消息数 = 3

[场景 3] MQ 服务恢复 - 重放 outbox 消息

[producer] replay outbox messages: 3

状态：队列中的消息数 = 8

=== 队列内容 ===
  [msg-001] 优先级：normal | 内容：这是第 1 条消息的内容
  [msg-002] 优先级：normal | 内容：这是第 2 条消息的内容
  [msg-003] 优先级：critical | 内容：这是第 3 条消息的内容
  [msg-004] 优先级：normal | 内容：这是第 4 条消息的内容
  [msg-005] 优先级：normal | 内容：这是第 5 条消息的内容
  [msg-006] 优先级：critical | 内容：这是第 6 条消息（MQ 宕机时）
  [msg-007] 优先级：critical | 内容：这是第 7 条消息（MQ 宕机时）
  [msg-008] 优先级：critical | 内容：这是第 8 条消息（MQ 宕机时）

=== 发送者统计 ===
发送到队列的消息数：8
暂存到 outbox 的消息数：3
```

---

### 3. 运行消息接收者演示

```bash
java -cp target/classes com.testmq.rabbitmq.SimpleConsumer
```

#### 运行输出：

```
=== RabbitMQ 消息接收者演示 ===

当前队列中的消息数：8

[场景 1] MySQL 正常 - 消费并持久化消息

[consumer] start consume, backlog=8
[consumer] persist to mysql: msg-001
[consumer] persist to mysql: msg-002
[consumer] persist to mysql: msg-003
[consumer] persist to mysql: msg-004
[consumer] persist to mysql: msg-005
[consumer] persist to mysql: msg-006
[consumer] persist to mysql: msg-007
[consumer] persist to mysql: msg-008

状态：数据库中的记录数 = 8

[场景 2] MySQL 宕机 - 消息暂存到 pending_db

[producer] send to queue: new-msg-001 (normal)
[producer] send to queue: new-msg-002 (normal)
[producer] send to queue: new-msg-003 (normal)
[consumer] start consume, backlog=3
[consumer] mysql down, retry 1/2: new-msg-001
[consumer] mysql down, retry 2/2: new-msg-001
[consumer] write to pending_db: new-msg-001
[consumer] mysql down, retry 1/2: new-msg-002
[consumer] mysql down, retry 2/2: new-msg-002
[consumer] write to pending_db: new-msg-002
[consumer] mysql down, retry 1/2: new-msg-003
[consumer] mysql down, retry 2/2: new-msg-003
[consumer] write to pending_db: new-msg-003

状态：pending_db 中的消息数 = 3

[场景 3] MySQL 恢复 - 刷新 pending_db 到数据库

[consumer] flush pending_db messages: 3

状态：数据库中的记录数 = 11

=== 数据库内容 ===
  [msg-001] 优先级：normal | 内容：这是第 1 条消息的内容
  [msg-002] 优先级：normal | 内容：这是第 2 条消息的内容
  [msg-003] 优先级：critical | 内容：这是第 3 条消息的内容
  [msg-004] 优先级：normal | 内容：这是第 4 条消息的内容
  [msg-005] 优先级：normal | 内容：这是第 5 条消息的内容
  [msg-006] 优先级：critical | 内容：这是第 6 条消息（MQ 宕机时）
  [msg-007] 优先级：critical | 内容：这是第 7 条消息（MQ 宕机时）
  [msg-008] 优先级：critical | 内容：这是第 8 条消息（MQ 宕机时）
  [new-msg-001] 优先级：normal | 内容：MySQL 宕机时的新消息 1
  [new-msg-002] 优先级：normal | 内容：MySQL 宕机时的新消息 2
  [new-msg-003] 优先级：normal | 内容：MySQL 宕机时的新消息 3

=== 消费者统计 ===
持久化到数据库的消息数：11
重试次数：6
降级处理的消息数：0
写入 pending_db 的消息数：3
```

---

## 🎯 核心场景解析

### 场景 1：MQ 服务正常

**流程：**
```
生产者 → Queue → 消费者 → MySQL
```

**结果：**
- ✅ 5 条消息成功发送到队列
- ✅ 队列正常接收所有消息

---

### 场景 2：MQ 服务宕机

**流程：**
```
生产者 → Outbox (暂存)
            ↓
        MQ 恢复后重放 → Queue
```

**容错机制：**
- ❌ MQ 不可用时，消息暂存到 **outbox**
- ✅ MQ 恢复后，自动重放 outbox 中的消息
- ✅ 保证消息不丢失

**数据：**
- 3 条消息暂存到 outbox
- MQ 恢复后全部重放到队列

---

### 场景 3：MySQL 宕机

**流程：**
```
生产者 → Queue → 消费者 → 重试 (2 次) → Pending DB
                                      ↓
                                  MySQL 恢复后刷新 → DB
```

**容错机制：**
- ❌ MySQL 不可用时，自动重试（最多 2 次）
- ✅ 重试失败后，写入 **pending_db** 暂存
- ✅ MySQL 恢复后，批量刷新到数据库
- ✅ 保证数据不丢失

**数据：**
- 6 次重试（3 条消息 × 2 次）
- 3 条消息写入 pending_db
- MySQL 恢复后全部刷新到数据库

---

## 📈 最终统计

| 指标 | 数值 | 说明 |
|------|------|------|
| **持久化到数据库** | 11 条 | 成功写入 MySQL 的消息总数 |
| **重试次数** | 6 次 | MySQL 宕机时的重试总次数 |
| **降级处理** | 0 条 | 未被降级的消息 |
| **写入 pending_db** | 3 条 | MySQL 宕机时暂存的消息 |
| **最终数据库记录** | 11 条 | DB 中的最终数据量 |
| **outbox 剩余** | 0 条 | 已全部重放 |
| **pending_db 剩余** | 0 条 | 已全部刷新 |

---

## 💡 关键设计

### 1. **本地持久化（文件模拟）**

使用文件系统模拟消息队列和数据库：
- `queue.txt` - 消息队列
- `outbox.txt` - MQ 宕机时的暂存区
- `pending_db.txt` - MySQL 宕机时的暂存区
- `db.txt` - 最终数据库
- `dead_letter.txt` - 降级处理的消息

### 2. **故障检测与恢复**

```java
// 生产者检测 MQ 状态
if (mqUp) {
    sendToQueue();
} else {
    spoolToOutbox();  // 暂存
}

// 消费者检测 MySQL 状态
if (mysqlUp) {
    persistToDb();
} else {
    retry(maxRetry);
    if (retryFailed) {
        writeToPendingDb();  // 暂存
    }
}
```

### 3. **自动重放机制**

- MQ 恢复后自动重放 outbox
- MySQL 恢复后自动刷新 pending_db
- 保证**至少一次投递**

### 4. **消息优先级与降级**

- 支持 `critical` 和 `normal` 优先级
- 积压超过阈值时，降级低优先级消息
- 保证关键业务优先处理

---

## 🔧 代码结构

```
rabbitmq/java/
├── src/main/java/com/testmq/rabbitmq/
│   ├── ProducerApp.java      # 生产者核心逻辑
│   ├── ConsumerApp.java      # 消费者核心逻辑
│   ├── Message.java          # 消息模型
│   ├── ScenarioSupport.java  # 场景支持工具
│   ├── SimpleProducer.java   # 简单生产者演示
│   ├── SimpleConsumer.java   # 简单消费者演示
│   └── DemoApp.java          # 完整场景演示
├── pom.xml                   # Maven 配置
└── runtime/                  # 运行时文件
    ├── queue.txt
    ├── outbox.txt
    ├── pending_db.txt
    ├── db.txt
    └── dead_letter.txt
```

---

## ✅ 演示总结

通过这个演示，我们成功展示了：

1. ✅ **MQ 正常时**：消息正常流转
2. ✅ **MQ 宕机时**：本地暂存，恢复后重放
3. ✅ **MySQL 宕机时**：重试 + 暂存，恢复后刷新
4. ✅ **双重保障**：outbox + pending_db 机制
5. ✅ **零消息丢失**：所有消息最终都成功处理

这个设计确保了在分布式系统中，即使 MQ 或 MySQL 出现故障，也能保证**消息不丢失**、**数据一致性**和**系统高可用**！🎉

---

## 🚀 快速运行

```bash
# 进入项目目录
cd D:\develops\git\github\cpp\test_mq\rabbitmq\java

# 编译
mvn clean compile

# 运行发送者
java -cp target/classes com.testmq.rabbitmq.SimpleProducer

# 运行接收者（新开终端）
java -cp target/classes com.testmq.rabbitmq.SimpleConsumer
```

或者运行完整演示：
```bash
java -cp target/classes com.testmq.rabbitmq.DemoApp
```
