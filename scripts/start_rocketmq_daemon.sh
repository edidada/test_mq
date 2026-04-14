#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "=== 启动 NameServer (后台)..."
nohup ./bin/mqnamesrv > /opt/rocketmq/namesrv.log 2>&1 &
echo $! > /opt/rocketmq/namesrv.pid
sleep 5

echo "=== 检查 NameServer..."
if ps -p $(cat /opt/rocketmq/namesrv.pid) > /dev/null; then
    echo "✅ NameServer 启动成功, PID: $(cat /opt/rocketmq/namesrv.pid)"
    cat /opt/rocketmq/namesrv.log
else
    echo "❌ NameServer 启动失败"
    cat /opt/rocketmq/namesrv.log
    exit 1
fi

echo ""
echo "=== 启动 Broker (后台)..."
nohup ./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /opt/rocketmq/broker.log 2>&1 &
echo $! > /opt/rocketmq/broker.pid
sleep 10

echo "=== 检查 Broker..."
if ps -p $(cat /opt/rocketmq/broker.pid) > /dev/null; then
    echo "✅ Broker 启动成功, PID: $(cat /opt/rocketmq/broker.pid)"
    tail -30 /opt/rocketmq/broker.log
else
    echo "❌ Broker 启动失败"
    cat /opt/rocketmq/broker.log
    exit 1
fi

echo ""
echo "🎉 RocketMQ 全部启动成功！"
echo "NameServer: 127.0.0.1:9876"
echo "Broker:     127.0.0.1:10911"
