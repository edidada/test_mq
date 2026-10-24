#!/usr/bin/env bash
set -euo pipefail

test_namespace="${NAMESPACE:-rocketmq-resilience-test}"
middleware_namespace="${MIDDLEWARE_NAMESPACE:-middleware}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="${PROJECT_DIR:-${script_dir}/../..}"
reset="${RESET:-false}"

require_file() {
  [[ -f "$1" ]] || { echo "required file is missing: $1" >&2; exit 1; }
}

for file in \
  "$script_dir/namespace.yaml" \
  "$script_dir/client-job.yaml" \
  "$project_dir/java/pom.xml" \
  "$project_dir/java/src/main/java/com/testmq/ConfigLoader.java" \
  "$project_dir/java/src/main/java/com/testmq/MQProducer.java" \
  "$project_dir/java/src/main/java/com/testmq/MQConsumer.java" \
  "$project_dir/java/src/main/resources/application.properties" \
  "$project_dir/java/src/main/resources/log4j2.xml"; do
  require_file "$file"
done

if ! kubectl get namespace "$middleware_namespace" >/dev/null 2>&1; then
  echo "required middleware namespace $middleware_namespace does not exist" >&2
  exit 2
fi

if kubectl get namespace "$test_namespace" >/dev/null 2>&1; then
  if [[ "$reset" != "true" ]]; then
    echo "namespace $test_namespace already exists; preserve it by default. Use RESET=true to recreate it." >&2
    exit 2
  fi
  kubectl delete namespace "$test_namespace" --wait=true
fi

kubectl apply -f "$script_dir/namespace.yaml"
kubectl -n "$test_namespace" create configmap rocketmq-java-source \
  --from-file=pom.xml="$project_dir/java/pom.xml" \
  --from-file=ConfigLoader.java="$project_dir/java/src/main/java/com/testmq/ConfigLoader.java" \
  --from-file=MQProducer.java="$project_dir/java/src/main/java/com/testmq/MQProducer.java" \
  --from-file=MQConsumer.java="$project_dir/java/src/main/java/com/testmq/MQConsumer.java" \
  --from-file=application.properties="$project_dir/java/src/main/resources/application.properties" \
  --from-file=log4j2.xml="$project_dir/java/src/main/resources/log4j2.xml" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$middleware_namespace" rollout status deployment/rocketmq-namesrv --timeout=180s
kubectl -n "$middleware_namespace" rollout status statefulset/rocketmq-broker --timeout=240s
kubectl -n "$test_namespace" delete deployment rocketmq-namesrv rocketmq-broker --ignore-not-found
kubectl -n "$test_namespace" delete service rocketmq-namesrv rocketmq-broker --ignore-not-found
kubectl -n "$test_namespace" delete job rocketmq-java-e2e --ignore-not-found
kubectl apply -f "$script_dir/client-job.yaml"
kubectl -n "$test_namespace" wait --for=condition=complete job/rocketmq-java-e2e --timeout=600s
kubectl -n "$test_namespace" logs job/rocketmq-java-e2e
echo "RocketMQ shared-middleware Java end-to-end test completed successfully"
