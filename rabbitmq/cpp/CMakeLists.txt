cmake_minimum_required(VERSION 3.16)
project(rabbitmq_resilience_demo)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 查找 rabbitmq-c 库
find_package(PkgConfig REQUIRED)
pkg_check_modules(RABBITMQ REQUIRED rabbitmq)

# 包含 rabbitmq-c 头文件路径
include_directories(${RABBITMQ_INCLUDE_DIRS})

# 真实 RabbitMQ 生产者
add_executable(true_producer
    src/true_producer.cpp
)
target_link_libraries(true_producer ${RABBITMQ_LIBRARIES})
target_include_directories(true_producer PRIVATE ${RABBITMQ_INCLUDE_DIRS})

# 真实 RabbitMQ 消费者
add_executable(true_consumer
    src/true_consumer.cpp
)
target_link_libraries(true_consumer ${RABBITMQ_LIBRARIES})
target_include_directories(true_consumer PRIVATE ${RABBITMQ_INCLUDE_DIRS})

# 文件系统模拟版本（保留原有代码）
add_executable(demo
    src/demo.cpp
)
