package com.testmq.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * “一个消息多个消费者”演示 —— 消费者。
 *
 * 启动多个本进程的“不同 group”实例（用环境变量 KAFKA_GROUP 区分），
 * 例如一个用 broadcast-group-A，另一个用 broadcast-group-B，
 * 它们都会各自收到 PubSubProducer 发出的全部 5 条消息：
 *   一条消息 -> 多个消费者（每个消费组一个）。
 *
 * 注意与“竞争消费”的区别：若在【同一个 group】里起多个消费者，
 * 同一条消息只会被组内某一个消费者拿到，那是负载均衡，不是广播。
 */
public class PubSubConsumer {
    private static final Logger logger = LogManager.getLogger(PubSubConsumer.class);

    public static void main(String[] args) {
        String bootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "127.0.0.1:9092");
        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", "BroadcastTopic");
        // 不同 group 的消费者各自收到一份完整消息；这就是“一个消息多个消费者”
        String group = System.getenv().getOrDefault("KAFKA_GROUP", "broadcast-group-A");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            logger.info("消费者组={} 启动，等待消息...", group);
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (TopicPartition p : records.partitions()) {
                    List<ConsumerRecord<String, String>> list = records.records(p);
                    for (ConsumerRecord<String, String> r : list) {
                        logger.info("组={} 收到 value={} offset={}", group, r.value(), r.offset());
                    }
                    long last = list.get(list.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(p, new OffsetAndMetadata(last + 1)));
                }
            }
        }
    }
}
