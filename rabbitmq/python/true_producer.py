"""RabbitMQ producer with durable local outbox fallback."""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

import pika
from pika.exceptions import AMQPError


@dataclass(frozen=True)
class Message:
    message_id: str
    priority: str
    body: str

    def encode(self) -> str:
        return f"{self.message_id}|{self.priority}|{self.body}"


class JsonlStore:
    """Append-only local durable store used for outage recovery."""

    def __init__(self, path: Path) -> None:
        self.path = path

    def append(self, message: Message) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(asdict(message), ensure_ascii=False) + "\n")
            handle.flush()
            os.fsync(handle.fileno())

    def read(self) -> list[Message]:
        if not self.path.exists():
            return []
        return [Message(**json.loads(line)) for line in self.path.read_text(encoding="utf-8").splitlines() if line]

    def replace(self, messages: Iterable[Message]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.path.with_suffix(self.path.suffix + ".tmp")
        with temporary.open("w", encoding="utf-8") as handle:
            for message in messages:
                handle.write(json.dumps(asdict(message), ensure_ascii=False) + "\n")
            handle.flush()
            os.fsync(handle.fileno())
        temporary.replace(self.path)


class TrueProducer:
    def __init__(self) -> None:
        self.host = os.getenv("RABBITMQ_HOST", "localhost")
        self.port = int(os.getenv("RABBITMQ_PORT", "5672"))
        # guest is deliberately the local-development default. Use a dedicated
        # account from environment variables in production.
        self.username = os.getenv("RABBITMQ_USERNAME", "guest")
        self.password = os.getenv("RABBITMQ_PASSWORD", "guest")
        self.queue_name = os.getenv("RABBITMQ_QUEUE", "test_queue")
        runtime_dir = Path(os.getenv("MQ_DEMO_RUNTIME_DIR", Path(__file__).resolve().parent / "runtime"))
        self.outbox = JsonlStore(runtime_dir / "outbox.jsonl")
        self.connection: pika.BlockingConnection | None = None
        self.channel: pika.channel.Channel | None = None

    def connect(self) -> None:
        if self.connection and self.connection.is_open:
            return
        print(f"[producer] connecting to RabbitMQ at {self.host}:{self.port}...")
        parameters = pika.ConnectionParameters(
            host=self.host,
            port=self.port,
            credentials=pika.PlainCredentials(self.username, self.password),
            connection_attempts=3,
            retry_delay=1,
            socket_timeout=3,
            blocked_connection_timeout=5,
        )
        self.connection = pika.BlockingConnection(parameters)
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
        self.channel.confirm_delivery()
        print("[producer] connected successfully")

    def _publish(self, message: Message) -> None:
        self.connect()
        assert self.channel is not None
        self.channel.basic_publish(
            exchange="",
            routing_key=self.queue_name,
            body=message.encode().encode("utf-8"),
            properties=pika.BasicProperties(
                content_type="text/plain",
                delivery_mode=2,
                priority=10 if message.priority == "critical" else 0,
                message_id=message.message_id,
            ),
            mandatory=True,
        )

    def send(self, message: Message) -> bool:
        try:
            self._publish(message)
            print(f"[producer] sent: {message.message_id} ({message.priority})")
            return True
        except (AMQPError, OSError) as exc:
            self.close()
            self.outbox.append(message)
            print(f"[producer] RabbitMQ unavailable, saved to outbox: {message.message_id} ({exc!r})")
            return False

    def send_batch(self, messages: Iterable[Message]) -> None:
        batch = list(messages)
        print(f"[producer] sending batch of {len(batch)} messages")
        for message in batch:
            self.send(message)

    def replay_outbox(self) -> None:
        remaining: list[Message] = []
        stored = self.outbox.read()
        print(f"[producer] replaying {len(stored)} outbox messages")
        for index, message in enumerate(stored):
            try:
                self._publish(message)
                print(f"[producer] replayed: {message.message_id}")
            except (AMQPError, OSError) as exc:
                self.close()
                remaining.extend(stored[index:])
                print(f"[producer] replay paused; RabbitMQ is unavailable ({exc!r})")
                break
        self.outbox.replace(remaining)

    def close(self) -> None:
        if self.channel and self.channel.is_open:
            self.channel.close()
        if self.connection and self.connection.is_open:
            self.connection.close()
        self.channel = None
        self.connection = None


def build_messages(prefix: str, count: int, critical_every: int = 0) -> list[Message]:
    return [
        Message(
            message_id=f"{prefix}-{index:03d}",
            priority="critical" if critical_every and index % critical_every == 0 else "normal",
            body=f"{prefix} payload #{index}",
        )
        for index in range(1, count + 1)
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--prefix", default="msg")
    parser.add_argument("--replay-only", action="store_true")
    args = parser.parse_args()

    producer = TrueProducer()
    try:
        producer.replay_outbox()
        if not args.replay_only:
            producer.send_batch(build_messages(args.prefix, args.count, critical_every=3))
    finally:
        producer.close()


if __name__ == "__main__":
    main()
