# 编译和运行指南

## 编译命令

```bash
# 编译消费者示例
g++ -std=c++11 -pthread -o consumer consumer.cpp

# 编译生产者示例
g++ -std=c++11 -pthread -o producer producer.cpp

# 编译故障处理器示例
g++ -std=c++11 -pthread -o handler handler.cpp
```

## 运行示例

```bash
# 运行消费者
./consumer

# 运行生产者
./producer

# 运行故障处理器
./handler
```

## 代码说明

### consumer.cpp
- 模拟MQ消息消费者
- 实现了消息积压处理
- 实现了MySQL故障处理
- 实现了重试机制和死信队列

### producer.cpp
- 模拟MQ消息生产者
- 实现了MQ故障处理
- 实现了本地消息存储
- 实现了重连机制

### handler.cpp
- 故障处理核心逻辑
- MQ故障处理流程
- MySQL故障处理流程
- 双故障处理流程
- 监控和统计功能

## 运行环境

- C++11 或更高版本
- 支持多线程的编译器
- 无外部依赖
