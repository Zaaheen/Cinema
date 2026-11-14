# Cinema Management

Website quản lý rạp chiếu phim — backend Spring Boot, frontend React + Vite + JavaScript/JSX, database Supabase PostgreSQL dùng chung cho môi trường development.

## Công nghệ

- Backend: Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Spring Security (OAuth2 Resource Server), Flyway, springdoc OpenAPI.
- Frontend: React 19, Vite 8, JavaScript/JSX, React Router, Axios, TanStack Query, React Hook Form, Zod.
- Database: Supabase PostgreSQL. Docker Compose chỉ là phương án PostgreSQL local tùy chọn.

## Cấu trúc

```text
cinema/
├── backend/                # Spring Boot (Gradle wrapper)
│   ├── src/main/java/com/cinema/
│   │   ├── config/         # JWT, CORS, OpenAPI properties
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   ├── src/main/resources/db/migration/
│   ├── Dockerfile
│   ├── build.gradle
│   └── gradlew / gradlew.bat
├── frontend/               # React + Vite + JavaScript/JSX
│   └── src/
│       ├── api/            # axios client, endpoints, types
│       ├── auth/           # AuthProvider, useAuth, context
│       ├── layouts/
│       ├── pages/
│       └── routes/
├── docs/                   # ERD, API contract, kế hoạch
├── .github/workflows/ci.yml
├── docker-compose.yml
└── README.md
```

## Yêu cầu môi trường

- JDK 21
- Node.js 20+ và npm
- Tài khoản/connection details của Supabase project chung
- Docker Desktop hoặc PostgreSQL 15+ (tùy chọn, chỉ khi muốn chạy database local riêng)

## Chạy nhanh

### 1. Database

```powershell
Copy-Item backend/.env.example backend/.env
```

Điền connection details từ **Supabase Dashboard → Connect** vào `backend/.env`. Không commit file này. Sau đó cấu hình IntelliJ EnvFile dùng `backend/.env` khi chạy Spring Boot.

Nếu cần database local riêng thay vì Supabase, mới chạy:

```powershell
docker compose up -d postgres
```

### 2. Backend

```bash
cd backend
./gradlew bootRun
```

- Profile mặc định: `local`; datasource đọc từ biến môi trường trong `backend/.env`.
- Flyway tự chạy migration khi khởi động.
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`. Tạo `frontend/.env` từ `frontend/.env.example` nếu cần đổi API URL.

## Biến môi trường

Không commit `.env` hoặc secret. Tạo file local từ `.env.example`.

Backend (`backend/.env.example`):

| Biến | Ý nghĩa |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` hoặc `prod` |
| `SPRING_DATASOURCE_URL` | JDBC URL do Supabase Connect cung cấp, có SSL |
| `SPRING_DATASOURCE_USERNAME` | Tên đăng nhập do Supabase cung cấp |
| `SPRING_DATASOURCE_PASSWORD` | Database password của Supabase |
| `JWT_SECRET` | Khóa HS256, tối thiểu 32 byte |
| `CORS_ALLOWED_ORIGINS` | Danh sách origin frontend, phân tách bằng dấu phẩy |
| `CLOUDINARY_CLOUD_NAME` | Cloud name trong Cloudinary Dashboard |
| `CLOUDINARY_API_KEY` | API key trong Cloudinary Dashboard |
| `CLOUDINARY_API_SECRET` | API secret trong Cloudinary Dashboard; giữ bí mật |
| `PORT` | Cổng backend khi deploy (mặc định 8080) |

Frontend (`frontend/.env.example`):

| Biến | Ý nghĩa |
|---|---|
| `VITE_API_BASE_URL` | URL API, ví dụ `http://localhost:8080/api/v1` |

Khi dùng Supabase, đặt `SPRING_PROFILES_ACTIVE=prod` và thêm `?sslmode=require` vào JDBC URL.

## Kiểm thử

```bash
cd backend
./gradlew test

cd ../frontend
npm run lint
npm run build
```

Backend test dùng H2 in-memory (profile `test`), không cần PostgreSQL hay Docker.

## Docker

```bash
docker build -t cinema-backend ./backend
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/cinema \
  -e SPRING_DATASOURCE_USERNAME=cinema \
  -e SPRING_DATASOURCE_PASSWORD=cinema \
  -e JWT_SECRET=local-development-secret-key-change-me-please-0123456789 \
  cinema-backend
```

## Auth

- `POST /api/v1/auth/register` — đăng ký thành viên.
- `POST /api/v1/auth/login` — trả access token (15 phút) và set refresh cookie (7 ngày).
- `POST /api/v1/auth/refresh` — làm mới access token, rotation refresh token.
- `POST /api/v1/auth/logout` — thu hồi refresh token và xóa cookie.
- `GET /api/v1/auth/me` — thông tin tài khoản đang đăng nhập.

Access token gửi qua header `Authorization: Bearer <token>`; frontend giữ token trong bộ nhớ (không localStorage).

## Tài liệu

- `docs/ERD.md` — mô hình dữ liệu và ràng buộc.
- `docs/API_CONTRACT.md` — hợp đồng REST API `/api/v1`.
- `docs/PLAN.md` — kế hoạch 4 sprint chính thức.
- `docs/REQUIREMENTS_MAPPING.md` — ánh xạ yêu cầu SRS.
- `docs/DEPLOYMENT.md` — hướng dẫn deploy.

## Quy ước

- Mỗi PR nhỏ, có test, review chéo trước khi merge.
- Không commit `target/`, `node_modules/`, `dist/`, `.env` hoặc credential.
- Không đổi hợp đồng API mà không cập nhật `docs/API_CONTRACT.md`.
