# Kafka, AutoMQ and Pulsar K3s Tests

This deployment uses the existing K3s cluster. Stateful middleware workloads
are deployed to `middleware`; only disposable E2E client Jobs run in the
isolated `streaming-mq-test` namespace. It deploys:

- Apache Kafka 3.9.2 in single-node KRaft mode.
- AutoMQ 1.5.5 with an isolated MinIO S3-compatible backend.
- Apache Pulsar 3.3.9 in standalone mode.

Kafka and Pulsar are pulled through `docker.m.daocloud.io`; MinIO and its
client use `docker.1panel.live`; AutoMQ uses `docker.1ms.run`. These proxies
are reachable from the existing cluster where direct Docker Hub access times
out.

Each broker has an independent CLI Job that creates a topic, produces 10
messages, consumes 10 messages, and fails if the count differs.

```bash
cd k8s/streaming
bash run-k3s-test.sh
```

Kafka, Pulsar and AutoMQ MinIO each use a 20Gi `ReadWriteOnce` PVC backed by
the cluster's `local-path` StorageClass. The script never deletes the
`middleware` namespace or these PVCs. `RESET=true bash run-k3s-test.sh`
recreates only `streaming-mq-test`; it does not reset broker data. Pass
`REMOVE_LEGACY_STREAMING_BROKERS=true` once during migration to remove the
old broker Deployments, Services and bucket-init Job from the test namespace.

The brokers remain single-replica functional fixtures, not production HA
topologies. The PVCs validate persistence for this cluster but `local-path`
storage remains node-local; it is not a substitute for a multi-node,
multi-failure-domain production storage design.

Pulsar standalone runs its embedded ZooKeeper/BookKeeper compatibility mode
with a fixed local BookKeeper endpoint. This is a test-fixture compatibility
setting, not a production pattern.

The storage migration and its remaining limits are documented in
[emptyDir and PVC guidance](emptyDir与PVC说明.md).

The completed cluster evidence is in [Test report](测试报告-2026-10-25-middleware-pvc.md).
