#include <iostream>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>
#include <atomic>
#include <random>

class MQProducer {
private:
    std::queue<std::string> messageQueue;
    std::mutex queueMutex;
    std::condition_variable cv;
    std::atomic<bool> running{true};
    std::atomic<int>发送成功{0};
    std::atomic<int>发送失败{0};
    std::atomic<int>积压量{0};
    const int MAX_QUEUE_SIZE = 1000;
    
    // 模拟MQ连接状态
    std::atomic<bool> mqConnected{true};
    std::atomic<int> mqDisconnectCount{0};
    
public:
    void produceMessage(const std::string& msg) {
        if (!mqConnected.load()) {
            std::cout << "[生产者] MQ未连接，消息暂存本地: " << msg << std::endl;
            storeLocally(msg);
            return;
        }
        
        {
            std::lock_guard<std::mutex> lock(queueMutex);
            if (messageQueue.size() < MAX_QUEUE_SIZE) {
                messageQueue.push(msg);
                积压量++;
                std::cout << "[生产者] 消息发送成功: " << msg << std::endl;
                发送成功++;
            } else {
                std::cout << "[生产者] 队列满，消息丢失: " << msg << std::endl;
                发送失败++;
            }
        }
        cv.notify_one();
    }
    
    void connectToMQ() {
        std::cout << "[生产者] 正在连接MQ..." << std::endl;
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        
        // 模拟连接失败场景
        if (mqDisconnectCount < 3) {
            mqConnected.store(false);
            mqDisconnectCount++;
            std::cout << "[生产者] 连接失败 (第 " << mqDisconnectCount << " 次)" << std::endl;
        } else {
            mqConnected.store(true);
            std::cout << "[生产者] 连接成功" << std::endl;
        }
    }
    
    void reconnectMQ() {
        std::cout << "\n[生产者] 尝试重连MQ..." << std::endl;
        connectToMQ();
    }
    
    void storeLocally(const std::string& msg) {
        // 模拟本地存储
        static std::vector<std::string> localStore;
        static std::mutex localMutex;
        
        {
            std::lock_guard<std::mutex> lock(localMutex);
            localStore.push_back(msg);
        }
        
        if (localStore.size() % 5 == 0) {
            std::cout << "[本地存储] 已暂存 " << localStore.size() << " 条消息" << std::endl;
        }
    }
    
    void flushLocalMessages() {
        std::cout << "\n[生产者] 开始刷新本地消息到MQ..." << std::endl;
        
        std::vector<std::string> messages;
        {
            std::lock_guard<std::mutex> lock(queueMutex);
            while (!messageQueue.empty()) {
                messages.push_back(messageQueue.front());
                messageQueue.pop();
            }
        }
        
        for (const auto& msg : messages) {
            produceMessage(msg);
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }
        
        std::cout << "[生产者] 本地消息刷新完成" << std::endl;
    }
    
    void startProducer(int intervalMs = 100) {
        std::thread t([this, intervalMs]() {
            std::random_device rd;
            std::mt19937 gen(rd());
            std::uniform_int_distribution<> dis(1, 100);
            
            while (running.load()) {
                int msgId = dis(gen);
                std::string msg = "msg_" + std::to_string(msgId);
                
                if (dis(gen) > 95) {
                    msg = "critical_" + std::to_string(msgId); // 关键消息
                }
                
                produceMessage(msg);
                std::this_thread::sleep_for(std::chrono::milliseconds(intervalMs));
            }
        });
        t.detach();
        std::cout << "[生产者] 启动生产者线程" << std::endl;
    }
    
    void showStatus() {
        std::cout << "\n=== 生产者状态 ===" << std::endl;
        std::cout << "MQ连接状态: " << (mqConnected.load() ? "已连接" : "未连接") << std::endl;
        std::cout << "发送成功: " << 发送成功.load() << std::endl;
        std::cout << "发送失败: " << 发送失败.load() << std::endl;
        std::cout << "当前积压: " << 积压量.load() << std::endl;
    }
    
    void stop() {
        running.store(false);
    }
    
    void simulateMQCrash() {
        std::cout << "\n=== 模拟MQ故障 ===" << std::endl;
        mqConnected.store(false);
        std::cout << "[系统] MQ服务已停止" << std::endl;
    }
    
    void simulateMQRecovery() {
        std::cout << "\n=== 模拟MQ恢复 ===" << std::endl;
        mqConnected.store(true);
        std::cout << "[系统] MQ服务已恢复" << std::endl;
        reconnectMQ();
    }
};

int main() {
    MQProducer producer;
    
    // 连接MQ
    producer.connectToMQ();
    
    // 启动生产者
    producer.startProducer(200);
    
    // 模拟各种场景
    std::this_thread::sleep_for(std::chrono::seconds(3));
    producer.showStatus();
    
    // 模拟MQ故障
    producer.simulateMQCrash();
    std::this_thread::sleep_for(std::chrono::seconds(2));
    producer.showStatus();
    
    // 模拟MQ恢复
    producer.simulateMQRecovery();
    std::this_thread::sleep_for(std::chrono::seconds(2));
    producer.showStatus();
    
    // 停止
    producer.stop();
    
    return 0;
}
