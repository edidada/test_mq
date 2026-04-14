@echo off
cd /d C:\rocketmq\rocketmq-all-5.3.1-bin-release\bin
start mqnamesrv.cmd
timeout /t 3 /nobreak >nul
start mqbroker.cmd -c conf\broker.conf
echo RocketMQ服务已启动
echo Namesrv: 127.0.0.1:9876
