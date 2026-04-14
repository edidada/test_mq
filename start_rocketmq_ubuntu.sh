#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
sudo chown -R $(whoami):$(whoami) /opt/rocketmq

echo "Starting NameServer..."
nohup bin/mqnamesrv > /opt/rocketmq/namesrv.log 2>&1 &
echo $! > /opt/rocketmq/namesrv.pid
sleep 3
echo "NameServer started, PID: $(cat /opt/rocketmq/namesrv.pid)"
echo "=== NameServer log ==="
tail -20 /opt/rocketmq/namesrv.log

echo ""
echo "Starting Broker..."
nohup bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf > /opt/rocketmq/broker.log 2>&1 &
echo $! > /opt/rocketmq/broker.pid
sleep 5
echo "Broker started, PID: $(cat /opt/rocketmq/broker.pid)"
echo "=== Broker log ==="
tail -30 /opt/rocketmq/broker.log
