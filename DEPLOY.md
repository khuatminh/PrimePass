# Hướng dẫn Deploy lên VPS Ubuntu với Docker + HTTPS + CI/CD

**VPS IP:** `103.228.36.244`  
**Domain:** `acc.khuatminh.com`  
**Thư mục deploy:** `/opt/marketplace`  
**Stack:** Spring Boot 3 · MySQL 8 · Nginx · Docker Compose · Let's Encrypt · GitHub Actions

---

## Mục lục

1. [Kiến trúc tổng quan](#1-kiến-trúc-tổng-quan)
2. [Chuẩn bị VPS](#2-chuẩn-bị-vps)
3. [Trỏ DNS về VPS](#3-trỏ-dns-về-vps)
4. [Cấu hình biến môi trường (.env)](#4-cấu-hình-biến-môi-trường-env)
5. [Cấp SSL lần đầu (Let's Encrypt)](#5-cấp-ssl-lần-đầu-lets-encrypt)
6. [Cấu hình CI/CD với GitHub Actions](#6-cấu-hình-cicd-với-github-actions)
7. [Deploy lần đầu (thủ công)](#7-deploy-lần-đầu-thủ-công)
8. [Kiểm tra & bảo trì](#8-kiểm-tra--bảo-trì)
9. [Checklist tổng kết](#9-checklist-tổng-kết)

---

## 1. Kiến trúc tổng quan

```
Internet
   │  80/443
   ▼
[Nginx :80/:443]  ──proxy──►  [Spring Boot :8386]
                                      │
                               [MySQL :3306]

Docker Compose — 3 services:
  nginx    →  public  (port 80, 443)
  app      →  internal only (port 8386)
  mysql    →  internal only (port 3306)
```

---

## 2. Chuẩn bị VPS

### 2.1 Kết nối VPS

```bash
ssh root@103.228.36.244
```

### 2.2 Cài Docker & Docker Compose

```bash
# Cập nhật hệ thống
apt update && apt upgrade -y

# Cài Docker (bước này mất 2–5 phút, KHÔNG có output, cứ chờ)
curl -fsSL https://get.docker.com | sh

# Kiểm tra
docker --version
docker compose version
```

### 2.3 Cài Git

```bash
apt install git -y
```

### 2.4 Clone repo vào VPS

> **Quan trọng:** Phải có dấu `.` ở cuối lệnh `git clone` để clone thẳng vào thư mục hiện tại, không tạo thư mục con.

```bash
mkdir -p /opt/marketplace
cd /opt/marketplace
git clone https://github.com/khuatminh/PrimePass.git .
```

Kiểm tra file đã có:

```bash
ls -la /opt/marketplace
# Phải thấy: Dockerfile  docker-compose.yml  .env.example  nginx/  ...
```

### 2.5 Tạo SSH Deploy Key (dùng cho CI/CD)

```bash
# Tạo key pair
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key -N ""

# Đăng ký public key để GitHub Actions được phép SSH vào
cat ~/.ssh/deploy_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# In private key — copy toàn bộ nội dung này vào GitHub Secret VPS_SSH_KEY
cat ~/.ssh/deploy_key
```

---

## 3. Trỏ DNS về VPS

Vào DNS manager của domain `khuatminh.com`, thêm bản ghi A:

| Type | Name | Value            | TTL  |
|------|------|------------------|------|
| A    | acc  | `103.228.36.244` | 3600 |

Kiểm tra DNS đã lan truyền (chờ 5–30 phút):

```bash
nslookup acc.khuatminh.com
# Kết quả mong đợi: Address: 103.228.36.244
```

---

## 4. Cấu hình biến môi trường (.env)

File `.env` chứa thông tin nhạy cảm — **KHÔNG commit lên git**, chỉ tạo trực tiếp trên VPS.

### 4.1 Tạo file .env từ template

```bash
cd /opt/marketplace
cp .env.example .env
```

### 4.2 Mở file để chỉnh sửa

```bash
nano .env
```

### 4.3 Nội dung file .env — điền đầy đủ

```dotenv
# ─── Database ────────────────────────────────────────────────────────
# Tên database MySQL (giữ nguyên nếu không cần đổi)
DB_NAME=digital_marketplace

# User MySQL (mặc định là root)
DB_USER=root

# Mật khẩu MySQL — ĐẶT MẬT KHẨU MẠNH, ít nhất 16 ký tự
# Ví dụ: Mk@2024#Secure!99
DB_PASS=THAY_BANG_MAT_KHAU_MANH

# ─── VNPay ───────────────────────────────────────────────────────────
# Lấy từ https://sandbox.vnpayment.vn sau khi đăng ký merchant
VNPAY_TMN_CODE=DEMO0001
VNPAY_HASH_SECRET=DEMOHASHDEMOHASHDEMOHASHDEMO1234

# Môi trường sandbox (giữ nguyên khi test, đổi khi go-live)
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

# URL callback sau khi thanh toán — PHẢI dùng domain thật
VNPAY_RETURN_URL=https://acc.khuatminh.com/payment/callback
```

**Lưu file:** `Ctrl+O` → `Enter` → `Ctrl+X`

### 4.4 Phân quyền file .env

```bash
# Chỉ root mới đọc được file này
chmod 600 /opt/marketplace/.env
```

### 4.5 Kiểm tra nội dung đã đúng

```bash
cat /opt/marketplace/.env
```

---

## 5. Cấp SSL lần đầu (Let's Encrypt)

> **Yêu cầu:** DNS `acc.khuatminh.com` phải trỏ về `103.228.36.244` trước bước này.

### Bước 1: Tạo thư mục cho Certbot

```bash
cd /opt/marketplace
mkdir -p certbot/conf certbot/www
```

### Bước 2: Tạo cấu hình Nginx tạm (chỉ HTTP)

```bash
cat > /opt/marketplace/nginx/conf.d/default.conf << 'EOF'
server {
    listen 80;
    server_name acc.khuatminh.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 "OK";
        add_header Content-Type text/plain;
    }
}
EOF
```

### Bước 3: Khởi động Nginx (chỉ mình nginx, chưa cần app + mysql)

```bash
cd /opt/marketplace
docker compose up -d nginx
```

Kiểm tra Nginx đang chạy:

```bash
docker compose ps
curl http://acc.khuatminh.com
# Kết quả mong đợi: OK
```

### Bước 4: Chạy Certbot cấp chứng chỉ SSL

```bash
docker run --rm \
  -v /opt/marketplace/certbot/conf:/etc/letsencrypt \
  -v /opt/marketplace/certbot/www:/var/www/certbot \
  certbot/certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email sonh9594@gmail.com \
  --agree-tos \
  --no-eff-email \
  -d acc.khuatminh.com
```

Kết quả thành công sẽ có dòng:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/acc.khuatminh.com/fullchain.pem
```

### Bước 5: Khôi phục cấu hình Nginx đầy đủ (HTTPS)

```bash
cat > /opt/marketplace/nginx/conf.d/default.conf << 'EOF'
server {
    listen 80;
    server_name acc.khuatminh.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name acc.khuatminh.com;

    ssl_certificate     /etc/letsencrypt/live/acc.khuatminh.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/acc.khuatminh.com/privkey.pem;
    include             /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam         /etc/letsencrypt/ssl-dhparams.pem;

    client_max_body_size 50M;

    location / {
        proxy_pass         http://app:8386;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        proxy_pass         http://app:8386;
        proxy_set_header   Host $host;
        expires            7d;
        add_header         Cache-Control "public, no-transform";
    }
}
EOF
```

### Bước 6: Tự động gia hạn SSL (cronjob)

```bash
crontab -e
```

Thêm dòng sau vào cuối file (gia hạn lúc 3h sáng mỗi 2 tháng):

```
0 3 1 */2 * docker run --rm -v /opt/marketplace/certbot/conf:/etc/letsencrypt -v /opt/marketplace/certbot/www:/var/www/certbot certbot/certbot renew --quiet && docker compose -f /opt/marketplace/docker-compose.yml exec nginx nginx -s reload
```

---

## 6. Cấu hình CI/CD với GitHub Actions

### 6.1 Thêm GitHub Secrets

Vào repo **https://github.com/khuatminh/PrimePass** → **Settings → Secrets and variables → Actions → New repository secret**

Thêm lần lượt 4 secrets:

| Secret name   | Giá trị cần điền                                              |
|---------------|---------------------------------------------------------------|
| `VPS_HOST`    | `103.228.36.244`                                              |
| `VPS_USER`    | `root`                                                        |
| `VPS_SSH_KEY` | Toàn bộ nội dung file `/root/.ssh/deploy_key` (private key)  |
| `DEPLOY_PATH` | `/opt/marketplace`                                            |

**Cách lấy nội dung VPS_SSH_KEY:**

```bash
# Chạy trên VPS, copy toàn bộ output (kể cả dòng -----BEGIN và -----END)
cat /root/.ssh/deploy_key
```

Output mẫu cần copy:
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAA...
(nhiều dòng)
...AAAAA==
-----END OPENSSH PRIVATE KEY-----
```

### 6.2 Kiểm tra CI/CD hoạt động

Sau khi thêm secrets, push 1 commit bất kỳ lên `main`:

```bash
# Trên máy local
git commit --allow-empty -m "test: trigger CI/CD"
git push origin main
```

Vào **https://github.com/khuatminh/PrimePass/actions** để xem pipeline chạy.

---

## 7. Deploy lần đầu (thủ công)

Sau khi đã có SSL certificate, chạy toàn bộ stack:

```bash
cd /opt/marketplace

# Khởi động tất cả services (lần đầu build mất 5–10 phút)
docker compose up -d --build

# Theo dõi log app đang khởi động
docker compose logs -f app
```

Chờ xuất hiện dòng:
```
Started MarketplaceApplication in X.XXX seconds
```

Truy cập: **https://acc.khuatminh.com**

---

## 8. Kiểm tra & bảo trì

### Xem trạng thái containers

```bash
docker compose ps
```

### Xem logs

```bash
# Theo dõi tất cả
docker compose logs -f

# Chỉ app
docker compose logs -f app

# Chỉ nginx
docker compose logs -f nginx

# Xem 100 dòng cuối
docker compose logs --tail=100 app
```

### Restart service

```bash
docker compose restart app
docker compose restart nginx
docker compose restart mysql
```

### Backup database

```bash
# Backup (thay YOUR_PASSWORD bằng giá trị DB_PASS trong .env)
docker compose exec mysql mysqldump -uroot -pYOUR_PASSWORD digital_marketplace > backup_$(date +%Y%m%d_%H%M).sql

# Hoặc đọc từ .env tự động
source /opt/marketplace/.env
docker compose exec mysql mysqldump -uroot -p${DB_PASS} digital_marketplace > backup_$(date +%Y%m%d_%H%M).sql
```

### Restore database

```bash
source /opt/marketplace/.env
docker compose exec -T mysql mysql -uroot -p${DB_PASS} digital_marketplace < backup_20260507_1200.sql
```

### Cập nhật app thủ công (không qua CI/CD)

```bash
cd /opt/marketplace
git pull origin main
docker compose up -d --build
docker image prune -f
```

### Kiểm tra & gia hạn SSL thủ công

```bash
# Xem thông tin certificate
docker run --rm \
  -v /opt/marketplace/certbot/conf:/etc/letsencrypt \
  certbot/certbot certificates

# Gia hạn thủ công
docker run --rm \
  -v /opt/marketplace/certbot/conf:/etc/letsencrypt \
  -v /opt/marketplace/certbot/www:/var/www/certbot \
  certbot/certbot renew --quiet

docker compose exec nginx nginx -s reload
```

---

## 9. Checklist tổng kết

### Phần 1 — Chuẩn bị
- [ ] SSH vào VPS: `ssh root@103.228.36.244`
- [ ] Cài Docker: `curl -fsSL https://get.docker.com | sh`
- [ ] Cài Git: `apt install git -y`
- [ ] Clone repo: `git clone https://github.com/khuatminh/PrimePass.git .` (có dấu `.`)
- [ ] Tạo SSH deploy key và thêm vào `authorized_keys`

### Phần 2 — Cấu hình
- [ ] Trỏ DNS A record `acc` → `103.228.36.244`
- [ ] Tạo file `.env` từ `.env.example` và điền đầy đủ
- [ ] Đặt `chmod 600 .env`

### Phần 3 — SSL
- [ ] Tạo thư mục `certbot/conf` và `certbot/www`
- [ ] Chạy Nginx tạm (HTTP only)
- [ ] Chạy Certbot cấp SSL thành công
- [ ] Khôi phục config Nginx đầy đủ (HTTPS)
- [ ] Thêm cronjob tự động gia hạn

### Phần 4 — CI/CD
- [ ] Thêm `VPS_HOST` = `103.228.36.244` vào GitHub Secrets
- [ ] Thêm `VPS_USER` = `root` vào GitHub Secrets
- [ ] Thêm `VPS_SSH_KEY` (nội dung `/root/.ssh/deploy_key`) vào GitHub Secrets
- [ ] Thêm `DEPLOY_PATH` = `/opt/marketplace` vào GitHub Secrets

### Phần 5 — Go live
- [ ] Chạy `docker compose up -d --build` lần đầu
- [ ] Chờ log `Started MarketplaceApplication`
- [ ] Truy cập `https://acc.khuatminh.com` thành công
- [ ] Push 1 commit test để xác nhận CI/CD hoạt động
