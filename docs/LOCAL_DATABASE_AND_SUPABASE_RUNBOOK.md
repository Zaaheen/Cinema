# Local database runbook

## Phạm vi

Runbook này chỉ mô tả development local: PostgreSQL 16 tại `127.0.0.1:5432`, database `cinema`. Supabase nằm ngoài local-development workflow; không đưa connection string, credential hoặc lệnh remote vào quick start này.

Không đọc hoặc commit root `.env`, `backend/.env` hay bất kỳ credential file thật nào. Chỉ dùng template `backend/.env.example` và `frontend/.env.example` để biết tên biến môi trường.

## Điều kiện và cấu hình local

Profile mặc định của backend là `local`. Các biến liên quan gồm:

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD`, `SPRING_FLYWAY_TARGET`
- `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`

`backend/scripts/start-local.ps1` chỉ cho phép URL local `jdbc:postgresql://127.0.0.1:5432/cinema`, profile `local` và Flyway target `6`. Password PostgreSQL/JWT secret phải được cung cấp qua process environment hoặc secure prompt; launcher không nạp `backend/.env`.

## Flyway và startup

Repository chứa migration `V1`–`V6`. Flyway được bật trong Spring Boot và launcher local đặt target tại V6. Trước khi startup, maintainer phải kiểm tra database đích và `flyway_schema_history`; không được suy ra rằng migration đã hoàn tất chỉ từ source code hoặc từ Hibernate.

`ddl-auto=validate` chỉ yêu cầu Hibernate kiểm tra schema. Nó không vô hiệu hóa Flyway. Khởi động backend có thể kết nối database và chạy migration còn thiếu, vì vậy không xem startup là thao tác read-only.

Không dùng `flyway clean`, `baseline`, `repair`, `DROP DATABASE`, `TRUNCATE` hoặc bất kỳ thao tác ghi phá huỷ nào trong workflow này khi chưa có phê duyệt rõ ràng.

## Docker PostgreSQL

`docker-compose.yml` khai báo PostgreSQL 16, database local `cinema` và persistent volume. Lệnh sau có thể tạo mới hoặc tái sử dụng container/volume:

```powershell
docker compose up -d postgres
```

Kiểm tra port 5432, container và volume hiện có trước khi chạy. Compose đang dùng credential development cố định; chúng chỉ phù hợp cho local development, không được sao chép sang môi trường chia sẻ/production. Việc tách credential Compose thành cấu hình an toàn hơn là công việc riêng, không thuộc runbook này.

## Sao lưu và kiểm tra an toàn

Trước migration, seed hoặc thao tác quản trị có ghi dữ liệu:

1. Xác nhận host, port, database và user là local như mong đợi.
2. Kiểm tra Flyway history và trạng thái dữ liệu hiện hữu bằng truy vấn read-only phù hợp.
3. Tạo backup riêng tư, xác minh có thể đọc/restore vào database cô lập và không đưa backup vào Git.
4. Chỉ thực hiện migration sau khi có phê duyệt cho đúng database.

Quy tắc `.gitignore` hiện chưa có pattern chuyên biệt cho database dump/backup; đây là follow-up security/configuration riêng. Không lưu backup trong repository cho đến khi quy tắc lưu trữ và ignore được phê duyệt.

## Seed data

`backend/src/main/resources/db/seed/stitch-development.sql` là manual development seed, không thuộc Flyway và không được chạy khi khởi động thường ngày. Không chạy seed trên dữ liệu hiện có nếu chưa biết đầy đủ tác động và chưa được phê duyệt. Launcher local đặt `SPRING_SQL_INIT_MODE=never`, nhưng điều này không thay thế kiểm tra thủ công.

## Troubleshooting không ghi dữ liệu

- Kiểm tra process environment và template thay vì mở credential file thật.
- Kiểm tra container/volume/port bằng lệnh Docker hoặc hệ điều hành chỉ đọc.
- Kiểm tra JDBC URL, profile và Flyway target trong launcher trước khi khởi động.
- Nếu trạng thái database không chắc chắn, dừng lại và lấy backup/approval; không thử `clean`, reset hoặc seed để “sửa nhanh”.

## Ghi chú Supabase

Supabase không phải đích của local startup và không có lệnh Supabase nào trong tài liệu này. Mọi triển khai hoặc vận hành remote cần runbook, approval, backup và credential handling riêng; không suy diễn quyền thực hiện từ tài liệu local này.
