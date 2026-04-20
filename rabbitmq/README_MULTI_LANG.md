# RabbitMQ 多语言实现对比

本项目提供 **Java**、**Python** 和 **C++** 三种语言的 RabbitMQ 实现，每种语言都包含：
1. **文件系统模拟** - 适合学习和演示
2. **真实 RabbitMQ** - 使用官方客户端库，适合生产环境

---

## 📊 总览表

| 语言 | 文件系统模拟 | 真实 RabbitMQ | 客户端库 |
|------|------------|-------------|---------|
| **Java** | ✅ SimpleProducer.java | ✅ TrueProducer.java | amqp-client 5.17.0 |
| **Python** | ✅ producer.py | ✅ true_producer.py | pika 1.3.1 |
| **C++** | ✅ demo.cpp | ✅ true_producer.cpp | rabbitmq-c |

---

## 🗂️ 目录结构

```
rabbitmq/
├── java/
│   ├── pom.xml
│   ├── src/main/java/com/testmq/rabbitmq/
│   │   ├── ProducerApp.java          # 文件系统模拟
│   │   ├── ConsumerApp.java          # 文件系统模拟
│   │   ├── SimpleProducer.java       # 文件系统模拟演示
│   │   ├── SimpleConsumer.java       # 文件系统模拟演示
│   │   ├── TrueProducer.java         # 真实 RabbitMQ ✨
│   │   └── TrueConsumer.java         # 真实 RabbitMQ ✨
│   ├── README.md                     # 文件系统模拟文档
│   ├── README_REAL_RABBITMQ.md       # 真实 RabbitMQ 文档
│   └── COMPARISON.md                 # Java 实现对比
│
├── python/
│   ├── producer.py                   # 文件系统模拟
│   ├── consumer.py                   # 文件系统模拟
│   ├── true_producer.py              # 真实 RabbitMQ ✨
│   ├── true_consumer.py              # 真实 RabbitMQ ✨
│   ├── requirements.txt              # Python 依赖
│   └── README_REAL_RABBITMQ.md       # 真实 RabbitMQ 文档
│
└── cpp/
    ├── CMakeLists.txt
    ├── src/
    │   ├── demo.cpp                  # 文件系统模拟
    │   ├── true_producer.cpp         # 真实 RabbitMQ ✨
    │   └── true_consumer.cpp         # 真实 RabbitMQ ✨
    └── README_REAL_RABBITMQ.md       # 真实 RabbitMQ 文档
```

---

## 🚀 快速开始

### Java 版本

#### 1. 编译项目

```bash
cd rabbitmq/java
mvn clean compile
```

#### 2. 运行真实 RabbitMQ 示例

```bash
# 运行生产者
java -cp target/classes com.testmq.rabbitmq.TrueProducer

# 运行消费者
java -cp target/classes com.testmq.rabbitmq.TrueConsumer
```

### Python 版本

#### 1. 安装依赖

```bash
cd rabbitmq/python
pip install -r requirements.txt
```

#### 2. 运行真实 RabbitMQ 示例

```bash
# 运行生产者
python true_producer.py

# 运行消费者
python true_consumer.py
```

### C++ 版本

#### 1. 安装 rabbitmq-c 库

```bash
# Ubuntu/Debian
sudo apt install librabbitmq-dev

# Windows (vcpkg)
vcpkg install rabbitmq-c:x64-windows
```

#### 2. 编译项目

```bash
cd rabbitmq/cpp
mkdir build && cd build
cmake ..
cmake --build . --config Release
```

#### 3. 运行真实 RabbitMQ 示例

```bash
# 运行生产者
./Release/true_producer.exe

# 运行消费者
./Release/true_consumer.exe
```

---

## 📦 依赖对比

### Java (Maven)

```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.17.0</version>
</dependency>
```

### Python (pip)

```txt
pika==1.3.1
```

### C++ (CMake + pkg-config)

```cmake
find_package(PkgConfig REQUIRED)
pkg_check_modules(RABBITMQ REQUIRED rabbitmq)
target_link_libraries(your_target ${RABBITMQ_LIBRARIES})
```

或者系统包：
```bash
# Ubuntu/Debian
sudo apt install librabbitmq-dev

# Windows (vcpkg)
vcpkg install rabbitmq-c:x64-windows

# macOS (Homebrew)
brew install rabbitmq-c
```

---

## 💻 代码对比

### 连接 RabbitMQ

**Java:**
```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");

Connection connection = factory.newConnection();
Channel channel = connection.createChannel();
```

**Python:**
```python
credentials = pika.PlainCredentials('guest', 'guest')
parameters = pika.ConnectionParameters(host='localhost', credentials=credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
```

**C++:**
```cpp
amqp_connection_state_t conn = amqp_new_connection();
amqp_socket_t* socket = amqp_tcp_socket_new(conn);
amqp_socket_open(socket, "localhost", 5672);

amqp_login(conn, "localhost", 5672, 0, 131072, 0,
           AMQP_SASL_METHOD_PLAIN, "guest", "guest");
amqp_channel_open(conn, 1);
```

### 发送消息

**Java:**
```java
String message = "Hello RabbitMQ!";
channel.basicPublish(
    "", 
    "test_queue", 
    new AMQP.BasicProperties.Builder()
        .deliveryMode(2)
        .build(),
    message.getBytes(StandardCharsets.UTF_8)
);
```

**Python:**
```python
message = "Hello RabbitMQ!"
channel.basic_publish(
    exchange='',
    routing_key='test_queue',
    body=message,
    properties=pika.BasicProperties(delivery_mode=2)
)
```

**C++:**
```cpp
const char* message = "Hello RabbitMQ!";
amqp_basic_properties_t props;
props._flags = AMQP_BASIC_DELIVERY_MODE_FLAG;
props.delivery_mode = 2;

amqp_basic_publish(conn, channel,
                  amqp_empty_bytes,
                  amqp_cstring_bytes("test_queue"),
                  0, 0,
                  &props,
                  amqp_cstring_bytes(message));
```

### 消费消息

**Java:**
```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    System.out.println("Received: " + message);
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
};

channel.basicConsume("test_queue", false, deliverCallback, consumerTag -> {});
```

**Python:**
```python
def callback(channel, method, properties, body):
    message = body.decode('utf-8')
    print(f"Received: {message}")
    channel.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_consume(queue='test_queue', on_message_callback=callback, auto_ack=False)
channel.start_consuming()
```

**C++:**
```cpp
amqp_basic_consume(conn, channel,
                  amqp_cstring_bytes("test_queue"),
                  amqp_empty_bytes, 0, 0, 0);

amqp_envelope_t envelope;
amqp_rpc_reply_t ret = amqp_consume_message(conn, &envelope, nullptr, 0);

if (ret.reply_type == AMQP_RESPONSE_NORMAL) {
    std::string message(
        static_cast<char*>(envelope.message.body.bytes),
        envelope.message.body.len
    );
    std::cout << "Received: " << message << std::endl;
    amqp_basic_ack(conn, channel, envelope.delivery_tag, 0);
    amqp_destroy_envelope(&envelope);
}
```

---

## 🎯 实现方式对比

### 文件系统模拟

**优点:**
- ✅ 无需安装 RabbitMQ
- ✅ 开箱即用，零配置
- ✅ 易于理解和调试
- ✅ 适合学习消息队列概念

**缺点:**
- ❌ 不是真正的消息队列
- ❌ 不支持并发
- ❌ 没有 ACK 机制
- ❌ 性能受限

### 真实 RabbitMQ

**优点:**
- ✅ 真正的消息队列系统
- ✅ 支持高并发
- ✅ 完整的 ACK 机制
- ✅ 支持优先级、TTL、死信队列等高级特性
- ✅ 可用于生产环境

**缺点:**
- ❌ 需要安装 RabbitMQ Server
- ❌ 需要配置和管理
- ❌ 学习曲线较陡

---

## 📚 学习路径建议

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

## 🔧 故障处理

### RabbitMQ 未启动

**错误:**
```
Connection refused (Java)
AMQPConnectionError (Python)
```

**解决方案:**
```bash
# 使用 Docker 启动
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# 访问管理界面
http://localhost:15672 (guest/guest)
```

---

## 📖 相关文档

### Java
- [文件系统模拟 README](java/README.md)
- [真实 RabbitMQ README](java/README_REAL_RABBITMQ.md)
- [实现对比](java/COMPARISON.md)

### Python
- [真实 RabbitMQ README](python/README_REAL_RABBITMQ.md)

### C++
- [真实 RabbitMQ README](cpp/README_REAL_RABBITMQ.md)

---

## 🎉 总结

本项目同时提供了 **Java**、**Python** 和 **C++** 三种语言的实现，每种语言都有：

1. **文件系统模拟** - 适合学习和演示概念
2. **真实 RabbitMQ** - 使用官方客户端库（amqp-client / pika / rabbitmq-c）

两种实现各有优劣，可以根据需求选择合适的方案。建议先用文件系统模拟理解概念，再学习真实的 RabbitMQ 实现！🚀

---

## 📊 关键特性对比

| 特性 | 文件系统模拟 | 真实 RabbitMQ |
|------|------------|-------------|
| **协议** | 文件系统 | AMQP 0-9-1 |
| **持久化** | 文本文件 | 内存 + 磁盘 |
| **并发** | 单线程 | 多线程/多消费者 |
| **ACK** | 无 | 手动/自动 |
| **优先级** | 模拟 | 原生支持 (0-10) |
| **监控** | 查看文件 | Web 管理界面 |
| **适用场景** | 学习/演示 | 生产环境 |

选择适合你的方案，开始消息队列之旅吧！🎓
