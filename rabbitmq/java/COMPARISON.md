# RabbitMQ 实现对比

本项目包含两种 RabbitMQ 实现方式：

## 📊 对比表

| 特性 | 文件系统模拟 | 真实 RabbitMQ |
|------|------------|-------------|
| **实现方式** | Java SE IO API | amqp-client 库 |
| **依赖** | 无 | com.rabbitmq:amqp-client:5.17.0 |
| **服务器** | 无需 | 需要 RabbitMQ Server |
| **协议** | 文件系统 | AMQP 0-9-1 |
| **端口** | - | 5672 (AMQP), 15672 (管理) |
| **持久化** | 文本文件 | 内存 + 磁盘 |
| **并发支持** | 单线程 | 多线程/多消费者 |
| **ACK 机制** | 无 | 手动/自动 ACK |
| **消息优先级** | 模拟 | 原生支持 (0-10) |
| **监控** | 查看文件 | Web 管理界面 |
| **适用场景** | 学习/演示/原型 | 生产环境 |

---

## 📁 文件系统模拟实现

### 代码位置
```
src/main/java/com/testmq/rabbitmq/
├── ProducerApp.java
├── ConsumerApp.java
├── SimpleProducer.java
├── SimpleConsumer.java
└── DemoApp.java
```

### 核心原理

**使用文件系统模拟队列:**
```java
// 队列 = 文本文件
Path queueFile = runtimeDir.resolve("queue.txt");

// 发送消息 = 追加写入文件
Files.write(queueFile, 
    (message + "\n").getBytes(), 
    StandardOpenOption.APPEND);

// 消费消息 = 读取并清空文件
List<String> messages = Files.readAllLines(queueFile);
Files.write(queueFile, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
```

### 优点
✅ 无需安装 RabbitMQ  
✅ 开箱即用，零配置  
✅ 易于理解和调试  
✅ 适合学习消息队列概念  

### 缺点
❌ 不是真正的消息队列  
❌ 不支持并发  
❌ 没有 ACK 机制  
❌ 性能受限  

### 运行示例
```bash
# 编译
mvn clean compile

# 运行生产者
java -cp target/classes com.testmq.rabbitmq.SimpleProducer

# 运行消费者
java -cp target/classes com.testmq.rabbitmq.SimpleConsumer
```

---

## 🚀 真实 RabbitMQ 实现

### 代码位置
```
src/main/java/com/testmq/rabbitmq/
├── TrueProducer.java
└── TrueConsumer.java
```

### 核心原理

**使用 AMQP 协议与 RabbitMQ 通信:**
```java
// 1. 创建连接工厂
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");

// 2. 建立连接和通道
Connection connection = factory.newConnection();
Channel channel = connection.createChannel();

// 3. 声明队列
channel.queueDeclare("test_queue", true, false, false, null);

// 4. 发送消息
channel.basicPublish("", "test_queue", properties, message.getBytes());

// 5. 消费消息
channel.basicConsume("test_queue", false, deliverCallback, cancelCallback);
```

### 优点
✅ 真正的消息队列系统  
✅ 支持高并发  
✅ 完整的 ACK 机制  
✅ 支持优先级、TTL、死信队列等高级特性  
✅ 可用于生产环境  

### 缺点
❌ 需要安装 RabbitMQ Server  
❌ 需要配置和管理  
❌ 学习曲线较陡  

### 安装 RabbitMQ

**使用 Docker (推荐):**
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

**访问管理界面:** http://localhost:15672  
- 用户名：`guest`
- 密码：`guest`

### 运行示例
```bash
# 1. 确保 RabbitMQ 已启动
docker ps | grep rabbitmq

# 2. 编译
mvn clean compile

# 3. 运行生产者
java -cp target/classes com.testmq.rabbitmq.TrueProducer

# 4. 运行消费者
java -cp target/classes com.testmq.rabbitmq.TrueConsumer
```

---

## 🎯 使用建议

### 选择文件系统模拟，如果：
- ✅ 学习消息队列基本概念
- ✅ 快速原型开发
- ✅ 演示容错机制（outbox、pending_db）
- ✅ 没有条件安装 RabbitMQ

### 选择真实 RabbitMQ，如果：
- ✅ 开发生产环境应用
- ✅ 需要高并发支持
- ✅ 需要完整的消息队列特性
- ✅ 进行性能测试

---

## 📝 依赖配置

### pom.xml

```xml
<dependencies>
    <!-- 真实 RabbitMQ 实现需要这个依赖 -->
    <dependency>
        <groupId>com.rabbitmq</groupId>
        <artifactId>amqp-client</artifactId>
        <version>5.17.0</version>
    </dependency>
</dependencies>
```

---

## 🔍 代码对比

### 发送消息

**文件系统模拟:**
```java
public void send(Message message) throws IOException {
    Path target = mqUp ? queueFile : outboxFile;
    ScenarioSupport.appendLine(target, message.encode());
    System.out.println("[producer] send: " + message.getMessageId());
}
```

**真实 RabbitMQ:**
```java
public void sendMessage(String messageId, String priority, String body) throws Exception {
    String message = messageId + "|" + priority + "|" + body;
    
    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
        .deliveryMode(2)
        .priority("critical".equals(priority) ? 10 : 0)
        .build();
    
    channel.basicPublish("", QUEUE_NAME, props, message.getBytes());
    System.out.println("[producer] sent: " + messageId);
}
```

### 消费消息

**文件系统模拟:**
```java
public void processQueue(int threshold, int maxRetry) throws IOException {
    List<Message> messages = ScenarioSupport.readMessages(queueFile);
    ScenarioSupport.truncate(queueFile);
    
    for (Message message : messages) {
        persistWithRetry(message, maxRetry);
    }
}
```

**真实 RabbitMQ:**
```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    processMessage(message);
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
};

channel.basicConsume(QUEUE_NAME, false, deliverCallback, cancelCallback -> {});
```

---

## 🎓 学习路径建议

1. **第一步**: 使用文件系统模拟理解基本概念
   - 消息队列是什么
   - 生产者和消费者模式
   - 容错机制（outbox、pending_db）

2. **第二步**: 学习真实 RabbitMQ
   - AMQP 协议
   - Connection、Channel、Queue
   - ACK 机制
   - 持久化

3. **第三步**: 进阶特性
   - Exchange（Direct、Fanout、Topic）
   - 死信队列
   - 延迟队列
   - 集群和高可用

---

## 📚 相关文档

- [文件系统模拟 README](README.md)
- [真实 RabbitMQ README](README_REAL_RABBITMQ.md)
- [运行结果](RUN_RESULTS.md)

---

## 🎉 总结

本项目同时提供了两种实现方式：

1. **文件系统模拟** - 适合学习和演示
2. **真实 RabbitMQ** - 使用 amqp-client 5.17.0，适合生产环境

两种实现各有优劣，可以根据需求选择合适的方案。建议先用文件系统模拟理解概念，再学习真实的 RabbitMQ 实现！🚀
