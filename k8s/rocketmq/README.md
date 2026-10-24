# RocketMQ K3s Deployment

This directory deploys the repository's Java RocketMQ producer and consumer as
an isolated Job in `rocketmq-resilience-test`. It reuses the existing persistent
RocketMQ NameServer and Broker in `middleware`, rather than creating a second
broker pair. The Job verifies that 10 messages are produced and consumed.

Run from a host with cluster access:

```bash
cd k8s/rocketmq
bash run-k3s-test.sh
```

The namespace is retained after a successful or failed run, including the Job
logs. `middleware` and its RocketMQ PVC are never deleted by this script. To
recreate only the test namespace deliberately:

```bash
RESET=true bash run-k3s-test.sh
```

Remove it only when no longer needed:

```bash
kubectl delete namespace rocketmq-resilience-test
```

The Java example validates RocketMQ delivery through
`rocketmq-namesrv.middleware.svc.cluster.local:9876`. It has no MySQL
persistence implementation, so it does not claim to cover MySQL outage
recovery. The prior test-namespace NameServer and Broker are removed when the
script runs; they had no PVC-backed data to migrate.

The shared-middleware migration and test evidence are recorded in
[the 2026-10-25 report](../测试报告-2026-10-25-共享中间件迁移.md).
