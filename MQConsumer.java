import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class MQConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test_consumer_group");
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.subscribe("TestTopic", "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            private int count = 0;
            
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        count++;
                        String body = new String(msg.getBody());
                        if (count % 50 == 0) {
                            System.out.println("已消费 " + count + " 条消息: " + body);
                        }
                        
                        Thread.sleep(10);
                        
                    } catch (Exception e) {
                        System.out.println("消费失败: " + e.getMessage());
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        System.out.println("=== 消费者启动，等待消息... ===");
        consumer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== 消费者停止 ===");
            consumer.shutdown();
        }));
    }
}
