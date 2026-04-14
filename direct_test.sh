#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

pkill -9 java 2>/dev/null || true
sleep 2

echo "Starting NameServer..."
./bin/mqnamesrv > /tmp/ns.log 2>&1 &
sleep 4
echo "NameServer started"

echo ""
echo "Starting Broker with direct output..."
./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf
