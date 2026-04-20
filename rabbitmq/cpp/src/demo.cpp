#include <filesystem>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace fs = std::filesystem;

struct Message {
    std::string message_id;
    std::string priority;
    std::string body;

    std::string encode() const {
        return message_id + "|" + priority + "|" + body;
    }

    static Message decode(const std::string& line) {
        std::stringstream ss(line);
        std::string id;
        std::string prio;
        std::string content;
        if (!std::getline(ss, id, '|') || !std::getline(ss, prio, '|') || !std::getline(ss, content)) {
            throw std::runtime_error("invalid message line: " + line);
        }
        return {id, prio, content};
    }
};

std::vector<Message> build_messages(const std::string& prefix, int count, int critical_every) {
    std::vector<Message> messages;
    for (int i = 1; i <= count; ++i) {
        std::ostringstream id;
        id << prefix << "-";
        if (i < 10) {
            id << "00";
        } else if (i < 100) {
            id << "0";
        }
        id << i;
        std::string priority = (critical_every > 0 && i % critical_every == 0) ? "critical" : "normal";
        messages.push_back({id.str(), priority, prefix + " payload #" + std::to_string(i)});
    }
    return messages;
}

void append_line(const fs::path& path, const std::string& line) {
    fs::create_directories(path.parent_path());
    std::ofstream out(path, std::ios::app);
    out << line << '\n';
}

std::vector<Message> read_messages(const fs::path& path) {
    std::vector<Message> messages;
    if (!fs::exists(path)) {
        return messages;
    }

    std::ifstream input(path);
    std::string line;
    while (std::getline(input, line)) {
        if (!line.empty()) {
            messages.push_back(Message::decode(line));
        }
    }
    return messages;
}

void truncate_file(const fs::path& path) {
    fs::create_directories(path.parent_path());
    std::ofstream out(path, std::ios::trunc);
}

int count_lines(const fs::path& path) {
    if (!fs::exists(path)) {
        return 0;
    }
    std::ifstream input(path);
    std::string line;
    int count = 0;
    while (std::getline(input, line)) {
        if (!line.empty()) {
            ++count;
        }
    }
    return count;
}

void reset_runtime(const fs::path& runtime_dir) {
    if (fs::exists(runtime_dir)) {
        fs::remove_all(runtime_dir);
    }
    fs::create_directories(runtime_dir);
}

class Producer {
public:
    explicit Producer(const fs::path& runtime_dir)
        : queue_file_(runtime_dir / "queue.txt"),
          outbox_file_(runtime_dir / "outbox.txt") {
    }

    void set_mq_up(bool mq_up) {
        mq_up_ = mq_up;
    }

    void send(const Message& message) {
        const fs::path& target = mq_up_ ? queue_file_ : outbox_file_;
        append_line(target, message.encode());
        if (mq_up_) {
            ++sent_to_queue_;
            std::cout << "[producer] send to queue: " << message.message_id
                      << " (" << message.priority << ")" << std::endl;
        } else {
            ++spooled_to_outbox_;
            std::cout << "[producer] mq down, spool to outbox: " << message.message_id << std::endl;
        }
    }

    void send_batch(const std::vector<Message>& messages) {
        for (const auto& message : messages) {
            send(message);
        }
    }

    void replay_outbox() {
        if (!mq_up_) {
            std::cout << "[producer] mq still down, skip outbox replay" << std::endl;
            return;
        }

        auto messages = read_messages(outbox_file_);
        if (messages.empty()) {
            std::cout << "[producer] outbox empty, nothing to replay" << std::endl;
            return;
        }

        std::cout << "[producer] replay outbox messages: " << messages.size() << std::endl;
        for (const auto& message : messages) {
            append_line(queue_file_, message.encode());
            ++sent_to_queue_;
        }
        truncate_file(outbox_file_);
    }

private:
    fs::path queue_file_;
    fs::path outbox_file_;
    bool mq_up_ = true;
    int sent_to_queue_ = 0;
    int spooled_to_outbox_ = 0;
};

class Consumer {
public:
    explicit Consumer(const fs::path& runtime_dir)
        : queue_file_(runtime_dir / "queue.txt"),
          pending_db_file_(runtime_dir / "pending_db.txt"),
          dead_letter_file_(runtime_dir / "dead_letter.txt"),
          db_file_(runtime_dir / "db.txt") {
    }

    void set_mysql_up(bool mysql_up) {
        mysql_up_ = mysql_up;
    }

    void process_queue(int backlog_threshold, int max_retry) {
        auto messages = read_messages(queue_file_);
        if (messages.empty()) {
            std::cout << "[consumer] queue empty" << std::endl;
            return;
        }

        int backlog = static_cast<int>(messages.size());
        std::cout << "[consumer] start consume, backlog=" << backlog << std::endl;

        std::vector<Message> keep;
        if (backlog > backlog_threshold) {
            std::cout << "[consumer] backlog threshold exceeded, enter surge mode" << std::endl;
            for (const auto& message : messages) {
                if (message.priority == "critical") {
                    keep.push_back(message);
                } else if (static_cast<int>(keep.size()) < backlog_threshold) {
                    keep.push_back(message);
                } else {
                    append_line(dead_letter_file_, message.encode());
                    ++degraded_;
                    std::cout << "[consumer] degrade low priority message: " << message.message_id << std::endl;
                }
            }
        } else {
            keep = messages;
        }

        truncate_file(queue_file_);
        for (const auto& message : keep) {
            persist_with_retry(message, max_retry);
        }
    }

    void flush_pending_db() {
        auto messages = read_messages(pending_db_file_);
        if (messages.empty()) {
            std::cout << "[consumer] pending_db empty" << std::endl;
            return;
        }
        if (!mysql_up_) {
            std::cout << "[consumer] mysql still down, skip pending_db flush" << std::endl;
            return;
        }

        std::cout << "[consumer] flush pending_db messages: " << messages.size() << std::endl;
        truncate_file(pending_db_file_);
        for (const auto& message : messages) {
            append_line(db_file_, message.encode());
            ++persisted_;
        }
    }

    int persisted() const {
        return persisted_;
    }

    int retried() const {
        return retried_;
    }

    int degraded() const {
        return degraded_;
    }

    int pending() const {
        return pending_;
    }

private:
    void persist_with_retry(const Message& message, int max_retry) {
        if (mysql_up_) {
            append_line(db_file_, message.encode());
            ++persisted_;
            std::cout << "[consumer] persist to mysql: " << message.message_id << std::endl;
            return;
        }

        for (int retry = 1; retry <= max_retry; ++retry) {
            ++retried_;
            std::cout << "[consumer] mysql down, retry " << retry << "/" << max_retry
                      << ": " << message.message_id << std::endl;
        }

        append_line(pending_db_file_, message.encode());
        ++pending_;
        std::cout << "[consumer] write to pending_db: " << message.message_id << std::endl;
    }

    fs::path queue_file_;
    fs::path pending_db_file_;
    fs::path dead_letter_file_;
    fs::path db_file_;
    bool mysql_up_ = true;
    int persisted_ = 0;
    int retried_ = 0;
    int degraded_ = 0;
    int pending_ = 0;
};

int main() {
    fs::path runtime_dir = fs::absolute(fs::path("rabbitmq") / "cpp" / "runtime");
    reset_runtime(runtime_dir);

    Producer producer(runtime_dir);
    Consumer consumer(runtime_dir);

    std::cout << "=== C++ RabbitMQ resilience demo ===" << std::endl;

    std::cout << "\n[scenario-1] backlog" << std::endl;
    producer.send_batch(build_messages("backlog", 10, 4));
    std::cout << "[state] queue backlog before consume: " << count_lines(runtime_dir / "queue.txt") << std::endl;
    consumer.process_queue(6, 2);
    std::cout << "[state] queue backlog after consume: " << count_lines(runtime_dir / "queue.txt") << std::endl;

    std::cout << "\n[scenario-2] mq down" << std::endl;
    producer.set_mq_up(false);
    producer.send_batch(build_messages("mq-down", 4, 2));
    std::cout << "[state] outbox size while mq down: " << count_lines(runtime_dir / "outbox.txt") << std::endl;
    producer.set_mq_up(true);
    producer.replay_outbox();
    std::cout << "[state] queue size after mq recovery: " << count_lines(runtime_dir / "queue.txt") << std::endl;

    std::cout << "\n[scenario-3] mysql down" << std::endl;
    consumer.set_mysql_up(false);
    consumer.process_queue(20, 2);
    std::cout << "[state] pending_db size while mysql down: " << count_lines(runtime_dir / "pending_db.txt") << std::endl;
    consumer.set_mysql_up(true);
    consumer.flush_pending_db();
    std::cout << "[state] db size after mysql recovery: " << count_lines(runtime_dir / "db.txt") << std::endl;

    std::cout << "\n[scenario-4] mq and mysql both down" << std::endl;
    producer.set_mq_up(false);
    consumer.set_mysql_up(false);
    producer.send_batch(build_messages("double-down", 3, 1));
    std::cout << "[state] outbox size during double-down: " << count_lines(runtime_dir / "outbox.txt") << std::endl;
    producer.set_mq_up(true);
    producer.replay_outbox();
    consumer.process_queue(20, 2);
    std::cout << "[state] pending_db size after double-down consume: " << count_lines(runtime_dir / "pending_db.txt") << std::endl;
    consumer.set_mysql_up(true);
    consumer.flush_pending_db();

    std::cout << "\n=== Summary ===" << std::endl;
    std::cout << "persisted=" << consumer.persisted() << std::endl;
    std::cout << "retried=" << consumer.retried() << std::endl;
    std::cout << "degraded=" << consumer.degraded() << std::endl;
    std::cout << "pending_written=" << consumer.pending() << std::endl;
    std::cout << "db_records=" << count_lines(runtime_dir / "db.txt") << std::endl;
    std::cout << "dead_letters=" << count_lines(runtime_dir / "dead_letter.txt") << std::endl;
    std::cout << "outbox_remaining=" << count_lines(runtime_dir / "outbox.txt") << std::endl;
    std::cout << "pending_remaining=" << count_lines(runtime_dir / "pending_db.txt") << std::endl;

    return 0;
}
