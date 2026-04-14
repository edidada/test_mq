#!/bin/bash
echo "=== RocketMQ状态检查 ==="
echo ""

cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

NS_COUNT=$(ps aux | grep mqnamesrv | grep -v grep | wc -l)
BROKER_COUNT=$(ps aux | grep mqbroker | grep -v grep | wc -l)

if [ "$NS_COUNT" -gt 0 ]; then
    echo "✅ NameServer: 运行中"
    ps aux | grep mqnamesrv | grep -v grep | awk '{print "   PID:", $2}'
else
    echo "❌ NameServer: 未运行"
fi

echo ""

if [ "$BROKER_COUNT" -gt 0 ]; then
    echo "✅ Broker: 运行中"
    ps aux | grep mqbroker | grep -v grep | awk '{print "   PID:", $2}'
else
    echo "❌ Broker: 未运行"
fi

echo ""
echo "连接信息："
echo "   NameServer: 127.0.0.1:9876"
echo "   Broker:     127.0.0.1:10911"
