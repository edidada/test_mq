#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Starting Broker without config..."
nohup ./bin/mqbroker -n 127.0.0.1:9876 > /opt/rocketmq/broker_simple.log 2>&1 &
BROKER_PID=$!
echo $BROKER_PID > /opt/rocketmq/broker.pid
echo "Broker PID: $BROKER_PID"

sleep 12

echo ""
echo "Checking process..."
ps aux | grep java | grep -v grep

echo ""
echo "Broker log (last 80 lines):"
tail -80 /opt/rocketmq/broker_simple.log
