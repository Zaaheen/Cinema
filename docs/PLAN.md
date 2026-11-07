# Kế hoạch triển khai — 4 Sprint chính thức

## 1. Căn cứ và quyết định đã chốt

- **4 sprint trong `03. [CGDN]_Project_Plan.xlsx` là kế hoạch chính thức.** Bảng 8 giai đoạn trước đây chỉ là phân rã nội bộ và được ánh xạ vào 4 sprint ở mục 5.
- Database: **APPROVED nội bộ (31/10/2026)** — Supabase PostgreSQL dùng chung cho development; vẫn phải tuân thủ ràng buộc của giảng viên/đề bài nếu có.
- Auth: JWT access token ngắn hạn + refresh token lưu trong cookie HttpOnly; refresh có rotation và thu hồi khi logout.
- Ghế: `seat_type` (`STANDARD`, `VIP`) tách khỏi trạng thái khả dụng.
- Vé: bảng `tickets` riêng, mỗi ghế đã xác nhận sinh một vé.
- Giữ ghế 5 phút; hết hạn tự giải phóng; database chặn đặt trùng ghế trong cùng suất chiếu.
- Thanh toán mô phỏng; chỉ cộng điểm sau khi thanh toán thành công.
- Tỷ lệ cộng/đổi điểm, thời hạn hủy và chính sách hoàn tiền vẫn chờ nhóm chốt.

## 2. Timeline

| Sprint | Thời gian | Coding | Merge | Review | Mục tiêu chính |
|---|---|---|---|---|---|
| Sprint 0 | 29/10 – 08/11/2026 | 29/10 – 06/11 | 07/11 | 08/11 | Chốt thiết kế, dựng skeleton chạy được |
| Sprint 1 | 10/11 – 20/11/2026 | 10/11 – 15/11 | 17/11 – 18/11 | 19/11 – 20/11 | Auth, phân quyền, dữ liệu công khai |
| Sprint 2 | 21/11 – 02/12/2026 | 21/11 – 27/11 | 28/11 – 29/11 | 01/12 – 02/12 | Phim, phòng/ghế, suất chiếu, giá vé |
| Sprint 3 | 03/12 – 13/12/2026 | 03/12 – 09/12 | 10/12 – 11/12 | 12/12 – 13/12 | Đặt vé end-to-end, giữ ghế, thanh toán mô phỏng |
| Sprint 4 | 15/12 – 25/12/2026 | 15/12 – 20/12 | 22/12 – 23/12 | 24/12 – 25/12 | Nghiệp vụ nội bộ, điểm, báo cáo, deploy |

## 3. Deliverables theo sprint

### Sprint 0 — Nền tảng (đang thực hiện)
- [x] Cấu trúc monorepo `backend/`, `frontend/`, `docs/`.
- [x] Spring Boot 4.1.2 / Java 21 / Gradle wrapper; React 19 / Vite 8 / JavaScript + JSX.
- [x] Flyway `V1__init_schema.sql` + seed role.
- [x] Profile `local`, `prod`, `test`; datasource qua biến môi trường.
- [x] Response envelope, global exception handler, Swagger.
- [x] JWT access token + refresh cookie luân chuyển; CORS; BCrypt.
- [x] Test context H2; Docker Compose PostgreSQL tùy chọn; Gradle Dockerfile.
- [x] VERIFIED (local, 31/10/2026): Flyway V1/V2 chạy trên PostgreSQL 16 với database rỗng; seed đủ role nền tảng.
- [ ] Closeout Sprint 1: cần chạy lại Flyway V1/V2/V3 trên PostgreSQL local/container rỗng và xác minh refresh rotation cạnh tranh trên PostgreSQL. Không được coi H2 là bằng chứng tương đương.
- [x] VERIFIED (local, 31/10/2026): Gradle backend test và frontend lint/build chạy thành công.
- [x] VERIFIED (GitHub Actions, 31/10/2026): CI Gradle workflow chạy thành công cho commit khởi tạo.
- [x] Frontend router, axios refresh interceptor, AuthContext, layout/pages.
- [x] Commit khởi tạo đã được push lên GitHub (`b286a05`).
- [x] APPROVED nội bộ: dùng Supabase PostgreSQL cho development dùng chung; Docker là tùy chọn.
- [ ] Tạo Supabase project, phân quyền thành viên và chạy/kiểm chứng Flyway trên database Supabase rỗng.

### Sprint 1 — Auth và dữ liệu công khai (10/11 – 20/11)
- [x] Hoàn thiện đăng ký/đăng nhập/đăng xuất/làm mới token; phân quyền `ADMIN`, `EMPLOYEE`, `MEMBER`.
- [x] Đổi mật khẩu, cập nhật hồ sơ thành viên.
- [x] API phim đang chiếu/sắp chiếu, tìm kiếm, chi tiết phim, khuyến mãi, giá vé.
- [x] Trang đăng nhập/đăng ký, danh sách phim, chi tiết phim; loading/empty/error.
- [x] Test tích hợp auth và catalog. Seed phim demo là tùy chọn; frontend hiển thị trạng thái rỗng khi chưa có dữ liệu.

### Sprint 2 — Phòng, ghế, suất chiếu (21/11 – 02/12)
- CRUD phim, phòng chiếu, cấu hình ghế, suất chiếu (Admin).
- Chống trùng lịch phòng; kiểm tra thời lượng phim và khoảng nghỉ.
- API lịch chiếu theo phim/ngày, sơ đồ ghế theo suất chiếu.
- UI quản trị phim/phòng/suất chiếu; UI chọn suất và xem sơ đồ ghế.
- Test ràng buộc lịch phòng và quyền Admin.

### Sprint 3 — Đặt vé end-to-end (03/12 – 13/12)
- Tạo đơn, giữ ghế 5 phút, hết hạn giải phóng; transaction và khóa chống đặt trùng.
- Cập nhật trạng thái ghế realtime (WebSocket) cho các client đang xem sơ đồ.
- Thanh toán mô phỏng, xác nhận đơn, phát hành vé.
- UI chọn ghế, đếm ngược giữ chỗ, xác nhận thanh toán, chi tiết đơn.
- Test hai request tranh cùng một ghế: chỉ một đơn thắng.

### Sprint 4 — Nghiệp vụ nội bộ và phát hành (15/12 – 25/12)
- Bán vé tại quầy, tra cứu thành viên, quản lý đặt vé (EMPLOYEE).
- Quản lý thành viên, nhân viên, khuyến mãi, thống kê doanh thu (Admin).
- Cộng điểm sau thanh toán thành công, lịch sử điểm, chống cộng trùng.
- Hardening: validation, phân quyền, responsive, xử lý lỗi, test tích hợp.
- Deploy frontend Cloudflare Pages, backend Cloud Run, Supabase; smoke test và README.

## 4. Phân công của Thuận (theo `0.1 Task.xlsx`)

| Sprint | Mã chức năng | Nội dung |
|---|---|---|
| Sprint 1 | 6.1, 6.3 | Đăng nhập, phân quyền; đăng ký thành viên |
| Sprint 2 | 6.5.1, 6.5.2, 6.5.3 | Xem danh sách phim, tìm kiếm, chi tiết phim |
| Sprint 3 | 6.2.1 → 6.2.4 | Chọn suất/ghế, đặt vé, giữ ghế, cập nhật trạng thái ghế |
| Sprint 4 | 6.4.1 → 6.4.3 | Quản lý vé tài khoản, tài khoản, lịch sử điểm |

Các thành viên còn lại bám theo sheet phân công trong `0.1 Task.xlsx`; mỗi task phải có owner duy nhất và acceptance criteria.

## 5. Ánh xạ 8 giai đoạn nội bộ vào 4 sprint

| Giai đoạn nội bộ cũ | Sprint chính thức |
|---|---|
| Sprint 1 — Nền tảng | Sprint 0 |
| Sprint 2 — Auth & dữ liệu công khai | Sprint 1 |
| Sprint 3 — Phòng/ghế/suất chiếu | Sprint 2 |
| Sprint 4 — Booking end-to-end | Sprint 3 |
| Sprint 5 — Nghiệp vụ nội bộ | Sprint 4 |
| Sprint 6 — Admin, báo cáo, hardening | Sprint 4 |
| Sprint 7 — Deploy/demo | Sprint 4 (cuối sprint) |

## 6. Definition of Done

- Build backend và frontend sạch; CI xanh.
- API có validation, phân quyền và response đúng `docs/API_CONTRACT.md`.
- Có test cho luồng chính và ràng buộc quan trọng (đặc biệt đặt trùng ghế).
- Không commit secret; biến môi trường có trong `.env.example`.
- PR nhỏ, review chéo trước khi merge; không merge khi test fail.

## 7. Phụ thuộc và rủi ro

- Booking phụ thuộc auth, phim, phòng/ghế, suất chiếu và giá vé.
- Chống trùng ghế phải hoàn tất ở Sprint 3, không vá ở cuối.
- Điểm/khuyến mãi phụ thuộc tỷ lệ và chính sách hủy do nhóm chốt.
- Refresh cookie cross-site (Pages ↔ Cloud Run) cần `SameSite=None; Secure`, CORS credentials; nên cân nhắc subdomain chung hoặc proxy.
- Deploy phụ thuộc quyền dùng Supabase và biến môi trường trên Cloud Run.

## 8. Phạm vi MVP

1. Xem/tìm kiếm phim, chi tiết và lịch chiếu.
2. Đăng ký/đăng nhập thành viên.
3. Chọn ghế và đặt vé có chống trùng.
4. Xem vé/đơn của tài khoản.
5. Admin quản lý phim, phòng/ghế, suất chiếu.
6. Thanh toán mô phỏng và phát hành vé.
7. Deploy online và smoke test.

Điểm tích lũy, khuyến mãi nâng cao, quản lý nhân sự đầy đủ và báo cáo có thể giản lược nếu hết thời gian, nhưng không được bỏ âm thầm nếu rubric yêu cầu.
