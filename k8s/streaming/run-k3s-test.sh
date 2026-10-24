#!/usr/bin/env bash
set -euo pipefail

test_namespace="${TEST_NAMESPACE:-streaming-mq-test}"
middleware_namespace="${MIDDLEWARE_NAMESPACE:-middleware}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
reset="${RESET:-false}"
remove_legacy="${REMOVE_LEGACY_STREAMING_BROKERS:-false}"

if ! kubectl get namespace "$middleware_namespace" >/dev/null 2>&1; then
  echo "required middleware namespace $middleware_namespace does not exist" >&2
  exit 2
fi

if kubectl get namespace "$test_namespace" >/dev/null 2>&1 && [[ "$reset" == "true" ]]; then
  kubectl delete namespace "$test_namespace" --wait=true
fi

kubectl apply -f "$script_dir/namespace.yaml"
if [[ "$remove_legacy" == "true" ]]; then
  kubectl -n "$test_namespace" delete deployment automq automq-minio kafka pulsar --ignore-not-found
  kubectl -n "$test_namespace" delete service automq automq-minio kafka pulsar --ignore-not-found
  kubectl -n "$test_namespace" delete job automq-bucket-init --ignore-not-found
fi

kubectl apply -f "$script_dir/middleware-pvc.yaml"
kubectl apply -f "$script_dir/minio.yaml"
kubectl -n "$middleware_namespace" rollout status deployment/automq-minio --timeout=180s
kubectl -n "$middleware_namespace" delete job automq-bucket-init --ignore-not-found
kubectl apply -f "$script_dir/automq-bucket-init.yaml"
kubectl -n "$middleware_namespace" wait --for=condition=complete job/automq-bucket-init --timeout=180s

kubectl apply -f "$script_dir/automq.yaml"
kubectl apply -f "$script_dir/kafka.yaml"
kubectl apply -f "$script_dir/pulsar.yaml"
kubectl -n "$middleware_namespace" rollout status deployment/automq --timeout=900s
kubectl -n "$middleware_namespace" rollout status deployment/kafka --timeout=900s
kubectl -n "$middleware_namespace" rollout status deployment/pulsar --timeout=900s
kubectl -n "$middleware_namespace" get pvc kafka-data pulsar-data automq-minio-data

kubectl -n "$test_namespace" delete job kafka-e2e automq-e2e pulsar-e2e --ignore-not-found
kubectl apply -f "$script_dir/client-jobs.yaml"
for job in kafka-e2e automq-e2e pulsar-e2e; do
  kubectl -n "$test_namespace" wait --for=condition=complete "job/$job" --timeout=600s
  kubectl -n "$test_namespace" logs "job/$job"
done

echo "Kafka, AutoMQ, and Pulsar middleware end-to-end tests completed successfully"
