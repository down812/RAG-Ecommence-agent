# RAG-E-commerce 智能电商平台

基于检索增强生成(RAG)技术的智能电商平台，提供AI智能导购功能。

## 技术架构

- **后端**：Spring Boot 3.2.0 + Spring AI 1.0.0-M6 + MySQL + Elasticsearch
- **前端**：Android 原生应用 (Jetpack Compose + Kotlin)

## 项目结构

```
├── server/                 # 后端 Spring Boot 项目
│   ├── src/main/java/
│   │   ├── controller/    # 控制器层
│   │   │   ├── ai/        # AI对话模块
│   │   │   └── user/      # 用户/商品/购物车模块
│   │   ├── service/impl/  # 服务实现
│   │   ├── model/         # 数据模型
│   │   ├── mapper/        # MyBatis Mapper
│   │   ├── config/        # 配置类
│   │   ├── advisor/       # AI咨询器
│   │   └── tool/          # AI工具
│   └── src/main/resources/
│       └── application.yaml
│
├── client/                 # 前端 Android 项目
│   └── app/src/main/java/
│       ├── core/network/  # 网络模块
│       ├── data/          # 数据层
│       │   ├── remote/api/    # API接口
│       │   └── remote/sse/    # SSE流式处理
│       ├── domain/        # 领域模型
│       └── ui/            # UI层
│           ├── chat/      # AI对话页面
│           ├── product/   # 商品页面
│           ├── cart/      # 购物车页面
│           └── admin/     # 管理后台
│
└── docs/                  # 文档
```

## 快速开始

### 后端启动

```bash
cd server
# 配置 application.yaml 中的数据库和Redis连接
mvn spring-boot:run
```

### 前端构建

```bash
cd client
./gradlew assembleDebug
```

## 核心功能

| 功能 | 说明 |
|------|------|
| AI智能导购 | 基于RAG的对话式商品搜索和推荐 |
| 商品管理 | 商品浏览、详情查看 |
| 购物车 | 添加商品、修改数量、结算 |
| 知识库管理 | 上传文档构建RAG知识库 |

## API端口

- 后端服务端口：`11650`
- 前端连接地址：`http://10.0.2.2:11650` (Android模拟器)

## 主要依赖

### 后端
- Spring Boot 3.2.0
- Spring AI 1.0.0-M6
- MyBatis-Plus 3.5.12
- Elasticsearch (向量存储)
- MySQL + Redis

### 前端
- Jetpack Compose
- Retrofit + OkHttp
- Coil (图片加载)
- StateFlow