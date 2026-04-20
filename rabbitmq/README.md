# RabbitMQ 故障处理实践
✔ 开发 / 测试环境：用 admin
✔ 生产环境：必须新建专用用户（app_user）

权限太大，代码里用有安全风险
只给运维后台用，不给代码用
❌ rpc_user
这是系统内部用户
不是给你业务代码用的
容易冲突、权限不对

wsl bash -c "sudo rabbitmqctl add_user app_user 123456"
wsl bash -c "sudo rabbitmqctl set_permissions -p / app_user '.*' '.*' '.*' && sudo rabbitmqctl set_user_tags app_user management"

本目录按 `README_rabbitmq.md` 新建了三套示例：

- `python/`
- `java/`
- `cpp/`

三套示例都覆盖同一主题：

1. 消息积压：积压超过阈值时进入 surge mode，加速消费并降级丢弃低优先级消息。
2. MQ 挂掉：生产者切换到本地 `outbox` 文件，恢复后自动补发。
3. MySQL 挂掉：消费者重试失败后写入本地 `pending_db`，恢复后自动补偿。
4. MQ 和 MySQL 同时挂掉：全面降级到本地文件，待恢复后统一回放。

## 说明

当前仓库运行环境无法直接操控 README 中提到的 WSL Ubuntu 服务，因此这里提供的是“可本地直接运行”的故障演练代码：

- 不依赖外部 RabbitMQ / MySQL 服务
- 用本地文件模拟 RabbitMQ 队列、生产者 outbox、数据库落库结果
- 处理策略和真实系统一致：重试、降级、补偿、死信

如果你后续在 WSL 中启用了真实 RabbitMQ/MySQL，可以把这些处理策略迁移到真实客户端代码中。

## 运行

### Python

```powershell
python rabbitmq/python/demo.py
```

### Java

```powershell
$out = Join-Path (Resolve-Path .) 'rabbitmq\java\out'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$sources = Get-ChildItem 'rabbitmq\java\src\main\java' -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d $out $sources
java -cp rabbitmq\java\out com.testmq.rabbitmq.DemoApp
```

### C++

```powershell
$outDir = Join-Path (Resolve-Path .) 'build\rabbitmq_cpp'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
g++ -std=c++17 rabbitmq\cpp\src\demo.cpp -o (Join-Path $outDir 'demo.exe')
.\build\rabbitmq_cpp\demo.exe
```

## 运行产物

每种语言都会在自己的 `runtime/` 目录下生成：

- `queue.txt`：模拟 MQ 队列
- `outbox.txt`：MQ 挂掉时生产者暂存区
- `pending_db.txt`：MySQL 挂掉时消费者暂存区
- `db.txt`：模拟成功落库的数据
- `dead_letter.txt`：降级或重试耗尽后的消息
