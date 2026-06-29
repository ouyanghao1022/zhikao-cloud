# Cloudflare Tunnel 公网暴露步骤

> 零成本方案:服务器不暴露任何公网端口,Cloudflare Edge 通过加密隧道主动连到你的机器。
> 自动 HTTPS、免备案、免证书。每月免费额度 100 万次请求,学习/演示完全够用。

## 0. 前置条件
- 已在 Cloudflare 注册并把你的域名的 NS 改到 Cloudflare(`yourdomain.com` 在 dashboard 里是 Active 状态)
- 已有可访问域名的 Cloudflare 账号
- 服务器上 `docker compose up -d` 跑起来了,本机 `curl http://localhost` 能看到登录页

## 1. 安装 cloudflared(在云服务器上)

```bash
# Debian/Ubuntu
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt update && sudo apt install -y cloudflared

# 或直接下载二进制
curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o /usr/local/bin/cloudflared
chmod +x /usr/local/bin/cloudflared
```

## 2. 登录并创建 Tunnel

```bash
cloudflared tunnel login
# 浏览器会跳转到 Cloudflare 授权页,选你的域名授权

cloudflared tunnel create zhikao
# 输出会告诉你 TUNNEL_ID 和 credentials 文件路径
```

## 3. 写配置

```bash
mkdir -p ~/.cloudflared
cp /path/to/zhikao-cloud/deploy/cloudflared-config.yml.example ~/.cloudflared/config.yml
```

编辑 `~/.cloudflared/config.yml`,把 `<TUNNEL_ID>` 替换成第 2 步得到的 ID,把 `yourdomain.com` 替换成你的域名。

## 4. DNS 指向 Tunnel

```bash
cloudflared tunnel route dns zhikao yourdomain.com
cloudflared tunnel route dns zhikao www.yourdomain.com
```

这一步会在 Cloudflare DNS 里加两条 CNAME 记录,指向你的 tunnel。

## 5. 后台运行

```bash
# 装成 systemd 服务(推荐)
sudo cloudflared service install
sudo systemctl enable cloudflared
sudo systemctl start cloudflared
sudo systemctl status cloudflared

# 或前台调试
cloudflared tunnel run zhikao
```

## 6. 验证

打开浏览器访问 `https://yourdomain.com`,应该看到登录页。
- SSL 证书:Cloudflare 自动签发,小绿锁会自动出现。
- 任何时候改 `cloudflared-config.yml` 后,`systemctl restart cloudflared`。

## 故障排查

| 现象 | 排查 |
|---|---|
| 502 Bad Gateway | 云服务器本机 `curl http://localhost` 不通 → docker compose 没起来 |
| 404 Not Found | DNS 没生效,等 1-2 分钟;或 `cloudflared tunnel route dns` 没执行 |
| SSL 报错 | Cloudflare dashboard → SSL/TLS → 选 "Full" 模式 |
| tunnel 状态 Down | `cloudflared tunnel info zhikao` 看 health 字段 |

## 免费额度

- 100 万次请求/月
- 100 GB 出口流量/月
- 个人学习/演示完全够用,超出会停服不收费(可以升级到 $5/月 Unlimited)