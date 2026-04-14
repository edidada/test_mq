cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

pkill -9 java 2>/dev/null || true
sleep 2

echo "1. Starting NameServer..."
./bin/mqnamesrv &
sleep 5

echo "2. NameServer status:"
ps aux | grep mqnamesrv | grep -v grep

echo ""
echo "3. Starting Broker..."
./bin/mqbroker -n 127.0.0.1:9876 &
BROKER_PID=$!
echo "Broker PID: $BROKER_PID"
sleep 10

echo ""
echo "4. Final status:"
ps aux | grep java | grep -v grep
