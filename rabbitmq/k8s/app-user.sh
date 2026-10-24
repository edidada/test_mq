#!/usr/bin/env bash
set -euo pipefail

# This only runs in the isolated exercise database. It keeps the test client
# dependency small; production deployments should retain caching_sha2_password.
mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
ALTER USER '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
SQL
