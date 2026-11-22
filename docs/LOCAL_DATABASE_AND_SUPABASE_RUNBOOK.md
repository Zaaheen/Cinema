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

Checkout này **không có** `backend/scripts/start-local.ps1`. `backend/gradlew.bat` có tồn tại, nhưng không tự kiểm tra database đích: chỉ chạy `bootRun` sau khi người vận hành xác minh process environment, JDBC/Flyway URL, profile, target, quyền truy cập và có phê duyệt migration cho đúng database. Không dựa vào `.env` hoặc giá trị mặc định của `application-local.yaml` để suy ra đích an toàn. Cung cấp password/JWT secret qua cơ chế bí mật của môi trường; không in ra log.

## Flyway và startup

Repository chứa migration `V1`–`V8`. Flyway được bật trong Spring Boot; nếu không giới hạn target, startup có thể áp dụng mọi migration còn thiếu đến V8. Trước khi startup, maintainer phải kiểm tra database đích và `flyway_schema_history`, đối chiếu checksum với source và xác định target được phép; không được suy ra rằng migration đã hoàn tất chỉ từ source code hoặc từ Hibernate. Không sửa migration đã áp dụng.

`ddl-auto=validate` chỉ yêu cầu Hibernate kiểm tra schema. Nó không vô hiệu hóa Flyway. Khởi động backend có thể kết nối database và chạy migration còn thiếu, vì vậy không xem startup là thao tác read-only.

Không dùng `flyway clean`, `baseline`, `repair`, `DROP DATABASE`, `TRUNCATE` hoặc bất kỳ thao tác ghi phá huỷ nào trong workflow này khi chưa có phê duyệt rõ ràng.

### Preflight read-only trước V6

V1 cho phép `price = 0`, còn V6 thay `ck_seat_prices_price` bằng `price > 0`; V6 sẽ thất bại nếu còn dòng không dương. Chỉ chạy các truy vấn sau trên đúng database đã được xác nhận, với kết nối read-only; không chạy V6 từ runbook này:

```sql
SELECT version, success, checksum
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT count(*) AS existing_price_count,
       count(*) FILTER (WHERE price <= 0) AS non_positive_price_count
FROM seat_prices;

SELECT id, seat_type, movie_version, price, valid_from, valid_to
FROM seat_prices
WHERE price <= 0
ORDER BY id;
```

Điều kiện trước V6: V1–V5 phải đúng history/checksum dự kiến, V6 chưa áp dụng, `non_positive_price_count = 0`, backup và quyền `btree_gist` đã được xác minh. Nếu có dòng `price <= 0`, **dừng** và xin quyết định riêng cho dữ liệu/migration; không tự sửa hoặc xóa. V6 phân loại dữ liệu cũ thành `LEGACY_UNREVIEWED` hoặc `LOCAL_FIXTURE` và đặt `INACTIVE`, không biến chúng thành giá chính thức. Vì vậy `GET /api/v1/ticket-prices` có thể trả danh sách rỗng sau V6. Chỉ ADMIN hợp lệ, theo bảng giá đã được chủ dự án phê duyệt, mới dùng Admin Price API tạo DRAFT, kiểm tra quy tắc rồi PATCH kích hoạt bằng `rowVersion` hiện tại; xác minh audit và public API sau đó. Không dùng fixture hoặc legacy làm giá approved và không tự thực hiện quy trình này khi chưa được phê duyệt cho database đích.

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
- Kiểm tra JDBC URL, profile và Flyway target trong process environment/cấu hình thực tế trước khi khởi động; checkout này không có launcher `start-local.ps1`.
- Nếu trạng thái database không chắc chắn, dừng lại và lấy backup/approval; không thử `clean`, reset hoặc seed để “sửa nhanh”.

## Ghi chú Supabase

Supabase không phải đích của local startup và không có lệnh Supabase nào trong tài liệu này. Mọi triển khai hoặc vận hành remote cần runbook, approval, backup và credential handling riêng; không suy diễn quyền thực hiện từ tài liệu local này.
