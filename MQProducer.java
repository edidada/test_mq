import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class MQProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("test_producer_group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        System.out.println("=== 开始发送消息 ===");
        for (int i = 1; i <= 1000; i++) {
            try {
                Message msg = new Message(
                    "TestTopic",
                    "TagA",
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET)
                );
                SendResult sendResult = producer.send(msg);
                if (i % 100 == 0) {
                    System.out.println("已发送 " + i + " 条消息");
                }
            } catch (Exception e) {
                System.out.println("发送失败: " + e.getMessage());
            }
        }
        System.out.println("=== 发送完成 ===");

        producer.shutdown();
    }
}
