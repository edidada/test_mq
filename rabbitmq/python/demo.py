from __future__ import annotations

from pathlib import Path
import shutil

from consumer import Consumer
from producer import Producer, build_messages


def reset_runtime(runtime_dir: Path) -> None:
    if runtime_dir.exists():
        shutil.rmtree(runtime_dir)
    runtime_dir.mkdir(parents=True, exist_ok=True)


def count_lines(path: Path) -> int:
    if not path.exists():
        return 0
    return len([line for line in path.read_text(encoding="utf-8").splitlines() if line.strip()])


def main() -> None:
    runtime_dir = Path(__file__).resolve().parent / "runtime"
    reset_runtime(runtime_dir)

    producer = Producer(runtime_dir)
    consumer = Consumer(runtime_dir)

    print("=== Python RabbitMQ resilience demo ===")

    print("\n[scenario-1] backlog")
    producer.send_batch(build_messages("backlog", 10, critical_every=4))
    print(f"[state] queue backlog before consume: {count_lines(runtime_dir / 'queue.txt')}")
    consumer.process_queue(backlog_threshold=6, max_retry=2)
    print(f"[state] queue backlog after consume: {count_lines(runtime_dir / 'queue.txt')}")

    print("\n[scenario-2] mq down")
    producer.set_mq_up(False)
    producer.send_batch(build_messages("mq-down", 4, critical_every=2))
    print(f"[state] outbox size while mq down: {count_lines(runtime_dir / 'outbox.txt')}")
    producer.set_mq_up(True)
    producer.replay_outbox()
    print(f"[state] queue size after mq recovery: {count_lines(runtime_dir / 'queue.txt')}")

    print("\n[scenario-3] mysql down")
    consumer.set_mysql_up(False)
    consumer.process_queue(backlog_threshold=20, max_retry=2)
    print(f"[state] pending_db size while mysql down: {count_lines(runtime_dir / 'pending_db.txt')}")
    consumer.set_mysql_up(True)
    consumer.flush_pending_db()
    print(f"[state] db size after mysql recovery: {count_lines(runtime_dir / 'db.txt')}")

    print("\n[scenario-4] mq and mysql both down")
    producer.set_mq_up(False)
    consumer.set_mysql_up(False)
    producer.send_batch(build_messages("double-down", 3, critical_every=1))
    print(f"[state] outbox size during double-down: {count_lines(runtime_dir / 'outbox.txt')}")
    producer.set_mq_up(True)
    producer.replay_outbox()
    consumer.process_queue(backlog_threshold=20, max_retry=2)
    print(f"[state] pending_db size after double-down consume: {count_lines(runtime_dir / 'pending_db.txt')}")
    consumer.set_mysql_up(True)
    consumer.flush_pending_db()

    persisted, retried, degraded, pending = consumer.summary()
    print("\n=== Summary ===")
    print(f"persisted={persisted}")
    print(f"retried={retried}")
    print(f"degraded={degraded}")
    print(f"pending_written={pending}")
    print(f"db_records={count_lines(runtime_dir / 'db.txt')}")
    print(f"dead_letters={count_lines(runtime_dir / 'dead_letter.txt')}")
    print(f"outbox_remaining={count_lines(runtime_dir / 'outbox.txt')}")
    print(f"pending_remaining={count_lines(runtime_dir / 'pending_db.txt')}")


if __name__ == "__main__":
    main()
