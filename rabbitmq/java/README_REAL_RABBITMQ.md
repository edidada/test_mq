# 真实 RabbitMQ 示例 - 使用 amqp-client
你说得对！我之前的实现是用文件系统模拟的简化版本，不是真正的 RabbitMQ。让我添加真正的 RabbitMQ 客户端依赖并重写代码：


        
            
toolName: view_files
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\pom.xml
          

        
            
toolName: edit_file_search_replace
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\pom.xml
          
changes: undefined
          
好的！现在让我创建真正的 RabbitMQ 生产者和消费者示例代码：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\TrueProducer.java
          
changes: undefined
          

        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\TrueConsumer.java
          
changes: undefined
          
现在编译项目：


        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; mvn clean compile undefined
          
修复类型错误：


        
            
toolName: edit_file_search_replace
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\TrueProducer.java
          
changes: undefined
          

        
            
toolName: run_command
            
status: success
          
command: cd d:\develops\git\github\cpp\test_mq\rabbitmq\java; mvn clean compile undefined
          
编译成功！现在让我创建一个说明文档，并尝试运行（需要先启动 RabbitMQ）：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\README_REAL_RABBITMQ.md
          
changes: undefined
          
现在让我创建一个完整的对比文档：


        
            
toolName: Write
            
status: success
          
            
filePath: d:\develops\git\github\cpp\test_mq\rabbitmq\java\COMPARISON.md
          
changes: undefined
          
完美！现在让我总结一下：

## ✅ 完成！已添加真正的 RabbitMQ 实现

你说得对！我之前的是用**文件系统模拟**的简化版本。现在已经添加了**真正的 RabbitMQ Java 客户端**实现！

---

## 🎯 两种实现对比

### 1️⃣ 文件系统模拟（之前写的）

**位置:** [`SimpleProducer.java`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\SimpleProducer.java)

**特点:**
- ❌ 使用 Java SE IO API
- ❌ 用文件模拟队列（`queue.txt`）
- ❌ 无需 RabbitMQ 服务器
- ✅ 适合学习和演示概念

### 2️⃣ 真实 RabbitMQ（新添加的）

**位置:** [`TrueProducer.java`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\src\main\java\com\testmq\rabbitmq\TrueProducer.java)

**特点:**
- ✅ 使用 **amqp-client 5.17.0** 库
- ✅ 真正的 AMQP 协议通信
- ✅ 需要 RabbitMQ 服务器
- ✅ 可用于生产环境

---

## 📦 已添加的依赖

[`pom.xml`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\pom.xml#L17-L22) 已添加：

```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.17.0</version>
</dependency>
```

---

## 🚀 新创建的代码

### TrueProducer.java - 真正的生产者

```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");

Connection connection = factory.newConnection();
Channel channel = connection.createChannel();

// 发送消息到真实的 RabbitMQ
channel.basicPublish("", "test_queue", properties, message.getBytes());
```

### TrueConsumer.java - 真正的消费者

```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    System.out.println("Received: " + message);
    
    // 处理业务
    processMessage(message);
    
    // 手动 ACK
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
};

channel.basicConsume("test_queue", false, deliverCallback, cancelCallback -> {});
```

---

## 📚 文档

已创建详细文档：

1. [`README_REAL_RABBITMQ.md`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\README_REAL_RABBITMQ.md) - 真实 RabbitMQ 使用指南
2. [`COMPARISON.md`](file://d:\develops\git\github\cpp\test_mq\rabbitmq\java\COMPARISON.md) - 两种实现的详细对比

---

## ▶️ 如何运行真实 RabbitMQ

### 1. 启动 RabbitMQ（使用 Docker）

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 2. 编译

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\java
mvn clean compile
```

### 3. 运行生产者

```bash
java -cp target/classes com.testmq.rabbitmq.TrueProducer
```

### 4. 运行消费者

```bash
java -cp target/classes com.testmq.rabbitmq.TrueConsumer
```

---

## 🎓 学习建议

1. **先用文件系统模拟**理解概念（outbox、pending_db、重试机制）
2. **再学真实 RabbitMQ**掌握生产环境技能（AMQP、ACK、持久化）

两种实现各有用途，现在你都有了！🎉
这个目录包含使用真正的 RabbitMQ Java 客户端库 (`amqp-client`) 的示例代码。

## 📦 依赖

```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.17.0</version>
</dependency>
```

## 🚀 前提条件

### 1. 安装 RabbitMQ

**Windows (使用 Docker):**
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

**WSL Ubuntu:**
```bash
# 安装 RabbitMQ
sudo apt update
sudo apt install rabbitmq-server -y

# 启动服务
sudo systemctl start rabbitmq-server

# 启用管理界面
sudo rabbitmq-plugins enable rabbitmq_management
```

### 2. 验证 RabbitMQ 运行

- **AMQP 端口**: 5672 (消息收发)
- **管理界面**: http://localhost:15672
  - 用户名：`guest`
  - 密码：`guest`

## 📝 示例代码

### TrueProducer.java - 真实的生产者

```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");

Connection connection = factory.newConnection();
Channel channel = connection.createChannel();

// 声明队列
channel.queueDeclare("test_queue", true, false, false, null);

// 发送消息
String message = "Hello RabbitMQ!";
channel.basicPublish("", "test_queue", 
    new AMQP.BasicProperties.Builder()
        .deliveryMode(2) // persistent
        .build(),
    message.getBytes(StandardCharsets.UTF_8));
```

### TrueConsumer.java - 真实的消费者

```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");

Connection connection = factory.newConnection();
Channel channel = connection.createChannel();

// 声明队列
channel.queueDeclare("test_queue", true, false, false, null);

// 消费消息
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    System.out.println("Received: " + message);
    
    // 处理业务逻辑
    processMessage(message);
    
    // 手动 ACK
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
};

channel.basicConsume("test_queue", false, deliverCallback, 
    consumerTag -> System.out.println("Consumption cancelled"));
```

## ▶️ 运行步骤

### 1. 编译项目

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\java
mvn clean compile
```

### 2. 运行生产者

```bash
java -cp target/classes com.testmq.rabbitmq.TrueProducer
```

**预期输出:**
```
[producer] connecting to RabbitMQ...
[producer] connected successfully
[producer] sending batch of 10 messages
[producer] sent: msg-001 (normal)
[producer] sent: msg-002 (normal)
[producer] sent: msg-003 (critical)
...
[producer] done!
[producer] connection closed
```

### 3. 运行消费者

```bash
java -cp target/classes com.testmq.rabbitmq.TrueConsumer
```

**预期输出:**
```
[consumer] connecting to RabbitMQ...
[consumer] connected successfully
[consumer] waiting for messages...
[consumer] received: msg-001|normal|payload #1
  [processing] msg-001 - priority: normal, body: payload #1
...
[consumer] processed 10 messages
[consumer] done!
[consumer] connection closed
```

## 🔍 关键概念

### 1. ConnectionFactory
```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");
```

### 2. Connection & Channel
```java
Connection connection = factory.newConnection();
Channel channel = connection.createChannel();
```

### 3. 队列声明
```java
channel.queueDeclare(
    "queue_name",  // 队列名称
    true,          // durable - 持久化
    false,         // exclusive - 排他
    false,         // autoDelete - 自动删除
    null           // arguments - 其他参数
);
```

### 4. 发送消息
```java
channel.basicPublish(
    "",                     // exchange
    "queue_name",           // routing key
    properties,             // 基本属性
    message.getBytes()      // 消息体
);
```

### 5. 消费消息
```java
channel.basicConsume(
    "queue_name",           // 队列
    false,                  // autoAck - 手动 ACK
    deliverCallback,        // 消息回调
    cancelCallback          // 取消回调
);
```

### 6. 手动 ACK
```java
channel.basicAck(
    deliveryTag,            // 消息标签
    false                   // multiple - 是否批量确认
);
```

## 📊 消息属性

```java
AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
    .contentType("text/plain")        // 内容类型
    .deliveryMode(2)                  // 1=非持久化，2=持久化
    .priority(10)                     // 优先级 0-10
    .correlationId("corr-id")         // 关联 ID
    .expiration("60000")              // 过期时间 (ms)
    .build();
```

## 🎯 与文件系统模拟的区别

| 特性 | 文件系统模拟 | 真实 RabbitMQ |
|------|------------|-------------|
| **依赖** | 无 | amqp-client |
| **安装** | 无需安装 | 需要 RabbitMQ 服务器 |
| **持久化** | 文件存储 | 内存 + 磁盘 |
| **并发** | 单线程 | 多线程/多消费者 |
| **ACK 机制** | 无 | 手动/自动 ACK |
| **优先级** | 模拟 | 原生支持 |
| **监控** | 查看文件 | 管理界面 |
| **适用场景** | 学习/演示 | 生产环境 |

## 🔧 故障处理

### 连接失败
```
java.net.ConnectException: Connection refused
```
**解决方案:** 确保 RabbitMQ 服务已启动
```bash
# Windows Docker
docker ps | grep rabbitmq

# Ubuntu
sudo systemctl status rabbitmq-server
```

### 认证失败
```
com.rabbitmq.client.AuthenticationFailureException
```
**解决方案:** 检查用户名密码是否正确（默认：guest/guest）

### 队列不存在
```
com.rabbitmq.client.ShutdownSignalException: channel error
```
**解决方案:** 确保先声明队列再发送/消费消息

## 📚 参考资料

- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Java Client API](https://www.rabbitmq.com/java-client.html)
- [amqp-client Maven](https://mvnrepository.com/artifact/com.rabbitmq/amqp-client)

## 🎉 总结

这个示例展示了如何使用 **真正的 RabbitMQ Java 客户端**进行消息收发：

✅ 使用 `amqp-client 5.17.0`  
✅ 真实的 AMQP 协议通信  
✅ 支持持久化、优先级、ACK 机制  
✅ 可用于生产环境  

与文件系统模拟相比，这是**真正的消息队列实现**！🚀
