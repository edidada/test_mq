package com.testmq.pulsar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;

public class OrderedConsumer {
    private static final Logger logger = LogManager.getLogger(OrderedConsumer.class);

    public static void main(String[] args) throws Exception {
        String serviceUrl = System.getenv().getOrDefault("PULSAR_URL", "pulsar://127.0.0.1:6650");
        String topic = System.getenv().getOrDefault("PULSAR_TOPIC", "persistent://public/default/OrderTopic");
        String subName = System.getenv().getOrDefault("PULSAR_SUB", "order-sub");

        try (PulsarClient client = PulsarClient.builder().serviceUrl(serviceUrl).build();
             Consumer<String> consumer = client.newConsumer(Schema.STRING)
                     .topic(topic)
                     .subscriptionName(subName)
                     // Key_Shared：相同 key 的消息分发到同一消费者且保序，同时可多消费者并行
                     // 若要求“单消费者严格顺序”，可改用 SubscriptionType.Exclusive
                     .subscriptionType(SubscriptionType.Key_Shared)
                     .subscribe()) {

            logger.info("=== 开始消费（按 key 有序）===");
            while (true) {
                Message<String> msg = consumer.receive();
                try {
                    logger.info("收到 key={} value={}", msg.getKey(), msg.getValue());
                    consumer.acknowledge(msg);
                } catch (Exception e) {
                    // 失败重试：Pulsar 会按 key 重新投递，顺序仍然保持
                    consumer.negativeAcknowledge(msg);
                }
            }
        }
    }
}
