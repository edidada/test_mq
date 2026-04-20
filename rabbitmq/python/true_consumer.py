"""
真正的 RabbitMQ 消费者 - 使用 pika 库
"""
import pika
import time
from typing import Optional, Callable


class TrueConsumer:
    def __init__(self, host: str = "localhost", port: int = 5672,
                 username: str = "guest", password: str = "guest") -> None:
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.queue_name = "test_queue"
        
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.channel.Channel] = None
        self.message_count = 0
        self.mysql_up = True
        self.persisted = 0
        self.retried = 0
        self.pending = 0

    def connect(self) -> None:
        """连接到 RabbitMQ 服务器"""
        print(f"[consumer] connecting to RabbitMQ at {self.host}:{self.port}...")
        
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
        
        # 设置 QoS - 每次只处理一条消息
        self.channel.basic_qos(prefetch_count=1)
        
        print("[consumer] connected successfully")

    def start_consuming(self, expected_messages: int = 0) -> None:
        """开始消费消息"""
        if self.channel is None:
            raise RuntimeError("Consumer not connected. Call connect() first.")
        
        def callback(channel, method, properties, body):
            """消息回调函数"""
            message_str = body.decode('utf-8')
            print(f"[consumer] received: {message_str}")
            
            # 解析消息
            parts = message_str.split("|", 2)
            if len(parts) == 3:
                message_id, priority, msg_body = parts
                print(f"  [processing] {message_id} - priority: {priority}, body: {msg_body}")
            
            # 模拟业务处理
            self._process_message(message_str)
            
            # 手动 ACK
            channel.basic_ack(delivery_tag=method.delivery_tag)
            self.message_count += 1
            self.persisted += 1
            
            print(f"[consumer] processed message #{self.message_count}")
        
        # 开始消费
        print(f"[consumer] waiting for messages...")
        self.channel.basic_consume(
            queue=self.queue_name,
            on_message_callback=callback,
            auto_ack=False  # 手动 ACK
        )
        
        # 如果指定了期望消息数，则消费相应数量后停止
        if expected_messages > 0:
            for _ in range(expected_messages):
                self.connection.process_data_events(time_limit=1)
                if self.message_count >= expected_messages:
                    break
            print(f"[consumer] processed {self.message_count} messages")
        else:
            # 持续消费
            self.channel.start_consuming()

    def _process_message(self, message: str) -> None:
        """处理消息（模拟业务逻辑）"""
        # 这里可以添加实际的业务逻辑
        time.sleep(0.01)  # 模拟处理时间

    def stop_consuming(self) -> None:
        """停止消费"""
        if self.channel and self.channel.is_open:
            self.channel.stop_consuming()
            print("[consumer] stopped consuming")

    def close(self) -> None:
        """关闭连接"""
        if self.channel and self.channel.is_open:
            self.channel.close()
        if self.connection and self.connection.is_open:
            self.connection.close()
        print("[consumer] connection closed")

    def __enter__(self) -> "TrueConsumer":
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.close()


if __name__ == "__main__":
    # 示例：消费消息
    consumer = TrueConsumer()
    try:
        consumer.connect()
        consumer.start_consuming(expected_messages=10)
        print("[consumer] done!")
    except KeyboardInterrupt:
        print("[consumer] interrupted by user")
    except Exception as e:
        print(f"[consumer] error: {e}")
    finally:
        consumer.close()
