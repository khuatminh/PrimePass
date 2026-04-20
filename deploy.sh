#!/bin/bash
# Deploy script - chay 1 lan tren VPS de setup

set -e

echo "=== Cap nhat he dieu hanh ==="
sudo apt update && sudo apt upgrade -y

echo "=== Cai dat Docker ==="
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com | sudo sh
    sudo usermod -aG docker $USER
    echo "Docker da cai xong. Dang xuat va dang nhap lai de co quyen docker."
    echo "Sau do chay lai script nay."
    exit 0
else
    echo "Docker da duoc cai dat."
fi

echo "=== Cai dat Docker Compose ==="
if ! command -v docker compose &> /dev/null; then
    sudo apt install -y docker-compose-plugin
    echo "Docker Compose da cai xong."
else
    echo "Docker Compose da duoc cai dat."
fi

echo "=== Tao file .env ==="
if [ ! -f .env ]; then
    cat > .env << 'ENVEOF'
DB_PASSWORD=changeme_secure_password
DB_ROOT_PASSWORD=root_secure_password
VNPAY_TMN_CODE=DEMO0001
VNPAY_HASH_SECRET=DEMOHASHDEMOHASHDEMOHASHDEMO1234
VNPAY_RETURN_URL=http://YOUR_VPS_IP:8386/payment/callback
ENVEOF
    echo "Da tao .env - HAY CHINH SUA GIA TRI TRUOC KHI DEPLOY!"
else
    echo ".env da ton tai."
fi

echo "=== Build va chay ==="
docker compose up -d --build

echo ""
echo "=== Hoan tat! ==="
echo "App:        http://$(hostname -I | awk '{print $1}'):8386"
echo "MySQL port: 3307 (tu ben ngoai)"
echo ""
echo "Lenh quan ly:"
echo "  docker compose logs -f          # Xem log"
echo "  docker compose down              # Dung app"
echo "  docker compose up -d --build     # Build lai va chay"
echo "  docker compose restart           # Khoi dong lai"