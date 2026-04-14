#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

echo "Fixing JVM options for Java 21..."

for file in bin/runserver.sh bin/runbroker.sh bin/tools.sh; do
    if [ -f "$file" ]; then
        echo "Processing $file..."
        sudo sed -i 's/-XX:+UseBiasedLocking//g' $file
        sudo sed -i 's/-XX:-UseGCOverheadLimit//g' $file
    fi
done

echo "JVM options fixed"
echo ""

echo "Cleaning up old processes..."
pkill -9 java 2>/dev/null || true
rm -f /opt/rocketmq/namesrv.pid /opt/rocketmq/broker.pid
sleep 2

echo "Starting NameServer..."
nohup bin/mqnamesrv > /opt/rocketmq/namesrv.log 2>&1 &
echo $! > /opt/rocketmq/namesrv.pid
sleep 4

echo "NameServer status:"
if kill -0 $(cat /opt/rocketmq/namesrv.pid) 2>/dev/null; then
    echo "✅ NameServer is running, PID: $(cat /opt/rocketmq/namesrv.pid)"
    tail -10 /opt/rocketmq/namesrv.log
else
    echo "❌ NameServer failed to start"
    cat /opt/rocketmq/namesrv.log
    exit 1
fi

echo ""
echo "Starting Broker..."
nohup bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /opt/rocketmq/broker.log 2>&1 &
echo $! > /opt/rocketmq/broker.pid
sleep 6

echo "Broker status:"
if kill -0 $(cat /opt/rocketmq/broker.pid) 2>/dev/null; then
    echo "✅ Broker is running, PID: $(cat /opt/rocketmq/broker.pid)"
    tail -20 /opt/rocketmq/broker.log
else
    echo "❌ Broker failed to start"
    cat /opt/rocketmq/broker.log
    exit 1
fi

echo ""
echo "🎉 RocketMQ started successfully!"
echo "NameServer: 127.0.0.1:9876"
echo "Broker:     127.0.0.1:10911"
