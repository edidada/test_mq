#!/usr/bin/env bash
set -euo pipefail

test_namespace="${NAMESPACE:-mq-resilience-test}"
middleware_namespace="${MIDDLEWARE_NAMESPACE:-middleware}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
app_dir="${APP_DIR:-${script_dir}/../python}"
reset="${RESET:-false}"
migrate_legacy_data="${MIGRATE_LEGACY_DATA:-false}"
remove_legacy="${REMOVE_LEGACY_TEST_MIDDLEWARE:-false}"

require_file() {
  [[ -f "$1" ]] || { echo "required file is missing: $1" >&2; exit 1; }
}

secret_value() {
  kubectl -n "$1" get secret "$2" -o jsonpath="{.data.$3}" | base64 -d
}

apply_test_credentials() {
  local rabbitmq_user rabbitmq_password mysql_user mysql_password
  rabbitmq_user="$(secret_value "$middleware_namespace" mq-resilience-credentials rabbitmq-user)"
  rabbitmq_password="$(secret_value "$middleware_namespace" mq-resilience-credentials rabbitmq-password)"
  mysql_user="$(secret_value "$middleware_namespace" mq-resilience-credentials mysql-user)"
  mysql_password="$(secret_value "$middleware_namespace" mq-resilience-credentials mysql-password)"

  kubectl -n "$test_namespace" create secret generic mq-resilience-credentials \
    --from-literal=rabbitmq-user="$rabbitmq_user" \
    --from-literal=rabbitmq-password="$rabbitmq_password" \
    --from-literal=mysql-user="$mysql_user" \
    --from-literal=mysql-password="$mysql_password" \
    --dry-run=client -o yaml | kubectl apply -f -
}

ensure_shared_credentials() {
  if kubectl -n "$middleware_namespace" get secret mq-resilience-credentials >/dev/null 2>&1; then
    return
  fi

  local rabbitmq_user rabbitmq_password mysql_user mysql_password
  if kubectl -n "$test_namespace" get secret mq-resilience-credentials >/dev/null 2>&1; then
    rabbitmq_user="$(secret_value "$test_namespace" mq-resilience-credentials rabbitmq-user)"
    rabbitmq_password="$(secret_value "$test_namespace" mq-resilience-credentials rabbitmq-password)"
    mysql_user="$(secret_value "$test_namespace" mq-resilience-credentials mysql-user)"
    mysql_password="$(secret_value "$test_namespace" mq-resilience-credentials mysql-password)"
  else
    rabbitmq_user="mqtest"
    rabbitmq_password="$(openssl rand -hex 20)"
    mysql_user="mqapp"
    mysql_password="$(openssl rand -hex 20)"
  fi

  kubectl -n "$middleware_namespace" create secret generic mq-resilience-credentials \
    --from-literal=rabbitmq-user="$rabbitmq_user" \
    --from-literal=rabbitmq-password="$rabbitmq_password" \
    --from-literal=mysql-user="$mysql_user" \
    --from-literal=mysql-password="$mysql_password"
}

configure_shared_database() {
  local shared_root_password mysql_user mysql_password
  shared_root_password="$(secret_value "$middleware_namespace" mysql-secret mysql-root-password)"
  mysql_user="$(secret_value "$middleware_namespace" mq-resilience-credentials mysql-user)"
  mysql_password="$(secret_value "$middleware_namespace" mq-resilience-credentials mysql-password)"

  kubectl -n "$middleware_namespace" exec deployment/mysql -- env MYSQL_PWD="$shared_root_password" \
    mysql -uroot -e "CREATE DATABASE IF NOT EXISTS mqresilience; CREATE USER IF NOT EXISTS '$mysql_user'@'%' IDENTIFIED BY '$mysql_password'; ALTER USER '$mysql_user'@'%' IDENTIFIED BY '$mysql_password'; GRANT ALL PRIVILEGES ON mqresilience.* TO '$mysql_user'@'%'; FLUSH PRIVILEGES;"
  kubectl -n "$middleware_namespace" exec -i deployment/mysql -- env MYSQL_PWD="$shared_root_password" \
    mysql -uroot mqresilience < "$script_dir/schema.sql"
}

migrate_legacy_mysql_data() {
  if [[ "$migrate_legacy_data" != "true" ]] || ! kubectl -n "$test_namespace" get deployment/mysql >/dev/null 2>&1; then
    return
  fi

  local legacy_root_password shared_root_password
  legacy_root_password="$(secret_value "$test_namespace" mq-resilience-credentials mysql-root-password)"
  shared_root_password="$(secret_value "$middleware_namespace" mysql-secret mysql-root-password)"
  kubectl -n "$test_namespace" exec deployment/mysql -- env MYSQL_PWD="$legacy_root_password" \
    mysqldump --no-tablespaces --single-transaction mqresilience | \
    kubectl -n "$middleware_namespace" exec -i deployment/mysql -- env MYSQL_PWD="$shared_root_password" mysql -uroot mqresilience
  echo "legacy mqresilience data imported into middleware/mysql"
}

for file in \
  "$script_dir/namespace.yaml" \
  "$script_dir/middleware-pvc.yaml" \
  "$script_dir/stack.yaml" \
  "$script_dir/job.yaml" \
  "$script_dir/schema.sql" \
  "$script_dir/verify.py" \
  "$app_dir/true_producer.py" \
  "$app_dir/true_consumer.py" \
  "$app_dir/requirements.txt"; do
  require_file "$file"
done

if ! kubectl get namespace "$middleware_namespace" >/dev/null 2>&1; then
  echo "required middleware namespace $middleware_namespace does not exist" >&2
  exit 2
fi

if kubectl get namespace "$test_namespace" >/dev/null 2>&1; then
  if [[ "$reset" == "true" ]]; then
    kubectl delete namespace "$test_namespace" --wait=true
  elif [[ "$migrate_legacy_data" != "true" ]]; then
    echo "namespace $test_namespace already exists; preserve it by default. Use RESET=true to recreate it or MIGRATE_LEGACY_DATA=true to move legacy MySQL data." >&2
    exit 2
  fi
fi

kubectl apply -f "$script_dir/namespace.yaml"
ensure_shared_credentials
kubectl -n "$middleware_namespace" rollout status deployment/mysql --timeout=180s
configure_shared_database
migrate_legacy_mysql_data
apply_test_credentials
kubectl -n "$test_namespace" create configmap mq-resilience-app \
  --from-file=true_producer.py="$app_dir/true_producer.py" \
  --from-file=true_consumer.py="$app_dir/true_consumer.py" \
  --from-file=requirements.txt="$app_dir/requirements.txt" \
  --from-file=verify.py="$script_dir/verify.py" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f "$script_dir/middleware-pvc.yaml"
kubectl apply -f "$script_dir/stack.yaml"
kubectl -n "$middleware_namespace" rollout status deployment/rabbitmq --timeout=180s

kubectl -n "$test_namespace" delete job resilience-test --ignore-not-found
kubectl apply -f "$script_dir/job.yaml"
kubectl -n "$test_namespace" wait --for=condition=complete job/resilience-test --timeout=600s
kubectl -n "$test_namespace" logs job/resilience-test

if [[ "$remove_legacy" == "true" ]]; then
  kubectl -n "$test_namespace" delete deployment rabbitmq mysql --ignore-not-found
  kubectl -n "$test_namespace" delete service rabbitmq mysql --ignore-not-found
  kubectl -n "$test_namespace" delete pvc mysql-data --ignore-not-found
  kubectl -n "$test_namespace" delete configmap mq-resilience-schema --ignore-not-found
fi

echo "Shared-middleware resilience test completed successfully"
