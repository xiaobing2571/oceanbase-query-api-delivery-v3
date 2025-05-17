# OceanBase 通用查询 API 服务

## 项目概述

OceanBase 通用查询 API 服务是一个基于 Spring Boot 的 RESTful API 服务，提供了对 OceanBase 数据库的通用查询能力。该服务支持数据源管理、SQL 模板管理、查询场景管理和查询执行等功能，可以满足各种 OceanBase 运维和业务查询需求。

## 主要功能

- **数据源管理**：支持注册、测试和管理多个 OceanBase 数据源
- **SQL 模板管理**：支持注册和管理 SQL 模板，包括参数化查询和版本控制
- **查询场景管理**：支持将多个 SQL 模板组合成查询场景，便于常见查询场景的复用
- **查询执行**：支持同步和异步执行查询，并提供结果获取接口
- **内置运维场景**：预置了多个常见的 OceanBase 运维查询场景和 SQL 模板

## 快速开始

### 环境要求

- JDK 11 或以上
- Maven 3.6 或以上
- MySQL 5.7 或以上（用于存储元数据）
- OceanBase 数据库（用于实际查询）

### 配置

1. 修改 `application.properties` 文件中的数据库连接信息：

```properties
# 元数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/query_api_metadata
spring.datasource.username=root
spring.datasource.password=password

# 应用配置
server.port=8080
```

2. 修改测试数据中的数据源配置（如果需要使用预置数据）：

编辑 `src/main/resources/test-data.sql` 文件中的数据源配置，确保连接信息有效。

### 构建与运行

```bash
# 构建项目
mvn clean package

# 运行应用（开发环境，自动加载测试数据）
java -jar target/oceanbase-query-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 运行应用（生产环境，不加载测试数据）
java -jar target/oceanbase-query-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## API 接口说明

### 数据源管理

- **测试数据源连通性**：`POST /api/query/v1/datasource/test`
- **注册数据源**：`POST /api/query/v1/datasource/register`
- **获取所有数据源**：`GET /api/query/v1/datasource`
- **获取数据源详情**：`GET /api/query/v1/datasource/{id}`
- **删除数据源**：`DELETE /api/query/v1/datasource/{id}`

### 模板管理

- **注册 SQL 模板**：`POST /api/query/v1/templates/register`
- **获取所有模板**：`GET /api/query/v1/templates`
- **获取模板详情**：`GET /api/query/v1/templates/{id}`
- **删除模板**：`DELETE /api/query/v1/templates/{id}`

### 场景管理

- **注册查询场景**：`POST /api/query/v1/scenes/register`
- **获取所有场景**：`GET /api/query/v1/scenes`
- **获取场景详情**：`GET /api/query/v1/scenes/{code}`
- **删除场景**：`DELETE /api/query/v1/scenes/{code}`

### 查询执行

- **执行查询场景**：`POST /api/query/v1/scenes/{scene_code}/execute`
- **获取异步任务结果**：`GET /api/query/v1/result/{task_id}`

## 内置运维场景

服务预置了多个常见的 OceanBase 运维查询场景，包括：

1. **集群状态监控**：监控 OceanBase 集群的整体状态
2. **性能诊断**：诊断 OceanBase 集群性能问题
3. **备份恢复监控**：监控 OceanBase 备份恢复任务的状态和进度
4. **合并监控**：监控 OceanBase 合并任务的状态和进度
5. **租户资源使用**：监控 OceanBase 租户的资源使用情况

详细的场景和模板说明请参考 [测试数据与内置场景说明](docs/test-data-guide.md)。

## 自动化测试

项目提供了完整的 Postman 测试集合，覆盖所有主要接口和内置场景。

详细的测试说明请参考 [Postman 测试指南](docs/postman-test-guide.md)。

## 项目结构

```
oceanbase-query-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/oceanbase/query/api/
│   │   │       ├── config/           # 配置类
│   │   │       ├── controller/       # 控制器
│   │   │       ├── domain/           # 领域模型
│   │   │       │   ├── entity/       # 实体类
│   │   │       │   └── repository/   # 仓库接口
│   │   │       ├── dto/              # 数据传输对象
│   │   │       └── service/          # 服务层
│   │   │           └── impl/         # 服务实现
│   │   └── resources/
│   │       ├── application.properties # 应用配置
│   │       └── test-data.sql         # 测试数据脚本
│   └── test/                         # 测试代码
├── docs/                             # 文档
│   ├── test-data-guide.md            # 测试数据与内置场景说明
│   ├── postman-test-guide.md         # Postman 测试指南
│   └── OceanBase_Query_API_Tests.postman_collection.json # Postman 测试集合
├── pom.xml                           # Maven 配置
└── README.md                         # 项目说明
```

## 注意事项

1. 在生产环境中使用时，请确保数据源连接信息的安全性
2. 对于需要长时间运行的查询，建议使用异步执行模式
3. SQL 模板中的参数应使用 `:参数名` 的格式，如 `:tenant_id`
4. 不同版本的 OceanBase 数据库表结构可能有差异，SQL 模板已尽量兼容不同版本

## 贡献与反馈

如有问题或建议，请提交 Issue 或 Pull Request。

## 许可证

本项目采用 MIT 许可证。
# oceanbase-query-api-delivery-v3
