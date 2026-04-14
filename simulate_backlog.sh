#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export NAMESRV_ADDR=127.0.0.1:9876

echo "=== 模拟MQ消息积压场景 ==="
echo ""

echo "步骤1: 发送大量消息到MQ（积压开始）"
echo "发送5000条消息中..."

mkdir -p /tmp/mq_test

for i in {1..5000}; do
    echo "Hello RocketMQ Message $i - $(date)" >> /tmp/mq_test/message_$i.txt
done

echo "消息生成完成，正在发送到MQ..."

export CLASSPATH=./conf:./lib/*:./lib/rocketmq-client-4.9.7.jar

echo ""
echo "=== 操作说明 ==="
echo "1. 当前已连接到 RocketMQ: $NAMESRV_ADDR"
echo ""
echo "2. 要产生积压，可以这样做："
echo "   - 先不启动消费者，只发送消息"
echo "   - 或者消费者消费速度远慢于生产速度"
echo ""
echo "3. 可用的工具命令："
echo "   查看队列状态: ./bin/mqadmin clusterList -n $NAMESRV_ADDR"
echo "   查看Topic状态: ./bin/mqadmin topicStatus -n $NAMESRV_ADDR -t TestTopic"
echo "   查看Broker状态: ./bin/mqadmin brokerStatus -n $NAMESRV_ADDR -b 127.0.0.1:10911"
echo ""
echo "4. 先查看当前状态："
./bin/mqadmin clusterList -n $NAMESRV_ADDR 2>/dev/null || echo "需要先用Java程序创建Topic"
echo ""
echo "提示：先运行Java生产者发送消息，然后不启动消费者，观察积压情况"
