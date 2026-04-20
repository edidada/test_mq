# 真实 RabbitMQ Python 示例 - 使用 pika 库

这个目录包含使用真正的 RabbitMQ Python 客户端库 (`pika`) 的示例代码。

## 📦 依赖

安装依赖：

```bash
pip install -r requirements.txt
```

或者手动安装：

```bash
pip install pika==1.3.1
```

## 🚀 前提条件

### 安装 RabbitMQ

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

### 验证 RabbitMQ 运行

- **AMQP 端口**: 5672
- **管理界面**: http://localhost:15672
  - 用户名：`guest`
  - 密码：`guest`

## 📝 示例代码

### true_producer.py - 真正的生产者

```python
import pika

# 创建连接
credentials = pika.PlainCredentials('guest', 'guest')
parameters = pika.ConnectionParameters(host='localhost', credentials=credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()

# 声明队列
channel.queue_declare(queue='test_queue', durable=True)

# 发送消息
message = "Hello RabbitMQ!"
channel.basic_publish(
    exchange='',
    routing_key='test_queue',
    body=message,
    properties=pika.BasicProperties(
        delivery_mode=2,  # 持久化
        priority=0
    )
)

connection.close()
```

### true_consumer.py - 真正的消费者

```python
import pika

# 创建连接
credentials = pika.PlainCredentials('guest', 'guest')
parameters = pika.ConnectionParameters(host='localhost', credentials=credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()

# 声明队列
channel.queue_declare(queue='test_queue', durable=True)

# 设置 QoS
channel.basic_qos(prefetch_count=1)

# 消费消息
def callback(channel, method, properties, body):
    message = body.decode('utf-8')
    print(f"Received: {message}")
    
    # 处理业务逻辑
    process_message(message)
    
    # 手动 ACK
    channel.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_consume(queue='test_queue', on_message_callback=callback, auto_ack=False)

print("Waiting for messages...")
channel.start_consuming()
```

## ▶️ 运行步骤

### 1. 安装依赖

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\python
pip install -r requirements.txt
```

### 2. 运行生产者

```bash
python true_producer.py
```

**预期输出:**
```
[producer] connecting to RabbitMQ at localhost:5672...
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
python true_consumer.py
```

**预期输出:**
```
[consumer] connecting to RabbitMQ at localhost:5672...
[consumer] connected successfully
[consumer] waiting for messages...
[consumer] received: msg-001|normal|payload #1
  [processing] msg-001 - priority: normal, body: payload #1
[consumer] processed message #1
...
[consumer] processed 10 messages
[consumer] done!
[consumer] connection closed
```

## 🔍 关键概念

### 1. 连接管理

```python
credentials = pika.PlainCredentials('guest', 'guest')
parameters = pika.ConnectionParameters(
    host='localhost',
    port=5672,
    credentials=credentials,
    heartbeat=600,
    blocked_connection_timeout=300
)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
```

### 2. 队列声明

```python
channel.queue_declare(
    queue='test_queue',
    durable=True  # 持久化
)
```

### 3. 发送消息

```python
channel.basic_publish(
    exchange='',
    routing_key='queue_name',
    body=message_body,
    properties=pika.BasicProperties(
        content_type='text/plain',
        delivery_mode=2,  # 持久化
        priority=10       # 优先级 0-10
    )
)
```

### 4. 消费消息

```python
def callback(channel, method, properties, body):
    message = body.decode('utf-8')
    print(f"Received: {message}")
    
    # 处理业务
    process_message(message)
    
    # 手动 ACK
    channel.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_consume(
    queue='test_queue',
    on_message_callback=callback,
    auto_ack=False  # 手动 ACK
)

channel.start_consuming()
```

### 5. QoS 设置

```python
channel.basic_qos(prefetch_count=1)  # 每次只处理一条消息
```

## 📊 消息属性

```python
properties = pika.BasicProperties(
    content_type='text/plain',        # 内容类型
    delivery_mode=2,                  # 1=非持久化，2=持久化
    priority=10,                      # 优先级 0-10
    correlation_id='corr-id',         # 关联 ID
    expiration='60000'                # 过期时间 (ms)
)
```

## 🎯 与文件系统模拟的对比

| 特性 | 文件系统模拟 | 真实 RabbitMQ (pika) |
|------|------------|---------------------|
| **依赖** | 无 | pika |
| **安装** | 无需安装 | 需要 RabbitMQ 服务器 |
| **协议** | 文件系统 | AMQP 0-9-1 |
| **持久化** | 文本文件 | 内存 + 磁盘 |
| **并发** | 单线程 | 多线程/多消费者 |
| **ACK 机制** | 无 | 手动/自动 ACK |
| **优先级** | 模拟 | 原生支持 (0-10) |
| **监控** | 查看文件 | Web 管理界面 |
| **适用场景** | 学习/演示 | 生产环境 |

## 🔧 故障处理

### 连接失败
```
pika.exceptions.AMQPConnectionError
```
**解决方案:** 确保 RabbitMQ 服务已启动
```bash
# 检查 Docker 容器
docker ps | grep rabbitmq

# 检查服务状态 (Ubuntu)
sudo systemctl status rabbitmq-server
```

### 认证失败
```
pika.exceptions.ProbableAuthenticationError
```
**解决方案:** 检查用户名密码是否正确（默认：guest/guest）

### 队列不存在
```
pika.exceptions.ChannelClosedByBroker
```
**解决方案:** 确保先声明队列再发送/消费消息

## 📚 进阶用法

### 批量发送

```python
for i in range(100):
    message = f"Message {i}"
    channel.basic_publish(
        exchange='',
        routing_key='test_queue',
        body=message
    )
```

### 确认送达

```python
def confirm_callback(method_frame):
    print(f"Message confirmed: {method_frame.delivery_tag}")

channel.confirm_delivery()
channel.basic_publish(
    exchange='',
    routing_key='test_queue',
    body=message,
    properties=pika.BasicProperties(delivery_mode=2)
)
```

### 死信队列

```python
# 声明死信队列
channel.queue_declare(queue='dead_letter_queue', durable=True)

# 声明主队列，设置死信路由
channel.queue_declare(
    queue='main_queue',
    durable=True,
    arguments={
        'x-dead-letter-exchange': '',
        'x-dead-letter-routing-key': 'dead_letter_queue'
    }
)
```

## 📚 参考资料

- [Pika 官方文档](https://pika.readthedocs.io/)
- [RabbitMQ Python 客户端](https://www.rabbitmq.com/python-client.html)
- [RabbitMQ 官方教程](https://www.rabbitmq.com/getstarted.html)

## 🎉 总结

这个示例展示了如何使用 **真正的 RabbitMQ Python 客户端 (pika)** 进行消息收发：

✅ 使用 `pika 1.3.1`  
✅ 真实的 AMQP 协议通信  
✅ 支持持久化、优先级、ACK 机制  
✅ 可用于生产环境  

与文件系统模拟相比，这是**真正的消息队列实现**！🚀
