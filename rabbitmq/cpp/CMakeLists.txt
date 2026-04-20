cmake_minimum_required(VERSION 3.16)
project(rabbitmq_resilience_demo)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 查找 rabbitmq-c 库（不使用 pkg-config）
find_path(RABBITMQ_INCLUDE_DIR NAMES amqp.h)
find_library(RABBITMQ_LIBRARY NAMES rabbitmq)

if(RABBITMQ_INCLUDE_DIR AND RABBITMQ_LIBRARY)
    message(STATUS "Found rabbitmq-c:")
    message(STATUS "  Include dir: ${RABBITMQ_INCLUDE_DIR}")
    message(STATUS "  Library: ${RABBITMQ_LIBRARY}")
else()
    message(FATAL_ERROR "Could not find rabbitmq-c")
endif()

# 包含 rabbitmq-c 头文件路径
include_directories(${RABBITMQ_INCLUDE_DIR})

# 真实 RabbitMQ 生产者
add_executable(true_producer
    src/true_producer.cpp
)
target_link_libraries(true_producer ${RABBITMQ_LIBRARY})

# 真实 RabbitMQ 消费者
add_executable(true_consumer
    src/true_consumer.cpp
)
target_link_libraries(true_consumer ${RABBITMQ_LIBRARY})

# 文件系统模拟版本（保留原有代码）
add_executable(demo
    src/demo.cpp
)
