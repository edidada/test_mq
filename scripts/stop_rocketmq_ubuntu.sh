#!/bin/bash
if [ -f /opt/rocketmq/broker.pid ]; then
    PID=$(cat /opt/rocketmq/broker.pid)
    if kill -0 $PID 2>/dev/null; then
        echo "Stopping Broker, PID: $PID"
        kill -9 $PID 2>/dev/null
        rm -f /opt/rocketmq/broker.pid
    fi
fi

if [ -f /opt/rocketmq/namesrv.pid ]; then
    PID=$(cat /opt/rocketmq/namesrv.pid)
    if kill -0 $PID 2>/dev/null; then
        echo "Stopping NameServer, PID: $PID"
        kill -9 $PID 2>/dev/null
        rm -f /opt/rocketmq/namesrv.pid
    fi
fi

echo "All services stopped"
