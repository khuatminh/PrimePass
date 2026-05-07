# Hướng dẫn Deploy lên VPS Ubuntu với Docker + HTTPS + CI/CD

**Domain:** `acc.khuatminh.com`  
**Stack:** Spring Boot 3 · MySQL 8 · Nginx · Docker Compose · Let's Encrypt · GitHub Actions

---

## Mục lục

1. [Kiến trúc tổng quan](#1-kiến-trúc-tổng-quan)
2. [Chuẩn bị VPS](#2-chuẩn-bị-vps)
3. [Trỏ DNS về VPS](#3-trỏ-dns-về-vps)
4. [Tạo các file cấu hình Docker](#4-tạo-các-file-cấu-hình-docker)
5. [Cấp SSL lần đầu (Let's Encrypt)](#5-cấp-ssl-lần-đầu-lets-encrypt)
6. [Cấu hình CI/CD với GitHub Actions](#6-cấu-hình-cicd-với-github-actions)
7. [Deploy lần đầu (thủ công)](#7-deploy-lần-đầu-thủ-công)
8. [Kiểm tra & bảo trì](#8-kiểm-tra--bảo-trì)

---

## 1. Kiến trúc tổng quan

```
Internet
   │  443/80
   ▼
[Nginx] ──proxy──► [Spring Boot :8386]
                         │
                    [MySQL :3306]

Docker Compose chạy 3 services:
  - nginx       (port 80, 443 → public)
  - app         (port 8386 → internal only)
  - mysql       (port 3306 → internal only)
```

---

## 2. Chuẩn bị VPS

### 2.1 Cài Docker & Docker Compose

```bash
# Kết nối VPS
ssh root@103.228.36.244

# Cập nhật hệ thống
apt update && apt upgrade -y

# Cài Docker
curl -fsSL https://get.docker.com | sh

# Thêm user vào group docker (nếu không dùng root)
usermod -aG docker $USER

# Kiểm tra
docker --version
docker compose version
```

### 2.2 Cài Git

```bash
apt install git -y
```

### 2.3 Tạo thư mục deploy & clone repo

```bash
mkdir -p /opt/marketplace
cd /opt/marketplace
git clone https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO>.git .
```

> **Lưu ý:** Thay `<YOUR_GITHUB_USERNAME>/<YOUR_REPO>` bằng repo thực tế.  
> Nếu repo private, dùng [Deploy Key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys).

### 2.4 Tạo SSH Deploy Key (cho CI/CD)

```bash
# Chạy trên VPS
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key -N ""

# Thêm public key vào authorized_keys
cat ~/.ssh/deploy_key.pub >> ~/.ssh/authorized_keys

# In private key ra để copy vào GitHub Secrets
cat ~/.ssh/deploy_key
```

---

## 3. Trỏ DNS về VPS

Vào DNS manager của domain `khuatminh.com`, thêm bản ghi:

| Type | Name | Value              | TTL  |
|------|------|--------------------|------|
| A    | acc  | `103.228.36.244`   | 3600 |

Kiểm tra DNS đã lan truyền:

```bash
nslookup acc.khuatminh.com
# hoặc
dig acc.khuatminh.com +short
```

---

## 4. Tạo các file cấu hình Docker

Tất cả file dưới đây tạo trong thư mục `/opt/marketplace` trên VPS (hoặc commit vào repo).

### 4.1 `Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/marketplace-0.0.1-SNAPSHOT.jar app.jar
VOLUME /app/uploads
EXPOSE 8386
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

### 4.2 `docker-compose.yml`

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: marketplace-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASS}
      MYSQL_DATABASE: ${DB_NAME:-digital_marketplace}
      MYSQL_CHARSET: utf8mb4
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASS}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - marketplace-net

  app:
    build: .
    container_name: marketplace-app
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      DB_HOST: mysql
      DB_NAME: ${DB_NAME:-digital_marketplace}
      DB_USER: ${DB_USER:-root}
      DB_PASS: ${DB_PASS}
      VNPAY_TMN_CODE: ${VNPAY_TMN_CODE}
      VNPAY_HASH_SECRET: ${VNPAY_HASH_SECRET}
      VNPAY_PAYMENT_URL: ${VNPAY_PAYMENT_URL:-https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}
      VNPAY_RETURN_URL: ${VNPAY_RETURN_URL:-https://acc.khuatminh.com/payment/callback}
    volumes:
      - uploads:/app/uploads
    networks:
      - marketplace-net

  nginx:
    image: nginx:1.27-alpine
    container_name: marketplace-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
    depends_on:
      - app
    networks:
      - marketplace-net

volumes:
  mysql_data:
  uploads:

networks:
  marketplace-net:
    driver: bridge
```

### 4.3 `nginx/conf.d/default.conf`

```bash
mkdir -p /opt/marketplace/nginx/conf.d
```

Tạo file `/opt/marketplace/nginx/conf.d/default.conf`:

```nginx
# HTTP → redirect HTTPS + cho Certbot xác thực
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

# HTTPS
server {
    listen 443 ssl;
    http2 on;
    server_name acc.khuatminh.com;

    ssl_certificate     /etc/letsencrypt/live/acc.khuatminh.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/acc.khuatminh.com/privkey.pem;
    include             /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam         /etc/letsencrypt/ssl-dhparams.pem;

    client_max_body_size 50M;

    # Proxy tới Spring Boot
    location / {
        proxy_pass         http://app:8386;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # Cache static files
    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        proxy_pass http://app:8386;
        proxy_set_header Host $host;
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }
}
```

### 4.4 `.env` (tạo trực tiếp trên VPS, **KHÔNG commit lên git**)

```bash
cat > /opt/marketplace/.env << 'EOF'
# Database
DB_NAME=digital_marketplace
DB_USER=root
DB_PASS=CHANGE_ME_STRONG_PASSWORD

# VNPay (thay bằng thông tin thật khi go-live)
VNPAY_TMN_CODE=DEMO0001
VNPAY_HASH_SECRET=DEMOHASHDEMOHASHDEMOHASHDEMO1234
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://acc.khuatminh.com/payment/callback
EOF

chmod 600 /opt/marketplace/.env
```

### 4.5 `.gitignore` — thêm các dòng sau (nếu chưa có)

```gitignore
.env
certbot/
nginx/conf.d/default.conf
```

---

## 5. Cấp SSL lần đầu (Let's Encrypt)

### Bước 1: Tạo cấu hình Nginx tạm (chỉ HTTP để Certbot xác thực)

Tạo file tạm `/opt/marketplace/nginx/conf.d/default.conf` với nội dung:

```nginx
server {
    listen 80;
    server_name acc.khuatminh.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 "OK";
    }
}
```

### Bước 2: Khởi động Nginx (chưa cần app và mysql)

```bash
cd /opt/marketplace
mkdir -p certbot/conf certbot/www

docker compose up -d nginx
```

### Bước 3: Chạy Certbot để cấp chứng chỉ

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

### Bước 4: Thay lại cấu hình Nginx đầy đủ (HTTPS)

Thay nội dung `nginx/conf.d/default.conf` bằng nội dung đầy đủ ở [mục 4.3](#43-nginxconfddefaultconf).

### Bước 5: Tự động gia hạn SSL

```bash
# Thêm cronjob gia hạn mỗi 2 tháng
crontab -e
# Thêm dòng:
0 3 1 */2 * docker run --rm -v /opt/marketplace/certbot/conf:/etc/letsencrypt -v /opt/marketplace/certbot/www:/var/www/certbot certbot/certbot renew --quiet && docker compose -f /opt/marketplace/docker-compose.yml exec nginx nginx -s reload
```

---

## 6. Cấu hình CI/CD với GitHub Actions

### 6.1 GitHub Secrets

Vào repo GitHub → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name   | Giá trị                                         |
|---------------|--------------------------------------------------|
| `VPS_HOST`    | `103.228.36.244`                                 |
| `VPS_USER`    | `root`                                           |
| `VPS_SSH_KEY` | Nội dung file `~/.ssh/deploy_key` (private key) |
| `DEPLOY_PATH` | `/opt/marketplace`                               |

### 6.2 `.github/workflows/ci-cd.yml` (thay thế file hiện có)

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # ── Job 1: Build & Test ──────────────────────────────────────────────
  build:
    name: Build & Test
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build & Test
        run: mvn clean package

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar
          retention-days: 1

  # ── Job 2: Deploy (chỉ khi push lên main) ───────────────────────────
  deploy:
    name: Deploy to VPS
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest

    steps:
      - name: Setup SSH key
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.VPS_SSH_KEY }}" > ~/.ssh/deploy_key
          chmod 600 ~/.ssh/deploy_key
          ssh-keyscan -H ${{ secrets.VPS_HOST }} >> ~/.ssh/known_hosts

      - name: Deploy to VPS
        run: |
          ssh -i ~/.ssh/deploy_key ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'ENDSSH'
            set -e
            cd ${{ secrets.DEPLOY_PATH }}

            echo "==> Pulling latest code..."
            git pull origin main

            echo "==> Building & restarting containers..."
            docker compose down --remove-orphans
            docker compose up -d --build

            echo "==> Cleaning up old images..."
            docker image prune -f

            echo "==> Checking health..."
            sleep 15
            if docker compose ps | grep -q "Up"; then
              echo "✅ Deploy thành công!"
            else
              echo "❌ Deploy thất bại — xem logs:"
              docker compose logs --tail=50
              exit 1
            fi
          ENDSSH

      - name: Notify on failure
        if: failure()
        run: echo "Deploy thất bại! Kiểm tra logs trên VPS."
```

---

## 7. Deploy lần đầu (thủ công)

Sau khi đã có SSL certificate:

```bash
cd /opt/marketplace

# 1. Đảm bảo .env đã được tạo đúng
cat .env

# 2. Khởi động toàn bộ stack
docker compose up -d --build

# 3. Xem logs theo dõi
docker compose logs -f app

# 4. Kiểm tra tất cả container đang chạy
docker compose ps
```

Truy cập: **https://acc.khuatminh.com**

---

## 8. Kiểm tra & bảo trì

### Xem logs

```bash
# Tất cả services
docker compose logs -f

# Chỉ app
docker compose logs -f app

# Chỉ nginx
docker compose logs -f nginx
```

### Restart service

```bash
docker compose restart app
docker compose restart nginx
```

### Backup database

```bash
# Backup
docker compose exec mysql mysqldump -uroot -p${DB_PASS} digital_marketplace > backup_$(date +%Y%m%d).sql

# Restore
docker compose exec -T mysql mysql -uroot -p${DB_PASS} digital_marketplace < backup_20260101.sql
```

### Cập nhật thủ công (không qua CI/CD)

```bash
cd /opt/marketplace
git pull origin main
docker compose up -d --build
docker image prune -f
```

### Kiểm tra SSL

```bash
# Kiểm tra hạn SSL
docker run --rm -v /opt/marketplace/certbot/conf:/etc/letsencrypt certbot/certbot certificates

# Gia hạn thủ công
docker run --rm \
  -v /opt/marketplace/certbot/conf:/etc/letsencrypt \
  -v /opt/marketplace/certbot/www:/var/www/certbot \
  certbot/certbot renew --quiet

docker compose exec nginx nginx -s reload
```

---

## Checklist tổng kết

- [ ] Trỏ DNS `acc` → `103.228.36.244`, chờ lan truyền
- [ ] Cài Docker & Docker Compose trên VPS
- [ ] Clone repo vào `/opt/marketplace`
- [ ] Tạo file `.env` với mật khẩu thật
- [ ] Tạo thư mục `nginx/conf.d/` và `certbot/`
- [ ] Copy các file: `Dockerfile`, `docker-compose.yml`, `nginx/conf.d/default.conf`
- [ ] Chạy Certbot cấp SSL lần đầu
- [ ] Thêm 5 GitHub Secrets
- [ ] Update file `.github/workflows/ci-cd.yml`
- [ ] Chạy `docker compose up -d --build` lần đầu
- [ ] Truy cập `https://acc.khuatminh.com` kiểm tra
- [ ] Push 1 commit nhỏ lên `main` để test CI/CD tự động
