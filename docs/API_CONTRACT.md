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
| GET | `/movies/{movieId}` | Public | Chi tiết phim |
| GET | `/movies/{movieId}/showtimes?date=` | Public | Suất chiếu theo phim/ngày |
| GET | `/showtimes/{showtimeId}/seats` | Public | Sơ đồ ghế và tình trạng ghế |
| GET | `/promotions?active=true` | Public | Khuyến mãi công khai |
| GET | `/ticket-prices` | Public | Giá vé đang áp dụng |

**Hành vi catalog đã triển khai:** `GET /movies` trả `PageResponse` với `content`, `page`, `size`, `totalElements`, `totalPages`; chỉ công bố `NOW_SHOWING`/`COMING_SOON`, `q` tìm không phân biệt hoa/thường theo tiêu đề và `status` chỉ lọc trong hai trạng thái công khai đó. Chi tiết phim không tồn tại trả `404`. `GET /promotions` chỉ trả promotion có `status=ACTIVE` trong khoảng thời gian hiệu lực hiện tại. `GET /ticket-prices` chỉ trả bảng giá có hiệu lực trong ngày hiện tại; API chỉ công bố giá, không tự suy diễn quy tắc tính giá/khuyến mãi khi đặt vé.

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
| GET/PATCH/DELETE | `/admin/movies/{id}` | ADMIN | Xem/sửa/ngừng phim |
| GET/POST | `/admin/rooms` | Admin/Manager | Danh sách/tạo phòng |
| PATCH | `/admin/rooms/{id}/seats` | Admin/Manager | Cập nhật sơ đồ/loại ghế |
| GET/POST | `/admin/showtimes` | Role được duyệt | Danh sách/tạo suất chiếu |
| PATCH/DELETE | `/admin/showtimes/{id}` | Role được duyệt | Sửa/ngừng suất chiếu |
| GET/POST | `/admin/promotions` | ADMIN | Danh sách/tạo khuyến mãi |
| GET/PATCH | `/admin/promotions/{id}` | ADMIN | Xem chi tiết/cập nhật khuyến mãi |
| GET | `/staff/members?q=` | EMPLOYEE/Admin | Tra cứu thành viên |
| GET | `/staff/bookings?q=&status=&page=&size=` | EMPLOYEE/Admin | Tra cứu đơn đặt |
| POST | `/staff/sales` | EMPLOYEE/Admin | Bán vé tại quầy |
| GET/POST/PATCH | `/admin/staff` và `/admin/staff/{id}` | Admin/Manager | Quản lý nhân viên |
| GET | `/admin/reports/revenue?from=&to=` | Admin/Manager | Báo cáo doanh thu |
| GET | `/admin/reports/tickets?from=&to=` | Admin/Manager | Báo cáo số vé |

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
