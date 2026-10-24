# MQ消息积压与故障处理指南
wsl bash -c "sudo apt update && sudo apt install -y librabbitmq-dev"
## 问题背景
在消息队列（MQ）系统中，当消息积压、MQ服务挂掉或MySQL数据库挂掉时，需要有完善的处理策略。

## 环境说明
本项目使用 RocketMQ 4.9.7 在 WSL Ubuntu 24.04 上进行实际操作演示。

K3s 隔离部署与 Java 端到端验证见 [`k8s/rocketmq/README.md`](k8s/rocketmq/README.md)，实际集群测试记录见 [`k8s/rocketmq/测试报告-2026-10-24.md`](k8s/rocketmq/测试报告-2026-10-24.md)。

## 快速开始
### 启动 RocketMQ
进入 WSL Ubuntu 环境，执行以下命令：
```bash
# 进入项目目录
cd /opt/rocketmq

# 启动 RocketMQ
bash rocketmq_start.sh

# 检查状态
bash rocketmq_status.sh

# 停止 RocketMQ
bash rocketmq_stop.sh
```

### 连接信息
- NameServer: 127.0.0.1:9876
- Broker: 127.0.0.1:10911

## 核心问题场景

### 1. MQ消息积压
- 大量消息在队列中堆积
- 消费速度跟不上生产速度
- 可能导致内存溢出或服务响应变慢

模拟方式：
1. 只启动生产者，不启动消费者
2. 发送大量消息到 Topic
3. 观察消息积压情况

解决方案：
- 增加消费者数量
- 优化消费逻辑，提高消费速度
- 临时降级，丢弃非关键消息
- 使用死信队列处理失败消息

### 2. MQ服务挂掉
- MQ服务不可用
- 消息无法正常发送或消费
- 需要快速恢复和容错处理

模拟方式：
1. 停止 Broker 或 NameServer
2. 尝试发送/消费消息
3. 观察应用报错情况

解决方案：
- 快速重启 MQ 服务
- 生产者切换到本地存储
- 待 MQ 恢复后重发消息

### 3. MySQL挂掉
- 数据库服务中断
- 消费者无法持久化数据
- 需要降级或重试策略

模拟方式：
1. 停止 MySQL 服务
2. 消费者尝试写入数据库
3. 观察消费失败和重试

解决方案：
- 消费失败自动重试
- 使用本地缓存暂存数据
- MySQL 恢复后批量重写
- 降级为只记录日志

### 4. 双双挂掉
- MQ和MySQL同时故障
- 最严重的场景
- 需要全面的容灾方案

模拟方式：
1. 同时停止 MQ 和 MySQL
2. 观察整体系统表现

解决方案：
- 全面降级，本地存储所有数据
- 定期检查服务状态
- 服务恢复后数据同步

## 处理策略

### 监控告警
- 实时监控消息积压量
- 设置合理的告警阈值
- 快速定位问题根源

### 扩容缩容
- 根据积压量动态调整消费者数量
- 使用弹性计算资源

### 降级方案
- 优先处理关键消息
- 临时丢弃非关键消息
- 使用本地缓存暂存数据

### 重试机制
- 消费失败自动重试
- 设置重试次数和间隔
- 死信队列处理失败消息

## 代码示例

本项目包含以下代码示例：

### C++ 模拟代码
- `consumer.cpp` - 消费者示例代码
- `producer.cpp` - 生产者示例代码
- `handler.cpp` - 故障处理逻辑

### Java 实际测试代码（配合 RocketMQ 使用）
- `MQProducer.java` - 消息生产者
- `MQConsumer.java` - 消息消费者

### 管理脚本
- `rocketmq_start.sh` - 启动 RocketMQ
- `rocketmq_stop.sh` - 停止 RocketMQ
- `rocketmq_status.sh` - 检查服务状态

## 最佳实践

1. 提前规划：设计时考虑各种故障场景
2. 监控先行：建立完善的监控体系
3. 自动恢复：尽可能使用自动恢复机制
4. 人工介入：设置人工确认和介入机制
5. 定期演练：定期进行故障恢复演练

## RocketMQ 常用管理命令

```bash
# 查看集群信息
./bin/mqadmin clusterList -n 127.0.0.1:9876

# 创建 Topic
./bin/mqadmin updateTopic -n 127.0.0.1:9876 -c DefaultCluster -t TestTopic

# 查看 Topic 状态
./bin/mqadmin topicStatus -n 127.0.0.1:9876 -t TestTopic

# 查看 Broker 状态
./bin/mqadmin brokerStatus -n 127.0.0.1:9876 -b 127.0.0.1:10911

# 查看消息堆积情况
./bin/mqadmin statsAll -n 127.0.0.1:9876
```


# 1. 启动 RocketMQ
wsl
cd /opt/rocketmq
bash rocketmq_start.sh

# 2. 创建 Topic
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
./bin/mqadmin updateTopic -n 127.0.0.1:9876 -c DefaultCluster -t TestTopic

# 3. 编译并运行生产者（发送 1000 条消息）
cd /mnt/d/develops/git/github/cpp/test_mq/java
mvn clean compile
mvn exec:java -Dexec.mainClass="com.testmq.MQProducer"

已发送 100 条消息
已发送 200 条消息
已发送 300 条消息
已发送 400 条消息
已发送 500 条消息
已发送 600 条消息
已发送 700 条消息
已发送 800 条消息
已发送 900 条消息
已发送 1000 条消息

# 4. 另开终端查看积压
wsl
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
./bin/mqadmin statsAll -n 127.0.0.1:9876

./bin/mqadmin statsAll -n 127.0.0.1:9876
#Topic                                                            #Consumer Group                                                  #Accumulation      #InTPS     #OutTPS   #InMsg24Hour  #OutMsg24Hour
TestTopic                                                                                                                                     0       48.23                       1000    NO_CONSUMER
SCHEDULE_TOPIC_XXXX                                                                                                                           0        0.00                          0    NO_CONSUMER
RMQ_SYS_TRANS_HALF_TOPIC                                                                                                                      0        0.00                          0    NO_CONSUMER
DefaultCluster_REPLY_TOPIC                                                                                                                    0        0.00                          0    NO_CONSUMER
BenchmarkTest                                                                                                                                 0        0.00                          0    NO_CONSUMER
OFFSET_MOVED_EVENT                                                                                                                            0        0.00                          0    NO_CONSUMER
TBW102                                                                                                                                        0        0.00                          0    NO_CONSUMER
SELF_TEST_TOPIC                                                                                                                               0        0.00                          0    NO_CONSUMER
LAPTOP-wdidada                                                                                                                                0        0.00                          0    NO_CONSUMER
DefaultCluster                                                                                                                                0        0.00                          0    NO_CONSUMER
wdidada@LAPTOP-wdidada:/opt/rocketmq/rocketmq-all-4.9.7-bin-release$



./bin/mqadmin topicStatus -n 127.0.0.1:9876 -t TestTopic

#Broker Name                      #QID  #Min Offset           #Max Offset             #Last Updated
LAPTOP-wdidada                    0     0                     125                     2026-04-14 19:39:26,727
LAPTOP-wdidada                    1     0                     125                     2026-04-14 19:39:26,724
LAPTOP-wdidada                    2     0                     125                     2026-04-14 19:39:26,724
LAPTOP-wdidada                    3     0                     125                     2026-04-14 19:39:26,725
LAPTOP-wdidada                    4     0                     125                     2026-04-14 19:39:26,725
LAPTOP-wdidada                    5     0                     125                     2026-04-14 19:39:26,726
LAPTOP-wdidada                    6     0                     125                     2026-04-14 19:39:26,726
LAPTOP-wdidada                    7     0                     125                     2026-04-14 19:39:26,727


 ./bin/mqadmin brokerStatus -n 127.0.0.1:9876 -b 127.0.0.1:10911
EndTransactionQueueSize         : 0
EndTransactionThreadPoolQueueCapacity: 100000
bootTimestamp                   : 1776166423407
brokerVersion                   : 407
brokerVersionDesc               : V4_9_7
commitLogDirCapacity            : Total : 1006.9 GiB, Free : 880.0 GiB.
commitLogDiskRatio              : 0.08
commitLogDiskRatio_/home/wdidada/store/commitlog: 0.08
commitLogMaxOffset              : 191893
commitLogMinOffset              : 0
consumeQueueDiskRatio           : 0.08
dispatchBehindBytes             : 0
dispatchMaxBuffer               : 0
earliestMessageTimeStamp        : -1
getFoundTps                     : 0.0 0.0 0.0
getMessageEntireTimeMax         : 0
getMissTps                      : 0.19998000199980004 0.03247122237916646 0.03258841644737378
getTotalTps                     : 0.19998000199980004 0.03247122237916646 0.03258841644737378
getTransferedTps                : 0.0 0.0 0.0
msgGetTotalTodayMorning         : 0
msgGetTotalTodayNow             : 0
msgGetTotalYesterdayMorning     : 0
msgPutTotalTodayMorning         : 0
msgPutTotalTodayNow             : 1000
msgPutTotalYesterdayMorning     : 0
pageCacheLockTimeMills          : 0
pullThreadPoolQueueCapacity     : 100000
pullThreadPoolQueueHeadWaitTimeMills: 0
pullThreadPoolQueueSize         : 0
putLatency99                    : 0.00
putLatency999                   : 0.00
putMessageAverageSize           : 191.893
putMessageDistributeTime        : [<=0ms]:0 [0~10ms]:0 [10~50ms]:0 [50~100ms]:0 [100~200ms]:0 [200~500ms]:0 [500ms~1s]:0 [1~2s]:0 [2~3s]:0 [3~4s]:0 [4~5s]:0 [5~10s]:0 [10s~]:0
putMessageEntireTimeMax         : 11
putMessageFailedTimes           : 0
putMessageSizeTotal             : 191893
putMessageTimesTotal            : 1000
putTps                          : 0.0 0.0 1.629420822368689
queryThreadPoolQueueCapacity    : 20000
queryThreadPoolQueueHeadWaitTimeMills: 0
queryThreadPoolQueueSize        : 0
remainHowManyDataToFlush        : 0 B
remainTransientStoreBufferNumbs : 2147483647
runtime                         : [ 0 days, 0 hours, 12 minutes, 20 seconds ]
sendThreadPoolQueueCapacity     : 10000
sendThreadPoolQueueHeadWaitTimeMills: 0
sendThreadPoolQueueSize         : 0
startAcceptSendRequestTimeStamp : 0
