# 数据库初始化

> `docker-compose.yml` 已经把 `sql/schema.sql` 挂到 MySQL 容器的 `docker-entrypoint-initdb.d/`,
> 首次启动会自动建库建表(创建库 `zhikao_cloud`,然后执行 schema.sql)。

## 首次部署

```bash
# 1. 启动 mysql(只启动这一个容器,执行 schema)
docker compose up -d mysql
# 等 healthcheck 通过

# 2. 启动其它服务
docker compose up -d backend frontend
```

## 增量脚本(按需)

`sql/` 目录里还有一些 ALTER 和种子数据脚本,生产部署**不要**直接挂载(会按字母序全跑),
按需手工执行:

```bash
# 把 SQL 拷进容器执行
docker compose cp sql/add_practice_support.sql mysql:/tmp/
docker compose exec mysql bash -c "mysql -u root -p\${MYSQL_ROOT_PASSWORD} zhikao_cloud < /tmp/add_practice_support.sql"
```

常用脚本作用:
- `schema.sql` — 全表结构 + 基础数据(必跑)
- `test_accounts.sql` — 测试账号(admin/123123,teacher/123123,student/123123),**演示环境用**
- `test_data.sql` — 完整测试数据(题目、试卷、考试记录),**演示环境用**
- `add_*.sql` / `fix-*.sql` — 增量 ALTER,看文件名含义决定是否需要

## 重置数据库

```bash
docker compose down -v           # 删除 mysql-data 卷
docker compose up -d mysql       # 重建,自动跑 schema.sql
```