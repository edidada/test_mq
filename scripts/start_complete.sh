#!/bin/bash

pkill -9 java 2>/dev/null || true
sleep 2

cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "=== 1. Starting NameServer ==="
nohup ./bin/mqnamesrv > /opt/rocketmq/namesrv.log 2>&1 &
NS_PID=$!
echo "NameServer PID: $NS_PID"
sleep 5

echo ""
echo "=== 2. Starting Broker ==="
nohup ./bin/mqbroker -n 127.0.0.1:9876 > /opt/rocketmq/broker.log 2>&1 &
BROKER_PID=$!
echo "Broker PID: $BROKER_PID"
sleep 12

echo ""
echo "=== 3. All Java processes ==="
ps aux | grep java | grep -v grep

echo ""
echo "=== 4. NameServer log ==="
tail -25 /opt/rocketmq/namesrv.log

echo ""
echo "=== 5. Broker log ==="
tail -80 /opt/rocketmq/broker.log
