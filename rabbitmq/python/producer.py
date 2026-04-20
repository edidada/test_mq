from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List


@dataclass
class Message:
    message_id: str
    priority: str
    body: str

    def encode(self) -> str:
        return f"{self.message_id}|{self.priority}|{self.body}"

    @staticmethod
    def decode(line: str) -> "Message":
        parts = line.rstrip("\n").split("|", 2)
        if len(parts) != 3:
            raise ValueError(f"invalid message line: {line!r}")
        return Message(parts[0], parts[1], parts[2])


class Producer:
    def __init__(self, runtime_dir: Path) -> None:
        self.runtime_dir = runtime_dir
        self.queue_file = runtime_dir / "queue.txt"
        self.outbox_file = runtime_dir / "outbox.txt"
        self.mq_up = True
        self.sent_to_queue = 0
        self.spooled_to_outbox = 0

    def set_mq_up(self, value: bool) -> None:
        self.mq_up = value

    def send(self, message: Message) -> None:
        target = self.queue_file if self.mq_up else self.outbox_file
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("a", encoding="utf-8") as handle:
            handle.write(message.encode() + "\n")
        if self.mq_up:
            self.sent_to_queue += 1
            print(f"[producer] send to queue: {message.message_id} ({message.priority})")
        else:
            self.spooled_to_outbox += 1
            print(f"[producer] mq down, spool to outbox: {message.message_id}")

    def send_batch(self, messages: Iterable[Message]) -> None:
        for message in messages:
            self.send(message)

    def replay_outbox(self) -> None:
        if not self.mq_up:
            print("[producer] mq still down, skip outbox replay")
            return
        if not self.outbox_file.exists():
            print("[producer] outbox empty, nothing to replay")
            return

        lines = self.outbox_file.read_text(encoding="utf-8").splitlines()
        if not lines:
            print("[producer] outbox empty, nothing to replay")
            return

        print(f"[producer] replay outbox messages: {len(lines)}")
        with self.queue_file.open("a", encoding="utf-8") as queue_handle:
            for line in lines:
                queue_handle.write(line + "\n")
                self.sent_to_queue += 1
        self.outbox_file.write_text("", encoding="utf-8")


def build_messages(prefix: str, count: int, critical_every: int = 0) -> List[Message]:
    messages: List[Message] = []
    for index in range(1, count + 1):
        priority = "critical" if critical_every and index % critical_every == 0 else "normal"
        message_id = f"{prefix}-{index:03d}"
        body = f"{prefix} payload #{index}"
        messages.append(Message(message_id, priority, body))
    return messages
