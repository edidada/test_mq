#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "=== RocketMQ启动脚本 ==="
echo ""

pkill -9 java 2>/dev/null || true
sleep 2

echo "1. 启动NameServer..."
./bin/mqnamesrv &
sleep 4
echo "   NameServer 启动完成"

echo "2. 启动Broker..."
./bin/mqbroker -n 127.0.0.1:9876 &
sleep 6

echo ""
echo "🎉 RocketMQ 服务已启动！"
echo "   NameServer: 127.0.0.1:9876"
echo "   Broker:     127.0.0.1:10911"
echo ""
echo "检查服务状态："
ps aux | grep -E "(mqnamesrv|mqbroker)" | grep -v grep
