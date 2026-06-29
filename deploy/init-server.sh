#!/usr/bin/env bash
# 在新买的云服务器(Ubuntu 22.04 / Debian 12)上首次执行
# 作用:装 Docker + 拉项目 + 启动 + 跑 Cloudflare Tunnel
# 用法:
#   scp -r zhikao-cloud root@<your-server>:/opt/
#   ssh root@<your-server>
#   cd /opt/zhikao-cloud && bash deploy/init-server.sh

set -e

echo "==> 1. 安装 Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi
if ! docker compose version >/dev/null 2>&1; then
  apt-get install -y docker-compose-plugin
fi

echo "==> 2. 准备 .env"
if [ ! -f .env ]; then
  cp .env.example .env
  # 生成随机 JWT 密钥
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -hex 32)|" .env
  # 生成随机 MySQL 密码
  MYSQL_PWD=$(openssl rand -hex 12)
  sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$MYSQL_PWD|" .env
  sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$MYSQL_PWD|" .env
  echo "  → 已生成 .env(密码已随机化)"
fi

echo "==> 3. 检查 db 初始化脚本"
ls backend/src/main/resources/db/ 2>/dev/null | head || echo "  ⚠️  没找到 db/ 目录,确认 schema 在哪里"

echo "==> 4. 构建并启动"
docker compose pull
docker compose up -d --build

echo "==> 5. 等 backend 就绪"
for i in $(seq 1 30); do
  if docker compose exec -T backend wget -qO- http://127.0.0.1:8080/api/v1/auth/login >/dev/null 2>&1; then
    echo "  ✓ backend 就绪"
    break
  fi
  sleep 3
done

echo "==> 6. 本机自检"
curl -sI http://localhost/ | head -3
curl -s http://localhost/api/v1/auth/login -X POST -H "Content-Type: application/json" -d '{"account":"admin","password":"123123"}' | head -c 200
echo

echo
echo "✅ 完成!现在可以:"
echo "  - 浏览器访问 http://<server-ip>/  (本机)"
echo "  - 按 deploy/CLOUDFLARE-TUNNEL.md 配置公网域名"