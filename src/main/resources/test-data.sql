-- 测试数据集：OceanBase 通用查询 API 服务

-- 1. 数据源配置
INSERT INTO datasource_config (datasource_id, db_type, url, username, password, extra_config) VALUES
('ob-cluster-prod', 'OCEANBASE', 'jdbc:oceanbase://obmt6a2ewvdhoj1c.huawei-cn-north-4-internet.oceanbase.cloud:3306/bx', 'bx', 'OceanBase_123#', '{"ssl": false, "connectTimeout": 5000}'),
('ob-cluster-test', 'OCEANBASE', 'jdbc:oceanbase://ob-test.example.com:2883/oceanbase', 'test_user', 'test_pass', '{"ssl": false, "connectTimeout": 3000}'),
('mysql-meta', 'MYSQL', 'jdbc:mysql://mysql.example.com:3306/metadata', 'meta_user', 'meta_pass', '{"ssl": true}');

-- 2. 查询场景
-- 2.1 集群状态监控场景
INSERT INTO query_scene (scene_code, scene_name, description, template_ids) VALUES
('cluster_status_monitor', '集群状态监控', '监控 OceanBase 集群的整体状态，包括租户、服务器、分区等信息', '["ob_cluster_info", "ob_tenant_status", "ob_server_status", "ob_partition_status"]');

-- 2.2 性能诊断场景
INSERT INTO query_scene (scene_code, scene_name, description, template_ids) VALUES
('performance_diagnosis', '性能诊断', '诊断 OceanBase 集群性能问题，包括慢 SQL、内存使用、CPU 使用等', '["ob_slow_sql", "ob_memory_usage", "ob_cpu_usage", "ob_io_stats"]');

-- 2.3 备份恢复监控场景
INSERT INTO query_scene (scene_code, scene_name, description, template_ids) VALUES
('backup_recovery_monitor', '备份恢复监控', '监控 OceanBase 备份恢复任务的状态和进度', '["ob_backup_status", "ob_backup_progress", "ob_recovery_status", "ob_recovery_progress"]');

-- 2.4 合并监控场景
INSERT INTO query_scene (scene_code, scene_name, description, template_ids) VALUES
('merge_monitor', '合并监控', '监控 OceanBase 合并任务的状态和进度', '["ob_major_freeze_status", "ob_merge_progress", "ob_merge_error"]');

-- 2.5 租户资源使用场景
INSERT INTO query_scene (scene_code, scene_name, description, template_ids) VALUES
('tenant_resource_usage', '租户资源使用', '监控 OceanBase 租户的资源使用情况，包括 CPU、内存、磁盘等', '["ob_tenant_cpu_usage", "ob_tenant_memory_usage", "ob_tenant_disk_usage"]');

-- 3. SQL 模板
-- 3.1 集群状态监控模板
-- 3.1.1 集群信息
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_cluster_info', 'cluster_status_monitor', '查询 OceanBase 集群基本信息', 'OCEANBASE', '[{"name": "cluster_id", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_cluster_info', '>=4.0.0', 'SELECT * FROM oceanbase.gv$ob_cluster WHERE cluster_id = IFNULL(:cluster_id, cluster_id)'),
('ob_cluster_info', '<4.0.0', 'SELECT * FROM oceanbase.__all_cluster WHERE cluster_id = IFNULL(:cluster_id, cluster_id)');

-- 3.1.2 租户状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_tenant_status', 'cluster_status_monitor', '查询 OceanBase 租户状态', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "tenant_name", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_tenant_status', '>=4.0.0', 'SELECT t.tenant_id, t.tenant_name, t.status, t.create_time, t.zone_list, ts.cpu_quota, ts.memory_quota, ts.disk_quota FROM oceanbase.gv$tenant t JOIN oceanbase.gv$tenant_stat ts ON t.tenant_id = ts.tenant_id WHERE t.tenant_id = IFNULL(:tenant_id, t.tenant_id) AND t.tenant_name = IFNULL(:tenant_name, t.tenant_name)'),
('ob_tenant_status', '<4.0.0', 'SELECT t.tenant_id, t.tenant_name, t.status, t.create_time, t.zone_list, ts.cpu_quota, ts.memory_quota, ts.disk_quota FROM oceanbase.__all_tenant t JOIN oceanbase.__all_tenant_stat ts ON t.tenant_id = ts.tenant_id WHERE t.tenant_id = IFNULL(:tenant_id, t.tenant_id) AND t.tenant_name = IFNULL(:tenant_name, t.tenant_name)');

-- 3.1.3 服务器状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_server_status', 'cluster_status_monitor', '查询 OceanBase 服务器状态', 'OCEANBASE', '[{"name": "zone", "type": "string", "required": false}, {"name": "svr_ip", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_server_status', '>=4.0.0', 'SELECT svr_ip, svr_port, zone, status, start_service_time, stop_time, build_version FROM oceanbase.gv$observer WHERE zone = IFNULL(:zone, zone) AND svr_ip = IFNULL(:svr_ip, svr_ip)'),
('ob_server_status', '<4.0.0', 'SELECT svr_ip, svr_port, zone, status, start_service_time, stop_time, build_version FROM oceanbase.__all_server WHERE zone = IFNULL(:zone, zone) AND svr_ip = IFNULL(:svr_ip, svr_ip)');

-- 3.1.4 分区状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_partition_status', 'cluster_status_monitor', '查询 OceanBase 分区状态', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "table_id", "type": "integer", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_partition_status', '>=4.0.0', 'SELECT tenant_id, table_id, partition_id, svr_ip, svr_port, role, replica_type, status FROM oceanbase.gv$partition_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND table_id = IFNULL(:table_id, table_id)'),
('ob_partition_status', '<4.0.0', 'SELECT tenant_id, table_id, partition_id, svr_ip, svr_port, role, replica_type, status FROM oceanbase.__all_partition WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND table_id = IFNULL(:table_id, table_id)');

-- 3.2 性能诊断模板
-- 3.2.1 慢 SQL
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_slow_sql', 'performance_diagnosis', '查询 OceanBase 慢 SQL', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "start_time", "type": "datetime", "required": true}, {"name": "end_time", "type": "datetime", "required": true}, {"name": "threshold_ms", "type": "integer", "required": false, "default": 1000}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_slow_sql', '>=4.0.0', 'SELECT tenant_id, tenant_name, sql_id, plan_id, execution_id, user_name, db_name, sql_text, elapsed_time, execute_time, affected_rows, return_rows FROM oceanbase.gv$ob_sql_audit WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND execute_time BETWEEN :start_time AND :end_time AND elapsed_time > IFNULL(:threshold_ms, 1000) ORDER BY elapsed_time DESC LIMIT 100'),
('ob_slow_sql', '<4.0.0', 'SELECT tenant_id, tenant_name, sql_id, plan_id, execution_id, user_name, db_name, sql_text, elapsed_time, execute_time, affected_rows, return_rows FROM oceanbase.__all_sql_audit WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND execute_time BETWEEN :start_time AND :end_time AND elapsed_time > IFNULL(:threshold_ms, 1000) ORDER BY elapsed_time DESC LIMIT 100');

-- 3.2.2 内存使用
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_memory_usage', 'performance_diagnosis', '查询 OceanBase 内存使用情况', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "svr_ip", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_memory_usage', '>=4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, context, sum(used) as used, sum(total) as total FROM oceanbase.gv$memory_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip) GROUP BY svr_ip, tenant_id, tenant_name, context ORDER BY used DESC'),
('ob_memory_usage', '<4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, context, sum(used) as used, sum(total) as total FROM oceanbase.__all_memory_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip) GROUP BY svr_ip, tenant_id, tenant_name, context ORDER BY used DESC');

-- 3.2.3 CPU 使用
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_cpu_usage', 'performance_diagnosis', '查询 OceanBase CPU 使用情况', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "svr_ip", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_cpu_usage', '>=4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, cpu_quota, cpu_used, cpu_used/cpu_quota*100 as cpu_usage_percent FROM oceanbase.gv$tenant_cpu_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip) ORDER BY cpu_usage_percent DESC'),
('ob_cpu_usage', '<4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, cpu_quota, cpu_used, cpu_used/cpu_quota*100 as cpu_usage_percent FROM oceanbase.__all_tenant_cpu_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip) ORDER BY cpu_usage_percent DESC');

-- 3.2.4 IO 统计
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_io_stats', 'performance_diagnosis', '查询 OceanBase IO 统计信息', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "svr_ip", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_io_stats', '>=4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, file_type, read_count, read_bytes, write_count, write_bytes FROM oceanbase.gv$io_stat WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip)'),
('ob_io_stats', '<4.0.0', 'SELECT svr_ip, tenant_id, tenant_name, file_type, read_count, read_bytes, write_count, write_bytes FROM oceanbase.__all_io_stat WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND svr_ip = IFNULL(:svr_ip, svr_ip)');

-- 3.3 备份恢复监控模板
-- 3.3.1 备份状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_backup_status', 'backup_recovery_monitor', '查询 OceanBase 备份任务状态', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "start_time", "type": "datetime", "required": false}, {"name": "end_time", "type": "datetime", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_backup_status', '>=4.0.0', 'SELECT tenant_id, backup_type, status, start_time, completion_time, backup_dest FROM oceanbase.gv$backup_job WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (start_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY start_time DESC'),
('ob_backup_status', '<4.0.0', 'SELECT tenant_id, backup_type, status, start_time, completion_time, backup_dest FROM oceanbase.__all_backup_job WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (start_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY start_time DESC');

-- 3.3.2 备份进度
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_backup_progress', 'backup_recovery_monitor', '查询 OceanBase 备份任务进度', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "job_id", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_backup_progress', '>=4.0.0', 'SELECT tenant_id, job_id, backup_type, status, start_time, completion_time, total_bytes, finish_bytes, finish_bytes/total_bytes*100 as progress_percent FROM oceanbase.gv$backup_progress WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND job_id = IFNULL(:job_id, job_id) ORDER BY start_time DESC'),
('ob_backup_progress', '<4.0.0', 'SELECT tenant_id, job_id, backup_type, status, start_time, completion_time, total_bytes, finish_bytes, finish_bytes/total_bytes*100 as progress_percent FROM oceanbase.__all_backup_progress WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND job_id = IFNULL(:job_id, job_id) ORDER BY start_time DESC');

-- 3.3.3 恢复状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_recovery_status', 'backup_recovery_monitor', '查询 OceanBase 恢复任务状态', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "start_time", "type": "datetime", "required": false}, {"name": "end_time", "type": "datetime", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_recovery_status', '>=4.0.0', 'SELECT tenant_id, recovery_type, status, start_time, completion_time, backup_dest FROM oceanbase.gv$recovery_job WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (start_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY start_time DESC'),
('ob_recovery_status', '<4.0.0', 'SELECT tenant_id, recovery_type, status, start_time, completion_time, backup_dest FROM oceanbase.__all_recovery_job WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (start_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY start_time DESC');

-- 3.3.4 恢复进度
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_recovery_progress', 'backup_recovery_monitor', '查询 OceanBase 恢复任务进度', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "job_id", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_recovery_progress', '>=4.0.0', 'SELECT tenant_id, job_id, recovery_type, status, start_time, completion_time, total_bytes, finish_bytes, finish_bytes/total_bytes*100 as progress_percent FROM oceanbase.gv$recovery_progress WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND job_id = IFNULL(:job_id, job_id) ORDER BY start_time DESC'),
('ob_recovery_progress', '<4.0.0', 'SELECT tenant_id, job_id, recovery_type, status, start_time, completion_time, total_bytes, finish_bytes, finish_bytes/total_bytes*100 as progress_percent FROM oceanbase.__all_recovery_progress WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND job_id = IFNULL(:job_id, job_id) ORDER BY start_time DESC');

-- 3.4 合并监控模板
-- 3.4.1 主合并状态
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_major_freeze_status', 'merge_monitor', '查询 OceanBase 主合并状态', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_major_freeze_status', '>=4.0.0', 'SELECT tenant_id, frozen_scn, frozen_time, status FROM oceanbase.gv$major_freeze_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) ORDER BY frozen_time DESC LIMIT 10'),
('ob_major_freeze_status', '<4.0.0', 'SELECT tenant_id, frozen_scn, frozen_time, status FROM oceanbase.__all_major_freeze_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) ORDER BY frozen_time DESC LIMIT 10');

-- 3.4.2 合并进度
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_merge_progress', 'merge_monitor', '查询 OceanBase 合并进度', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_merge_progress', '>=4.0.0', 'SELECT tenant_id, frozen_scn, frozen_time, global_broadcast_scn, is_merging, merge_start_time, merge_finish_time, is_error, error_type FROM oceanbase.gv$merge_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) ORDER BY frozen_time DESC LIMIT 10'),
('ob_merge_progress', '<4.0.0', 'SELECT tenant_id, frozen_scn, frozen_time, global_broadcast_scn, is_merging, merge_start_time, merge_finish_time, is_error, error_type FROM oceanbase.__all_merge_info WHERE tenant_id = IFNULL(:tenant_id, tenant_id) ORDER BY frozen_time DESC LIMIT 10');

-- 3.4.3 合并错误
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_merge_error', 'merge_monitor', '查询 OceanBase 合并错误', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "start_time", "type": "datetime", "required": false}, {"name": "end_time", "type": "datetime", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_merge_error', '>=4.0.0', 'SELECT tenant_id, frozen_scn, error_type, error_msg, error_time FROM oceanbase.gv$merge_error WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (error_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY error_time DESC'),
('ob_merge_error', '<4.0.0', 'SELECT tenant_id, frozen_scn, error_type, error_msg, error_time FROM oceanbase.__all_merge_error WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND (error_time BETWEEN IFNULL(:start_time, DATE_SUB(NOW(), INTERVAL 7 DAY)) AND IFNULL(:end_time, NOW()) OR :start_time IS NULL) ORDER BY error_time DESC');

-- 3.5 租户资源使用模板
-- 3.5.1 租户 CPU 使用
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_tenant_cpu_usage', 'tenant_resource_usage', '查询 OceanBase 租户 CPU 使用情况', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "tenant_name", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_tenant_cpu_usage', '>=4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, cpu_quota, cpu_used, cpu_used/cpu_quota*100 as cpu_usage_percent FROM oceanbase.gv$tenant_cpu_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY cpu_usage_percent DESC'),
('ob_tenant_cpu_usage', '<4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, cpu_quota, cpu_used, cpu_used/cpu_quota*100 as cpu_usage_percent FROM oceanbase.__all_tenant_cpu_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY cpu_usage_percent DESC');

-- 3.5.2 租户内存使用
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_tenant_memory_usage', 'tenant_resource_usage', '查询 OceanBase 租户内存使用情况', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "tenant_name", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_tenant_memory_usage', '>=4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, memory_quota, memory_used, memory_used/memory_quota*100 as memory_usage_percent FROM oceanbase.gv$tenant_memory_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY memory_usage_percent DESC'),
('ob_tenant_memory_usage', '<4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, memory_quota, memory_used, memory_used/memory_quota*100 as memory_usage_percent FROM oceanbase.__all_tenant_memory_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY memory_usage_percent DESC');

-- 3.5.3 租户磁盘使用
INSERT INTO sql_template (template_id, scene_code, description, db_type, parameters) VALUES
('ob_tenant_disk_usage', 'tenant_resource_usage', '查询 OceanBase 租户磁盘使用情况', 'OCEANBASE', '[{"name": "tenant_id", "type": "integer", "required": false}, {"name": "tenant_name", "type": "string", "required": false}]');

INSERT INTO sql_template_version (template_id, version_condition, sql_text) VALUES
('ob_tenant_disk_usage', '>=4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, disk_quota, disk_used, disk_used/disk_quota*100 as disk_usage_percent FROM oceanbase.gv$tenant_disk_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY disk_usage_percent DESC'),
('ob_tenant_disk_usage', '<4.0.0', 'SELECT tenant_id, tenant_name, svr_ip, disk_quota, disk_used, disk_used/disk_quota*100 as disk_usage_percent FROM oceanbase.__all_tenant_disk_usage WHERE tenant_id = IFNULL(:tenant_id, tenant_id) AND tenant_name = IFNULL(:tenant_name, tenant_name) ORDER BY disk_usage_percent DESC');
