# Hướng dẫn Deploy lên VPS Ubuntu với Docker + HTTPS + CI/CD

**VPS public IPv4:** `<VPS_HOST>`

**Domain:** `<APP_DOMAIN>`

**Tài khoản deploy:** `<DEPLOY_USER>`

**Thư mục deploy:** `<DEPLOY_PATH>`

**GitHub repository:** `<GITHUB_REPOSITORY>`

**Email Let's Encrypt:** `<LETSENCRYPT_EMAIL>`

**Stack:** Spring Boot 3 · MySQL 8 · Nginx · Docker Compose · Let's Encrypt · GitHub Actions

Trước khi chạy lệnh, thay các giá trị mẫu bên dưới và chạy khối này ở đầu **mỗi phiên terminal** (máy local hoặc VPS). Địa chỉ `203.0.113.10` thuộc TEST-NET-3, chỉ dùng cho tài liệu và phải được thay bằng public IPv4 thật của VPS:

```bash
export VPS_HOST="203.0.113.10"
export APP_DOMAIN="app.your-domain.example"
export DEPLOY_USER="your-deploy-user"
export DEPLOY_PATH="/opt/your-app"
export GITHUB_REPOSITORY="your-owner/your-repository"
export LETSENCRYPT_EMAIL="admin@your-domain.example"
```

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

> **Điều kiện:** VPS phải có public IPv4 `<VPS_HOST>`; tài khoản `<DEPLOY_USER>` phải tồn tại, đăng nhập được qua SSH và có quyền `sudo`.

### 2.1 Kết nối VPS

```bash
ssh "${DEPLOY_USER}@${VPS_HOST}"
```

Trong phiên SSH vừa mở, chạy lại khối khai báo biến ở đầu tài liệu trước khi tiếp tục.

### 2.2 Cài Docker & Docker Compose

```bash
# Cập nhật hệ thống
sudo apt update && sudo apt upgrade -y

# Cài Docker (bước này mất 2–5 phút, KHÔNG có output, cứ chờ)
curl -fsSL https://get.docker.com | sudo sh

# Cho phép tài khoản deploy chạy Docker không cần sudo
sudo usermod -aG docker "${USER}"
```

Đăng xuất rồi SSH lại để quyền của nhóm `docker` có hiệu lực:

```bash
exit
ssh "${DEPLOY_USER}@${VPS_HOST}"
```

Chạy lại khối khai báo biến ở đầu tài liệu, sau đó kiểm tra Docker:

```bash
# Kiểm tra Docker sau khi đăng nhập lại
docker --version
docker compose version
```

> **Cảnh báo bảo mật:** Thành viên nhóm `docker` có thể kiểm soát Docker daemon và trên thực tế có quyền tương đương `root`. Hướng dẫn này chạy deployment qua Docker nên `<DEPLOY_USER>` vẫn có quyền root hiệu dụng. Hãy dùng tài khoản deploy và SSH key riêng chỉ cho CI/CD, cấp quyền repository/environment tối thiểu, cấu hình protection rules cho GitHub Environment ở nơi gói GitHub và cài đặt repository hỗ trợ, đồng thời xoay vòng hoặc thu hồi deploy key định kỳ và ngay khi nghi ngờ bị lộ.

### 2.3 Cài Git

```bash
sudo apt install git -y
```

### 2.4 Clone repo vào VPS

> **Quan trọng:** Phải có dấu `.` ở cuối lệnh `git clone` để clone thẳng vào thư mục hiện tại, không tạo thư mục con.

```bash
sudo mkdir -p "${DEPLOY_PATH}"
sudo chown -R "${USER}:$(id -gn)" "${DEPLOY_PATH}"
cd "${DEPLOY_PATH}"
git clone "https://github.com/${GITHUB_REPOSITORY}.git" .
```

Kiểm tra file đã có:

```bash
ls -la "${DEPLOY_PATH}"
# Phải thấy: Dockerfile  docker-compose.yml  .env.example  nginx/  ...
```

### 2.5 Tạo SSH Deploy Key (dùng cho CI/CD)

```bash
# Tạo key pair
mkdir -p "${HOME}/.ssh"
chmod 700 "${HOME}/.ssh"
ssh-keygen -t ed25519 -C "github-actions-deploy" -f "${HOME}/.ssh/deploy_key" -N ""

# Đăng ký public key để GitHub Actions được phép SSH vào
cat "${HOME}/.ssh/deploy_key.pub" >> "${HOME}/.ssh/authorized_keys"
chmod 600 "${HOME}/.ssh/authorized_keys"

# In private key — copy nội dung này vào environment secret VPS_SSH_KEY của production
cat "${HOME}/.ssh/deploy_key"
```

---

## 3. Trỏ DNS về VPS

Trong DNS zone đang quản lý domain, tạo bản ghi A cho toàn bộ `<APP_DOMAIN>` trỏ đến public IPv4 `<VPS_HOST>` của VPS. Tùy nhà cung cấp DNS, trường **Name** có thể yêu cầu full domain hoặc chỉ host label tương đối với zone (ví dụ `app` nếu domain là `app.example.com` và zone là `example.com`).

| Type | Name           | Value (public IPv4) | TTL  |
|------|----------------|---------------------|------|
| A    | `<APP_DOMAIN>` | `<VPS_HOST>`        | 3600 |

Kiểm tra DNS đã lan truyền (chờ 5–30 phút):

```bash
nslookup "${APP_DOMAIN}"
# Kết quả mong đợi: Address là public IPv4 ${VPS_HOST}
```

---

## 4. Cấu hình biến môi trường (.env)

File `.env` chứa thông tin nhạy cảm — **KHÔNG commit lên git**, chỉ tạo trực tiếp trên VPS.

### 4.1 Tạo file .env từ template

```bash
cd "${DEPLOY_PATH}"
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
VNPAY_TMN_CODE=CHANGE_ME_VNPAY_TMN_CODE
VNPAY_HASH_SECRET=CHANGE_ME_VNPAY_HASH_SECRET

# Môi trường sandbox (giữ nguyên khi test, đổi khi go-live)
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

# URL callback sau khi thanh toán — thay <APP_DOMAIN> bằng domain thật
VNPAY_RETURN_URL="https://<APP_DOMAIN>/payment/callback"
```

**Lưu file:** `Ctrl+O` → `Enter` → `Ctrl+X`

### 4.4 Phân quyền file .env

```bash
# Chỉ chủ sở hữu file (`${DEPLOY_USER}`) có quyền đọc và ghi file này
chmod 600 "${DEPLOY_PATH}/.env"
```

### 4.5 Kiểm tra nội dung đã đúng

```bash
cat "${DEPLOY_PATH}/.env"
```

---

## 5. Cấp SSL lần đầu (Let's Encrypt)

> **Yêu cầu:** DNS `<APP_DOMAIN>` phải trỏ về public IPv4 `<VPS_HOST>` của VPS trước bước này.

### Bước 1: Tạo thư mục cho Certbot

```bash
cd "${DEPLOY_PATH}"
mkdir -p certbot/conf certbot/www
```

### Bước 2: Tạo cấu hình Nginx tạm (chỉ HTTP)

```bash
cat > "${DEPLOY_PATH}/nginx/conf.d/default.conf" <<EOF
server {
    listen 80;
    server_name ${APP_DOMAIN};

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
cd "${DEPLOY_PATH}"
docker compose up -d --no-deps nginx
```

Kiểm tra Nginx đang chạy:

```bash
docker compose ps
curl "http://${APP_DOMAIN}"
# Kết quả mong đợi: OK
```

### Bước 4: Chạy Certbot cấp chứng chỉ SSL

```bash
docker run --rm \
  -v "${DEPLOY_PATH}/certbot/conf:/etc/letsencrypt" \
  -v "${DEPLOY_PATH}/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "${LETSENCRYPT_EMAIL}" \
  --agree-tos \
  --no-eff-email \
  -d "${APP_DOMAIN}"
```

Kết quả thành công sẽ có dòng:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/<APP_DOMAIN>/fullchain.pem
```

### Bước 5: Khôi phục cấu hình Nginx đầy đủ (HTTPS)

Khởi động database và app trước vì cấu hình Nginx đầy đủ có upstream `app`:

```bash
cd "${DEPLOY_PATH}"
docker compose up -d --build mysql app
```

```bash
cat > "${DEPLOY_PATH}/nginx/conf.d/default.conf" <<EOF
server {
    listen 80;
    server_name ${APP_DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name ${APP_DOMAIN};

    ssl_certificate     /etc/letsencrypt/live/${APP_DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${APP_DOMAIN}/privkey.pem;

    client_max_body_size 50M;

    location / {
        proxy_pass         http://app:8386;
        proxy_http_version 1.1;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_read_timeout 60s;
    }

    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        proxy_pass         http://app:8386;
        proxy_set_header   Host \$host;
        expires            7d;
        add_header         Cache-Control "public, no-transform";
    }
}
EOF

# Kiểm tra cấu hình không cần TTY, sau đó áp dụng HTTPS
docker compose exec -T nginx nginx -t
docker compose exec -T nginx nginx -s reload
```

### Bước 6: Tự động gia hạn SSL (cronjob)

```bash
crontab -e
```

Thêm hai dòng sau vào cuối file. Đặt `DEPLOY_PATH` đúng với giá trị đã khai báo ở đầu tài liệu; Certbot sẽ kiểm tra gia hạn mỗi ngày lúc 03:00:

```cron
DEPLOY_PATH=/opt/your-app
0 3 * * * docker run --rm -v "${DEPLOY_PATH}/certbot/conf:/etc/letsencrypt" -v "${DEPLOY_PATH}/certbot/www:/var/www/certbot" certbot/certbot renew --quiet && docker compose -f "${DEPLOY_PATH}/docker-compose.yml" exec -T nginx nginx -s reload
```

---

## 6. Cấu hình CI/CD với GitHub Actions

### 6.1 Tạo GitHub Environment và thêm secrets

Vào repo **`<GITHUB_REPOSITORY>`** → **Settings → Environments → New environment**, tạo environment có tên chính xác là **`production`**. Deploy job trong `.github/workflows/ci-cd.yml` tham chiếu environment này.

Trong environment `production`, cấu hình **Required reviewers** và giới hạn **Deployment branches and tags** cho nhánh `main` nếu các tùy chọn này có sẵn với gói GitHub, loại repository và cài đặt hiện tại. Không xem các protection rules là đã bật cho đến khi xác nhận chúng xuất hiện và được cấu hình trong giao diện.

Tiếp theo, tại **Environment secrets → Add environment secret**, thêm lần lượt 4 secrets sau. Không thêm chúng dưới dạng repository secrets:

| Secret name   | Giá trị cần điền                                              |
|---------------|---------------------------------------------------------------|
| `VPS_HOST`    | Public IPv4 của VPS: `<VPS_HOST>`                              |
| `VPS_USER`    | `<DEPLOY_USER>`                                                |
| `VPS_SSH_KEY` | Toàn bộ nội dung file `~/.ssh/deploy_key` (private key)       |
| `DEPLOY_PATH` | `<DEPLOY_PATH>`                                                |

**Cách lấy nội dung VPS_SSH_KEY:**

```bash
# Chạy trên VPS, copy toàn bộ output (kể cả dòng -----BEGIN và -----END)
cat "${HOME}/.ssh/deploy_key"
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

Sau khi thêm environment secrets, push 1 commit bất kỳ lên `main`:

```bash
# Trên máy local
git commit --allow-empty -m "test: trigger CI/CD"
git push origin main
```

Vào trang **Actions** của repo **`<GITHUB_REPOSITORY>`** để xem pipeline chạy. Nếu đã cấu hình protection rules, deploy job chỉ tiếp tục sau khi thỏa các rule khả dụng đó.

---

## 7. Deploy lần đầu (thủ công)

Sau khi đã có SSL certificate, chạy toàn bộ stack:

```bash
cd "${DEPLOY_PATH}"

# Khởi động tất cả services (lần đầu build mất 5–10 phút)
docker compose up -d --build

# Theo dõi log app đang khởi động
docker compose logs -f app
```

Chờ xuất hiện dòng:
```
Started MarketplaceApplication in X.XXX seconds
```

Truy cập: **https://<APP_DOMAIN>**

---

## 8. Kiểm tra & bảo trì

Chạy lại khối khai báo biến ở đầu tài liệu khi mở phiên SSH mới, sau đó chuyển vào thư mục deploy:

```bash
cd "${DEPLOY_PATH}"
```

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

Compose đặt `MYSQL_DATABASE` và `MYSQL_ROOT_PASSWORD` trong container `mysql`; dấu nháy đơn quanh `sh -c` bảo đảm các biến này được mở rộng bên trong container, không phải trên VPS.

```bash
# File backup mới chỉ cho phép chủ sở hữu đọc và ghi
umask 077
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > "backup_$(date +%Y%m%d_%H%M).sql"
```

### Restore database

```bash
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "backup_20260507_1200.sql"
```

### Cập nhật app thủ công (không qua CI/CD)

```bash
cd "${DEPLOY_PATH}"
git pull origin main
docker compose up -d --build
docker image prune -f
```

### Kiểm tra & gia hạn SSL thủ công

```bash
# Xem thông tin certificate
docker run --rm \
  -v "${DEPLOY_PATH}/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certificates

# Gia hạn thủ công
docker run --rm \
  -v "${DEPLOY_PATH}/certbot/conf:/etc/letsencrypt" \
  -v "${DEPLOY_PATH}/certbot/www:/var/www/certbot" \
  certbot/certbot renew --quiet

docker compose exec nginx nginx -s reload
```

---

## 9. Checklist tổng kết

### Phần 1 — Chuẩn bị
- [ ] Xác nhận `<DEPLOY_USER>` đăng nhập được qua SSH và có quyền `sudo`
- [ ] SSH vào public IPv4 của VPS: `ssh "${DEPLOY_USER}@${VPS_HOST}"`
- [ ] Cài Docker: `curl -fsSL https://get.docker.com | sudo sh`
- [ ] Thêm tài khoản deploy vào nhóm `docker`, sau đó đăng xuất và SSH lại
- [ ] Cài Git: `sudo apt install git -y`
- [ ] Tạo `<DEPLOY_PATH>` và chuyển quyền sở hữu cho tài khoản deploy
- [ ] Clone repo: `git clone "https://github.com/${GITHUB_REPOSITORY}.git" .` (có dấu `.`)
- [ ] Tạo SSH deploy key và thêm vào `authorized_keys`

### Phần 2 — Cấu hình
- [ ] Trỏ DNS A record `<APP_DOMAIN>` → public IPv4 `<VPS_HOST>`
- [ ] Tạo file `.env` từ `.env.example` và điền đầy đủ
- [ ] Đặt `chmod 600 .env`

### Phần 3 — SSL
- [ ] Tạo thư mục `certbot/conf` và `certbot/www`
- [ ] Chạy Nginx tạm (HTTP only)
- [ ] Chạy Certbot cấp SSL thành công
- [ ] Khôi phục config Nginx đầy đủ (HTTPS)
- [ ] Chạy `nginx -t` thành công và reload Nginx
- [ ] Thêm cronjob kiểm tra gia hạn SSL hàng ngày

### Phần 4 — CI/CD
- [ ] Tạo GitHub Environment tên chính xác là `production`
- [ ] Cấu hình required reviewers và deployment branch restrictions cho `production` nếu có sẵn
- [ ] Thêm public IPv4 `VPS_HOST` = `<VPS_HOST>` vào environment secrets của `production`
- [ ] Thêm `VPS_USER` = `<DEPLOY_USER>` vào environment secrets của `production`
- [ ] Thêm `VPS_SSH_KEY` (nội dung `~/.ssh/deploy_key`) vào environment secrets của `production`
- [ ] Thêm `DEPLOY_PATH` = `<DEPLOY_PATH>` vào environment secrets của `production`
- [ ] Giới hạn quyền deploy và thiết lập lịch xoay vòng key

### Phần 5 — Go live
- [ ] Chạy `docker compose up -d --build` lần đầu
- [ ] Chờ log `Started MarketplaceApplication`
- [ ] Truy cập `https://<APP_DOMAIN>` thành công
- [ ] Push 1 commit test để xác nhận CI/CD hoạt động
