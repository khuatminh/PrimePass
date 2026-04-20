# Design Spec: UI Redesign — Thymeleaf Templates Khớp HTML Mẫu

> **Ngày tạo:** 2026-04-20
> **Trạng thái:** Đã duyệt
> **Phạm vi:** Toàn bộ Thymeleaf templates (~27 files)

---

## 1. Vấn Đề

Các Thymeleaf template được implement với design generic (Bootstrap mặc định, Bootstrap Icons, background `#0f0f0f`), trong khi các file HTML mẫu trong root project có thiết kế riêng biệt. Hai hệ thống CSS xung đột nhau:

- `global.css` — brutalist dark, font Inter/Oswald, background `#030303`, border-radius 0
- `bootstrap-override.css` — purple NFT theme, font Work Sans/Space Mono, background `#2B2B2B`, border-radius 20px

Template hiện tại load cả hai → xung đột. Cần loại bỏ `global.css`, chỉ dùng `bootstrap-override.css`.

---

## 2. Design System (Nguồn Thật)

Tất cả pages đều dùng **`bootstrap-override.css`** làm nền tảng:

| Token | Giá trị |
|---|---|
| Background body | `#2B2B2B` |
| Background surface (card, sidebar) | `#3B3B3B` |
| Primary accent | `#A259FF` (purple) |
| Text primary | `#ffffff` |
| Text secondary | `#b0b0b0` |
| Font body | Work Sans (Google Fonts) |
| Font mono | Space Mono (Google Fonts) |
| Border radius | 20px (cards, buttons, inputs) |
| Icon library | Font Awesome 6.4.0 (CDN) |
| Bootstrap version | 5.3.0 (CDN) |

**Admin pages** dùng thêm inline style cho sidebar:
- Sidebar width: 280px, fixed left, `background: #3B3B3B`, `border-right: 1px solid #4a4a4a`
- Main content: `margin-left: 280px`

---

## 3. Cách Tiếp Cận: Layer-by-layer

1. **Fix layout fragments** trước (shared foundation)
2. **Convert customer pages** từ HTML mẫu → Thymeleaf
3. **Design trang còn lại** (order, auth, admin) theo cùng style

---

## 4. Phần 1 — Layout Fragments

### 4.1 `layout/css.html`

```html
<!-- Loại bỏ global.css -->
Bootstrap 5.3.0 CDN
Font Awesome 6.4.0 CDN
/css/bootstrap-override.css (th:href)
```

### 4.2 `layout/header.html`

Navbar fixed-top từ reference HTML mẫu:
- Logo: `fa-store` icon + "Digital Marketplace" text
- Nav links: Trang Chủ, Cửa Hàng
- Right side: cart icon với badge số lượng, nút Đăng nhập (khi chưa đăng nhập) / dropdown user (khi đã đăng nhập)
- Dropdown user: My Orders, Admin Panel (nếu ADMIN role), Logout
- `body` cần class `pt-5 mt-5` để compensate fixed navbar

### 4.3 `layout/footer.html`

Footer đơn giản: tên site + copyright, background `#3B3B3B`, border-top subtle.

### 4.4 `layout/js.html`

Bootstrap 5.3.0 bundle CDN.

### 4.5 `admin/layout/sidebar.html` (mới)

Sidebar 280px fixed từ `admin.html` reference:
- Logo section trên cùng
- Menu items: Dashboard, Danh Mục, Sản Phẩm, Kho Key, Đơn Hàng, Người Dùng, Mã Giảm Giá
- Mỗi item: FA icon + label, active state highlight purple
- Nút Về Trang Chủ ở dưới

### 4.6 `admin/layout/css.html` (mới)

Bootstrap 5.3.0 + Font Awesome 6.4 + `bootstrap-override.css` + inline sidebar/main-content style.

---

## 5. Phần 2 — Customer Pages

### 5.1 `index.html`

Từ reference `index.html` (383 dòng):
- **Hero section**: 2 cột — text trái (H1, mô tả, nút CTA, stats 12K+/100+/5K+) + card Netflix phải
- **Featured Products**: grid 3 cột `th:each="product"`, card với ảnh/tên/giá/badge category
- **Categories**: row badges link đến marketplace với `?categoryId=`

### 5.2 `marketplace.html`

Từ reference `marketplace.html` (369 dòng):
- Search bar full-width trên cùng
- 2 cột: sidebar categories (25%) + product grid (75%)
- Sidebar: list-group danh mục, active highlight purple
- Grid: card sản phẩm, badge sale nếu có, giá, nút Xem Chi Tiết
- Pagination dưới cùng

### 5.3 `cart.html`

Từ reference `cart.html` (207 dòng):
- Bảng items: ảnh thumbnail, tên sản phẩm, variant, đơn giá, số lượng, thành tiền, nút xóa
- Coupon input + nút Áp Dụng
- Order summary card: subtotal, discount, total, nút Đặt Hàng

### 5.4 `product-detail.html`

Từ reference `nft-page.html`:
- 2 cột: ảnh lớn trái + thông tin phải (tên, category badge, mô tả, variant selector, giá, nút thêm giỏ)
- Variant selector: radio buttons style cho từng variant, hiển thị giá theo variant
- Reviews section dưới: avg rating, danh sách reviews, form thêm review (khi đã mua)

### 5.5 `auth/register.html`

Từ reference `sign-up.html`:
- Layout 2 cột hoặc centered card
- Form: username, email, password, confirm password, nút Đăng Ký
- Link chuyển sang trang đăng nhập

### 5.6 `auth/login.html`

Thiết kế mới dựa trên style `sign-up.html`:
- Cùng layout, cùng card style
- Form: email/username, password, nút Đăng Nhập
- Link chuyển sang trang đăng ký
- Hiển thị lỗi nếu sai credentials

### 5.7 `order-history.html`

Thiết kế mới theo style purple theme:
- Tiêu đề "Lịch Sử Đơn Hàng"
- Bảng: mã đơn, ngày đặt, tổng tiền, trạng thái (badge màu: PENDING=yellow, PAID=blue, COMPLETED=green, FAILED=red), nút Xem Chi Tiết
- Empty state nếu chưa có đơn

### 5.8 `order-detail.html`

Thiết kế mới theo style purple theme:
- Thông tin đơn: mã, ngày, trạng thái, coupon nếu có
- Bảng order items: sản phẩm, variant, số lượng, giá
- Nếu status=COMPLETED: hiển thị key/account đã giao trong card nổi bật
- Tổng tiền

### 5.9 `payment-result.html`

Thiết kế mới:
- Centered card lớn
- Success: FA icon check-circle green, "Thanh toán thành công!", nút Xem Đơn Hàng
- Failed: FA icon x-circle red, "Thanh toán thất bại", nút Thử Lại

---

## 6. Phần 3 — Admin Pages

Tất cả dùng layout: sidebar fragment + main-content div.

### 6.1 `admin/dashboard.html`

Từ reference `admin.html` (285 dòng):
- 4 stats cards: Tổng Đơn, Doanh Thu, Người Dùng, Sản Phẩm (icon FA, con số, subtitle)
- Bảng "Đơn Hàng Gần Nhất": 5-10 đơn gần nhất, badge status

### 6.2 `admin/category/list.html`

Từ reference `admin-categories.html` (510 dòng):
- Header: tiêu đề + nút Thêm Danh Mục (mở modal)
- Bảng: ID, Tên, Số Sản Phẩm, Actions (Sửa/Xóa)
- Modal Bootstrap: form thêm/sửa danh mục (tên, description)
- Xác nhận xóa inline

### 6.3 `admin/product/list.html`

- Search bar + filter category
- Bảng: ảnh thumbnail nhỏ, tên, category, giá, stock, status, actions
- Nút Thêm Sản Phẩm

### 6.4 `admin/product/add.html`

- Form: tên, slug (auto-generate), description (textarea), category select, giá gốc, giá bán, upload ảnh, status toggle
- Nút Lưu / Hủy

### 6.5 `admin/product/edit.html`

- Giống add.html nhưng pre-filled với data hiện tại

### 6.6 `admin/product/variants.html`

- Tên sản phẩm header
- Bảng variants hiện có: label, giá, stock, active toggle, nút xóa
- Form thêm variant mới: label input, giá, stock

### 6.7 `admin/key/list.html`

- Filter by product, variant, status (available/sold)
- Bảng: ID, product, variant, status badge, sold_at nếu có
- Nút Nhập Kho Key (link tới add)

### 6.8 `admin/key/add.html`

- Select product + variant
- Textarea nhập nhiều key (mỗi dòng 1 key)
- Nút Import

### 6.9 `admin/order/list.html`

- Filter by status
- Bảng: mã đơn, user, ngày đặt, tổng tiền, status badge, actions

### 6.10 `admin/order/detail.html`

- Thông tin đơn đầy đủ
- Bảng items + key đã giao
- Form cập nhật status (select + submit)

### 6.11 `admin/user/list.html`

- Bảng: ID, username, email, role badge, ngày đăng ký

### 6.12 `admin/coupon/list.html`

- Bảng: code, discount%, max uses, used count, hết hạn, active toggle, actions
- Nút Thêm Coupon

### 6.13 `admin/coupon/add.html`

- Form: code, discount percentage, max uses, expiry date, active

---

## 7. Files Cần Tạo/Cập Nhật

| # | File | Hành động | Nguồn |
|---|---|---|---|
| 1 | `layout/css.html` | Cập nhật | Bỏ global.css, dùng FA6 |
| 2 | `layout/header.html` | Cập nhật | reference index.html navbar |
| 3 | `layout/footer.html` | Cập nhật | Tạo mới theo style |
| 4 | `layout/js.html` | Cập nhật | Bootstrap 5.3.0 bundle |
| 5 | `admin/layout/sidebar.html` | Tạo mới | reference admin.html sidebar |
| 6 | `admin/layout/css.html` | Tạo mới | FA6 + Bootstrap + override + sidebar style |
| 7 | `index.html` | Cập nhật | reference index.html |
| 8 | `marketplace.html` | Cập nhật | reference marketplace.html |
| 9 | `cart.html` | Cập nhật | reference cart.html |
| 10 | `product-detail.html` | Cập nhật | reference nft-page.html |
| 11 | `auth/register.html` | Cập nhật | reference sign-up.html |
| 12 | `auth/login.html` | Cập nhật | style sign-up.html |
| 13 | `order-history.html` | Cập nhật | Thiết kế mới |
| 14 | `order-detail.html` | Cập nhật | Thiết kế mới |
| 15 | `payment-result.html` | Cập nhật | Thiết kế mới |
| 16 | `admin/dashboard.html` | Cập nhật | reference admin.html |
| 17 | `admin/category/list.html` | Cập nhật | reference admin-categories.html |
| 18 | `admin/product/list.html` | Cập nhật | Thiết kế mới (admin style) |
| 19 | `admin/product/add.html` | Cập nhật | Thiết kế mới (admin style) |
| 20 | `admin/product/edit.html` | Cập nhật | Thiết kế mới (admin style) |
| 21 | `admin/product/variants.html` | Cập nhật | Thiết kế mới (admin style) |
| 22 | `admin/key/list.html` | Cập nhật | Thiết kế mới (admin style) |
| 23 | `admin/key/add.html` | Cập nhật | Thiết kế mới (admin style) |
| 24 | `admin/order/list.html` | Cập nhật | Thiết kế mới (admin style) |
| 25 | `admin/order/detail.html` | Cập nhật | Thiết kế mới (admin style) |
| 26 | `admin/user/list.html` | Cập nhật | Thiết kế mới (admin style) |
| 27 | `admin/coupon/list.html` | Cập nhật | Thiết kế mới (admin style) |
| 28 | `admin/coupon/add.html` | Cập nhật | Thiết kế mới (admin style) |

---

## 8. Phạm Vi Không Bao Gồm

- Thay đổi backend Java (controllers, services, entities)
- CSS mới ngoài bootstrap-override.css đã có
- Animation phức tạp ngoài transition đã có trong CSS
- Responsive mobile breakpoints (ưu tiên desktop)
