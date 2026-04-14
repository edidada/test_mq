cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Java version:"
java -version

echo ""
echo "Cleaning..."
pkill -9 java 2>/dev/null || true
sleep 2

echo ""
echo "Starting NameServer..."
./bin/mqnamesrv &
NS_PID=$!
echo "NameServer PID: $NS_PID"
sleep 5

echo ""
echo "NameServer is running? $NS_PID"
ps -p $NS_PID

echo ""
echo "Starting Broker..."
./bin/mqbroker -n 127.0.0.1:9876 -c conf/broker.conf 2>&1
