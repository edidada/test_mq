/**
 * @file true_producer.cpp
 * @brief 真正的 RabbitMQ 生产者 - 使用 rabbitmq-c 库
 */

#include <iostream>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>
#include <cstdlib>
#include <cstring>

// rabbitmq-c 头文件
#include <amqp.h>
#include <amqp_tcp_socket.h>

// 消息结构体
struct Message {
    std::string message_id;
    std::string priority;
    std::string body;
    
    std::string encode() const {
        return message_id + "|" + priority + "|" + body;
    }
    
    static Message decode(const std::string& line) {
        size_t pos1 = line.find('|');
        size_t pos2 = line.find('|', pos1 + 1);
        
        if (pos1 == std::string::npos || pos2 == std::string::npos) {
            throw std::runtime_error("invalid message format: " + line);
        }
        
        Message msg;
        msg.message_id = line.substr(0, pos1);
        msg.priority = line.substr(pos1 + 1, pos2 - pos1 - 1);
        msg.body = line.substr(pos2 + 1);
        return msg;
    }
};

// RabbitMQ 生产者类
class TrueProducer {
private:
    std::string host;
    int port;
    std::string username;
    std::string password;
    std::string queue_name;
    
    amqp_connection_state_t conn;
    amqp_channel_t channel;
    int sent_to_queue;
    
    amqp_bytes_t to_bytes(const std::string& str) {
        amqp_bytes_t bytes;
        bytes.len = str.length();
        bytes.bytes = const_cast<char*>(str.c_str());
        return bytes;
    }
    
    void die_on_error(int code, const char* context) {
        if (code < 0) {
            std::cerr << "[producer] error: " << context << ": " 
                      << amqp_error_string2(code) << std::endl;
            exit(1);
        }
        
        amqp_rpc_reply_t ret = amqp_get_rpc_reply(conn);
        if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
            std::cerr << "[producer] error: " << context << ": RPC error" << std::endl;
            exit(1);
        }
    }

public:
    TrueProducer(const std::string& h = "localhost", int p = 5672,
                 const std::string& u = "guest", const std::string& pw = "guest")
        : host(h), port(p), username(u), password(pw), queue_name("test_queue"),
          sent_to_queue(0) {
        conn = nullptr;
        channel = 0;
    }
    
    void connect() {
        std::cout << "[producer] connecting to RabbitMQ at " << host << ":" << port << "..." << std::endl;
        
        // 创建连接
        conn = amqp_new_connection();
        
        // 创建 TCP socket
        amqp_socket_t* socket = amqp_tcp_socket_new(conn);
        if (!socket) {
            std::cerr << "[producer] error: failed to create TCP socket" << std::endl;
            exit(1);
        }
        
        // 连接到服务器
        amqp_socket_open(socket, host.c_str(), port);
        
        // 登录
        die_on_error(amqp_login(conn, host.c_str(), port, 0, 131072, 0, 
                                AMQP_SASL_METHOD_PLAIN, username.c_str(), password.c_str()).reply_type,
                     "Login");
        
        // 打开通道
        amqp_channel_open(conn, channel);
        die_on_error(amqp_get_rpc_reply(conn).reply_type, "Opening channel");
        
        // 声明队列（持久化）
        amqp_queue_declare(conn, channel, to_bytes(queue_name),
                          true,    // durable
                          false,   // passive
                          false,   // exclusive
                          false,   // auto_delete
                          amqp_empty_table);
        die_on_error(amqp_get_rpc_reply(conn).reply_type, "Declaring queue");
        
        std::cout << "[producer] connected successfully" << std::endl;
    }
    
    void send(const Message& message) {
        if (!conn) {
            throw std::runtime_error("Producer not connected. Call connect() first.");
        }
        
        std::string message_body = message.encode();
        
        // 设置消息属性
        amqp_basic_properties_t props;
        props._flags = AMQP_BASIC_CONTENT_TYPE_FLAG | AMQP_BASIC_DELIVERY_MODE_FLAG | AMQP_BASIC_PRIORITY_FLAG;
        props.content_type = amqp_bytes_malloc_dup(to_bytes("text/plain"));
        props.delivery_mode = 2;  // 持久化
        props.priority = (message.priority == "critical") ? 10 : 0;
        
        // 发布消息
        amqp_basic_publish(conn, channel,
                          amqp_empty_bytes,           // exchange
                          to_bytes(queue_name),       // routing key
                          0,                          // mandatory
                          0,                          // immediate
                          &props,                     // properties
                          to_bytes(message_body));    // body
        
        die_on_error(amqp_get_rpc_reply(conn).reply_type, "Publishing message");
        
        // 释放内存
        amqp_bytes_free(props.content_type);
        
        sent_to_queue++;
        std::cout << "[producer] sent: " << message.message_id << " (" << message.priority << ")" << std::endl;
    }
    
    void send_batch(const std::vector<Message>& messages) {
        std::cout << "[producer] sending batch of " << messages.size() << " messages" << std::endl;
        for (const auto& message : messages) {
            send(message);
        }
    }
    
    void close() {
        if (conn) {
            amqp_channel_close(conn, channel, AMQP_REPLY_SUCCESS);
            amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
            amqp_destroy_connection(conn);
            conn = nullptr;
            std::cout << "[producer] connection closed" << std::endl;
        }
    }
    
    ~TrueProducer() {
        close();
    }
};

// 构建测试消息
std::vector<Message> build_messages(const std::string& prefix, int count, int critical_every = 0) {
    std::vector<Message> messages;
    for (int i = 1; i <= count; i++) {
        Message msg;
        std::ostringstream oss;
        oss << prefix << "-" << std::setfill('0') << std::setw(3) << i;
        msg.message_id = oss.str();
        msg.priority = (critical_every && i % critical_every == 0) ? "critical" : "normal";
        msg.body = prefix + " payload #" + std::to_string(i);
        messages.push_back(msg);
    }
    return messages;
}

int main(int argc, char* argv[]) {
    TrueProducer producer;
    
    try {
        producer.connect();
        
        // 发送 10 条消息，每 3 条中有一条 critical
        auto messages = build_messages("msg", 10, 3);
        producer.send_batch(messages);
        
        std::cout << "[producer] done!" << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "[producer] error: " << e.what() << std::endl;
        return 1;
    }
    
    return 0;
}
