# Kafka、Pulsar、AutoMQ-MinIO 从 `emptyDir` 迁移到 PVC

## 结论

当前 `k8s/streaming` 将 Kafka、Pulsar、AutoMQ-MinIO 作为中间件部署到
`middleware` namespace；Kafka、Pulsar 与 MinIO 各自申请一个 20Gi
`ReadWriteOnce` PVC。`streaming-mq-test` 只保留可删除的 E2E 客户端 Job。

这次迁移消除了 Pod 被删除时立即丢失 broker/对象数据的 `emptyDir` 风险，但没有
把单副本 Demo 变成生产 HA 集群。Kubernetes 在 Pod 从节点删除时会永久删除
`emptyDir` 数据；它只在容器崩溃、Pod 仍留在同一节点时保留数据。
[Kubernetes emptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)

| 组件 | 当前数据目录/后端 | 当前形式 | 目的 |
| --- | --- | --- | --- |
| Kafka | `/var/lib/kafka/data` | `middleware` 单副本 `Deployment` + `kafka-data` PVC | 单 KRaft 节点消息收发和卷重挂载验证 |
| Pulsar | `/pulsar/data` | `middleware` 单副本 `Deployment` + `pulsar-data` PVC | `pulsar standalone` 消息收发和卷重挂载验证 |
| AutoMQ | S3 URI 指向 `automq-minio` | MinIO 的 `/data` 使用 `automq-minio-data` PVC | 验证 AutoMQ 对持久化 S3 兼容后端的读写 |

## 为什么之前使用 `emptyDir`

1. 测试验收的是 topic 创建、生产和消费各 10 条消息，以及客户端连通性；不验收
   磁盘恢复、长期保留、备份恢复或高可用。
2. 可以重新创建没有历史 Kafka KRaft 元数据、Pulsar ledger 或 MinIO bucket 的
   环境，避免上一次测试数据影响本次断言。
3. 不依赖集群的 `StorageClass`、PVC 动态供给、卷绑定和回收策略，可快速部署。

现在中间件转移到 `middleware` 后，测试脚本不再删除中间件数据。需要从零验证时，
必须由维护者在确认数据可丢弃后手动删除对应 PVC，而不是使用 `RESET=true`。

## 这意味着什么

迁移前，删除或重建 Pod、驱逐 Pod、节点故障后，相关数据都可能丢失：

- Kafka 会丢失 broker 日志、consumer offset 和 KRaft 元数据。
- Pulsar `standalone` 会丢失其本地 ledger、BookKeeper 和元数据状态。
- AutoMQ 的对象数据会随测试 MinIO 的 `emptyDir` 一起丢失。

迁移后，PVC 可让同一 broker 的数据在 Pod 重建后重新挂载；但当前默认
`local-path` PV 是节点本地存储，PVC 无法跨节点接管。E2E 通过仍不能作为
Kafka/Pulsar/AutoMQ 的多副本高可用、节点故障、容量、备份或跨集群容灾证据。

## 为什么不能只把 `emptyDir` 改成一个 PVC

单副本 `Deployment` 加一个 PVC 只能让单实例在卷可重新挂载时保留数据，仍不具备
稳定 broker 身份、故障域隔离、quorum、滚动升级和副本恢复能力。它还会保留历史
Kafka/Pulsar/MinIO 状态，因此 E2E 使用固定 topic 和幂等创建，而不是假定空存储。

生产部署应另建生产 Overlay，使用 `StatefulSet` 或官方 Operator/Helm chart，为
每个有状态副本建立独立 PVC，并先完成以下设计：

- 明确 `StorageClass`、IOPS/吞吐、容量水位、扩容能力、加密、快照、备份和
  `reclaimPolicy`；PVC 是否在卸载时删除必须符合数据保留策略。
- 通过反亲和、拓扑分布约束、专用节点池和 PDB，使副本跨节点/可用区；不要把
  多副本调度到同一故障域。
- 建立受控升级、故障转移、监控告警、备份恢复和节点丢失演练，而不是只验证 Pod
  能重新启动。

Kafka 可使用 Strimzi 的生产 Operator 模式。其文档将 `persistent-claim` 用于
持久卷，并给出每个 broker 独立 PVC 的命名与配置；生产 Kafka 还应采用多 broker
和适当的副本/ISR 策略，而不是这个单 KRaft broker。
[Strimzi persistent storage](https://strimzi.io/docs/operators/0.50.1/deploying#con-configuring-persistent-storage-str)

Pulsar 生产环境不应使用当前的 `pulsar standalone`。Apache Pulsar 官方 Helm chart
默认启用持久卷声明，并明确将关闭持久化定位为 Minikube 等开发环境的选项；生产
需要规划 BookKeeper、元数据组件与 broker 的完整集群。
[Apache Pulsar Helm persistence](https://pulsar.apache.org/docs/4.0.x/helm-deploy/#persistence)

当前持久化验证中的 Pulsar standalone 使用嵌入式 ZooKeeper/BookKeeper 兼容模式，
以固定本地 BookKeeper endpoint 保持单节点 PVC 重启可用。这是单节点 E2E 兼容处理，
不能复制到生产。生产 Pulsar 应使用独立 BookKeeper StatefulSet、每个 bookie 的 PVC
和稳定的 Pod/Service 身份。

AutoMQ 的生产设计也不能依赖这里的单实例测试 MinIO。虽然当前 MinIO 已使用 PVC，
它仍是单副本测试后端，不具备生产对象存储所需的高可用能力。生产环境应使用具备持久性和
故障域设计的对象存储服务，按其保留、可用性、权限、加密和跨集群复制能力设计；
测试用 MinIO 只验证 S3 兼容访问路径。

## 推荐的仓库演进

保留 `k8s/streaming/` 作为中间件持久化验证与 E2E 客户端夹具，不要将其直接当作
生产清单。新增 `k8s/mq/` 的生产 Overlay，例如：

```text
k8s/mq/
  base/                 # 通用策略、监控和安全基线
  overlays/
    prod-a/             # 本地 HA Kafka / AutoMQ / Pulsar，独立 PVC
    prod-b/             # 第二生产集群，独立存储和身份
    dr-a/               # 灾备集群与复制入口
    test/               # 引用现有 streaming E2E 或专门测试配置
```

生产 Kafka 建议采用 Operator 管理的多副本 broker/controller 与每副本 PVC；Pulsar
采用其生产 Helm chart 或等价 Operator，持久化 metadata 和 BookKeeper；AutoMQ
配置生产级对象存储。再分别验证 Pod 重建、节点丢失、卷重挂载、备份恢复与跨集群
复制切换。

跨集群和 namespace 的完整边界见
[MQ Kubernetes 跨集群部署规范](../跨集群部署规范.md)，当前实测证据见
[测试报告](测试报告-2026-10-25-middleware-pvc.md)。
