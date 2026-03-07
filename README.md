# 💳 Digital Wallet – Backend API

Dự án **ví điện tử (digital wallet)** xây dựng bằng **Spring Boot 4**, được thiết kế theo kiến trúc RESTful API. Đây là project học tập / thực hành cá nhân, tập trung vào các kỹ thuật backend thực tế trong lĩnh vực fintech.

---

## 🛠️ Tech Stack

| Layer | Công nghệ |
|---|---|
| Backend Framework | Spring Boot 4.0.3 (Java 17) |
| Database | PostgreSQL |
| Cache / Redis | Spring Data Redis + Lettuce |
| ORM | Spring Data JPA + Hibernate |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| Build Tool | Maven |

---

## Những gì đã làm được

### 1. Authentication Module (`/api/auth`)
- **Đăng ký tài khoản** (`POST /api/auth/register`) – tạo User + Wallet đồng thời
- **Đăng nhập** (`POST /api/auth/login`) – trả về Access Token + Refresh Token
- **Refresh Token** (`POST /api/auth/refresh`) – cấp lại Access Token khi hết hạn
- **Logout** (`POST /api/auth/logout`) – blacklist Access Token + xóa Refresh Token trên Redis

**Cơ chế JWT:**
- Access Token hết hạn sau **15 phút**
- Refresh Token hết hạn sau **7 ngày**
- Token bị logout được lưu vào Redis Blacklist để phòng replay attack

---

### 2. Wallet Module (`/api/wallets`)
- **Xem thông tin ví** (`GET /api/wallets/me`) – số dư, trạng thái ví của user đang đăng nhập
- **Cài đặt PIN** (`POST /api/wallets/set-pin`) – đặt mã PIN lần đầu
- **Đổi PIN** (`POST /api/wallets/change-pin`) – xác nhận PIN cũ, đặt PIN mới

**Bảo vệ PIN:**
- PIN được hash bằng BCrypt trước khi lưu
- Có `pinFailedCount` + `pinLockedUntil` → **khóa ví tạm thời** nếu nhập sai nhiều lần
- Wallet dùng `@Version` (Optimistic Locking) để tránh race condition khi 2 request cập nhật cùng lúc

---

### 3. Transaction Module (`/api/transsactions`)
- **Chuyển tiền P2P** (`POST /api/transsactions/transfer`) – yêu cầu header `X-Idempotency-Key`
- **Lịch sử giao dịch** (`GET /api/transsactions/history`) – phân trang (page, size)
- **Chi tiết giao dịch** (`GET /api/transsactions/{id}`)

**Các cơ chế bảo vệ giao dịch:**
- **Idempotency Key** (Redis) – chống double-submit, replay attack
- **Distributed Lock** (Redis `SETNX`) – chống race condition khi chuyển tiền đồng thời
- Lưu `balanceBefore` / `balanceAfter` cho mỗi giao dịch để kiểm toán
- Trạng thái giao dịch: `PENDING` → `COMPLETED` / `FAILED`

---

### 4. Async Event System

Sau khi một giao dịch hoàn thành, hệ thống publish `TransactionCompletedEvent`. Hai listener xử lý **bất đồng bộ** trên thread pool riêng (`walletExecutor`):

| Listener | Chức năng |
|---|---|
| `NotificationListener` | Tạo Notification cho người gửi & người nhận |
| `AuditLogListener` | Ghi Audit Log (IP, User-Agent, loại hành động) |

- Cả hai listener dùng `@Async("walletExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)` → chỉ kích hoạt khi transaction chính đã commit thành công
- Thread pool: core = 5, max = 20, queue = 100

---

### 5. Security & Configuration

- **Stateless JWT Security** – không dùng Session/Cookie
- **BCrypt** mã hóa mật khẩu
- Phân quyền: `/api/auth/**` public, còn lại phải có token hợp lệ
- `JwtAuthenticationFilter` đứng trước `UsernamePasswordAuthenticationFilter`

---

### 6. Exception Handling (Global)

`GlobalExceptionHandler` xử lý tập trung tất cả exception và trả về `ErrorResponse` chuẩn hóa:

| HTTP Status | Exception |
|---|---|
| 400 | Validation errors, InvalidDataException |
| 401 | BadCredentialsException |
| 403 | ForbiddenException, AccessDeniedException |
| 404 | ResourceNotFoundException |
| 409 | DuplicateResourceException, DuplicateRequestException |
| 423 | WalletLockedException |
| 500 | Fallback – không lộ message nội bộ ra ngoài |

---

### 7. Domain Model

```
User ──(1:1)──► Wallet
User ──(1:N)──► FundingSource
User ──(1:N)──► Notification
User ──(1:N)──► AuditLog
Transaction ──(N:1)──► Wallet (source / destination)
Transaction ──(N:1)──► FundingSource
Transaction ──(1:N)──► Notification
```

**Entities chính:** `User`, `Wallet`, `Transaction`, `FundingSource`, `Notification`, `AuditLog`

**Enums:** `Role`, `Status`, `WalletStatus`, `TransactionType` (TRANSFER, DEPOSIT, WITHDRAW), `TransactionStatus`, `NotificationType`, `AuditLogAction`, `FundingSourceType/Status`

---

## 🗄️ Cấu hình môi trường

Tạo file `.env` hoặc set biến môi trường:

```env
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
SECRET_KEY=your_jwt_secret_key_at_least_256bit
```

Database mặc định: `jdbc:postgresql://localhost:5432/digital_wallet`

Redis mặc định: `localhost:6379`

---

## 🚀 Chạy project

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

> **Lưu ý:** `spring.jpa.hibernate.ddl-auto=none` – schema được tạo thủ công, không auto-generate từ Entity.

---

## 📁 Cấu trúc package

```
vn.huy.digital_wallet
├── config/          # Security, JWT, Redis, Async config
├── controller/      # REST Controllers (Auth, Wallet, Transaction)
├── dto/             # Request / Response DTOs + ApiResponse wrapper
├── event/           # TransactionCompletedEvent + Listeners
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── mapper/          # MapStruct mappers
├── model/           # JPA Entities
├── repository/      # Spring Data JPA Repositories
├── service/         # Business logic interfaces + implementations
└── common/          # Enums dùng chung
```
