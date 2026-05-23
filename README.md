# KnowHub - 企业级多租户 AI 知识库平台

## 项目简介
支持多租户隔离的企业内部知识库平台，用户可上传文档，通过 AI 进行智能问答。

## 技术栈
| 层次 | 技术                                                               |
|------|------------------------------------------------------------------|
| 框架 | Java 17, Spring Boot 3.5                                         |
| 数据库 | MySQL 8.0, PostgreSQL 16 + pgvector                              |
| 缓存 | Redis 7, Redisson                                                |
| AI | Spring AI, 通义千问 Embedding, qwen-turbo                            |
| 消息队列 | RabbitMQ (TODO)                                                  |
| 基础设施 | Docker Compose, JMeter (TODO), Prometheus (TODO), Grafana (TODO) |
## 核心功能
- 多租户知识库管理，数据严格隔离
- 文档上传解析（txt/md），向量化存储
- RAG 智能问答，SSE 流式输出打字机效果
- Redis 缓存，接口限流，分布式锁

## 快速启动
```bash
docker compose up -d
# 设置环境变量
export DASHSCOPE_API_KEY=your_key
# 启动应用
mvn spring-boot:run
```

## 架构图
TODO