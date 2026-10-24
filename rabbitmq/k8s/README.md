# K3s Resilience Test

`run-k3s-test.sh` deploys isolated RabbitMQ and MySQL instances plus a Python test Job into the `mq-resilience-test` namespace. It does not use, read, or modify existing application namespaces, persistent volumes, credentials, or VM resources.

The script validates normal delivery, an actual MySQL Deployment outage and recovery, and an actual RabbitMQ Deployment outage and recovery. The Job checks that all seven messages reach MySQL and that both local recovery files are empty. The namespace is deleted by default after logs are emitted.

Run it on a host with `kubectl` access to the existing K3s cluster:

```bash
cd rabbitmq/k8s
bash run-k3s-test.sh
```

The script preserves `mq-resilience-test` by default, including its Pods, Job,
Secrets, ConfigMaps, MySQL PVC, and node-pulled images, so a later manual investigation can
reuse the environment. To run the full scenario again from a clean namespace,
use `RESET=true bash ./run-k3s-test.sh`. Remove it manually when it is no longer
needed:

```bash
kubectl delete namespace mq-resilience-test
```

The isolated MySQL Pod enables `mysql_native_password` solely so the lightweight
test client can run without a large optional crypto wheel. Do not copy that
authentication setting into a production MySQL deployment.
