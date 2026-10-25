package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.List;

/**
 * RocketMQ 顺序消息 —— 生产者端。
 *
 * RocketMQ 的顺序消息是一等公民：发送时用 MessageQueueSelector 把“同一业务 key”
 * 路由到同一个 MessageQueue；消费端配合 MessageListenerOrderly 单线程顺序消费该队列。
 * 这样就得到了“某个订单”维度的严格顺序。
 */
public class OrderedProducer {
    private static final Logger logger = LogManager.getLogger(OrderedProducer.class);

    public static void main(String[] args) throws Exception {
        String namesrv = System.getenv().getOrDefault("ROCKETMQ_NAMESRV", "127.0.0.1:9876");
        String topic = System.getenv().getOrDefault("ROCKETMQ_TOPIC", "OrderTopic");
        String group = System.getenv().getOrDefault("ROCKETMQ_PRODUCER_GROUP", "ordered_producer_group");

        DefaultMQProducer producer = new DefaultMQProducer(group);
        producer.setNamesrvAddr(namesrv);
        producer.start();

        String[] orders = {"ORDER-1001", "ORDER-1002", "ORDER-1003"};
        for (String orderId : orders) {
            for (int step = 1; step <= 5; step++) {
                String body = orderId + " 步骤" + step; // 例如 创建->支付->发货->签收->评价
                Message msg = new Message(topic, "TagA", orderId,
                        body.getBytes(RemotingHelper.DEFAULT_CHARSET));
                // 关键：MessageQueueSelector 让相同 orderId 落到同一个 MessageQueue，从而保证顺序
                SendResult res = producer.send(msg, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                        String id = (String) arg;
                        int idx = Math.abs(id.hashCode()) % mqs.size();
                        return mqs.get(idx);
                    }
                }, orderId);
                logger.info("发送 {} -> queue={}", body, res.getMessageQueue().getQueueId());
            }
        }
        producer.shutdown();
        logger.info("=== RocketMQ 顺序消息发送完成 ===");
    }
}
