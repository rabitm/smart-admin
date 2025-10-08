# RocketMQ 单节点生产级部署说明

## 📋 目录

- [部署架构](#部署架构)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [性能优化](#性能优化)
- [监控与运维](#监控与运维)
- [常见问题](#常见问题)
- [参考资料](#参考资料)

---

## 🏗️ 部署架构

本方案采用 **单节点生产级部署**，包含以下核心组件:

```
┌─────────────────────────────────────────────────┐
│               RocketMQ 单节点架构                │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────┐      ┌──────────────┐       │
│  │  NameServer  │      │   Dashboard  │       │
│  │  (9876)      │      │   (19876)    │       │
│  └──────┬───────┘      └──────────────┘       │
│         │                                       │
│  ┌──────▼───────────────────────────┐         │
│  │         Broker Master             │         │
│  │  (10909, 10911, 10912)           │         │
│  │                                   │         │
│  │  ┌────────────┐  ┌────────────┐ │         │
│  │  │ CommitLog  │  │ ConsumeQ   │ │         │
│  │  └────────────┘  └────────────┘ │         │
│  └───────────────────────────────────┘         │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 组件说明

| 组件 | 端口 | 说明 |
|------|------|------|
| **NameServer** | 9876 | 路由注册中心，管理 Broker 的路由信息 |
| **Broker** | 10909, 10911, 10912 | 消息存储和转发核心组件 |
| **Dashboard** | 19876 | Web 管理控制台 (映射到宿主机 19876 端口) |

---

## 💻 环境要求

### 硬件配置建议

| 资源 | 最低配置 | 推荐配置 | 说明 |
|------|----------|----------|------|
| **CPU** | 2 核 | 4 核以上 | 建议使用多核 CPU |
| **内存** | 4 GB | 8 GB 以上 | NameServer 512MB, Broker 2GB, Dashboard 256MB |
| **磁盘** | 50 GB | 200 GB 以上 SSD | 消息存储需要较大空间，建议使用 SSD |
| **网络** | 1 Gbps | 10 Gbps | 高吞吐量场景建议使用万兆网卡 |

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **操作系统**: Linux (推荐 Ubuntu 20.04+, CentOS 7+) / Windows / macOS

---

## 🚀 快速启动

### 1. 克隆或准备配置文件

确保以下文件结构存在:

```
smart-admin/
├── rocketmq-docker-compose.yml          # Docker Compose 配置文件
└── rocketmq/
    └── conf/
        └── broker.conf                   # Broker 配置文件
```

### 2. 启动 RocketMQ 集群

```bash
# 进入项目目录
cd I:\Claude\code\smart-admin

# 启动所有服务 (后台运行)
docker-compose -f rocketmq-docker-compose.yml up -d

# 查看服务状态
docker-compose -f rocketmq-docker-compose.yml ps

# 查看实时日志
docker-compose -f rocketmq-docker-compose.yml logs -f
```

### 3. 验证部署

**检查服务状态:**

```bash
# 查看所有容器状态
docker ps | grep rocketmq

# 应该看到以下 3 个容器运行中:
# - rocketmq-namesrv
# - rocketmq-broker
# - rocketmq-dashboard
```

**访问 Dashboard:**

打开浏览器访问: `http://localhost:19876`

如果能正常访问管理界面，说明部署成功！

### 4. 测试消息收发

**发送测试消息:**

```bash
# 进入 Broker 容器
docker exec -it rocketmq-broker bash

# 创建 Topic
sh mqadmin updateTopic -n rocketmq-namesrv:9876 -t TestTopic -c DefaultCluster

# 发送测试消息
sh tools.sh org.apache.rocketmq.example.quickstart.Producer

# 消费测试消息
sh tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

---

## ⚙️ 配置说明

### Docker Compose 配置

**关键配置项说明:**

```yaml
# NameServer JVM 配置
JAVA_OPT_EXT: >
  -Xms512m              # 初始堆内存 512MB
  -Xmx512m              # 最大堆内存 512MB
  -XX:+UseG1GC          # 使用 G1 垃圾回收器

# Broker JVM 配置
JAVA_OPT_EXT: >
  -Xms2g                # 初始堆内存 2GB
  -Xmx2g                # 最大堆内存 2GB
  -XX:+UseG1GC          # 使用 G1 垃圾回收器
  -XX:MaxGCPauseMillis=100  # GC 最大暂停时间 100ms
```

### Broker 配置文件详解

**核心配置项 (`rocketmq/conf/broker.conf`):**

```properties
# 集群与身份配置
brokerClusterName = DefaultCluster    # 集群名称
brokerName = broker-a                 # Broker 名称
brokerId = 0                          # 0=Master, >0=Slave

# 网络配置
brokerIP1 = rocketmq-broker           # Broker 对外服务地址
listenPort = 10911                    # Broker 服务端口

# 存储配置
storePathRootDir = /home/rocketmq/store              # 根存储目录
mapedFileSizeCommitLog = 1073741824                  # CommitLog 文件大小 1GB
fileReservedTime = 48                                # 消息保留时间 48 小时
diskMaxUsedSpaceRatio = 90                           # 磁盘最大使用率 90%

# 刷盘策略
flushDiskType = ASYNC_FLUSH           # 异步刷盘 (高性能)
flushIntervalCommitLog = 500          # 刷盘间隔 500ms

# 主从复制
brokerRole = ASYNC_MASTER             # 异步 Master 模式

# 线程池配置
sendMessageThreadPoolNums = 16       # 发送消息线程数
pullMessageThreadPoolNums = 16       # 拉取消息线程数

# 自动创建
autoCreateTopicEnable = true         # 自动创建 Topic
autoCreateSubscriptionGroup = true   # 自动创建订阅组
```

---

## 🚄 性能优化

### 1. JVM 参数优化

**NameServer 优化 (512MB 堆内存):**

```bash
-server                              # 服务器模式
-Xms512m -Xmx512m -Xmn256m          # 堆内存设置
-XX:+UseG1GC                         # G1 垃圾回收器
-XX:MaxGCPauseMillis=100            # GC 最大暂停时间
-XX:+ParallelRefProcEnabled          # 并行处理引用
-XX:+UseStringDeduplication          # 字符串去重
```

**Broker 优化 (2GB 堆内存):**

```bash
-server                              # 服务器模式
-Xms2g -Xmx2g -Xmn1g                # 堆内存设置
-XX:+UseG1GC                         # G1 垃圾回收器
-XX:G1HeapRegionSize=16m            # G1 区域大小
-XX:InitiatingHeapOccupancyPercent=30  # GC 触发阈值
-XX:MaxGCPauseMillis=100            # GC 最大暂停时间
-XX:+HeapDumpOnOutOfMemoryError     # OOM 时生成堆转储
```

### 2. 存储性能优化

**建议使用 SSD:**

- CommitLog 顺序写入性能要求高
- ConsumeQueue 随机读取性能要求高
- 建议使用 SSD 或 NVMe 存储

**磁盘配置建议:**

```bash
# Linux 系统调优
# 增加文件句柄数
ulimit -n 65535

# 调整 IO 调度策略
echo deadline > /sys/block/sda/queue/scheduler

# 关闭 swap
swapoff -a
```

### 3. 网络性能优化

**Broker 网络参数:**

```properties
# broker.conf 中添加
sendMessageThreadPoolNums = 32       # 根据 CPU 核心数调整
pullMessageThreadPoolNums = 32       # 根据 CPU 核心数调整
clientManagerThreadPoolNums = 64     # 客户端管理线程数
```

### 4. 消息存储优化

**根据业务场景调整:**

```properties
# 高吞吐量场景 (推荐异步刷盘)
flushDiskType = ASYNC_FLUSH
flushIntervalCommitLog = 500

# 高可靠性场景 (同步刷盘，性能较低)
flushDiskType = SYNC_FLUSH

# 批量消息优化
maxMessageSize = 4194304             # 最大消息 4MB
```

---

## 📊 监控与运维

### 1. Dashboard 使用

访问: `http://localhost:19876`

**主要功能:**

- **驾驶舱**: 查看集群运行状态
- **运维**: 管理 NameServer、Broker
- **主题**: 创建、删除、修改 Topic
- **消费者**: 查看消费者组状态
- **生产者**: 查看生产者状态
- **消息**: 消息查询和轨迹追踪

### 2. 日志管理

**日志位置:**

```bash
# NameServer 日志
docker logs rocketmq-namesrv

# Broker 日志
docker logs rocketmq-broker

# Dashboard 日志
docker logs rocketmq-dashboard

# 查看 GC 日志
docker exec rocketmq-broker tail -f /home/rocketmq/logs/gc_broker.log
```

### 3. 健康检查

**服务健康检查:**

```bash
# 检查 NameServer
curl -s http://localhost:9876/status.html || echo "NameServer 异常"

# 检查 Broker 端口
netstat -an | grep 10911 || echo "Broker 异常"

# 检查 Dashboard
curl -s http://localhost:19876 || echo "Dashboard 异常"
```

### 4. 数据备份

**备份重要数据:**

```bash
# 备份 Broker 存储数据
docker run --rm -v rocketmq-broker-store:/data -v $(pwd):/backup \
  alpine tar czf /backup/broker-store-backup-$(date +%Y%m%d).tar.gz /data

# 备份配置文件
tar czf rocketmq-config-backup-$(date +%Y%m%d).tar.gz rocketmq/conf/
```

### 5. 性能监控指标

**关键指标:**

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| **CPU 使用率** | Broker CPU 占用 | > 80% |
| **内存使用率** | JVM 堆内存使用 | > 85% |
| **磁盘使用率** | 存储目录占用 | > 85% |
| **消息堆积量** | 未消费消息数量 | 业务自定 |
| **TPS** | 每秒消息吞吐量 | 业务自定 |
| **消息延迟** | 消息生产到消费延迟 | > 1000ms |

---

## 🛠️ 常见问题

### 1. 启动失败

**问题: Broker 无法连接 NameServer**

```bash
# 解决方案: 检查网络配置
docker network inspect rocketmq-network

# 确保容器在同一网络中
docker-compose -f rocketmq-docker-compose.yml restart
```

**问题: 端口冲突**

```bash
# 解决方案: 修改端口映射
# 编辑 rocketmq-docker-compose.yml，修改 ports 配置
ports:
  - "19877:8080"  # 将 Dashboard 端口改为 19877
```

### 2. 性能问题

**问题: 消息发送/消费缓慢**

```bash
# 解决方案 1: 增加 Broker 堆内存
# 修改 docker-compose.yml 中 JAVA_OPT_EXT
-Xms4g -Xmx4g  # 增加到 4GB

# 解决方案 2: 增加线程池大小
# 修改 broker.conf
sendMessageThreadPoolNums = 32
pullMessageThreadPoolNums = 32
```

**问题: 磁盘写入慢**

```bash
# 解决方案: 检查磁盘 IO
iostat -x 1

# 如果 %util 接近 100%，建议:
# 1. 更换 SSD
# 2. 调整刷盘策略为异步
```

### 3. 数据丢失

**问题: 消息丢失**

```bash
# 原因分析:
# 1. 使用异步刷盘 (ASYNC_FLUSH)
# 2. Broker 异常宕机

# 解决方案:
# 1. 使用同步刷盘 (牺牲性能)
flushDiskType = SYNC_FLUSH

# 2. 启用主从同步 (高可用方案)
brokerRole = SYNC_MASTER
```

### 4. 磁盘空间不足

**问题: 磁盘占满**

```bash
# 解决方案 1: 调整消息保留时间
# 修改 broker.conf
fileReservedTime = 24  # 改为 24 小时

# 解决方案 2: 手动清理
docker exec rocketmq-broker sh mqadmin cleanExpiredCQ -n rocketmq-namesrv:9876

# 解决方案 3: 增加磁盘空间
# 扩展 Docker Volume 或挂载新磁盘
```

### 5. 消息堆积

**问题: 消费者消费缓慢，消息堆积**

```bash
# 解决方案 1: 增加消费者实例数
# 部署多个消费者应用

# 解决方案 2: 增加消费线程数
# 客户端配置
consumer.setConsumeThreadMin(20);
consumer.setConsumeThreadMax(64);

# 解决方案 3: 优化消费逻辑
# 减少消费者业务处理时间
```

---

## 🔄 运维操作

### 停止服务

```bash
# 停止所有服务
docker-compose -f rocketmq-docker-compose.yml stop

# 停止并删除容器
docker-compose -f rocketmq-docker-compose.yml down

# 停止并删除容器及数据卷 (危险操作!)
docker-compose -f rocketmq-docker-compose.yml down -v
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f rocketmq-docker-compose.yml restart

# 重启单个服务
docker-compose -f rocketmq-docker-compose.yml restart rocketmq-broker
```

### 更新配置

```bash
# 1. 修改配置文件
vim rocketmq/conf/broker.conf

# 2. 重启 Broker 服务
docker-compose -f rocketmq-docker-compose.yml restart rocketmq-broker

# 3. 查看日志确认
docker logs -f rocketmq-broker
```

### 数据清理

```bash
# 清理过期消息
docker exec rocketmq-broker sh mqadmin cleanExpiredCQ -n rocketmq-namesrv:9876

# 删除指定 Topic
docker exec rocketmq-broker sh mqadmin deleteTopic -n rocketmq-namesrv:9876 -t TestTopic
```

---

## 📚 参考资料

### 官方文档

- [RocketMQ 官方网站](https://rocketmq.apache.org/)
- [RocketMQ GitHub](https://github.com/apache/rocketmq)
- [RocketMQ Docker Hub](https://hub.docker.com/r/apache/rocketmq)
- [RocketMQ Dashboard](https://github.com/apache/rocketmq-dashboard)

### 推荐阅读

- [RocketMQ 架构设计](https://rocketmq.apache.org/docs/architecture/)
- [RocketMQ 最佳实践](https://rocketmq.apache.org/docs/bestPractice/)
- [RocketMQ 运维指南](https://rocketmq.apache.org/docs/operations/)
- [RocketMQ 性能调优](https://rocketmq.apache.org/docs/performanceTuning/)

### 社区资源

- [RocketMQ 中文文档](https://github.com/apache/rocketmq/tree/master/docs/cn)
- [RocketMQ 用户邮件列表](mailto:users@rocketmq.apache.org)
- [RocketMQ 开发者邮件列表](mailto:dev@rocketmq.apache.org)

---

## 📝 附录

### A. 端口说明

| 端口 | 协议 | 说明 |
|------|------|------|
| 9876 | TCP | NameServer 对外服务端口 |
| 10909 | TCP | Broker VIP 通道端口 (快速通道) |
| 10911 | TCP | Broker 对外服务端口 (生产者/消费者连接) |
| 10912 | TCP | Broker HA 主从同步端口 |
| 19876 | HTTP | Dashboard Web 管理界面 |

### B. 目录结构

```
/home/rocketmq/
├── store/                           # 数据存储目录
│   ├── commitlog/                   # CommitLog 文件
│   ├── consumequeue/                # ConsumeQueue 文件
│   ├── index/                       # 索引文件
│   ├── checkpoint                   # 检查点文件
│   └── abort                        # 异常关闭标记
└── logs/                            # 日志目录
    ├── rocketmqlogs/                # 业务日志
    ├── gc_broker.log                # GC 日志
    └── heap_dump.hprof              # OOM 堆转储 (如果发生)
```

### C. 环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `NAMESRV_ADDR` | NameServer 地址 | `rocketmq-namesrv:9876` |
| `JAVA_OPT_EXT` | JVM 扩展参数 | `-Xms2g -Xmx2g` |

### D. 快捷命令

```bash
# 创建别名 (可选)
alias rmq-start='docker-compose -f rocketmq-docker-compose.yml up -d'
alias rmq-stop='docker-compose -f rocketmq-docker-compose.yml stop'
alias rmq-restart='docker-compose -f rocketmq-docker-compose.yml restart'
alias rmq-logs='docker-compose -f rocketmq-docker-compose.yml logs -f'
alias rmq-ps='docker-compose -f rocketmq-docker-compose.yml ps'

# 使用方式
rmq-start    # 启动
rmq-logs     # 查看日志
```

---

## ✅ 部署检查清单

**部署前:**

- [ ] 确认硬件资源满足要求 (CPU ≥ 2核, 内存 ≥ 4GB, 磁盘 ≥ 50GB)
- [ ] 安装 Docker 和 Docker Compose
- [ ] 准备配置文件 (`broker.conf`)
- [ ] 检查端口是否被占用 (9876, 10909, 10911, 10912, 19876)

**部署中:**

- [ ] 执行 `docker-compose up -d` 启动服务
- [ ] 检查容器状态 `docker ps | grep rocketmq`
- [ ] 查看日志确认无错误 `docker logs rocketmq-broker`

**部署后:**

- [ ] 访问 Dashboard (http://localhost:19876)
- [ ] 发送测试消息验证功能
- [ ] 配置监控告警
- [ ] 制定备份计划
- [ ] 文档归档

---

**文档版本:** v2.0
**更新日期:** 2025-10-07
**维护者:** SmartAdmin Team
**适用版本:** RocketMQ 5.3.1+

---

如有问题，请联系技术支持或提交 Issue。
