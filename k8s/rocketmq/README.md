# RocketMQ K3s Deployment

This directory deploys the repository's Java RocketMQ producer and consumer to
an isolated `rocketmq-resilience-test` namespace on an existing K3s cluster.
It creates a NameServer, Broker, and one Java end-to-end Job that verifies 10
messages are produced and consumed.

Run from a host with cluster access:

```bash
cd k8s/rocketmq
bash run-k3s-test.sh
```

The namespace is retained after a successful or failed run, including the
RocketMQ images and Job logs. To recreate it deliberately:

```bash
RESET=true bash run-k3s-test.sh
```

Remove it only when no longer needed:

```bash
kubectl delete namespace rocketmq-resilience-test
```

The Java example validates RocketMQ delivery only. It has no MySQL persistence
implementation, so it does not claim to cover MySQL outage recovery.
