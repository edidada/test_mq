#include <iostream>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>
#include <atomic>
#include <functional>

class MQConsumer {
private:
    std::queue<std::string> messageQueue;
    std::mutex queueMutex;
    std::condition_variable cv;
    std::atomic<bool> running{true};
    std::atomic<int> 积压量{0};
    std::atomic<int> 失败次数{0};
    std::atomic<int> 成功次数{0};
    std::queue<std::string> 死信队列;
    const int MAX_RETRY = 3;
    const int MAX_QUEUE_SIZE = 10000;
    
public:
    void consumeMessage() {
        while (running.load()) {
            std::string message;
            
            {
                std::unique_lock<std::mutex> lock(queueMutex);
                cv.wait_for(lock, std::chrono::milliseconds(100), [this] {
                    return !messageQueue.empty() || !running.load();
                });
                
                if (messageQueue.empty()) continue;
                
                message = messageQueue.front();
                messageQueue.pop();
                积压量--;
            }
            
            processMessage(message);
        }
    }
    
    void processMessage(const std::string& msg) {
        std::cout << "处理消息: " << msg << std::endl;
        
        if (msg.find("fail") != std::string::npos) {
            if (失败次数 < MAX_RETRY) {
                std::cout << "处理失败，加入重试队列 (尝试 " << (失败次数 + 1) << "/" << MAX_RETRY << ")" << std::endl;
                std::this_thread::sleep_for(std::chrono::seconds(1));
                失败次数++;
                {
                    std::lock_guard<std::mutex> lock(queueMutex);
                    messageQueue.push(msg);
                    积压量++;
                }
                cv.notify_one();
            } else {
                std::cout << "重试次数耗尽，移入死信队列" << std::endl;
                {
                    std::lock_guard<std::mutex> lock(queueMutex);
                    死信队列.push(msg);
                }
            }
            return;
        }
        
        if (!saveToMySQL(msg)) {
            std::cout << "MySQL操作失败，消息回滚" << std::endl;
            return;
        }
        
        成功次数++;
        失败次数 = 0;
        std::cout << "消息处理成功" << std::endl;
    }
    
    bool saveToMySQL(const std::string& msg) {
        static int mysqlFailCount = 0;
        mysqlFailCount++;
        
        if (mysqlFailCount > 5 && mysqlFailCount < 10) {
            std::cout << "[模拟] MySQL连接失败" << std::endl;
            return false;
        }
        
        if (mysqlFailCount >= 10) {
            mysqlFailCount = 0;
        }
        
        std::cout << "[模拟] 保存到MySQL成功" << std::endl;
        return true;
    }
    
    void handleMySQLDown() {
        std::cout << "\n=== MySQL故障处理 ===" << std::endl;
        std::cout << "1. 暂停消息消费" << std::endl;
        std::cout << "2. 使用本地缓存暂存消息" << std::endl;
        std::cout << "3. 启动重试机制" << std::endl;
        std::cout << "4. 告警通知运维人员" << std::endl;
        std::cout << "5. 等待MySQL恢复后继续消费" << std::endl;
    }
    
    void handleMQDown() {
        std::cout << "\n=== MQ服务故障处理 ===" << std::endl;
        std::cout << "1. 检查MQ服务状态" << std::endl;
        std::cout << "2. 尝试重连MQ服务" << std::endl;
        std::cout << "3. 启用本地消息表机制" << std::endl;
        std::cout << "4. 记录未处理消息" << std::endl;
        std::cout << "5. 告警通知运维人员" << std::endl;
    }
    
    void handleBacklog() {
        std::cout << "\n=== 消息积压处理 ===" << std::endl;
        std::cout << "当前积压量: " << 积压量.load() << std::endl;
        
        if (积压量.load() > MAX_QUEUE_SIZE) {
            std::cout << "积压量超过阈值，启动降级方案" << std::endl;
            std::cout << "1. 增加消费者数量" << std::endl;
            std::cout << "2. 优先处理关键消息" << std::endl;
            std::cout << "3. 临时丢弃非关键消息" << std::endl;
            std::cout << "4. 启用批量处理模式" << std::endl;
        }
    }
    
    void startConsumers(int count = 3) {
        for (int i = 0; i < count; i++) {
            std::thread t(&MQConsumer::consumeMessage, this);
            t.detach();
        }
        std::cout << "启动 " << count << " 个消费者线程" << std::endl;
    }
    
    void addMessage(const std::string& msg) {
        {
            std::lock_guard<std::mutex> lock(queueMutex);
            if (messageQueue.size() < MAX_QUEUE_SIZE) {
                messageQueue.push(msg);
                积压量++;
            } else {
                std::cout << "队列已满，丢弃消息: " << msg << std::endl;
            }
        }
        cv.notify_one();
    }
    
    void stop() {
        running.store(false);
        cv.notify_all();
    }
    
    void showStatus() {
        std::cout << "\n=== 系统状态 ===" << std::endl;
        std::cout << "消息积压量: " << 积压量.load() << std::endl;
        std::cout << "成功处理: " << 成功次数.load() << std::endl;
        std::cout << "失败次数: " << 失败次数.load() << std::endl;
        std::cout << "死信队列: " << 死信队列.size() << std::endl;
    }
};

int main() {
    MQConsumer consumer;
    
    consumer.startConsumers(3);
    
    for (int i = 0; i < 20; i++) {
        std::string msg = "msg_" + std::to_string(i);
        if (i == 5 || i == 10) {
            msg = "fail_" + std::to_string(i);
        }
        consumer.addMessage(msg);
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    
    std::this_thread::sleep_for(std::chrono::seconds(2));
    consumer.handleBacklog();
    consumer.handleMySQLDown();
    consumer.handleMQDown();
    consumer.showStatus();
    
    std::this_thread::sleep_for(std::chrono::seconds(3));
    consumer.stop();
    
    return 0;
}
