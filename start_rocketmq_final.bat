@echo off
set ROCKETMQ_HOME=C:\rocketmq\rocketmq-all-5.3.1-bin-release
set JAVA_HOME=%JAVA_HOME%
set NAMESRV_ADDR=127.0.0.1:9876
set ROCKETMQ_LOG_HOME=D:\rocketmq\logs
set ROCKETMQ_STORE_HOME=D:\rocketmq\data

cd /d %ROCKETMQ_HOME%\bin

echo Starting RocketMQ Namesrv...
start mqnamesrv.cmd

timeout /t 5 /nobreak >nul

echo Starting RocketMQ Broker...
start mqbroker.cmd -n %NAMESRV_ADDR% -c d:\develops\git\github\cpp\test_mq\broker.conf

echo RocketMQ启动完成！
echo Namesrv: %NAMESRV_ADDR%
echo 日志目录: %ROCKETMQ_LOG_HOME%
echo 数据目录: %ROCKETMQ_STORE_HOME%
pause
