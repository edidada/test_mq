# Middleware PVC 迁移测试报告（2026-10-25）

## 结论

通过。Kafka、AutoMQ、MinIO 和 Pulsar 已从 `streaming-mq-test` 迁移到
`middleware` namespace，数据目录改为三个独立的 20Gi PVC。客户端 E2E Job
继续隔离在 `streaming-mq-test`，并通过跨 namespace 的 Kubernetes Service FQDN
访问中间件。迁移后首次 E2E 和删除 Pulsar Pod 后的第二次 E2E 均通过。

测试现场按要求保留：`middleware`、`streaming-mq-test`、三个 PVC、Broker
Deployment、MinIO bucket-init Job 和已完成的 E2E Job 均未删除。

## 部署范围

| 组件 | 运行 namespace | 持久化方式 | 客户端地址 |
| --- | --- | --- | --- |
| Kafka 3.9.2 | `middleware` | `kafka-data` PVC，20Gi，挂载 `/var/lib/kafka/data` | `kafka.middleware.svc.cluster.local:9092` |
| AutoMQ 1.5.5 | `middleware` | 对象数据写入 MinIO | `automq.middleware.svc.cluster.local:9092` |
| MinIO | `middleware` | `automq-minio-data` PVC，20Gi，挂载 `/data` | `automq-minio.middleware.svc.cluster.local:9000` |
| Pulsar 3.3.9 | `middleware` | `pulsar-data` PVC，20Gi，挂载 `/pulsar/data` | `pulsar.middleware.svc.cluster.local:6650` |
| E2E client Job | `streaming-mq-test` | 无持久数据 | 使用上述 FQDN |

集群的 `local-path` StorageClass 为 `rancher.io/local-path`，回收策略为
`Delete`，卷绑定模式为 `WaitForFirstConsumer`。三个本次新增 PVC 均为
`ReadWriteOnce` 且状态为 `Bound`。

## 执行过程

1. 应用 `middleware-pvc.yaml`，随后在 `middleware` 部署 MinIO、AutoMQ、Kafka
   和 Pulsar；旧 `streaming-mq-test` 中的同名 Broker Deployment、Service 和
   bucket-init Job 已移除。
2. 等待全部 Deployment 就绪，确认 `kafka-data`、`pulsar-data`、
   `automq-minio-data` 均已绑定到 20Gi `local-path` PV。
3. 删除并重建 `streaming-mq-test` 中三个 Job，避免 Kubernetes Job 的 Pod 模板
   不可变限制，并运行生产 10 条、消费 10 条的端到端校验。
4. 删除 Pulsar Pod，但不删除 `pulsar-data` PVC；由 Deployment 重建 Pod 后，再次
   执行三个 E2E Job，验证跨 namespace 访问与卷重挂载路径。

执行入口：

```bash
cd k8s/streaming
bash run-k3s-test.sh
```

`RESET=true` 只会重建 `streaming-mq-test`。脚本不会删除 `middleware` 或其中的
PVC；如需从零开始，必须由维护者确认数据可丢弃后手动处理对应 PVC。

## 测试结果

| 用例 | 结果 | 证据 |
| --- | --- | --- |
| Kafka 跨 namespace E2E | 通过 | Job `kafka-e2e` 完成，49 秒；日志为 `Kafka verification passed: 10 messages produced and consumed` |
| AutoMQ 跨 namespace E2E | 通过 | Job `automq-e2e` 完成，64 秒；日志为 `AutoMQ verification passed: 10 messages produced and consumed` |
| Pulsar 跨 namespace E2E | 通过 | Job `pulsar-e2e` 完成，54 秒；日志为 `Pulsar verification passed: 10 messages produced and consumed` |
| PVC 状态 | 通过 | `kafka-data`、`pulsar-data`、`automq-minio-data` 均为 `Bound`、20Gi、RWO、`local-path` |
| Pulsar Pod 删除后的恢复 | 通过 | 重建 Pod 后 Deployment `1/1 Ready`；日志显示 messaging service ready，随后三个 E2E Job 全部完成 |

Pulsar standalone 使用嵌入式 ZooKeeper/BookKeeper 兼容模式，并固定本地
BookKeeper endpoint 为 `127.0.0.1:3181`。重建后的日志显示该 endpoint 被解析，且
`pulsar.middleware.svc.cluster.local:6650` 可被测试 namespace 客户端连接。

## 处理的问题

- MinIO 的 Daocloud 镜像拉取返回 403，直连 Docker Hub 在现有网络超时；已使用
  集群可访问的 `docker.1panel.live` 固定版本镜像。
- Pulsar 3.3 standalone 使用 RocksDB 模式时会为内嵌 BookKeeper 选择不稳定的
  endpoint，导致 PVC 重挂载后的单节点恢复不可靠。测试夹具切换为嵌入式
  ZooKeeper/BookKeeper 兼容模式后，固定 BookKeeper 地址并完成重启后 E2E 验证。
- Kubernetes 已创建的 Job 不能原位更新 Pod 模板；测试脚本始终删除并重新创建三个
  E2E Job，而不影响中间件数据面。

## 限制与后续要求

这是一套单副本功能与卷重挂载夹具，不是生产 HA 部署。`local-path` PV 是节点本地
存储，节点丢失或不可调度时不能由其他节点接管；Kafka、AutoMQ、Pulsar 都没有副本
quorum、跨故障域调度、备份恢复或跨集群复制验证。

生产环境应分别使用具备稳定身份和每副本独立 PVC 的 Kafka Operator/StatefulSet、
Pulsar Helm/Operator（独立 BookKeeper 与元数据组件）及高可用对象存储；同时按
[`跨集群部署规范`](../跨集群部署规范.md) 完成容量、备份、故障演练与跨集群灾备设计。
