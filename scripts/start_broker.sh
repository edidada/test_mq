#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Starting Broker..."
nohup ./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /opt/rocketmq/broker.log 2>&1 &
BROKER_PID=$!
echo $BROKER_PID > /opt/rocketmq/broker.pid
echo "Broker PID: $BROKER_PID"

sleep 10

echo ""
echo "Checking process..."
if ps -p $BROKER_PID > /dev/null; then
    echo "✅ Broker is running!"
    ps aux | grep mqbroker | grep -v grep
else
    echo "❌ Broker failed to start"
fi

echo ""
echo "Broker log (last 60 lines):"
tail -60 /opt/rocketmq/broker.log
