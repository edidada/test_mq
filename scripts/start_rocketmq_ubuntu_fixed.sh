#!/bin/bash
cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
sudo chown -R $(whoami):$(whoami) /opt/rocketmq

JAVA_MAJOR_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
echo "Java major version: $JAVA_MAJOR_VERSION"

if [ "$JAVA_MAJOR_VERSION" -gt 8 ]; then
    echo "Fixing JVM options for Java $JAVA_MAJOR_VERSION"
    
    for file in bin/runserver.sh bin/runbroker.sh bin/tools.sh; do
        if [ -f "$file" ]; then
            sudo sed -i 's/-XX:+UseBiasedLocking//g' $file
            sudo sed -i 's/-XX:-UseGCOverheadLimit//g' $file
        fi
    done
fi

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
