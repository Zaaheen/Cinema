# AGENTS.md — Quy chuẩn bắt buộc cho AI Coding Agent

**Dự án:** `cinema` — website đặt vé và quản lý rạp chiếu phim.  
**Hiệu lực:** Áp dụng cho mọi AI agent và mọi phiên làm việc trong repository.

## 1. Nguồn yêu cầu và thứ tự ưu tiên

Khi có mâu thuẫn, ưu tiên theo thứ tự:
1. Yêu cầu mới nhất đã được chủ dự án/nhóm xác nhận.
2. SRS đã được chấp thuận.
3. Quyết định nghiệp vụ/kiến trúc được đánh dấu `APPROVED`.
4. `docs/API_CONTRACT.md`, `docs/ERD.md`, `docs/PLAN.md` đã được duyệt.
5. Code và test hiện có.
6. Đề xuất của AI.

Không biến suy luận của AI thành yêu cầu chính thức. Nếu thiếu căn cứ, ghi `TBD — cần xác nhận`, nêu câu hỏi và không tự quyết định các quy tắc ảnh hưởng đến dữ liệu, tiền, quyền hoặc trạng thái.

## 2. Trước khi sửa code

1. Đọc file này và các `AGENTS.md` liên quan.
2. Đọc phần SRS, API contract, ERD và plan liên quan đến task.
3. Kiểm tra branch, `git status`, diff và thay đổi chưa commit; không ghi đè thay đổi có sẵn.
4. Kiểm tra cấu trúc/code pattern hiện tại.
5. Tóm tắt phạm vi, file dự kiến sửa, rủi ro và test dự kiến.
6. Nếu thiếu quyết định nghiệp vụ quan trọng, dừng và hỏi người phụ trách.

Với task audit/review, không sửa file, chạy migration phá hủy hoặc commit/push trừ khi được cho phép rõ ràng.

## 3. Giới hạn phạm vi

- Chỉ làm đúng task; không tiện tay thêm tính năng/refactor.
- Không tự đổi kiến trúc, package, endpoint, response format, schema, role, trạng thái hoặc convention đã duyệt.
- Không thêm/xóa bảng, cột, API, dependency, service hay thư mục nếu không cần cho yêu cầu đã duyệt.
- Không sửa test chỉ để làm test pass.
- Không xóa code/tài liệu của người khác nếu chưa được phép.
- Không commit, push, merge, reset, clean hoặc xóa branch/worktree nếu chưa được yêu cầu.
- Không đưa secrets, token, mật khẩu hoặc credential vào Git, log, báo cáo hay fixture.

## 4. Kiến trúc backend

Theo pattern hiện có; mặc định `Controller → Service → Repository`.

- Controller xử lý HTTP, gọi Service và trả DTO; không chứa nghiệp vụ hoặc truy vấn DB trực tiếp.
- Dùng DTO cho request/response; không trả JPA Entity trực tiếp.
- Service chứa nghiệp vụ và ranh giới transaction.
- Repository chỉ truy cập dữ liệu.
- Dùng Bean Validation và exception handler thống nhất.
- Không trả stack trace/thông tin nội bộ cho client.
- Không tạo abstraction hoặc pattern mới nếu không có lợi ích rõ ràng.

## 5. API contract

- Không đổi path, HTTP method, field, kiểu dữ liệu, pagination, error format hoặc status code đã duyệt nếu chưa được chấp thuận.
- API mới phải cập nhật contract, DTO, validation, authorization, Swagger/OpenAPI và test trong cùng thay đổi.
- Mỗi endpoint phải xác định authentication, authorization, validation, response và error cases.
- Backend phải kiểm tra quyền trên từng endpoint và tài nguyên; ẩn nút ở frontend không phải kiểm soát quyền.
- Không cho người dùng truy cập booking/dữ liệu của người khác nếu chưa có quyền rõ ràng.

## 6. Database và migration

- PostgreSQL + Supabase là lựa chọn hiện tại, với điều kiện không trái yêu cầu của giảng viên/đề bài.
- Mọi thay đổi schema phải có Flyway migration mới. Không sửa migration đã chạy trên môi trường dùng chung/đã phát hành.
- Không dùng Hibernate `create`, `create-drop` hoặc `update` để quản lý schema production.
- Không chạy migration lên DB thật/dùng chung, xóa dữ liệu hoặc reset schema khi chưa được cho phép.
- Dùng constraint ở DB để bảo vệ invariant trước race condition; check-then-insert không đủ.
- Kiểm tra PostgreSQL compatibility, index, foreign key, unique constraint và nullability.
- Seed data phải idempotent khi có thể chạy lặp, không ghi đè dữ liệu tùy chỉnh ngoài ý muốn.

## 7. Bảo mật

### Authentication và refresh token
- Không lưu token dài hạn trong `localStorage`.
- Refresh-token rotation phải bảo đảm token cũ chỉ dùng được một lần kể cả khi có request đồng thời.
- Dùng thao tác nguyên tử (conditional update/locking phù hợp); chỉ kiểm tra rồi cập nhật sau là chưa đủ.
- Khi refresh, phải kiểm tra trạng thái user hiện tại. User không còn `ACTIVE` phải bị từ chối và xử lý thu hồi token theo chính sách đã duyệt.
- Logout phải thu hồi token/session theo contract.
- Không log token, cookie, mật khẩu hoặc secret.

### Cookie, CSRF và CORS
- Không tắt CSRF toàn cục chỉ để frontend gọi API dễ hơn.
- Endpoint dùng cookie tự động gửi kèm và làm thay đổi trạng thái phải có CSRF token hoặc biện pháp tương đương đã được đánh giá.
- `SameSite=None` phải đi cùng `Secure`.
- CORS chỉ cho phép origin cụ thể; không dùng wildcard origin cùng credentials.
- CORS không thay thế CSRF.

### Authorization
- Không mặc định `ADMIN`, `MANAGER`, `STAFF` là tương đương.
- Nếu SRS ghi Admin/Manager nhưng schema/security chỉ có `ADMIN`, `STAFF`, `MEMBER`, ghi nhận mâu thuẫn và hỏi người phụ trách; không tự cấp quyền.
- Test cả trường hợp được phép và bị từ chối.

## 8. Booking, payment, ticket và điểm

- State machine booking/payment/confirm/cancel phải được ghi rõ trong contract trước khi triển khai.
- Không tự đặt thời hạn giữ ghế, thời hạn hủy, quy tắc hoàn tiền hoặc tỷ lệ điểm chưa được duyệt.
- Việc chống đặt trùng ghế phải được bảo vệ ở DB/transaction; WebSocket chỉ đồng bộ UI, DB/backend là nguồn dữ liệu có thẩm quyền.
- Xác định idempotency cho payment, confirm, ticket issuance và cộng điểm; retry không được tạo giao dịch/vé/điểm trùng.
- Nếu dùng idempotency key, server phải lưu/kiểm tra key, không chỉ nhận header rồi bỏ qua.
- Chỉ phát hành ticket khi booking đạt trạng thái đủ điều kiện theo state machine đã duyệt.
- Giao dịch điểm phải có lịch sử và cơ chế chống ghi nhận trùng.
- Nếu tỷ lệ điểm chưa chốt, đánh dấu `TBD`, cấu hình có validation và không hard-code giá trị tùy ý.

## 9. Dependency và phiên bản

- Không tự thêm/nâng cấp dependency, Spring Boot, Java hoặc plugin build ngoài phạm vi task.
- Trước khi thêm dependency, kiểm tra `pom.xml`, Java/Spring Boot version và dependency hiện có.
- Nếu có incompatibility, nêu bằng chứng và phương án đề xuất trước khi thay đổi.
- Không sao chép cấu hình tutorial có phiên bản khác mà không kiểm tra tương thích.

## 10. Kiểm thử bắt buộc theo rủi ro

Không được tuyên bố hoàn thành chỉ vì compile hoặc một context-load test pass.

Tùy task, chạy build, unit test, integration test, authorization test, PostgreSQL migration test và concurrency test. Ghi rõ lệnh và kết quả thật.

Khi sửa auth/booking, tối thiểu cần kiểm tra:
- Hai request refresh cùng token: tối đa một request thành công.
- User không ACTIVE không refresh được.
- Hai đăng ký đồng thời với thông tin định danh trùng: một thành công, request kia trả conflict theo contract, không phải 500.
- CSRF bảo vệ endpoint cookie-based có thay đổi trạng thái.
- Hai booking cùng ghế/suất: tối đa một thành công.
- Retry payment/confirm không tạo ticket hoặc cộng điểm trùng.
- User A không thể đọc/sửa booking của user B.
- Migration và constraint được kiểm tra trên PostgreSQL thật/container khi có thay đổi SQL/schema. H2 hoặc Hibernate `create-drop` không được coi là bằng chứng tương đương PostgreSQL.

Nếu không chạy được test, nêu lý do và ghi rõ phần chưa xác minh. Không che giấu test thất bại.

## 11. Tài liệu và trạng thái

Khi thay đổi API/schema/state machine/security, cập nhật tài liệu tương ứng:
- `docs/API_CONTRACT.md`
- `docs/ERD.md`
- Tài liệu quyết định nghiệp vụ/kiến trúc
- `docs/PLAN.md` nếu phạm vi hoặc mốc thay đổi

Dùng nhãn chính xác:
- `APPROVED`: đã được người có thẩm quyền xác nhận.
- `PROPOSED`: đề xuất chưa duyệt.
- `TBD`: chưa đủ thông tin.
- `IMPLEMENTED`: đã có trong code.
- `VERIFIED`: có bằng chứng test/kiểm chứng.

Không ghi `VERIFIED` chỉ vì đã đọc code hoặc compile thành công.

## 12. Báo cáo bắt buộc

Trước khi code, nêu nguồn căn cứ, phạm vi, file dự kiến sửa, quyết định còn thiếu và test dự kiến.

Khi kết thúc, báo:
1. Kết quả: hoàn thành / một phần / bị chặn.
2. Files changed và mục đích.
3. Thay đổi API/schema/behavior.
4. Lệnh test đã chạy và kết quả thật.
5. Phần chưa kiểm chứng.
6. Rủi ro/TBD cần quyết định.
7. Git status; nêu rõ nếu chưa commit/push.

Không khẳng định test pass, migration đã chạy hoặc bảo mật đã được xác minh nếu không có bằng chứng.

## 13. Nhiệm vụ đầu tiên sau khi đọc file này

Không sửa code ngay. Trước tiên:
1. Đọc tài liệu và kiểm tra `git status`.
2. Đối chiếu SRS, `API_CONTRACT.md`, `ERD.md` với code hiện tại.
3. Báo các mâu thuẫn và kế hoạch sửa theo ưu tiên, gồm:
   - refresh-token rotation race condition;
   - refresh của tài khoản không ACTIVE;
   - CSRF/CORS/cookie;
   - đăng ký trùng đồng thời trả 500;
   - migration/constraint chưa test bằng PostgreSQL;
   - role ADMIN/MANAGER/STAFF không thống nhất;
   - booking/payment/confirm/idempotency/state machine chưa được duyệt.
4. Chờ xác nhận trước khi tự quyết định nghiệp vụ chưa được duyệt.

**Nguyên tắc cuối cùng:** Ưu tiên đúng đắn, an toàn và nhất quán hơn tốc độ sinh code. Khi thiếu bằng chứng, nói rõ chưa xác minh; không suy diễn và không tuyên bố quá mức.
