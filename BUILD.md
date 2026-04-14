# 项目构建说明

## 目录结构

```
test_mq/
├── README.md
├── 操作指南.md
├── BUILD.md
├──
├── java/                    # Maven Java 项目
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── testmq/
│                       ├── MQProducer.java
│                       └── MQConsumer.java
│
└── cpp/                     # CMake C++ 项目
    ├── CMakeLists.txt
    └── src/
        ├── consumer.cpp
        ├── producer.cpp
        └── handler.cpp
```

## Java 项目构建 (Maven)

### 前置要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本

### 编译

```bash
cd java
mvn clean compile
```

### 打包

```bash
mvn clean package
```

### 运行

运行生产者：
```bash
mvn exec:java -Dexec.mainClass="com.testmq.MQProducer"
```

运行消费者：
```bash
mvn exec:java -Dexec.mainClass="com.testmq.MQConsumer"
```

## C++ 项目构建 (CMake)

### 前置要求

- C++11 兼容的编译器 (g++ 4.8+, clang 3.3+, MSVC 2015+)
- CMake 3.10 或更高版本

### Windows 编译

使用 PowerShell：

```bash
cd cpp
mkdir build
cd build
cmake ..
cmake --build . --config Release
```

或者使用 MinGW：

```bash
cd cpp
mkdir build
cd build
cmake -G "MinGW Makefiles" ..
mingw32-make
```

### Linux/WSL 编译

```bash
cd cpp
mkdir -p build && cd build
cmake ..
make -j$(nproc)
```

### 运行可执行文件

编译成功后，可执行文件位于 `build/` 目录下：

```bash
# 运行消费者
./build/consumer

# 运行生产者
./build/producer

# 运行故障处理器
./build/handler
```

## 快速开始

### Java 快速测试

```bash
cd java

# 编译
mvn clean install

# 启动消费者 (在一个终端)
mvn exec:java -Dexec.mainClass="com.testmq.MQConsumer"

# 启动生产者 (在另一个终端)
mvn exec:java -Dexec.mainClass="com.testmq.MQProducer"
```

### C++ 快速测试

```bash
cd cpp
mkdir build && cd build
cmake .. && make

# 运行各个示例
./consumer
./producer
./handler
```
