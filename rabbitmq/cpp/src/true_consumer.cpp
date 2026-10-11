/**
 * @file true_consumer.cpp
 * @brief 真正的 RabbitMQ 消费者 - 使用 rabbitmq-c 库
 */

#include <iostream>
#include <string>
#include <vector>
#include <cstdlib>
#include <cstring>
#include <thread>
#include <chrono>

// rabbitmq-c 头文件
#include <amqp.h>
#include <amqp_tcp_socket.h>

// RabbitMQ 消费者类
class TrueConsumer {
private:
    std::string host;
    int port;
    std::string username;
    std::string password;
    std::string queue_name;
    
    amqp_connection_state_t conn;
    amqp_channel_t channel;
    int message_count;
    int persisted;
    
    amqp_bytes_t to_bytes(const std::string& str) {
        amqp_bytes_t bytes;
        bytes.len = str.length();
        bytes.bytes = const_cast<char*>(str.c_str());
        return bytes;
    }
    
    std::string from_bytes(amqp_bytes_t bytes) {
        return std::string(static_cast<char*>(bytes.bytes), bytes.len);
    }
    
    void die_on_error(int code, const char* context) {
        if (code != AMQP_STATUS_OK) {
            std::cerr << "[consumer] error: " << context << ": " 
                      << amqp_error_string2(code) << std::endl;
            exit(1);
        }
    }

    void die_on_rpc_error(const char* context) {
        amqp_rpc_reply_t ret = amqp_get_rpc_reply(conn);
        if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
            std::cerr << "[consumer] error: " << context << ": RPC error" << std::endl;
            exit(1);
        }
    }
    
    void process_message(const std::string& message_str) {
        // 解析消息
        size_t pos1 = message_str.find('|');
        size_t pos2 = message_str.find('|', pos1 + 1);
        
        if (pos1 != std::string::npos && pos2 != std::string::npos) {
            std::string message_id = message_str.substr(0, pos1);
            std::string priority = message_str.substr(pos1 + 1, pos2 - pos1 - 1);
            std::string body = message_str.substr(pos2 + 1);
            
            std::cout << "  [processing] " << message_id 
                      << " - priority: " << priority 
                      << ", body: " << body << std::endl;
        }
        
        // 模拟业务处理
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

public:
    TrueConsumer(const std::string& h = "localhost", int p = 5672,
                 const std::string& u = "guest", const std::string& pw = "guest")
        : host(h), port(p), username(u), password(pw), queue_name("test_queue"),
          message_count(0), persisted(0) {
        conn = nullptr;
        channel = 1;  // 使用 channel 1
    }
    
    void connect() {
        std::cout << "[consumer] connecting to RabbitMQ at " << host << ":" << port << "..." << std::endl;
        
        // 创建连接
        conn = amqp_new_connection();
        
        // 创建 TCP socket
        amqp_socket_t* socket = amqp_tcp_socket_new(conn);
        if (!socket) {
            std::cerr << "[consumer] error: failed to create TCP socket" << std::endl;
            exit(1);
        }
        
        // 连接到服务器
        int status = amqp_socket_open(socket, host.c_str(), port);
        if (status != AMQP_STATUS_OK) {
            std::cerr << "[consumer] error: failed to open socket: " << status << std::endl;
            exit(1);
        }
        
        // 登录
        std::cout << "[consumer] logging in as " << username << "..." << std::endl;
        amqp_rpc_reply_t ret = amqp_login_with_properties(
            conn,
            "/",  // vhost
            0,    // channel_max
            AMQP_DEFAULT_FRAME_SIZE,
            AMQP_DEFAULT_HEARTBEAT,
            NULL, // properties
            AMQP_SASL_METHOD_PLAIN,
            username.c_str(),
            password.c_str()
        );
        
        if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
            std::cerr << "[consumer] error: Login failed!" << std::endl;
            std::cerr << "  Reply type: " << ret.reply_type << std::endl;
            if (ret.reply_type == AMQP_RESPONSE_LIBRARY_EXCEPTION) {
                std::cerr << "  Library error: " << ret.library_error << std::endl;
            } else if (ret.reply_type == AMQP_RESPONSE_SERVER_EXCEPTION) {
                std::cerr << "  Server exception" << std::endl;
            }
            exit(1);
        }
        std::cout << "[consumer] logged in successfully" << std::endl;
        
        // 打开通道
        std::cout << "[consumer] opening channel..." << std::endl;
        amqp_channel_open_ok_t *channel_ret = amqp_channel_open(conn, channel);
        if (!channel_ret) {
            amqp_rpc_reply_t ret = amqp_get_rpc_reply(conn);
            std::cerr << "[consumer] error: Opening channel: RPC error" << std::endl;
            std::cerr << "  Reply type: " << ret.reply_type << std::endl;
            if (ret.reply_type == AMQP_RESPONSE_SERVER_EXCEPTION) {
                std::cerr << "  Server exception" << std::endl;
            }
            exit(1);
        }
        std::cout << "[consumer] channel opened successfully" << std::endl;
        
        // Declare the dead-letter queue before the main priority queue.
        amqp_queue_declare(conn, channel, to_bytes(queue_name + ".dlq"),
                           0, true, false, false, amqp_empty_table);
        die_on_rpc_error("Declaring dead-letter queue");

        amqp_table_entry_t entries[3];
        entries[0].key = amqp_cstring_bytes("x-max-priority");
        entries[0].value.kind = AMQP_FIELD_KIND_I32;
        entries[0].value.value.i32 = 10;
        entries[1].key = amqp_cstring_bytes("x-dead-letter-exchange");
        entries[1].value.kind = AMQP_FIELD_KIND_UTF8;
        entries[1].value.value.bytes = amqp_cstring_bytes("");
        entries[2].key = amqp_cstring_bytes("x-dead-letter-routing-key");
        entries[2].value.kind = AMQP_FIELD_KIND_UTF8;
        entries[2].value.value.bytes = to_bytes(queue_name + ".dlq");
        amqp_table_t arguments{3, entries};

        // Declare a persistent priority queue with a dead-letter target.
        amqp_queue_declare_ok_t *queue_ret = amqp_queue_declare(
            conn, channel, to_bytes(queue_name),
            0,     // passive
            true,  // durable
            false, // exclusive
            false, // auto_delete
            arguments
        );
        if (!queue_ret) {
            std::cerr << "[consumer] error: failed to declare queue" << std::endl;
            exit(1);
        }
        
        // 设置 QoS - 每次只处理一条消息
        amqp_basic_qos(conn, channel, 0, 1, 0);
        die_on_rpc_error("Setting QoS");
        
        std::cout << "[consumer] connected successfully" << std::endl;
    }
    
    void start_consuming(int expected_messages = 0) {
        std::cout << "[consumer] waiting for messages..." << std::endl;
        
        // 开始消费
        amqp_basic_consume(conn, channel,
                          to_bytes(queue_name),
                          amqp_empty_bytes,  // consumer_tag
                          0,                 // no_local
                          0,                 // no_ack (false = manual ACK)
                          0,                 // exclusive
                          amqp_empty_table); // arguments
        die_on_rpc_error("Consuming");
        
        while (true) {
            // 等待消息
            amqp_maybe_release_buffers(conn);
            amqp_envelope_t envelope;
            
            amqp_rpc_reply_t ret = amqp_consume_message(conn, &envelope, nullptr, 0);
            
            if (ret.reply_type == AMQP_RESPONSE_NORMAL) {
                // 收到消息
                std::string message_body = from_bytes(envelope.message.body);
                std::cout << "[consumer] received: " << message_body << std::endl;
                
                // 处理消息
                process_message(message_body);
                
                // 手动 ACK
                amqp_basic_ack(conn, channel, envelope.delivery_tag, 0);
                
                message_count++;
                persisted++;
                std::cout << "[consumer] processed message #" << message_count << std::endl;
                
                // 销毁 envelope
                amqp_destroy_envelope(&envelope);
                
                // 如果指定了期望消息数，达到后退出
                if (expected_messages > 0 && message_count >= expected_messages) {
                    break;
                }
            } else if (ret.reply_type == AMQP_RESPONSE_LIBRARY_EXCEPTION) {
                amqp_rpc_reply_t ex_ret = amqp_get_rpc_reply(conn);
                if (ex_ret.library_error == AMQP_STATUS_UNEXPECTED_STATE) {
                    std::cerr << "[consumer] error: unexpected state" << std::endl;
                    break;
                }
            } else {
                std::cerr << "[consumer] error: consuming message failed" << std::endl;
                break;
            }
        }
        
        std::cout << "[consumer] processed " << message_count << " messages" << std::endl;
    }
    
    void close() {
        if (conn) {
            amqp_channel_close(conn, channel, AMQP_REPLY_SUCCESS);
            amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
            amqp_destroy_connection(conn);
            conn = nullptr;
            std::cout << "[consumer] connection closed" << std::endl;
        }
    }
    
    ~TrueConsumer() {
        close();
    }
};

int main(int argc, char* argv[]) {
    TrueConsumer consumer("localhost", 5672, "guest", "guest");
    
    try {
        consumer.connect();
        
        // 消费 10 条消息
        consumer.start_consuming(10);
        
        std::cout << "[consumer] done!" << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "[consumer] error: " << e.what() << std::endl;
        return 1;
    }
    
    return 0;
}
