"""Assert that the isolated Kubernetes resilience exercise recovered fully."""

from __future__ import annotations

import os
from pathlib import Path

import pymysql


def main() -> None:
    runtime_dir = Path(os.environ["MQ_DEMO_RUNTIME_DIR"])
    for filename in ("outbox.jsonl", "pending_db.jsonl"):
        path = runtime_dir / filename
        if path.exists() and path.read_text(encoding="utf-8").strip():
            raise AssertionError(f"{filename} is not empty after recovery")

    connection = pymysql.connect(
        host=os.environ["MYSQL_HOST"],
        port=int(os.environ["MYSQL_PORT"]),
        user=os.environ["MYSQL_USER"],
        password=os.environ["MYSQL_PASSWORD"],
        database=os.environ["MYSQL_DATABASE"],
    )
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT COUNT(*) FROM processed_messages")
        count = cursor.fetchone()[0]
    finally:
        connection.close()

    if count != 7:
        raise AssertionError(f"expected 7 durable records, found {count}")
    print("verification passed: 7 records persisted; recovery stores are empty")


if __name__ == "__main__":
    main()
