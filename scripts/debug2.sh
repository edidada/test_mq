#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

echo "=== Step 1: Cleanup ==="
pkill -9 java 2>/dev/null || true
rm -f /tmp/namesrv.log /tmp/broker.log
sleep 2

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
echo "Starting Broker..."
./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /tmp/broker.log 2>&1 &
BROKER_PID=$!
echo "Broker PID: $BROKER_PID"
sleep 8

echo ""
echo "=== Broker Status ==="
if ps -p $BROKER_PID > /dev/null; then
    echo "✅ Broker is running!"
    echo "   Log:"
    tail -40 /tmp/broker.log
else
    echo "❌ Broker failed"
    echo "   Log:"
    cat /tmp/broker.log
fi
