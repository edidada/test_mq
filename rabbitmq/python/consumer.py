from __future__ import annotations

from pathlib import Path
from typing import List, Tuple

from producer import Message


class Consumer:
    def __init__(self, runtime_dir: Path) -> None:
        self.runtime_dir = runtime_dir
        self.queue_file = runtime_dir / "queue.txt"
        self.pending_db_file = runtime_dir / "pending_db.txt"
        self.dead_letter_file = runtime_dir / "dead_letter.txt"
        self.db_file = runtime_dir / "db.txt"
        self.mysql_up = True
        self.persisted = 0
        self.retried = 0
        self.degraded = 0
        self.pending = 0

    def set_mysql_up(self, value: bool) -> None:
        self.mysql_up = value

    def process_queue(self, backlog_threshold: int = 6, max_retry: int = 2) -> None:
        messages = self._read_messages(self.queue_file)
        if not messages:
            print("[consumer] queue empty")
            return

        backlog = len(messages)
        print(f"[consumer] start consume, backlog={backlog}")

        keep: List[Message] = []
        if backlog > backlog_threshold:
            print("[consumer] backlog threshold exceeded, enter surge mode")
            for message in messages:
                if message.priority == "critical":
                    keep.append(message)
                elif len(keep) < backlog_threshold:
                    keep.append(message)
                else:
                    self._append(self.dead_letter_file, message)
                    self.degraded += 1
                    print(f"[consumer] degrade low priority message: {message.message_id}")
        else:
            keep = messages

        self.queue_file.write_text("", encoding="utf-8")
        for message in keep:
            self._persist_with_retry(message, max_retry)

    def flush_pending_db(self) -> None:
        messages = self._read_messages(self.pending_db_file)
        if not messages:
            print("[consumer] pending_db empty")
            return
        if not self.mysql_up:
            print("[consumer] mysql still down, skip pending_db flush")
            return

        print(f"[consumer] flush pending_db messages: {len(messages)}")
        self.pending_db_file.write_text("", encoding="utf-8")
        for message in messages:
            self._append(self.db_file, message)
            self.persisted += 1

    def _persist_with_retry(self, message: Message, max_retry: int) -> None:
        if self.mysql_up:
            self._append(self.db_file, message)
            self.persisted += 1
            print(f"[consumer] persist to mysql: {message.message_id}")
            return

        for retry in range(1, max_retry + 1):
            self.retried += 1
            print(f"[consumer] mysql down, retry {retry}/{max_retry}: {message.message_id}")

        self._append(self.pending_db_file, message)
        self.pending += 1
        print(f"[consumer] write to pending_db: {message.message_id}")

    def summary(self) -> Tuple[int, int, int, int]:
        return self.persisted, self.retried, self.degraded, self.pending

    @staticmethod
    def _append(target: Path, message: Message) -> None:
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("a", encoding="utf-8") as handle:
            handle.write(message.encode() + "\n")

    @staticmethod
    def _read_messages(target: Path) -> List[Message]:
        if not target.exists():
            return []
        lines = [line for line in target.read_text(encoding="utf-8").splitlines() if line.strip()]
        return [Message.decode(line) for line in lines]
