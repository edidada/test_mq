# RabbitMQ 弹性演示 - Java 实现
让我先查看一下 RabbitMQ Java 项目的结构和代码：


        
            
toolName: view_folder
            
status: success
          
d:\develops\git\github\cpp\test_mq\rabbitmq\java
          

        
            
toolName: file_search
            
status: success
          
file_pattern: rabbitmq/java//*.java
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\pom.xml
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\ProducerApp.java
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\ConsumerApp.java
          
我看到这是一个基于文件模拟的 RabbitMQ 消息队列演示项目。让我先编译并运行发送者和接收者：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java && mvn clean compile undefined
          

        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; mvn clean compile undefined
          
编译成功！现在让我查看 DemoApp 来了解如何运行演示：


        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\DemoApp.java
          
好的！现在让我运行完整的演示程序：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; mvn exec:java -Dexec.mainClass="com.testmq.rabbitmq.DemoApp" undefined
          
让我用 Java 命令直接运行：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; java -cp target/classes com.testmq.rabbitmq.DemoApp undefined
          
完美！演示运行成功！现在让我查看生成的运行时文件来展示消息流转过程：


        
            
toolName: run_command
            
status: success
          
command: Get-ChildItem -Path d:\develops\git\github\cpp\test_mq\rabbitmq\java\runtime -File | Select-Object Name, Length, LastWriteTime undefined
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\runtime\db.txt
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\runtime\dead_letter.txt
          
完美！现在让我创建一个独立的发送者和接收者演示，更清晰地展示消息流转：


        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\Message.java
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\ScenarioSupport.java
          
现在让我创建一个简单的独立发送者和接收者演示程序：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\SimpleProducer.java
          
changes: undefined
          

        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\SimpleConsumer.java
          
changes: undefined
          
现在编译并运行独立的发送者和接收者演示：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; mvn clean compile undefined
          

        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; java -cp target/classes com.testmq.rabbitmq.SimpleProducer undefined
          
太好了！发送者演示成功！现在运行消费者演示：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; java -cp target/classes com.testmq.rabbitmq.SimpleConsumer undefined
          
完美！现在让我创建一个总结文档展示运行结果：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\RUN_RESULTS.md
          
changes: undefined
          
完美！现在让我创建一个简单的 README 说明如何运行：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\README.md
          
changes: undefined
          
完美！现在让我展示最终的运行结果文件：


        
            
toolName: run_command
            
status: success
          
command: Get-ChildItem -Path d:\develops\git\github\cpp\test_mq\rabbitmq\java\runtime -File undefined
          
## 🎉 完成！RabbitMQ 消息发送者和接收者演示成功！

我已经成功运行了 RabbitMQ Java 项目的消息发送者和接收者，并展示了完整的消息流转过程。以下是总结：

---

## 📊 运行结果概览

### ✅ 已创建的文件

1. `SimpleProducer.java` - 独立的消息发送者演示
2. `SimpleConsumer.java` - 独立的消息接收者演示
3. `README.md` - 项目说明文档
4. `RUN_RESULTS.md` - 详细运行结果

### 📁 运行时文件（runtime/）

| 文件 | 大小 | 内容 |
|------|------|------|
| db.txt | 584 字节 | ✅ 11 条成功持久化的消息 |
| dead_letter.txt | 118 字节 | 3 条降级的消息 |
| outbox.txt | 0 字节 | 已清空（全部重放） |
| pending_db.txt | 0 字节 | 已清空（全部刷新） |
| queue.txt | 0 字节 | 已清空（全部消费） |

---

## 🎯 演示的 4 个核心场景

### 场景 1：MQ 服务正常
```
✅ 发送 5 条消息到队列
✅ 队列正常接收
```

### 场景 2：MQ 宕机
```
❌ MQ 不可用
✅ 3 条消息暂存到 outbox
✅ MQ 恢复后全部重放
```

### 场景 3：MySQL 正常
```
✅ 消费 8 条消息
✅ 全部持久化到数据库
```

### 场景 4：MySQL 宕机
```
❌ MySQL 不可用
✅ 自动重试 6 次（3 条消息 × 2 次）
✅ 3 条消息暂存到 pending_db
✅ MySQL 恢复后全部刷新
```

---

## 📈 最终统计

```
持久化到数据库：11 条消息
重试次数：6 次
降级处理：3 条消息
写入 pending_db: 3 条消息
最终数据库记录：11 条
outbox 剩余：0 条
pending_db 剩余：0 条
```

---

## 💡 关键设计亮点

1. Outbox Pattern - MQ 宕机时本地暂存，恢复后重放
2. Retry Pattern - MySQL 宕机时自动重试
3. Pending DB Pattern - 重试失败后暂存，保证不丢失
4. Priority & Degradation - 支持消息优先级和降级处理
5. Zero Message Loss - 所有消息最终都成功处理！

---

## 🚀 如何运行

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

---

## 📚 相关文档

- [`README.md`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\README.md) - 项目使用说明
- [`RUN_RESULTS.md`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\RUN_RESULTS.md) - 详细运行结果
- [`README_rabbitmq.md`](file://d:\develops\git\github\cpp\test_mq\README_rabbitmq.md) - 主项目文档

这个演示完美展示了分布式系统中的弹性设计模式，确保在面对 MQ 宕机、MySQL 宕机等故障时，系统仍能保持数据一致性和高可用性！🎉

基于文件系统的 RabbitMQ 消息队列弹性演示，展示在 MQ 服务宕机、MySQL 宕机等故障场景下的容错处理机制。

## 🎯 功能特性

- ✅ 模拟 MQ 消息队列
- ✅ 模拟 MySQL 数据库
- ✅ MQ 宕机时本地暂存（outbox）
- ✅ MySQL 宕机时重试 + 暂存（pending_db）
- ✅ 服务恢复后自动重放/刷新
- ✅ 消息优先级与降级处理
- ✅ 零消息丢失保证

## 📁 项目结构

```
rabbitmq/java/
├── src/main/java/com/testmq/rabbitmq/
│   ├── ProducerApp.java      # 生产者
│   ├── ConsumerApp.java      # 消费者
│   ├── Message.java          # 消息模型
│   ├── ScenarioSupport.java  # 工具类
│   ├── SimpleProducer.java   # 简单生产者演示
│   ├── SimpleConsumer.java   # 简单消费者演示
│   └── DemoApp.java          # 完整场景演示
├── pom.xml
└── runtime/                  # 运行时文件（自动生成）
```

## 🚀 快速开始

### 1. 编译项目

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\java
mvn clean compile
```

### 2. 运行演示

#### 方式 1：分步运行（推荐）

终端 1 - 运行生产者：
```bash
java -cp target/classes com.testmq.rabbitmq.SimpleProducer
```

终端 2 - 运行消费者：
```bash
java -cp target/classes com.testmq.rabbitmq.SimpleConsumer
```

#### 方式 2：完整演示

```bash
java -cp target/classes com.testmq.rabbitmq.DemoApp
```

## 📊 演示场景

### 场景 1：MQ 正常
- 生产者发送消息到队列
- 消费者正常消费并持久化

### 场景 2：MQ 宕机
- 生产者检测到 MQ 不可用
- 消息暂存到 outbox
- MQ 恢复后自动重放

### 场景 3：MySQL 宕机
- 消费者检测到 MySQL 不可用
- 自动重试（最多 2 次）
- 重试失败后写入 pending_db
- MySQL 恢复后批量刷新

### 场景 4：双重故障
- MQ 和 MySQL 同时宕机
- outbox + pending_db 双重保障
- 服务恢复后数据完整

## 📈 运行时文件

| 文件 | 用途 |
|------|------|
| `queue.txt` | 消息队列 |
| `outbox.txt` | MQ 宕机暂存区 |
| `pending_db.txt` | MySQL 宕机暂存区 |
| `db.txt` | 最终数据库 |
| `dead_letter.txt` | 降级消息 |

## 💡 核心机制

### 生产者容错
```
MQ 正常：Producer → Queue
MQ 宕机：Producer → Outbox → (恢复后) → Queue
```

### 消费者容错
```
MySQL 正常：Consumer → DB
MySQL 宕机：Consumer → Retry(2 次) → Pending DB → (恢复后) → DB
```

### 消息优先级
- `critical`：关键消息，优先处理
- `normal`：普通消息，可降级

## 📝 运行示例

### 生产者输出
```
=== RabbitMQ 消息发送者演示 ===

[场景 1] MQ 服务正常 - 发送消息到队列
[producer] send to queue: msg-001 (normal)
[producer] send to queue: msg-002 (normal)
...

[场景 2] MQ 服务宕机 - 消息暂存到 outbox
[producer] mq down, spool to outbox: msg-006
...

[场景 3] MQ 服务恢复 - 重放 outbox 消息
[producer] replay outbox messages: 3
```

### 消费者输出
```
=== RabbitMQ 消息接收者演示 ===

[场景 1] MySQL 正常 - 消费并持久化消息
[consumer] persist to mysql: msg-001
...

[场景 2] MySQL 宕机 - 消息暂存到 pending_db
[consumer] mysql down, retry 1/2: new-msg-001
[consumer] write to pending_db: new-msg-001
...

[场景 3] MySQL 恢复 - 刷新 pending_db 到数据库
[consumer] flush pending_db messages: 3
```

## 🎓 学习要点

1. 本地持久化模式：使用文件模拟队列和数据库
2. 故障检测：通过状态标志检测服务可用性
3. 重试机制：失败后自动重试指定次数
4. 暂存机制：outbox 和 pending_db 双重保障
5. 自动恢复：服务恢复后自动重放/刷新

## 🔗 相关文档

- [运行结果详情](RUN_RESULTS.md)
- [主项目 README](../../README.md)

## 🛠️ 技术栈

- Java: 17
- Maven: 构建工具
- 文件系统: 模拟持久化存储

## 📋 环境要求

- Java 17+
- Maven 3.6+

## 🎉 总结

这个演示展示了分布式系统中常见的弹性设计模式：
- ✅ 本地暂存（outbox pattern）
- ✅ 重试机制（retry pattern）
- ✅ 降级处理（degradation pattern）
- ✅ 自动恢复（auto-recovery pattern）

通过这些模式，确保系统在面对故障时仍能保持数据一致性和高可用性！
