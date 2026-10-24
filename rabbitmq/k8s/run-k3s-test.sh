#!/usr/bin/env bash
set -euo pipefail

namespace="${NAMESPACE:-mq-resilience-test}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
app_dir="${APP_DIR:-${script_dir}/../python}"
cleanup="${CLEANUP:-false}"
reset="${RESET:-false}"

require_file() {
  [[ -f "$1" ]] || { echo "required file is missing: $1" >&2; exit 1; }
}

wait_for_log() {
  local marker="$1"
  # Initial dependency installation can be slow on a cold cluster node.
  local timeout_seconds="${2:-420}"
  local deadline=$((SECONDS + timeout_seconds))

  until kubectl -n "$namespace" logs job/resilience-test 2>/dev/null | grep -q "$marker"; do
    if [[ $SECONDS -ge $deadline ]]; then
      echo "timed out waiting for log marker: $marker" >&2
      kubectl -n "$namespace" logs job/resilience-test --all-containers=true || true
      return 1
    fi
    sleep 2
  done
}

wait_for_no_pods() {
  local selector="$1"
  local deadline=$((SECONDS + 30))

  while kubectl -n "$namespace" get pods -l "$selector" -o name | grep -q .; do
    [[ $SECONDS -lt $deadline ]] || {
      echo "timed out waiting for pods selected by $selector to stop" >&2
      return 1
    }
    sleep 1
  done
}

cleanup_resources() {
  if [[ "$cleanup" == "true" ]]; then
    kubectl delete namespace "$namespace" --ignore-not-found --wait=false
  fi
}

on_exit() {
  status=$?
  kubectl -n "$namespace" logs job/resilience-test --all-containers=true 2>/dev/null || true
  cleanup_resources
  exit "$status"
}
trap on_exit EXIT

require_file "$app_dir/true_producer.py"
require_file "$app_dir/true_consumer.py"
require_file "$app_dir/requirements.txt"
require_file "$script_dir/schema.sql"
require_file "$script_dir/app-user.sh"
require_file "$script_dir/verify.py"

if kubectl get namespace "$namespace" >/dev/null 2>&1; then
  if [[ "$reset" != "true" ]]; then
    echo "namespace $namespace already exists; preserve it by default. Use RESET=true to recreate it." >&2
    exit 2
  fi
  kubectl delete namespace "$namespace" --wait=true
fi
kubectl apply -f "$script_dir/namespace.yaml"

rabbitmq_password="$(openssl rand -hex 20)"
mysql_root_password="$(openssl rand -hex 20)"
mysql_password="$(openssl rand -hex 20)"

kubectl -n "$namespace" create secret generic mq-resilience-credentials \
  --from-literal=rabbitmq-user=mqtest \
  --from-literal=rabbitmq-password="$rabbitmq_password" \
  --from-literal=mysql-root-password="$mysql_root_password" \
  --from-literal=mysql-user=mqapp \
  --from-literal=mysql-password="$mysql_password"

kubectl -n "$namespace" create configmap mq-resilience-schema \
  --from-file=01-schema.sql="$script_dir/schema.sql" \
  --from-file=02-app-user.sh="$script_dir/app-user.sh"
kubectl -n "$namespace" create configmap mq-resilience-app \
  --from-file=true_producer.py="$app_dir/true_producer.py" \
  --from-file=true_consumer.py="$app_dir/true_consumer.py" \
  --from-file=requirements.txt="$app_dir/requirements.txt" \
  --from-file=verify.py="$script_dir/verify.py"

kubectl apply -f "$script_dir/stack.yaml"
kubectl -n "$namespace" rollout status deployment/rabbitmq --timeout=120s
kubectl -n "$namespace" rollout status deployment/mysql --timeout=120s
kubectl apply -f "$script_dir/job.yaml"

wait_for_log "PHASE:MYSQL_DOWN"
kubectl -n "$namespace" scale deployment/mysql --replicas=0
wait_for_no_pods app=mysql

wait_for_log "PHASE:MYSQL_UP"
kubectl -n "$namespace" scale deployment/mysql --replicas=1
kubectl -n "$namespace" rollout status deployment/mysql --timeout=120s

wait_for_log "PHASE:RABBITMQ_DOWN"
kubectl -n "$namespace" scale deployment/rabbitmq --replicas=0
wait_for_no_pods app=rabbitmq

wait_for_log "PHASE:RABBITMQ_UP"
kubectl -n "$namespace" scale deployment/rabbitmq --replicas=1
kubectl -n "$namespace" rollout status deployment/rabbitmq --timeout=120s

kubectl -n "$namespace" wait --for=condition=complete job/resilience-test --timeout=420s
echo "K3s resilience test completed successfully"
