#!/bin/bash

cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

echo "1. Cleaning up old processes..."
pkill -9 java 2>/dev/null || true
rm -f /opt/rocketmq/namesrv.pid /opt/rocketmq/broker.pid
sleep 2

echo "2. Starting NameServer..."
nohup bin/mqnamesrv > /opt/rocketmq/namesrv.log 2>&1 &
echo $! > /opt/rocketmq/namesrv.pid
sleep 4

echo "3. NameServer status:"
if kill -0 $(cat /opt/rocketmq/namesrv.pid) 2>/dev/null; then
    echo "   ✅ NameServer running, PID: $(cat /opt/rocketmq/namesrv.pid)"
    tail -5 /opt/rocketmq/namesrv.log
else
    echo "   ❌ NameServer failed"
    cat /opt/rocketmq/namesrv.log
    exit 1
fi

echo "4. Starting Broker..."
nohup bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /opt/rocketmq/broker.log 2>&1 &
echo $! > /opt/rocketmq/broker.pid
sleep 8

echo "5. Broker status:"
if kill -0 $(cat /opt/rocketmq/broker.pid) 2>/dev/null; then
    echo "   ✅ Broker running, PID: $(cat /opt/rocketmq/broker.pid)"
    tail -25 /opt/rocketmq/broker.log
else
    echo "   ❌ Broker failed"
    cat /opt/rocketmq/broker.log
    exit 1
fi

echo ""
echo "🎉 RocketMQ is UP!"
echo "NameServer: 127.0.0.1:9876"
echo "Broker:     127.0.0.1:10911"
