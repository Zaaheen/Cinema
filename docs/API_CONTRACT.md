# API Contract — REST v0

## 1. Quy ước chung
- Base path: `/api/v1`
- JSON UTF-8; ngày giờ dạng ISO-8601; tiền dạng decimal.
- Phản hồi thành công đề xuất:
```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "timestamp": "2026-01-01T10:00:00+07:00"
}
```
- Phản hồi lỗi đề xuất:
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errors": [{"field": "email", "message": "Email không hợp lệ"}],
  "timestamp": "2026-01-01T10:00:00+07:00"
}
```
- Mã trạng thái: `200` đọc/cập nhật thành công; `201` tạo mới; `204` xóa thành công không body; `400` validation/nghiệp vụ sai; `401` chưa xác thực; `403` không đủ quyền; `404` không tìm thấy; `409` xung đột trạng thái/ghế; `500` lỗi ngoài dự kiến.
- Endpoint dưới đây là đề xuất thiết kế, cần khớp với ERD và quyết định nghiệp vụ đã duyệt.

## 2. Xác thực và tài khoản
| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| POST | `/auth/register` | Public | Đăng ký thành viên |
| POST | `/auth/login` | Public | Đăng nhập, trả access token và set refresh cookie |
| POST | `/auth/refresh` | Public (cookie) | Làm mới access token bằng refresh cookie |
| POST | `/auth/logout` | Public (cookie) | Thu hồi refresh token và xóa cookie |
| GET | `/auth/csrf` | Public | Bootstrap CSRF cookie and response token for refresh/logout |
| GET | `/auth/me` | Auth | Thông tin tài khoản đang đăng nhập |
| GET | `/members/me` | Member | Xem hồ sơ |
| PATCH | `/members/me` | Member | Cập nhật hồ sơ |
| POST | `/members/me/change-password` | Member | Đổi mật khẩu |
| GET | `/members/me/point-transactions?from=&to=&type=` | Member | Xem lịch sử điểm |
| GET | `/members/me/bookings?status=&page=&size=` | Member | Xem vé/đơn của mình |

**Cơ chế đã chốt:** JWT HS256 cho access token (mặc định 15 phút, giữ trong bộ nhớ frontend); refresh token opaque (mặc định 7 ngày) lưu hash trong bảng `refresh_tokens` và gửi qua cookie `HttpOnly`, `SameSite=Lax` khi cùng site hoặc `SameSite=None; Secure` khi cross-site. Mỗi lần refresh sẽ thu hồi token cũ và phát token mới (rotation); logout thu hồi token hiện tại. Backend bật CORS credentials cho đúng origin frontend.

**CSRF for cookie refresh/logout:** call `GET /auth/csrf` with credentials first. It sets the `XSRF-TOKEN` cookie and returns `{ "token": "..." }` in `data`; send that response value as the `X-XSRF-TOKEN` header on `POST /auth/refresh` and `POST /auth/logout`. This allows a frontend on a different allowed origin to bootstrap CSRF without reading an API-origin cookie. The browser still sends the cookie with credentials and Spring Security requires the cookie/header match. The CSRF cookie uses the same `SameSite` and `Secure` settings as the refresh cookie, including `SameSite=None; Secure` for cross-site deployment.

**Cập nhật hồ sơ:** `PATCH /members/me` nhận `fullName`, `email` và `phone`. `POST /members/me/change-password` nhận `currentPassword` và `newPassword`; mật khẩu mới phải từ 8 đến 72 ký tự, khác mật khẩu hiện tại. Đổi mật khẩu thu hồi toàn bộ refresh token và yêu cầu đăng nhập lại.

## 3. Dữ liệu công khai
| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| GET | `/movies?status=&q=&page=&size=` | Public | Danh sách/tìm kiếm phim |
| GET | `/home` | Public | Dữ liệu banner, phim và suất chiếu cho trang chủ |
| GET | `/movies/{movieId}` | Public | Chi tiết phim |
| GET | `/movies/{movieId}/showtimes?date=` | Public | Suất chiếu theo phim/ngày |
| GET | `/showtimes/{showtimeId}/seat-map` | Public | Sơ đồ ghế chỉ đọc và trạng thái khả dụng |
| GET | `/promotions?active=true` | Public | Khuyến mãi công khai |
| GET | `/promotions/{promotionId}` | Public | Chi tiết khuyến mãi công khai |
| GET | `/ticket-prices?date=YYYY-MM-DD` | Public | Giá vé đã phê duyệt, hiệu lực theo ngày |

**Hành vi catalog đã triển khai:** `GET /movies` trả `PageResponse` với `content`, `page`, `size`, `totalElements`, `totalPages`; chỉ công bố `NOW_SHOWING`/`COMING_SOON`, `q` tìm không phân biệt hoa/thường theo tiêu đề và `status` chỉ lọc trong hai trạng thái công khai đó. Chi tiết phim không tồn tại trả `404`. `GET /promotions` và `GET /promotions/{promotionId}` chỉ công bố promotion `ACTIVE` trong khoảng hiệu lực; mục không công khai trả `404`. `GET /ticket-prices?date=` chỉ công bố rule `ADMIN_APPROVED` + `ACTIVE` hiệu lực ở ngày yêu cầu (hoặc hôm nay theo `Asia/Ho_Chi_Minh`), không tự suy diễn tổng tiền booking. `GET /showtimes/{showtimeId}/seat-map` không ghi dữ liệu và không tiết lộ người đang giữ ghế.

**Quy tắc lịch chiếu — APPROVED:** `GET /movies/{movieId}/showtimes?date=YYYY-MM-DD` dùng múi giờ nghiệp vụ `Asia/Ho_Chi_Minh` và chỉ công bố movie có trạng thái `NOW_SHOWING` hoặc `COMING_SOON`; movie không tồn tại hoặc không công khai trả `404`. Ngày đã qua trả danh sách rỗng. Với hôm nay và ngày tương lai, API chỉ trả showtime có `status=SCHEDULED` còn mở bán online; một suất còn mở bán khi `now < startsAt - 30 phút`. Không giới hạn cứng thời gian mở lịch: ngày chưa có lịch chiếu trả danh sách rỗng. Mỗi phần tử trả `id`, `startsAt`, `endsAt`, `roomName`, `status`.

## 4. Đặt vé và thanh toán
| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| POST | `/bookings` | Member | Tạo đơn và giữ ghế tạm trong 5 phút |
| GET | `/bookings/{bookingId}` | Chủ đơn hoặc EMPLOYEE/ADMIN | Xem chi tiết đơn |
| POST | `/bookings/{bookingId}/confirm` | Member hoặc luồng hệ thống | Xác nhận đơn sau khi thanh toán mô phỏng thành công |
| POST | `/bookings/{bookingId}/cancel` | Chủ đơn nếu đủ điều kiện; EMPLOYEE/ADMIN theo quyền | Hủy đơn theo chính sách |
| GET | `/bookings/{bookingId}/tickets` | Chủ đơn hoặc EMPLOYEE/ADMIN | Xem vé phát hành |
| POST | `/bookings/{bookingId}/payments` | Chủ đơn/EMPLOYEE | Ghi nhận kết quả thanh toán mô phỏng |

**Quy tắc đã chốt:** giữ ghế 5 phút; thanh toán mô phỏng; chỉ cộng điểm sau khi thanh toán thành công; backend/database chống đặt trùng ghế theo suất chiếu. Khi hết hạn giữ ghế mà chưa thanh toán thành công, đơn hết hạn và ghế được giải phóng. Backend phải kiểm tra lại điều kiện mở bán online trong transaction khi tạo booking/giữ ghế: showtime phải là `SCHEDULED` và phải còn hơn 30 phút trước `startsAt`; thời gian giữ ghế 5 phút không được vượt quá thời điểm đóng bán.

**TBD trước khi implement:** request schema, idempotency key, chi tiết state machine, tỷ lệ cộng/đổi điểm, thời hạn hủy cụ thể và chính sách hoàn tiền/hoàn điểm. Không triển khai hoàn tiền thật hoặc tự đặt ra thời hạn hủy.

### Payload khởi tạo đơn — ví dụ đề xuất
```json
{
  "showtimeId": 123,
  "seatIds": [501, 502],
  "promotionCode": null,
  "pointsToUse": 0
}
```
Backend phải tự xác thực giá, khuyến mãi, điểm và tình trạng ghế; không tin giá/tổng tiền gửi từ frontend.

## 5. Quản trị — EMPLOYEE/ADMIN
| Method | Endpoint | Quyền đề xuất | Mục đích |
|---|---|---|---|
| GET/POST | `/admin/movies` | ADMIN | Danh sách/tạo phim |
| GET/PATCH | `/admin/movies/{id}` | ADMIN | Xem/sửa phim |
| POST | `/admin/movies/{id}/poster` | ADMIN | Upload poster PNG, JPG/JPEG hoặc WebP (tối đa 5 MiB) |
| POST | `/admin/movies/{id}/banner` | ADMIN | Upload banner ngang PNG, JPG/JPEG hoặc WebP vào `movies.backdrop_url` |
| PATCH | `/admin/movies/{id}/trailer` | ADMIN | Cập nhật/xóa URL YouTube hợp lệ |
| GET | `/admin/genres` | ADMIN | Danh sách genre canonical selectable |
| GET | `/admin/rooms` | ADMIN | Danh sách phòng chiếu thật, gồm `id`, `name`, `status` (kể cả phòng không ACTIVE), sắp theo `name,id`; không tạo hoặc sửa dữ liệu |
| GET/POST | `/admin/showtimes` | ADMIN | Danh sách/tạo suất chiếu |
| GET/PATCH | `/admin/showtimes/{id}` | ADMIN | Xem/sửa suất chiếu tương lai không có lịch sử booking |
| POST | `/admin/showtimes/{id}/cancel` | ADMIN | Hủy suất chiếu đủ điều kiện |
| GET/POST | `/admin/promotions` | ADMIN | Danh sách/tạo khuyến mãi |
| GET/PATCH | `/admin/promotions/{id}` | ADMIN | Xem chi tiết/cập nhật khuyến mãi |
| GET | `/staff/members?q=` | EMPLOYEE/Admin | Tra cứu thành viên |
| GET | `/staff/bookings?q=&status=&page=&size=` | EMPLOYEE/Admin | Tra cứu đơn đặt |
| POST | `/staff/sales` | EMPLOYEE/Admin | Bán vé tại quầy |
| GET/POST/PATCH | `/admin/staff` và `/admin/staff/{id}` | TBD | Chưa xác nhận contract quản lý nhân viên |
| GET | `/admin/reports/revenue?from=&to=` | TBD | Chưa xác nhận contract báo cáo |
| GET | `/admin/reports/tickets?from=&to=` | TBD | Chưa xác nhận contract báo cáo |

**Admin Room List — IMPLEMENTED:** `GET /api/v1/admin/rooms` trả `ApiResponse<List<AdminRoomResponse>>`, mỗi mục có `id`, `name`, `status`. Danh sách rỗng trả `200` với `data: []`. Chỉ ADMIN được truy cập; anonymous trả `401`, MEMBER/EMPLOYEE trả `403`. Client chỉ cho chọn phòng `ACTIVE` khi tạo/sửa lịch chiếu; backend vẫn xác thực trạng thái phòng khi ghi.

**Ảnh poster/banner phim — IMPLEMENTED:** `POST /api/v1/admin/movies/{id}/poster` và `/banner` nhận `multipart/form-data` với một phần `file`. Chỉ ADMIN; anonymous `401`, role khác `403`, phim không tồn tại `404`. Chấp nhận PNG, JPG/JPEG hoặc WebP với tên và MIME khớp định dạng, kiểm tra chữ ký và cấu trúc ảnh, tối đa 5 MiB, kích thước tối đa 4096 × 4096 và 12 triệu pixel; banner phải là ảnh ngang có tỉ lệ rộng:cao tối thiểu 7:5. PNG/JPEG được giải mã tại backend; WebP được kiểm tra container, frame và kích thước rồi dịch vụ lưu trữ kiểm tra khả năng giải mã. Sai dữ liệu trả `400`. Cloudinary chưa cấu hình trả `503`, lỗi lưu trữ trả `502`. Thành công trả `201` và `ApiResponse<{movieId, posterUrl}>` hoặc `ApiResponse<{movieId, backdropUrl}>`. Banner được lưu ở `movies.backdrop_url`, thay thế banner cũ của phim; `GET /home` trả `backdropUrl` trong phim đang/sắp chiếu. Không thêm hàng vào bảng `banners`; bảng đó dành cho banner nội dung độc lập.

**Trạng thái phim — IMPLEMENTED:** `PATCH /api/v1/admin/movies/{id}` cho phép ADMIN gửi `{"status":"COMING_SOON"}`, `{"status":"NOW_SHOWING"}` hoặc `{"status":"STOPPED"}`; có thể chuyển giữa ba trạng thái theo cả hai chiều. Trường bị bỏ qua giữ nguyên giá trị cũ; `null`, kiểu khác chuỗi hoặc trạng thái khác ba giá trị trên trả `400`. Phim không tồn tại trả `404`; anonymous `401`, MEMBER/EMPLOYEE `403`. Thành công trả `200` với `AdminMovieResponse.status` mới. `STOPPED` loại phim khỏi danh sách phim công khai và các nhóm phim trang chủ, đồng thời làm suất chiếu không mở bán trong sơ đồ ghế. Cập nhật trạng thái phim không đổi trạng thái của suất chiếu đã tạo; nếu cần hủy suất, quản trị viên dùng thao tác hủy suất riêng. API chi tiết phim theo ID hiện vẫn trả phim `STOPPED` nếu ID tồn tại (hành vi hiện có).

**Admin Seat Price PATCH — IMPLEMENTED:** `PATCH /api/v1/admin/seat-prices/{id}` chỉ cho cập nhật quy tắc `origin=ADMIN_APPROVED` với `rowVersion` hợp lệ. ID không tồn tại hoặc thuộc `LEGACY_UNREVIEWED`/`LOCAL_FIXTURE` trả `404`; version cũ trả `409 PRICE_RULE_CHANGED`. Legacy/fixture không được kích hoạt hay chuyển nguồn gốc qua API này. Quy tắc ACTIVE được revise bằng successor và audit hiện có.

**Admin Showtime conflicts — IMPLEMENTED:** Các thao tác tạo/sửa/hủy chỉ dành cho ADMIN. `POST /api/v1/admin/showtimes` và `PATCH /api/v1/admin/showtimes/{id}` trả `409 SHOWTIME_ROOM_CONFLICT` khi vi phạm chính xác exclusion constraint V8. `PATCH`/`POST .../cancel` trả `409 SHOWTIME_NOT_EDITABLE` nếu suất không còn là `SCHEDULED` tương lai, hoặc `409 SHOWTIME_HAS_BOOKING_HISTORY` nếu đã có lịch sử đặt vé. Backend tính khoảng chuẩn bị phòng 15 phút; client không tự áp dụng thêm buffer.

**Lưu ý:** các route quản trị ở đây là bản nháp; phân quyền và CRUD cụ thể phải theo SRS đã được nhóm thống nhất.

**Quản trị khuyến mãi — APPROVED:** chỉ `ADMIN` được gọi các endpoint trên. Request tạo/cập nhật nhận `title`, `description` (nullable), `discountType` (`PERCENT` hoặc `FIXED`), `discountValue`, `startsAt`, `endsAt`, `status` (nullable; khi tạo mặc định `ACTIVE`). `discountValue` phải lớn hơn 0; nếu `discountType=PERCENT` thì không được vượt 100; `endsAt` phải sau `startsAt`. Response trả `id`, các trường request và `status`. Tạo mới trả `201`; đọc/cập nhật thành công trả `200`; request không hợp lệ trả `400`; ID không tồn tại trả `404`. Không có endpoint xóa cứng khuyến mãi.

## 6. Quy tắc backend
- Validate request bằng Bean Validation và kiểm tra nghiệp vụ ở Service.
- Controller chỉ nhận/trả DTO; không trả entity JPA trực tiếp.
- Dùng transaction cho tạo booking, chiếm ghế, ghi payment và giao dịch điểm.
- Giữ ghế tối đa 5 phút; cần cơ chế xử lý đơn hết hạn để giải phóng ghế an toàn.
- Chỉ ghi giao dịch cộng điểm sau khi trạng thái thanh toán mô phỏng đã thành công; thao tác phải idempotent để tránh cộng điểm hai lần.
- Xử lý xung đột ghế bằng `409 Conflict`; frontend phải refresh sơ đồ ghế.
- Phân quyền cả ở route và service cho hành động nhạy cảm.
- Không trả password hash, secret, stack trace hoặc thông tin provider nhạy cảm.
- Bổ sung OpenAPI/Swagger; API contract phải được cập nhật khi thay đổi.
