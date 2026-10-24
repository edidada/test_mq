# RabbitMQ 消息积压与故障恢复实践

本目录提供 Java、Python、C++ 三套 RabbitMQ 生产者/消费者，以及可直接运行的故障演练。

K3s 隔离故障演练见 [`k8s/README.md`](k8s/README.md)，2026-10-24 的实际集群验证记录见 [`k8s/测试报告-2026-10-24.md`](k8s/测试报告-2026-10-24.md)。

| 目标 | 实现 |
| --- | --- |
| 消息积压 | 持久化优先级队列；critical 消息优先；可启动多个消费者扩容 |
| 无效消息 | `test_queue.dlq` 死信队列 |
| RabbitMQ 挂掉 | Python 生产者将消息 fsync 到 `outbox.jsonl`，恢复后确认发布并重放 |
| MySQL 挂掉 | Python 消费者有限重试，失败后 fsync 到 `pending_db.jsonl`，恢复后幂等补偿 |
| 重复投递 | MySQL 以 `message_id` 为主键，使用 upsert 保证幂等 |

`python/demo.py`、`java/.../DemoApp.java` 和 `cpp/src/demo.cpp` 是不依赖服务的文件系统模拟。`true_producer` / `true_consumer` 是连接真实 RabbitMQ 的版本；Python 真实消费者还连接 MySQL，完成端到端故障补偿。

## WSL 环境

以下命令已在 WSL Ubuntu 24.04、RabbitMQ 3.12、MySQL 8.0 上验证。安装依赖并启动服务：

```bash
sudo apt update
sudo apt install -y rabbitmq-server mysql-server librabbitmq-dev \
  python3-pika python3-pymysql cmake g++
sudo systemctl enable --now rabbitmq-server mysql
```

初始化本项目专用的数据库、表和演练账号：

```bash
mysql < /mnt/d/develops/git/github/cpp/test_mq/rabbitmq/sql/bootstrap_mysql.sql
```

`bootstrap_mysql.sql` 仅创建 `mq_demo` 数据库和 `mq_demo` 账号。账号密码仅限本地演练，生产环境必须用部署系统注入独立密钥。

RabbitMQ 本地演练默认使用 `guest/guest`。该账号只能从本机连接，生产环境应创建最小权限应用账号，并通过 `RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` 环境变量传入。

如果 Windows 已运行另一个 MySQL（本机 `3306` 被 `mysqld.exe` 占用），请像下面这样在 WSL 内运行 Python 演练，确保使用 WSL 的 `mq_demo` 数据库。

## 已验证的真实演练

设项目路径为：

```bash
cd /mnt/d/develops/git/github/cpp/test_mq/rabbitmq/python
export MQ_DEMO_RUNTIME_DIR=/tmp/mq-resilience-demo
```

正常收发：

```bash
python3 true_producer.py --count 3 --prefix normal
python3 true_consumer.py --count 3
```

MySQL 故障与恢复：

```bash
python3 true_producer.py --count 2 --prefix mysql-down
sudo systemctl stop mysql
python3 true_consumer.py --count 2
wc -l "$MQ_DEMO_RUNTIME_DIR/pending_db.jsonl"
sudo systemctl start mysql
python3 true_consumer.py --flush-pending-only
```

RabbitMQ 故障与恢复：

```bash
sudo systemctl stop rabbitmq-server
python3 true_producer.py --count 2 --prefix mq-down
wc -l "$MQ_DEMO_RUNTIME_DIR/outbox.jsonl"
sudo systemctl start rabbitmq-server
python3 true_producer.py --replay-only
python3 true_consumer.py --count 2
```

双故障与恢复：

```bash
sudo systemctl stop rabbitmq-server mysql
python3 true_producer.py --count 2 --prefix both-down
sudo systemctl start rabbitmq-server
python3 true_producer.py --replay-only
python3 true_consumer.py --count 2
sudo systemctl start mysql
python3 true_consumer.py --flush-pending-only
```

本次验证结果：正常消息 3 条落库；MySQL 停机时 2 条写入 `pending_db.jsonl` 并在恢复后补偿；RabbitMQ 停机时 2 条写入 `outbox.jsonl` 并在恢复后重放；双故障时另有 2 条先写 outbox、后写 pending_db、最终补偿。最终 `mq_demo.processed_messages` 为 9 条记录，两个本地恢复文件均为空。

## Java 和 C++ 真实客户端

Java 在 Windows PowerShell 中运行：

```powershell
mvn -f rabbitmq\java\pom.xml clean compile
mvn -f rabbitmq\java\pom.xml exec:java '-Dexec.mainClass=com.testmq.rabbitmq.TrueProducer'
mvn -f rabbitmq\java\pom.xml exec:java '-Dexec.mainClass=com.testmq.rabbitmq.TrueConsumer'
```

C++ 在 WSL 中构建并运行：

```bash
cmake -S /mnt/d/develops/git/github/cpp/test_mq/rabbitmq/cpp -B /tmp/test_mq_rabbitmq_build
cmake --build /tmp/test_mq_rabbitmq_build -j2
/tmp/test_mq_rabbitmq_build/true_producer
/tmp/test_mq_rabbitmq_build/true_consumer
```

Java 和 C++ 真实客户端都声明相同的 `x-max-priority`、死信交换机和死信路由键；本次均已完成 10 条生产和消费，critical 消息先被消费。
