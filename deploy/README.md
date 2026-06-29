# 部署到云端

零成本方案:Cloudflare Tunnel + Docker Compose。

## 文件清单

```
.
├── docker-compose.yml              一键编排 MySQL + backend + frontend
├── .env.example                    环境变量模板(拷贝为 .env 后填值)
├── backend/
│   ├── Dockerfile                  Spring Boot 镜像(Maven 多阶段构建)
│   └── src/main/resources/
│       └── application-prod.yml    生产配置(SPRING_PROFILES_ACTIVE=prod 时生效)
├── frontend/
│   ├── Dockerfile                  Vue 静态构建 → nginx
│   └── nginx.conf                  反向代理 + SPA 路由兜底
└── deploy/
    ├── init-server.sh              服务器首次初始化脚本
    ├── cloudflared-config.yml.example   Cloudflare Tunnel 配置模板
    ├── CLOUDFLARE-TUNNEL.md        公网暴露步骤
    └── SQL.md                      数据库初始化说明
```

## 一键部署流程

### 1. 在云服务器(Ubuntu 22.04/Debian 12,2核2G 起步)上

```bash
# 装 git + docker(全新机器)
sudo apt update && sudo apt install -y git
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker

# 拉代码(或 scp 上传)
git clone <your-repo-url> zhikao-cloud
cd zhikao-cloud

# 准备环境变量
cp .env.example .env
# 编辑 .env:把 MYSQL_PASSWORD / JWT_SECRET 都改成强随机
openssl rand -hex 32  # 生成 JWT_SECRET
openssl rand -hex 12  # 生成 MYSQL_PASSWORD

# 一键构建并启动
docker compose up -d --build
```

### 2. 验证本地访问

```bash
curl http://localhost/                                  # 看到登录页 HTML
curl http://localhost/api/v1/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"123123"}'          # 返回 JWT
```

### 3. 暴露到公网

按 [deploy/CLOUDFLARE-TUNNEL.md](CLOUDFLARE-TUNNEL.md) 操作,10 分钟搞定 HTTPS + 公网域名。

## 日常维护

```bash
docker compose ps                 # 看运行状态
docker compose logs -f backend    # 看后端日志
docker compose restart backend    # 重启单个服务
docker compose pull && docker compose up -d   # 拉新镜像并重启
docker compose down               # 全部停掉(数据卷保留)
docker compose down -v            # 全部停掉并删数据卷(慎用)
```

## 故障排查

| 现象 | 排查 |
|---|---|
| mysql 容器一直 restarting | 密码含特殊字符引号问题,改用纯字母数字 |
| backend 启动报 datasource 连不上 | `docker compose logs mysql` 看密码一致否 |
| 后端 404 on `/api/v1/xxx` | `context-path` 已加 `/api/v1`,前端 baseURL 必须配 `/api/v1` |
| 文件上传 413 | nginx.conf `client_max_body_size` 已设 50M,改大后 `docker compose restart frontend` |
| Cloudflare 502 | 服务器 `curl http://localhost` 不通,docker 没起来 |

## 升级到生产级

学习/演示用这套够用。如果要做商用:
- 加 HTTPS 直接访问(不用 Cloudflare):用 Caddy 反向代理,自动 Let's Encrypt 证书
- 加 CDN:Cloudflare 已经做了
- 加备份:`crontab -e` 加一条 `docker compose exec mysql mysqldump ... > backup.sql`
- 加监控:UptimeRobot(免费)轮询 `/api/v1/auth/login`