# Deployment — Cloudflare Pages + Cloud Run + Supabase

## 1. Kiến trúc mục tiêu đã thống nhất
```text
Browser
  └── React + Vite + JavaScript/JSX
        └── Cloudflare Pages (HTTPS)
              └── HTTPS API request
                    └── Spring Boot / Java / Gradle / Docker
                          └── Google Cloud Run
                                └── Supabase PostgreSQL
```

Mục tiêu: URL truy cập ổn định khi laptop tắt, chi phí thấp cho đồ án/demo. Đây không phải cam kết rằng mọi dịch vụ sẽ luôn miễn phí.

## 2. Điều kiện cần xác nhận trước triển khai
- **Database:** chỉ dùng Supabase PostgreSQL nếu giảng viên/đề bài không bắt buộc MySQL/SQL Server. Không chuyển DB chỉ vì tiện deploy.
- Xác nhận Java 21 có tương thích môi trường môn học; nếu bắt buộc Java 17, điều chỉnh build/runtime.
- Chọn auth (JWT hay session cookie) và cấu hình CORS/credentials phù hợp.
- Chọn nơi lưu poster/ảnh; không lưu ảnh tải lên trực tiếp trong filesystem ephemeral của Cloud Run.
- Không dùng database production làm môi trường test phá dữ liệu.

## 3. Frontend — Cloudflare Pages
- Kết nối GitHub repository và chọn thư mục frontend.
- Build command thường dùng: `npm ci && npm run build` (điều chỉnh theo package manager thực tế).
- Output directory: `dist`.
- Cấu hình biến môi trường build, ví dụ `VITE_API_BASE_URL=https://<cloud-run-service-url>/api/v1`.
- Không đặt secret trong biến `VITE_*`; các giá trị này có thể được đóng gói vào mã frontend.
- Cấu hình SPA fallback để route React Router tải lại không trả 404.
- Cấu hình CORS ở backend chỉ cho phép domain Pages production và domain preview cần thiết.

## 4. Backend — Google Cloud Run
- Build Docker image từ backend; chạy Spring Boot trên cổng do môi trường cung cấp (`PORT`).
- Service stateless; không phụ thuộc file local bền vững.
- Đặt service account quyền tối thiểu; secrets/DB credentials trong Secret Manager hoặc cơ chế secret tương đương.
- Cấu hình biến môi trường tối thiểu:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD` (secret)
  - `JWT_SECRET` (secret, nếu dùng JWT)
  - `CORS_ALLOWED_ORIGINS`
- Chạy migration Flyway theo quy trình kiểm soát trước/đúng lúc deploy; không để nhiều revision đồng thời tự chạy migration không kiểm soát.
- Cấu hình health/readiness endpoint; kiểm tra startup timeout, memory và CPU.
- Cloud Run có thể scale-to-zero tùy cấu hình, nên request đầu có thể cold start. Cần kiểm tra giá và hạn mức hiện hành trước khi công khai demo.

## 5. Database — Supabase PostgreSQL
- Tạo project và lưu connection details ở secret store, không commit vào repo.
- Dùng connection string/SSL theo hướng dẫn hiện hành của Supabase; chọn direct hoặc pooler phù hợp môi trường kết nối.
- Chạy Flyway migrations trên database mới và kiểm tra schema.
- Tạo dữ liệu demo không nhạy cảm.
- Xác nhận giới hạn gói Free, quy tắc pause do không hoạt động, backup/restore và giới hạn kết nối tại thời điểm triển khai.
- Dùng database riêng cho local/test và production/demo.

## 6. CI/CD tối thiểu
1. Pull request: build frontend, test backend, kiểm tra migration/test phù hợp.
2. Merge branch release: deploy backend và frontend.
3. Smoke test: trang chủ, danh sách phim, login, lịch chiếu, seat map, booking demo, endpoint health.
4. Nếu lỗi: quay lại revision frontend/backend trước; migration phải tương thích ngược hoặc có kế hoạch khôi phục riêng.
5. Không tự động chạy test phá hủy dữ liệu trên production.

## 7. Bảo mật và chi phí
- `.env`, service account keys, DB password, JWT secret và provider credentials phải nằm trong `.gitignore`.
- Commit `.env.example` chỉ có placeholder.
- Bật HTTPS; không cho phép CORS wildcard khi dùng credential.
- Không bật tài khoản admin mặc định với mật khẩu dễ đoán trên production.
- Free tiers có thể thay đổi, giới hạn hoặc yêu cầu billing account; xác nhận bảng giá/quota hiện hành trước khi tạo dịch vụ.
- Cloud Run/Cloudflare/Supabase không thay thế việc kiểm tra backup, quyền truy cập và bảo mật ứng dụng.

## 8. Checklist trước demo
- [ ] Database engine được giảng viên chấp thuận.
- [ ] Migration Flyway chạy trên database rỗng.
- [ ] Secrets không có trong Git history/repository.
- [ ] CORS chỉ cho domain frontend cần thiết.
- [ ] Đăng nhập và phân quyền đúng.
- [ ] Hai client đặt cùng ghế không thể tạo hai booking hợp lệ.
- [ ] URL frontend vẫn truy cập khi laptop tắt.
- [ ] Kiểm tra cold start, lỗi DB/timeout và trang lỗi.
- [ ] Có dữ liệu demo và tài khoản demo không chứa dữ liệu cá nhân thật.
- [ ] Có phương án rollback backend/frontend và backup dữ liệu.
