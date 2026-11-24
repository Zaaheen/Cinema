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
- Node.js và npm tương thích với `frontend/package-lock.json`
- Quyền truy cập một Supabase PostgreSQL **sạch, được chủ dự án phê duyệt** cho nhánh này
- Docker Desktop hoặc PostgreSQL 15+ (tùy chọn, chỉ khi muốn chạy database local riêng)

## Chạy trên Supabase dùng chung bằng IntelliJ

### 1. Clone và chuẩn bị công cụ

```powershell
git clone https://github.com/Zaaheen/Cinema.git
cd Cinema
```

Khi đánh giá PR #9 trước lúc merge, chọn nhánh `codex/consolidate-pr6-pr7-pr8` sau khi nhánh này đã được cập nhật lên GitHub. Dùng **JDK 21** cho Project SDK/Gradle và cài Node.js cùng npm. Import `backend` như Gradle project trong IntelliJ.

### 2. Chặn nhầm database trước khi bấm Run

**Không chạy ứng dụng với Supabase Cinema hiện tại.** Database đó đã áp dụng V4 `seed_demo_movies`, V5 `normalize_movie_genres`, V6 `prevent_overlapping_showtimes`, V7 `add_movie_id_to_seat_prices`; PR #9 có các migration V4–V7 khác. Flyway sẽ báo checksum/lineage mismatch. Không dùng `repair`, `clean`, `baseline`, bỏ validation, đổi checksum hoặc sửa migration cũ để vượt lỗi.

Chủ dự án phải xác nhận **đúng project/database đích** và chạy preflight chỉ đọc trước khi cho nhóm khởi động backend:

```sql
SELECT current_database(), current_user, inet_server_addr(), inet_server_port();
SELECT to_regclass('public.flyway_schema_history') AS flyway_history;
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name;
-- Chỉ chạy câu tiếp theo khi flyway_history tồn tại:
SELECT version, script, checksum, success
FROM public.flyway_schema_history ORDER BY installed_rank;
```

Nếu lịch sử hiện có khác **đúng** chuỗi V1–V9 của checkout này, hoặc có bảng/dữ liệu chưa được đánh giá, **dừng**. Database rỗng chỉ được chạy V1–V9 sau khi chủ dự án phê duyệt riêng đích migration, backup và cách khôi phục. Việc bấm Run có thể tự chạy Flyway, nên **không dùng Run làm phép thử kết nối**. Sau khi owner đã migrate và validate database được duyệt, các thành viên mới dùng cấu hình chia sẻ để chạy ứng dụng. Cấu hình chia sẻ giới hạn Flyway target ở V9 và giữ validation/baseline safety; nó không tự xác minh quyền phê duyệt database.

### 3. Cấu hình riêng của mỗi thành viên

```powershell
Copy-Item backend/.env.example backend/.env
Copy-Item frontend/.env.example frontend/.env
```

Trong `backend/.env`, lấy **JDBC Session pooler** từ [Supabase Dashboard → Connect](https://supabase.com/docs/guides/database/connecting-to-postgres): host và username phải sao chép chính xác từ project được duyệt, port 5432, database `postgres`, SSL `sslmode=require`. Không dùng host/username suy đoán hoặc `.env` của Supabase Cinema cũ. Điền password và JWT secret riêng tư, không commit hoặc chụp màn hình có secret. `frontend/.env` trỏ `VITE_API_BASE_URL` về `http://localhost:8081/api/v1`.

Trong IntelliJ, chọn **`CinemaApplication - Shared Supabase`** từ `.run/CinemaApplication.run.xml`. Cấu hình Git này dùng main class `com.cinema.CinemaApplication`, profile `local`, cổng 8081; không chứa host, password, JWT hoặc đường dẫn cá nhân. **Sao chép cấu hình thành bản cá nhân** (không chọn *Store as project file*), sau đó trong *Environment variables* chọn file `backend/.env` bằng chức năng native của IntelliJ, hoặc dùng EnvFile plugin nếu nhóm đã cài. Có thể nhập các biến vào cấu hình cá nhân thay cho file. Đừng lưu đường dẫn cá nhân hay secret vào `.run/`. Đừng chọn `CinemaApplication` cũ nếu nó còn nạp `.env` của database khác. [Hướng dẫn IntelliJ về environment variables](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html).

**Chỉ sau preflight và phê duyệt database**, bấm Run ở bản cấu hình cá nhân. Kiểm tra [backend health](http://localhost:8081/actuator/health) và API tại `http://localhost:8081/api/v1`.

### 4. Frontend

```powershell
cd frontend
npm ci
npm run dev
```

Mở [http://localhost:5173](http://localhost:5173). Docker Compose/PostgreSQL QA trên máy cá nhân không cần cho luồng Supabase được duyệt này.

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
| `PORT` | Cổng backend; mẫu IntelliJ dùng 8081 |

Frontend (`frontend/.env.example`):

| Biến | Ý nghĩa |
|---|---|
| `VITE_API_BASE_URL` | URL API, ví dụ `http://localhost:8081/api/v1` |

IntelliJ development dùng profile `local`; môi trường Cloud Run dùng profile `prod` theo `docs/DEPLOYMENT.md`. `application-local.yaml` không còn credential/URL mặc định; thiếu biến datasource sẽ làm startup thất bại thay vì tự chọn PostgreSQL local.

## Kiểm thử

```bash
cd backend
./gradlew test

cd ../frontend
npm run lint
npm run build
```

Các API test thông thường dùng H2 in-memory (`test` profile); PostgreSQL migration/concurrency tests dùng PostgreSQL 16 Testcontainers và cần Docker Engine. H2 không thay thế bằng chứng migration PostgreSQL.

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
