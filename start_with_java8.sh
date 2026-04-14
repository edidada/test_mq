#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Java version:"
java -version

echo ""
echo "=== Step 1: Cleanup ==="
pkill -9 java 2>/dev/null || true
rm -f /tmp/namesrv.log /tmp/broker.log
sleep 2

echo ""
echo "=== Step 2: Start NameServer ==="
./bin/mqnamesrv > /tmp/namesrv.log 2>&1 &
NS_PID=$!
echo "NameServer PID: $NS_PID"
sleep 4

if ps -p $NS_PID > /dev/null; then
    echo "✅ NameServer is running"
    echo "   Log:"
    tail -5 /tmp/namesrv.log
else
    echo "❌ NameServer failed"
    cat /tmp/namesrv.log
    exit 1
fi

echo ""
echo "=== Step 3: Start Broker ==="
./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /tmp/broker.log 2>&1 &
BROKER_PID=$!
echo "Broker PID: $BROKER_PID"
sleep 10

echo ""
echo "=== Status Check ==="
if ps -p $NS_PID > /dev/null && ps -p $BROKER_PID > /dev/null; then
    echo "🎉 RocketMQ is UP!"
    echo "NameServer: 127.0.0.1:9876"
    echo "Broker:     127.0.0.1:10911"
    echo ""
    echo "Broker log (last 40 lines):"
    tail -40 /tmp/broker.log
else
    echo "❌ One or more services failed"
    if ! ps -p $NS_PID > /dev/null; then
        echo "NameServer failed, log:"
        cat /tmp/namesrv.log
    fi
    if ! ps -p $BROKER_PID > /dev/null; then
        echo "Broker failed, log:"
        cat /tmp/broker.log
    fi
fi
