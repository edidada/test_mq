cmake_minimum_required(VERSION 3.10)
project(test_mq)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Threads REQUIRED)

include_directories(${PROJECT_SOURCE_DIR}/include)

add_executable(consumer src/consumer.cpp)
target_link_libraries(consumer Threads::Threads)

add_executable(producer src/producer.cpp)
target_link_libraries(producer Threads::Threads)

add_executable(handler src/handler.cpp)
target_link_libraries(handler Threads::Threads)
