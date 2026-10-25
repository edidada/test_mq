package com.testmq.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * 自定义分区器：演示「相同业务 key 永远落到同一分区」。
 *
 * 实际上 Kafka 默认分区器对「带 key 的消息」已经是这个行为（按 key 的 murmur2 hash 取模）。
 * 自己写分区器的价值在于：可以按你的业务规则精确路由，例如：
 *   - 按用户 ID 取模到固定分区；
 *   - 按地区 / 租户映射到不同分区；
 *   - 某些 key 强制走同一个分区以保证顺序。
 *
 * 顺序消息的根基就一句话：同一个业务实体的所有消息，必须进同一个分区。
 */
public class OrderPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        // 没有 key 时，退化为按 value 散列（也可改成轮询/黏性分区）
        if (keyBytes == null) {
            return Utils.toPositive(Utils.murmur2(valueBytes)) % numPartitions;
        }

        // 有 key：相同 key 永远落在同一分区 —— 这是分区内有序的前提
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
