"""RabbitMQ consumer with MySQL persistence and outage-safe local fallback."""

from __future__ import annotations

import argparse
import os
import time
from pathlib import Path

import pymysql
import pika
from pika.exceptions import AMQPError

from true_producer import JsonlStore, Message


class MySQLRepository:
    def __init__(self) -> None:
        self.config = {
            "host": os.getenv("MYSQL_HOST", "localhost"),
            "port": int(os.getenv("MYSQL_PORT", "3306")),
            "user": os.getenv("MYSQL_USER", "mq_demo"),
            "password": os.getenv("MYSQL_PASSWORD", "mq_demo_dev_password"),
            "database": os.getenv("MYSQL_DATABASE", "mq_demo"),
            "connect_timeout": 3,
            # The local WSL MySQL instance is reached over loopback. Explicitly
            # disable TLS so the distro's older connector remains compatible
            # with Python 3.12, which no longer exposes ssl.wrap_socket.
        }

    def persist(self, message: Message) -> None:
        connection = pymysql.connect(**self.config)
        try:
            cursor = connection.cursor()
            cursor.execute(
                """
                INSERT INTO processed_messages (message_id, priority, body)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE priority = VALUES(priority), body = VALUES(body)
                """,
                (message.message_id, message.priority, message.body),
            )
            connection.commit()
        finally:
            connection.close()

    def persist_with_retry(self, message: Message) -> None:
        attempts = int(os.getenv("MYSQL_RETRY_ATTEMPTS", "3"))
        delay_seconds = float(os.getenv("MYSQL_RETRY_DELAY_SECONDS", "0.2"))
        last_error: pymysql.MySQLError | None = None

        for attempt in range(1, attempts + 1):
            try:
                self.persist(message)
                return
            except pymysql.MySQLError as exc:
                last_error = exc
                if attempt < attempts:
                    print(f"[consumer] MySQL retry {attempt}/{attempts}: {message.message_id}")
                    time.sleep(delay_seconds * attempt)

        assert last_error is not None
        raise last_error


class TrueConsumer:
    def __init__(self) -> None:
        self.host = os.getenv("RABBITMQ_HOST", "localhost")
        self.port = int(os.getenv("RABBITMQ_PORT", "5672"))
        self.username = os.getenv("RABBITMQ_USERNAME", "guest")
        self.password = os.getenv("RABBITMQ_PASSWORD", "guest")
        self.queue_name = os.getenv("RABBITMQ_QUEUE", "test_queue")
        runtime_dir = Path(os.getenv("MQ_DEMO_RUNTIME_DIR", Path(__file__).resolve().parent / "runtime"))
        self.pending_db = JsonlStore(runtime_dir / "pending_db.jsonl")
        self.repository = MySQLRepository()
        self.connection: pika.BlockingConnection | None = None
        self.channel: pika.channel.Channel | None = None
        self.persisted = 0
        self.deferred = 0
        self.dead_lettered = 0

    def connect(self) -> None:
        if self.connection and self.connection.is_open:
            return
        print(f"[consumer] connecting to RabbitMQ at {self.host}:{self.port}...")
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=self.host,
                port=self.port,
                credentials=pika.PlainCredentials(self.username, self.password),
                connection_attempts=3,
                retry_delay=1,
                socket_timeout=3,
            )
        )
        self.channel = self.connection.channel()
        self.channel.queue_declare(queue=f"{self.queue_name}.dlq", durable=True)
        self.channel.queue_declare(
            queue=self.queue_name,
            durable=True,
            arguments={
                "x-max-priority": 10,
                "x-dead-letter-exchange": "",
                "x-dead-letter-routing-key": f"{self.queue_name}.dlq",
            },
        )
        self.channel.basic_qos(prefetch_count=1)
        print("[consumer] connected successfully")

    def consume(self, expected_messages: int, idle_timeout_seconds: int = 5) -> None:
        self.connect()
        assert self.channel is not None
        handled = 0
        deadline = time.monotonic() + idle_timeout_seconds
        while handled < expected_messages and time.monotonic() < deadline:
            method, _properties, body = self.channel.basic_get(queue=self.queue_name, auto_ack=False)
            if method is None:
                time.sleep(0.2)
                continue

            try:
                message_id, priority, payload = body.decode("utf-8").split("|", 2)
                message = Message(message_id, priority, payload)
            except ValueError:
                self.channel.basic_nack(method.delivery_tag, requeue=False)
                self.dead_lettered += 1
                handled += 1
                print("[consumer] malformed message routed to dead-letter queue")
                continue

            try:
                self.repository.persist_with_retry(message)
                self.persisted += 1
                print(f"[consumer] persisted: {message.message_id}")
            except pymysql.MySQLError as exc:
                # The local append is durable before ACK, so the message can be
                # compensated after MySQL is restored without redelivery storms.
                self.pending_db.append(message)
                self.deferred += 1
                print(f"[consumer] MySQL unavailable; saved to pending_db: {message.message_id} ({exc!r})")

            self.channel.basic_ack(method.delivery_tag)
            handled += 1
            deadline = time.monotonic() + idle_timeout_seconds

        print(
            f"[consumer] handled={handled}, persisted={self.persisted}, "
            f"deferred={self.deferred}, dead_lettered={self.dead_lettered}"
        )

    def flush_pending_db(self) -> None:
        stored = self.pending_db.read()
        remaining: list[Message] = []
        print(f"[consumer] replaying {len(stored)} pending_db messages")
        for index, message in enumerate(stored):
            try:
                self.repository.persist_with_retry(message)
                self.persisted += 1
                print(f"[consumer] persisted pending message: {message.message_id}")
            except pymysql.MySQLError as exc:
                remaining.extend(stored[index:])
                print(f"[consumer] MySQL still unavailable; replay paused ({exc!r})")
                break
        self.pending_db.replace(remaining)

    def close(self) -> None:
        if self.channel and self.channel.is_open:
            self.channel.close()
        if self.connection and self.connection.is_open:
            self.connection.close()
        self.channel = None
        self.connection = None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--flush-pending-only", action="store_true")
    args = parser.parse_args()

    consumer = TrueConsumer()
    try:
        if args.flush_pending_only:
            consumer.flush_pending_db()
        else:
            consumer.consume(args.count)
    except AMQPError as exc:
        print(f"[consumer] RabbitMQ error: {exc!r}")
        raise SystemExit(1) from exc
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
