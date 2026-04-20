# 真实 RabbitMQ C++ 示例 - 使用 rabbitmq-c 库

这个目录包含使用真正的 RabbitMQ C/C++ 客户端库 (`rabbitmq-c`) 的示例代码。

## 📦 依赖

### 安装 rabbitmq-c 库

**Windows (使用 vcpkg):**
```bash
# 安装 vcpkg
git clone https://github.com/Microsoft/vcpkg.git
cd vcpkg
.\bootstrap-vcpkg.bat

# 安装 rabbitmq-c
.\vcpkg install rabbitmq-c:x64-windows
.\vcpkg integrate install
```

**WSL Ubuntu / Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install librabbitmq-dev -y

# 或者从源码编译
git clone https://github.com/alanxz/rabbitmq-c.git
cd rabbitmq-c
mkdir build && cd build
cmake ..
cmake --build . --config Release
sudo cmake --install .
```

**macOS (使用 Homebrew):**
```bash
brew install rabbitmq-c
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

### true_producer.cpp - 真正的生产者

```cpp
#include <amqp.h>
#include <amqp_tcp_socket.h>

// 创建连接
amqp_connection_state_t conn = amqp_new_connection();
amqp_socket_t* socket = amqp_tcp_socket_new(conn);
amqp_socket_open(socket, "localhost", 5672);

// 登录
amqp_login(conn, "localhost", 5672, 0, 131072, 0,
           AMQP_SASL_METHOD_PLAIN, "guest", "guest");

// 打开通道
amqp_channel_open(conn, 1);

// 声明队列
amqp_queue_declare(conn, 1, amqp_cstring_bytes("test_queue"),
                  true, false, false, false, amqp_empty_table);

// 发送消息
const char* message = "Hello RabbitMQ!";
amqp_basic_properties_t props;
props._flags = AMQP_BASIC_DELIVERY_MODE_FLAG;
props.delivery_mode = 2;  // 持久化

amqp_basic_publish(conn, 1,
                  amqp_empty_bytes,
                  amqp_cstring_bytes("test_queue"),
                  0, 0,
                  &props,
                  amqp_cstring_bytes(message));

// 关闭连接
amqp_channel_close(conn, 1, AMQP_REPLY_SUCCESS);
amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
amqp_destroy_connection(conn);
```

### true_consumer.cpp - 真正的消费者

```cpp
#include <amqp.h>
#include <amqp_tcp_socket.h>

// 创建连接（同上）
amqp_connection_state_t conn = amqp_new_connection();
// ... 连接代码 ...

// 声明队列
amqp_queue_declare(conn, 1, amqp_cstring_bytes("test_queue"),
                  true, false, false, false, amqp_empty_table);

// 设置 QoS
amqp_basic_qos(conn, 1, 1);

// 开始消费
amqp_basic_consume(conn, 1,
                  amqp_cstring_bytes("test_queue"),
                  amqp_empty_bytes,
                  0, 0, 0);

// 接收消息
amqp_envelope_t envelope;
amqp_rpc_reply_t ret = amqp_consume_message(conn, &envelope, nullptr, 0);

if (ret.reply_type == AMQP_RESPONSE_NORMAL) {
    // 处理消息
    std::string message(
        static_cast<char*>(envelope.message.body.bytes),
        envelope.message.body.len
    );
    std::cout << "Received: " << message << std::endl;
    
    // 手动 ACK
    amqp_basic_ack(conn, 1, envelope.delivery_tag, 0);
    
    // 销毁 envelope
    amqp_destroy_envelope(&envelope);
}

// 关闭连接
amqp_channel_close(conn, 1, AMQP_REPLY_SUCCESS);
amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
amqp_destroy_connection(conn);
```

## ▶️ 运行步骤

### 1. 编译项目

```bash
cd D:\develops\git\github\cpp\test_mq\rabbitmq\cpp
mkdir build && cd build
cmake ..
cmake --build . --config Release
```

### 2. 运行生产者

```bash
./Release/true_producer.exe
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
./Release/true_consumer.exe
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

```cpp
// 创建连接
amqp_connection_state_t conn = amqp_new_connection();

// 创建 TCP socket
amqp_socket_t* socket = amqp_tcp_socket_new(conn);

// 连接到服务器
amqp_socket_open(socket, "localhost", 5672);

// 登录
amqp_login(conn, "localhost", 5672, 0, 131072, 0,
           AMQP_SASL_METHOD_PLAIN, "guest", "guest");

// 打开通道
amqp_channel_open(conn, 1);
```

### 2. 队列声明

```cpp
amqp_queue_declare(conn, channel,
                  amqp_cstring_bytes("queue_name"),
                  true,    // durable - 持久化
                  false,   // passive
                  false,   // exclusive
                  false,   // auto_delete
                  amqp_empty_table);
```

### 3. 发送消息

```cpp
// 设置消息属性
amqp_basic_properties_t props;
props._flags = AMQP_BASIC_CONTENT_TYPE_FLAG | AMQP_BASIC_DELIVERY_MODE_FLAG | AMQP_BASIC_PRIORITY_FLAG;
props.content_type = amqp_cstring_bytes("text/plain");
props.delivery_mode = 2;  // 持久化
props.priority = 10;      // 优先级

// 发布消息
amqp_basic_publish(conn, channel,
                  amqp_empty_bytes,        // exchange
                  amqp_cstring_bytes("queue_name"),  // routing key
                  0, 0,                    // mandatory, immediate
                  &props,                  // properties
                  amqp_cstring_bytes(message));  // body
```

### 4. 消费消息

```cpp
// 设置 QoS
amqp_basic_qos(conn, channel, 1);

// 开始消费
amqp_basic_consume(conn, channel,
                  amqp_cstring_bytes("queue_name"),
                  amqp_empty_bytes,  // consumer_tag
                  0, 0, 0);          // no_local, no_ack, exclusive

// 接收消息
amqp_envelope_t envelope;
amqp_rpc_reply_t ret = amqp_consume_message(conn, &envelope, nullptr, 0);

if (ret.reply_type == AMQP_RESPONSE_NORMAL) {
    // 处理消息
    std::string message(
        static_cast<char*>(envelope.message.body.bytes),
        envelope.message.body.len
    );
    
    // 手动 ACK
    amqp_basic_ack(conn, channel, envelope.delivery_tag, 0);
    
    // 销毁 envelope
    amqp_destroy_envelope(&envelope);
}
```

### 5. 错误处理

```cpp
void die_on_error(int code, const char* context) {
    if (code < 0) {
        std::cerr << "Error: " << context << ": " 
                  << amqp_error_string2(code) << std::endl;
        exit(1);
    }
    
    amqp_rpc_reply_t ret = amqp_get_rpc_reply(conn);
    if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
        std::cerr << "Error: " << context << ": RPC error" << std::endl;
        exit(1);
    }
}
```

## 📊 消息属性

```cpp
amqp_basic_properties_t props;
props._flags = AMQP_BASIC_CONTENT_TYPE_FLAG | 
               AMQP_BASIC_DELIVERY_MODE_FLAG | 
               AMQP_BASIC_PRIORITY_FLAG |
               AMQP_BASIC_CORRELATION_ID_FLAG |
               AMQP_BASIC_EXPIRATION_FLAG;

props.content_type = amqp_cstring_bytes("text/plain");
props.delivery_mode = 2;        // 1=非持久化，2=持久化
props.priority = 10;            // 优先级 0-10
props.correlation_id = amqp_cstring_bytes("corr-id");
props.expiration = amqp_cstring_bytes("60000");  // 60 秒
```

## 🎯 与文件系统模拟的对比

| 特性 | 文件系统模拟 | 真实 RabbitMQ (rabbitmq-c) |
|------|------------|---------------------------|
| **依赖** | 无 | rabbitmq-c 库 |
| **安装** | 无需安装 | 需要 RabbitMQ 服务器 + 客户端库 |
| **协议** | 文件系统 | AMQP 0-9-1 |
| **持久化** | 文本文件 | 内存 + 磁盘 |
| **并发** | 单线程 | 多线程/多消费者 |
| **ACK 机制** | 无 | 手动/自动 ACK |
| **优先级** | 模拟 | 原生支持 (0-10) |
| **监控** | 查看文件 | Web 管理界面 |
| **适用场景** | 学习/演示 | 生产环境 |

## 🔧 故障处理

### 找不到 rabbitmq-c 库

**错误:**
```
CMake Error: Could not find rabbitmq
```

**解决方案:**
```bash
# Windows (vcpkg)
vcpkg install rabbitmq-c:x64-windows
vcpkg integrate install

# Ubuntu
sudo apt install librabbitmq-dev

# macOS
brew install rabbitmq-c
```

### 连接失败

**错误:**
```
Error: Login: Connection refused
```

**解决方案:** 确保 RabbitMQ 服务已启动
```bash
# 检查 Docker 容器
docker ps | grep rabbitmq

# 检查服务状态 (Ubuntu)
sudo systemctl status rabbitmq-server
```

### 认证失败

**错误:**
```
Error: Login: ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN
```

**解决方案:** 检查用户名密码是否正确（默认：guest/guest）

## 📚 进阶用法

### 批量发送

```cpp
for (int i = 0; i < 100; i++) {
    std::string message = "Message " + std::to_string(i);
    amqp_basic_publish(conn, channel,
                      amqp_empty_bytes,
                      amqp_cstring_bytes("test_queue"),
                      0, 0,
                      &props,
                      amqp_cstring_bytes(message.c_str()));
}
```

### 死信队列

```cpp
// 声明死信队列
amqp_queue_declare(conn, channel,
                  amqp_cstring_bytes("dead_letter_queue"),
                  true, false, false, false, amqp_empty_table);

// 声明主队列，设置死信路由
amqp_table_entry_t entries[2];
entries[0].key = amqp_cstring_bytes("x-dead-letter-exchange");
entries[0].value.kind = AMQP_FIELD_KIND_UTF8;
entries[0].value.value.bytes = amqp_cstring_bytes("");

entries[1].key = amqp_cstring_bytes("x-dead-letter-routing-key");
entries[1].value.kind = AMQP_FIELD_KIND_UTF8;
entries[1].value.value.bytes = amqp_cstring_bytes("dead_letter_queue");

amqp_table_t args;
args.num_entries = 2;
args.entries = entries;

amqp_queue_declare(conn, channel,
                  amqp_cstring_bytes("main_queue"),
                  true, false, false, false, args);
```

## 📚 参考资料

- [rabbitmq-c GitHub](https://github.com/alanxz/rabbitmq-c)
- [RabbitMQ C 客户端文档](https://alanxz.github.io/rabbitmq-c/)
- [RabbitMQ 官方教程](https://www.rabbitmq.com/getstarted.html)

## 🎉 总结

这个示例展示了如何使用 **真正的 RabbitMQ C++ 客户端 (rabbitmq-c)** 进行消息收发：

✅ 使用 `rabbitmq-c` 库  
✅ 真实的 AMQP 协议通信  
✅ 支持持久化、优先级、ACK 机制  
✅ 可用于生产环境  

与文件系统模拟相比，这是**真正的消息队列实现**！🚀
