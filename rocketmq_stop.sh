#!/bin/bash
echo "=== RocketMQ停止脚本 ==="
echo ""

cd /opt/rocketmq/rocketmq-all-4.9.7-bin-release
pkill -9 java 2>/dev/null || true
sleep 2

echo "✅ RocketMQ 服务已停止"
echo ""
echo "检查进程："
ps aux | grep -E "(mqnamesrv|mqbroker)" | grep -v grep || echo "  无相关进程"
