#include <iostream>
#include <thread>
#include <chrono>
#include <atomic>
#include <functional>
#include <map>

class FaultHandler {
private:
    std::atomic<int> mq故障次数{0};
    std::atomic<int> mysql故障次数{0};
    std::atomic<int> 双故障次数{0};
    std::atomic<bool> mq故障中{false};
    std::atomic<bool> mysql故障中{false};
    std::atomic<bool> 双故障中{false};
    
    std::map<std::string, int> 故障处理记录;
    
public:
    void handleMQFault() {
        std::cout << "\n=== 处理MQ故障 ===" << std::endl;
        mq故障次数++;
        mq故障中.store(true);
        
        故障处理记录["MQ故障"]++;
        
        std::cout << "[步骤1] 发送告警通知" << std::endl;
        std::cout << "  - 通知运维团队" << std::endl;
        std::cout << "  - 通知相关负责人" << std::endl;
        std::cout << "  - 记录故障时间" << std::endl;
        
        std::cout << "\n[步骤2] 检查故障范围" << std::endl;
        std::cout << "  - 检查MQ集群状态" << std::endl;
        std::cout << "  - 检查网络连接" << std::endl;
        std::cout << "  - 检查磁盘空间" << std::endl;
        
        std::cout << "\n[步骤3] 尝试自动恢复" << std::endl;
        std::cout << "  - 重启MQ服务" << std::endl;
        std::cout << "  - 切换到备用MQ节点" << std::endl;
        std::cout << "  - 启用本地消息表" << std::endl;
        
        std::cout << "\n[步骤4] 启用降级方案" << std::endl;
        std::cout << "  - 消息本地持久化" << std::endl;
        std::cout << "  - 暂停非关键业务发送" << std::endl;
        std::cout << "  - 启用消息压缩" << std::endl;
        
        std::cout << "\n[步骤5] 等待MQ恢复" << std::endl;
        std::cout << "  - 定时检查MQ状态" << std::endl;
        std::cout << "  - 恢复后批量重放消息" << std::endl;
        
        std::this_thread::sleep_for(std::chrono::seconds(2));
        mq故障中.store(false);
        std::cout << "\n[完成] MQ已恢复" << std::endl;
    }
    
    void handleMySQLFault() {
        std::cout << "\n=== 处理MySQL故障 ===" << std::endl;
        mysql故障次数++;
        mysql故障中.store(true);
        
        故障处理记录["MySQL故障"]++;
        
        std::cout << "[步骤1] 发送告警通知" << std::endl;
        std::cout << "  - 通知DBA团队" << std::endl;
        std::cout << "  - 通知相关负责人" << std::endl;
        
        std::cout << "\n[步骤2] 检查MySQL状态" << std::endl;
        std::cout << "  - 检查连接池状态" << std::endl;
        std::cout << "  - 检查主从复制" << std::endl;
        std::cout << "  - 检查慢查询" << std::endl;
        
        std::cout << "\n[步骤3] 尝试恢复数据库" << std::endl;
        std::cout << "  - 重启MySQL服务" << std::endl;
        std::cout << "  - 切换到从库" << std::endl;
        std::cout << "  - 启用读写分离" << std::endl;
        
        std::cout << "\n[步骤4] 启用降级方案" << std::endl;
        std::cout << "  - 使用本地缓存" << std::endl;
        std::cout << "  - 启用Redis缓存" << std::endl;
        std::cout << "  - 暂停非关键写入" << std::endl;
        
        std::cout << "\n[步骤5] 数据补偿机制" << std::endl;
        std::cout << "  - 记录未写入数据" << std::endl;
        std::cout << "  - 恢复后批量补偿" << std::endl;
        std::cout << "  - 数据一致性校验" << std::endl;
        
        std::this_thread::sleep_for(std::chrono::seconds(2));
        mysql故障中.store(false);
        std::cout << "\n[完成] MySQL已恢复" << std::endl;
    }
    
    void handleBothFault() {
        std::cout << "\n=== 处理MQ和MySQL同时故障 ===" << std::endl;
        双故障次数++;
        双故障中.store(true);
        
        故障处理记录["双故障"]++;
        
        std::cout << "[步骤1] 紧急告警" << std::endl;
        std::cout << "  - 一级告警通知" << std::endl;
        std::cout << "  - 启动应急预案" << std::endl;
        std::cout << "  - 通知所有相关方" << std::endl;
        
        std::cout << "\n[步骤2] 全面故障检查" << std::endl;
        std::cout << "  - 检查基础设施" << std::endl;
        std::cout << "  - 检查网络" << std::endl;
        std::cout << "  - 检查所有依赖服务" << std::endl;
        
        std::cout << "\n[步骤3] 全面降级" << std::endl;
        std::cout << "  - 关闭非核心功能" << std::endl;
        std::cout << "  - 启用纯缓存模式" << std::endl;
        std::cout << "  - 限制服务流量" << std::endl;
        
        std::cout << "\n[步骤4] 本地持久化所有数据" << std::endl;
        std::cout << "  - 消息本地文件存储" << std::endl;
        std::cout << "  - 数据本地数据库存储" << std::endl;
        std::cout << "  - 防止数据丢失" << std::endl;
        
        std::cout << "\n[步骤5] 等待服务恢复" << std::endl;
        std::cout << "  - 持续监控服务状态" << std::endl;
        std::cout << "  - 逐个恢复服务" << std::endl;
        std::cout << "  - 数据一致性校验" << std::endl;
        
        std::this_thread::sleep_for(std::chrono::seconds(3));
        双故障中.store(false);
        std::cout << "\n[完成] 所有服务已恢复" << std::endl;
    }
    
    void startMonitoring() {
        std::cout << "\n=== 启动监控系统 ===" << std::endl;
        
        std::thread t([this]() {
            while (true) {
                std::this_thread::sleep_for(std::chrono::seconds(5));
                
                if (mq故障中.load()) {
                    std::cout << "[监控] MQ仍在故障中..." << std::endl;
                }
                
                if (mysql故障中.load()) {
                    std::cout << "[监控] MySQL仍在故障中..." << std::endl;
                }
            }
        });
        t.detach();
    }
    
    void showStatistics() {
        std::cout << "\n=== 故障处理统计 ===" << std::endl;
        std::cout << "MQ故障次数: " << mq故障次数.load() << std::endl;
        std::cout << "MySQL故障次数: " << mysql故障次数.load() << std::endl;
        std::cout << "双故障次数: " << 双故障次数.load() << std::endl;
        
        std::cout << "\n故障处理记录:" << std::endl;
        for (const auto& pair : 故障处理记录) {
            std::cout << "  " << pair.first << ": " << pair.second << " 次" << std::endl;
        }
    }
    
    void runSimulation() {
        std::cout << "=== 故障处理模拟开始 ===" << std::endl;
        
        handleMQFault();
        std::this_thread::sleep_for(std::chrono::seconds(1));
        
        handleMySQLFault();
        std::this_thread::sleep_for(std::chrono::seconds(1));
        
        handleBothFault();
        
        showStatistics();
    }
};

int main() {
    FaultHandler handler;
    
    handler.startMonitoring();
    
    handler.runSimulation();
    
    std::cout << "\n=== 模拟完成 ===" << std::endl;
    
    return 0;
}
