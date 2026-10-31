# ERD — Mô hình dữ liệu đề xuất

## 1. Trạng thái
Đây là **mô hình khởi đầu**, chưa phải schema cuối cùng. SRS mô tả nghiệp vụ và màn hình nhưng không đủ để kết luận tất cả kiểu dữ liệu, cardinality và quy tắc giao dịch. Database dự kiến là Supabase PostgreSQL nếu đề bài không bắt buộc hệ quản trị khác.

## 2. Các thực thể đề xuất
| Bảng | Trường chính đề xuất | Mục đích |
|---|---|---|
| `users` | `id`, `username`, `password_hash`, `full_name`, `email`, `phone`, `status`, `role_id`, `created_at`, `updated_at` | Tài khoản đăng nhập dùng chung; role phải phản ánh phân quyền đã duyệt |
| `roles` | `id`, `code`, `name` | Vai trò: khách thành viên, nhân viên, quản lý/Admin; cách mô hình khách chưa đăng nhập cần thiết kế riêng |
| `members` | `id`, `user_id`, `member_code`, `birth_date`, `gender`, `address`, `identity_number` | Hồ sơ thành viên và thông tin tài khoản theo SRS |
| `point_transactions` | `id`, `member_id`, `booking_id` nullable, `type`, `points_delta`, `description`, `created_at` | Sổ giao dịch cộng/sử dụng điểm; không chỉ lưu mỗi tổng điểm |
| `movies` | `id`, `title`, `genre`, `release_date`, `duration_minutes`, `version`, `cast_text`, `director`, `studio`, `synopsis`, `trailer_url`, `poster_url`, `status` | Danh mục phim và chi tiết phim |
| `rooms` | `id`, `name`, `status` | Phòng chiếu |
| `seats` | `id`, `room_id`, `row_label`, `seat_number`, `seat_type`, `status` | Sơ đồ ghế; unique `(room_id, row_label, seat_number)` |
| `showtimes` | `id`, `movie_id`, `room_id`, `starts_at`, `ends_at`, `status` | Suất chiếu cụ thể của phim tại phòng |
| `seat_prices` | `id`, `seat_type`, `movie_version` nullable, `price`, `valid_from`, `valid_to` nullable | Giá vé; quy tắc áp dụng giá cần xác nhận |
| `bookings` | `id`, `member_id`, `showtime_id`, `booking_code`, `status`, `subtotal`, `discount_amount`, `points_used`, `total_amount`, `created_at`, `expires_at` nullable | Đơn đặt vé; `expires_at` xác định thời điểm hết hạn giữ ghế tạm 5 phút |
| `booking_seats` | `id`, `booking_id`, `showtime_id`, `seat_id`, `unit_price`, `status` | Ghế thuộc đơn; cần ràng buộc chống bán trùng theo suất |
| `payments` | `id`, `booking_id`, `method`, `amount`, `status`, `provider_reference` nullable, `created_at` | Trạng thái thanh toán; provider reference chỉ khi cần tích hợp |
| `promotions` | `id`, `title`, `description`, `starts_at`, `ends_at`, `discount_type`, `discount_value`, `status` | Chương trình khuyến mãi |
| `booking_promotions` | `booking_id`, `promotion_id`, `discount_amount` | Ghi nhận khuyến mãi áp dụng cho đơn |
| `tickets` | `id`, `booking_seat_id`, `ticket_code`, `status`, `issued_at` | Vé phát hành cho từng ghế nếu nghiệp vụ cần tách ticket khỏi booking |
| `audit_logs` (khuyến nghị) | `id`, `actor_user_id`, `action`, `entity_type`, `entity_id`, `created_at`, `metadata` | Lưu dấu vết hành động quản trị/đổi trạng thái nhạy cảm |

## 3. Quan hệ chính
- `roles 1—N users`
- `users 1—0..1 members` (nhân viên/quản lý có thể không có hồ sơ thành viên)
- `members 1—N bookings`
- `members 1—N point_transactions`
- `movies 1—N showtimes`
- `rooms 1—N seats` và `rooms 1—N showtimes`
- `showtimes 1—N bookings`
- `bookings 1—N booking_seats`; mỗi `booking_seat` tham chiếu một ghế và một suất chiếu
- `bookings 1—N payments` nếu cho phép nhiều lần thử thanh toán; nếu không, có thể giới hạn theo quy tắc dự án
- `bookings N—N promotions` qua `booking_promotions`
- `booking_seats 1—0..1 tickets` nếu mỗi ghế/đơn phát hành tối đa một vé

## 4. Ràng buộc quan trọng
1. Unique tài khoản: `users.username`; email/phone có unique hay không phải chốt theo yêu cầu đăng ký.
2. Unique ghế trong phòng: `(room_id, row_label, seat_number)`.
3. **Chống bán/đặt trùng ghế:** một `seat_id` chỉ được có tối đa một booking seat ở trạng thái chiếm chỗ còn hiệu lực cho cùng `showtime_id`. Backend dùng transaction cùng khóa/ràng buộc database; đơn giữ ghế hết hạn sau 5 phút phải được giải phóng an toàn. PostgreSQL partial unique index có thể hỗ trợ trạng thái cụ thể, nhưng phải thiết kế kỹ với đơn hết hạn/hủy.
4. Không chỉ kiểm tra ghế ở frontend. Backend phải dùng transaction và khóa/ràng buộc database để giải quyết hai yêu cầu đồng thời.
5. Giá bán cần được snapshot vào `booking_seats.unit_price` để thay đổi bảng giá sau này không làm thay đổi vé đã mua.
6. Ghi nhận điểm bằng `point_transactions`; tổng điểm có thể được tính từ sổ giao dịch hoặc lưu cache có cơ chế đối soát.
7. Không xóa cứng phim/phòng/khuyến mãi đã có dữ liệu nghiệp vụ liên quan nếu điều đó phá lịch sử; cân nhắc `status`/soft delete.
8. Mọi tiền tệ dùng kiểu decimal phù hợp, không dùng floating point.
9. Thời gian lưu thống nhất; API trả ISO-8601 có timezone. Hiển thị theo múi giờ Việt Nam.

## 5. Quyết định đã chốt và các mục còn mở
Đã chốt cho bản thiết kế:
- PostgreSQL được phép; dự kiến dùng Supabase PostgreSQL nếu phù hợp yêu cầu môn học và giới hạn gói Free.
- Giữ ghế tạm trong 5 phút; khi hết hạn mà chưa thanh toán thành công, ghế được giải phóng.
- Backend/PostgreSQL phải ngăn hai yêu cầu đồng thời đặt cùng ghế trong cùng suất chiếu.
- Thanh toán mô phỏng, không tích hợp cổng thanh toán thật trong phạm vi mặc định này.
- Chỉ cộng điểm thành viên sau khi thanh toán thành công.
- Chỉ cho phép hủy vé trong khoảng thời gian do nhóm quy định; không tự giả định có hoàn tiền.

Cần nhóm chốt trước khi hoàn thiện migration/logic:
- Công thức/tỷ lệ cộng và sử dụng điểm; giới hạn điểm nếu có.
- Thời hạn hủy cụ thể và chính sách hoàn tiền/hoàn điểm (nếu có).
- Ghế có các loại nào ngoài thường/VIP; có ghế không khả dụng không.
- Khuyến mãi áp dụng theo phần trăm hay số tiền cố định; có cộng dồn không.
- Có cần bảng `tickets` tách biệt hay `booking_seats` đã đủ cho phạm vi đồ án.
