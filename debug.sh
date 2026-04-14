#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

echo "=== Step 1: Cleanup ==="
pkill -9 java 2>/dev/null || true
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
echo "=== Step 3: Try to start Broker ==="
echo "Current dir: $(pwd)"
echo "Files in bin/:"
ls -la bin/

echo ""
echo "Trying to execute bin/mqbroker..."
if [ -x bin/mqbroker ]; then
    echo "  bin/mqbroker is executable"
    echo "  Full path: $(realpath bin/mqbroker)"
    
    echo ""
    echo "  Executing with bash -c..."
    bash -c "./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf" 2>&1
else
    echo "  ❌ bin/mqbroker not executable"
fi
