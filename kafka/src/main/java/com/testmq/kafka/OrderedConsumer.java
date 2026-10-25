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
 * Kafka 顺序消息 —— 消费者端示例。
 *
 * 消费端要做到「顺序」，核心是两点：
 *   1) 同一个分区内的记录天然有序，必须「按分区、按 offset 顺序」逐条处理，
 *      绝不能在单个分区内做并行消费；
 *   2) 关闭自动提交（enable.auto.commit=false），在处理成功后再手动提交 offset，
 *      否则进程崩溃可能在「处理了但没提交」时重放，或在「没处理就提交」时丢失。
 *
 * 注意：跨分区之间没有顺序保证。如果业务要求「全局唯一顺序」，那就只能消费单分区 topic。
 */
public class OrderedConsumer {
    private static final Logger logger = LogManager.getLogger(OrderedConsumer.class);

    public static void main(String[] args) {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "127.0.0.1:9092");
        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", "OrderTopic");
        String groupId = System.getenv().getOrDefault("KAFKA_GROUP", "order-group");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // 关闭自动提交：必须“处理完后再提交”，否则崩溃会重复/乱序消费
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 从最早开始消费，方便演示
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            logger.info("=== 开始消费（分区内有序）===");
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                // 按分区逐个处理：同一个分区内的记录天然有序，绝不能跨分区并行处理同一分区
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        // 业务处理：这里只是打印，但必须“顺序且同步”地消费同一分区的消息
                        logger.info("分区{} 收到 key={} value={} offset={}",
                                partition.partition(), record.key(), record.value(), record.offset());
                    }
                    // 整批处理完后，提交该分区当前进度（也可逐条提交）
                    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(
                            partition, new OffsetAndMetadata(lastOffset + 1)));
                }
            }
        }
    }
}
