"""
真正的 RabbitMQ 生产者 - 使用 pika 库
"""
import pika
from typing import Iterable, List, Optional


class Message:
    def __init__(self, message_id: str, priority: str, body: str) -> None:
        self.message_id = message_id
        self.priority = priority
        self.body = body

    def encode(self) -> str:
        return f"{self.message_id}|{self.priority}|{self.body}"

    @staticmethod
    def decode(line: str) -> "Message":
        parts = line.rstrip("\n").split("|", 2)
        if len(parts) != 3:
            raise ValueError(f"invalid message line: {line!r}")
        return Message(parts[0], parts[1], parts[2])


class TrueProducer:
    def __init__(self, host: str = "localhost", port: int = 5672, 
                 username: str = "app_user", password: str = "123456") -> None:
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.queue_name = "test_queue"
        
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.channel.Channel] = None
        self.sent_to_queue = 0
        self.spooled_to_outbox = 0

    def connect(self) -> None:
        """连接到 RabbitMQ 服务器"""
        print(f"[producer] connecting to RabbitMQ at {self.host}:{self.port}...")
        
        credentials = pika.PlainCredentials(self.username, self.password)
        parameters = pika.ConnectionParameters(
            host=self.host,
            port=self.port,
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
        )
        
        self.connection = pika.BlockingConnection(parameters)
        self.channel = self.connection.channel()
        
        # 声明持久化队列
        self.channel.queue_declare(queue=self.queue_name, durable=True)
        
        print("[producer] connected successfully")

    def send(self, message: Message) -> None:
        """发送单条消息到 RabbitMQ"""
        if self.channel is None:
            raise RuntimeError("Producer not connected. Call connect() first.")
        
        message_body = message.encode().encode('utf-8')
        
        # 设置消息属性
        properties = pika.BasicProperties(
            content_type='text/plain',
            delivery_mode=2,  # 持久化
            priority=10 if message.priority == "critical" else 0
        )
        
        # 发布消息
        self.channel.basic_publish(
            exchange='',
            routing_key=self.queue_name,
            body=message_body,
            properties=properties
        )
        
        self.sent_to_queue += 1
        print(f"[producer] sent: {message.message_id} ({message.priority})")

    def send_batch(self, messages: Iterable[Message]) -> None:
        """批量发送消息"""
        print(f"[producer] sending batch of {len(list(messages))} messages")
        for message in messages:
            self.send(message)

    def close(self) -> None:
        """关闭连接"""
        if self.channel and self.channel.is_open:
            self.channel.close()
        if self.connection and self.connection.is_open:
            self.connection.close()
        print("[producer] connection closed")

    def __enter__(self) -> "TrueProducer":
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.close()


def build_messages(prefix: str, count: int, critical_every: int = 0) -> List[Message]:
    """构建测试消息列表"""
    messages: List[Message] = []
    for index in range(1, count + 1):
        priority = "critical" if critical_every and index % critical_every == 0 else "normal"
        message_id = f"{prefix}-{index:03d}"
        body = f"{prefix} payload #{index}"
        messages.append(Message(message_id, priority, body))
    return messages


if __name__ == "__main__":
    # 示例：发送消息
    producer = TrueProducer("localhost", 5672, "app_user", "123456")
    try:
        producer.connect()
        
        # 发送 10 条消息，每 3 条中有一条 critical
        messages = build_messages("msg", 10, 3)
        producer.send_batch(messages)
        
        print("[producer] done!")
    except Exception as e:
        print(f"[producer] error: {e}")
    finally:
        producer.close()
