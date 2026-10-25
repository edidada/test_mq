package com.testmq.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * Kafka 顺序消息 —— 生产者端示例。
 *
 * 顺序消息并不是 Kafka 的一个独立开关，而是由三件事共同保证的：
 *   1) 同一业务的消息落到「同一个分区」（靠 key / 自定义分区器 / 单分区 topic）；
 *   2) 发送端在重试时不会把消息「重排」（靠 acks=all + 幂等，或 max.in.flight=1）；
 *   3) 消费端按分区顺序处理并提交 offset（见 OrderedConsumer）。
 *
 * 本例模拟 3 个订单，每个订单产生 5 个有先后关系的步骤事件，
 * 用「订单号」作为消息 key，从而每个订单的步骤都进入同一分区、保持先后顺序。
 */
public class OrderedProducer {
    private static final Logger logger = LogManager.getLogger(OrderedProducer.class);

    public static void main(String[] args) throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "127.0.0.1:9092");
        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", "OrderTopic");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // —— ① 保证「发送端不重排」的关键配置 ——
        // acks=all：leader 和 ISR 中的 follower 都落盘才返回，避免丢消息造成“漏序”
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // enable.idempotence=true：开启幂等。开启后 Kafka 会自动把 retries 设为最大值，
        // 并允许 max.in.flight.requests.per.connection 仍为 5，且保证重试不会重排。
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // 如果你不使用幂等，就必须把下面这个值设为 1，才能在发送失败重试时保持顺序：
        //   props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        // —— ② 路由到同一分区 ——
        // 方式 A（推荐）：用业务 key，默认分区器会按 key 的 hash 固定分区，本例就用这种方式。
        // 方式 B：使用自定义分区器，精确控制路由规则（见 OrderPartitioner）。
        // 方式 C：把 topic 建成「单分区」，可得到全局唯一顺序，但吞吐受限。
        // props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, OrderPartitioner.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String[] orders = {"ORDER-1001", "ORDER-1002", "ORDER-1003"};
            for (String orderId : orders) {
                for (int step = 1; step <= 5; step++) {
                    String value = orderId + " 步骤" + step; // 例如「创建->支付->发货->签收->评价」
                    // key = orderId：决定落到哪个分区；value = 业务内容
                    ProducerRecord<String, String> record =
                            new ProducerRecord<>(topic, orderId, value);
                    // 同步发送：拿到元数据确认落到了哪个分区，顺序语义最直观、最稳
                    RecordMetadata meta = producer.send(record).get();
                    logger.info("发送 {} -> 分区{} 偏移{}", value, meta.partition(), meta.offset());
                }
            }
            producer.flush();
        }
        logger.info("=== 顺序消息发送完成 ===");
    }
}
