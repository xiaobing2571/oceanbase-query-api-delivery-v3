# OceanBase 通用查询 API 服务 - 测试数据与内置场景说明

## 测试数据概述

本项目包含一套完整的测试数据集，用于接口测试和功能演示。测试数据包括：

1. **数据源配置**：预置了3个测试数据源
2. **查询场景**：包含5个常见的 OceanBase 运维场景
3. **SQL 模板**：包含19个常用的 OceanBase 运维查询 SQL 模板，每个模板都支持不同版本的 OceanBase

## 内置查询场景

| 场景代码 | 场景名称 | 说明 | 包含模板数量 |
|---------|---------|------|------------|
| cluster_status_monitor | 集群状态监控 | 监控 OceanBase 集群的整体状态，包括租户、服务器、分区等信息 | 4 |
| performance_diagnosis | 性能诊断 | 诊断 OceanBase 集群性能问题，包括慢 SQL、内存使用、CPU 使用等 | 4 |
| backup_recovery_monitor | 备份恢复监控 | 监控 OceanBase 备份恢复任务的状态和进度 | 4 |
| merge_monitor | 合并监控 | 监控 OceanBase 合并任务的状态和进度 | 3 |
| tenant_resource_usage | 租户资源使用 | 监控 OceanBase 租户的资源使用情况，包括 CPU、内存、磁盘等 | 3 |

## SQL 模板示例

### 集群状态监控

#### 集群信息查询 (ob_cluster_info)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT * FROM oceanbase.gv$ob_cluster WHERE cluster_id = IFNULL(:cluster_id, cluster_id)

-- OceanBase 4.0.0 以下版本
SELECT * FROM oceanbase.__all_cluster WHERE cluster_id = IFNULL(:cluster_id, cluster_id)
```

#### 租户状态查询 (ob_tenant_status)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT t.tenant_id, t.tenant_name, t.status, t.create_time, t.zone_list, 
       ts.cpu_quota, ts.memory_quota, ts.disk_quota 
FROM oceanbase.gv$tenant t 
JOIN oceanbase.gv$tenant_stat ts ON t.tenant_id = ts.tenant_id 
WHERE t.tenant_id = IFNULL(:tenant_id, t.tenant_id) 
  AND t.tenant_name = IFNULL(:tenant_name, t.tenant_name)
```

### 性能诊断

#### 慢 SQL 查询 (ob_slow_sql)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT tenant_id, tenant_name, sql_id, plan_id, execution_id, user_name, db_name, 
       sql_text, elapsed_time, execute_time, affected_rows, return_rows 
FROM oceanbase.gv$ob_sql_audit 
WHERE tenant_id = IFNULL(:tenant_id, tenant_id) 
  AND execute_time BETWEEN :start_time AND :end_time 
  AND elapsed_time > IFNULL(:threshold_ms, 1000) 
ORDER BY elapsed_time DESC LIMIT 100
```

### 备份恢复监控

#### 备份进度查询 (ob_backup_progress)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT tenant_id, job_id, backup_type, status, start_time, completion_time, 
       total_bytes, finish_bytes, finish_bytes/total_bytes*100 as progress_percent 
FROM oceanbase.gv$backup_progress 
WHERE tenant_id = IFNULL(:tenant_id, tenant_id) 
  AND job_id = IFNULL(:job_id, job_id) 
ORDER BY start_time DESC
```

### 合并监控

#### 主合并状态查询 (ob_major_freeze_status)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT tenant_id, frozen_scn, frozen_time, status 
FROM oceanbase.gv$major_freeze_info 
WHERE tenant_id = IFNULL(:tenant_id, tenant_id) 
ORDER BY frozen_time DESC LIMIT 10
```

### 租户资源使用

#### 租户 CPU 使用查询 (ob_tenant_cpu_usage)

```sql
-- OceanBase 4.0.0 及以上版本
SELECT tenant_id, tenant_name, svr_ip, cpu_quota, cpu_used, 
       cpu_used/cpu_quota*100 as cpu_usage_percent 
FROM oceanbase.gv$tenant_cpu_usage 
WHERE tenant_id = IFNULL(:tenant_id, tenant_id) 
  AND tenant_name = IFNULL(:tenant_name, tenant_name) 
ORDER BY cpu_usage_percent DESC
```

## 如何使用测试数据

### 自动加载测试数据

在开发和测试环境中，测试数据会在应用启动时自动加载。这是通过 `TestDataInitializer` 类实现的，该类使用 Spring 的 `@Profile` 注解确保只在 `dev` 和 `test` 环境中启用。

要启用自动加载，请确保在启动应用时设置正确的 profile：

```bash
java -jar oceanbase-query-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 手动加载测试数据

如果需要手动加载测试数据，可以通过以下方式：

1. 使用 `TestDataInitializer.manualInitTestData()` 方法：

```java
@Autowired
private DataSource dataSource;

// 在需要的地方调用
TestDataInitializer.manualInitTestData(dataSource);
```

2. 直接执行 SQL 脚本：

```bash
mysql -u username -p database_name < test-data.sql
```

## 接口测试示例

### 查询场景列表

```
GET /api/query/v1/scenes
```

### 执行集群状态监控场景

```
POST /api/query/v1/scenes/cluster_status_monitor/execute
{
  "datasource_type": "registered",
  "datasource_id": "ob-cluster-prod",
  "async": false
}
```

### 执行慢 SQL 查询

```
POST /api/query/v1/scenes/performance_diagnosis/execute
{
  "datasource_type": "registered",
  "datasource_id": "ob-cluster-prod",
  "extra_params": {
    "tenant_id": 1001,
    "start_time": "2025-05-01T00:00:00Z",
    "end_time": "2025-05-16T00:00:00Z",
    "threshold_ms": 2000
  },
  "async": false
}
```

## 注意事项

1. 测试数据中的数据源连接信息仅为示例，实际使用时需要替换为有效的连接信息
2. 部分 SQL 查询可能需要特定的权限才能执行
3. 不同版本的 OceanBase 数据库表结构可能有差异，SQL 模板已尽量兼容不同版本
