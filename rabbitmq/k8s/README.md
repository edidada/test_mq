# K3s Resilience Test

`run-k3s-test.sh` runs the Python test Job in `mq-resilience-test` while reusing
shared MySQL and a single persistent RabbitMQ instance in `middleware`. The
test database remains the dedicated `mqresilience` schema and its dedicated
application account; it does not use other MySQL schemas.

The script validates normal delivery plus MySQL and RabbitMQ client-side outage
handling without disrupting shared middleware. It temporarily uses an invalid
host name inside the Job to exercise `pending_db.jsonl` and `outbox.jsonl`,
then restores the real cross-namespace Service endpoint. The Job checks that
all seven messages reach MySQL and that both local recovery files are empty.

Run it on a host with `kubectl` access to the existing K3s cluster:

```bash
cd rabbitmq/k8s
bash run-k3s-test.sh
```

The script preserves `mq-resilience-test` by default, including its Job,
Secret, ConfigMap, and node-pulled images. The shared RabbitMQ PVC and
middleware namespace are never deleted. To run the scenario again from a clean
test namespace, use `RESET=true bash ./run-k3s-test.sh`.

To migrate an existing legacy test MySQL database into `middleware/mysql` and
release the duplicate test middleware after the regression passes, run:

```bash
MIGRATE_LEGACY_DATA=true REMOVE_LEGACY_TEST_MIDDLEWARE=true bash ./run-k3s-test.sh
```

The migration imports the `mqresilience` schema and data, then removes only the
legacy test RabbitMQ/MySQL Deployments, Services, test MySQL PVC, and obsolete
schema ConfigMap. It never deletes shared MySQL or the shared RabbitMQ PVC.
Remove the test namespace only when it is no longer needed:

```bash
kubectl delete namespace mq-resilience-test
```

The shared MySQL instance uses `mysql_native_password` for compatibility with
the lightweight test client. Do not treat that setting as a production
authentication recommendation.

The shared-middleware migration and test evidence are recorded in
[the 2026-10-25 report](../../k8s/测试报告-2026-10-25-共享中间件迁移.md).
