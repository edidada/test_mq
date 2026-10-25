package com.testmq.pulsar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

public class OrderedProducer {
    private static final Logger logger = LogManager.getLogger(OrderedProducer.class);

    public static void main(String[] args) throws Exception {
        String serviceUrl = System.getenv().getOrDefault("PULSAR_URL", "pulsar://127.0.0.1:6650");
        String topic = System.getenv().getOrDefault("PULSAR_TOPIC", "persistent://public/default/OrderTopic");

        try (PulsarClient client = PulsarClient.builder().serviceUrl(serviceUrl).build();
             Producer<String> producer = client.newProducer(Schema.STRING)
                     .topic(topic)
                     // 关闭批量可让“按 key 有序”更直观；开启批量时 Pulsar 仍按 key 保序
                     .enableBatching(false)
                     .create()) {

            String[] orders = {"ORDER-1001", "ORDER-1002", "ORDER-1003"};
            for (String orderId : orders) {
                for (int step = 1; step <= 5; step++) {
                    String value = orderId + " 步骤" + step;
                    // 关键：用业务 key（订单号）。相同 key 经 hash 路由到同一分区，分区内有序
                    MessageId id = producer.newMessage()
                            .key(orderId)
                            .value(value)
                            .send();
                    logger.info("发送 {} -> msgId={}", value, id);
                }
            }
            producer.flush();
        }
        logger.info("=== Pulsar 顺序消息发送完成 ===");
    }
}
