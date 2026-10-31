# Requirements Mapping — SRS → chức năng → tiêu chí nghiệm thu

## 1. Căn cứ và lưu ý
Nguồn: `Quan ly rap chieu phim_SRS_v1.1_SE03.docx`, mục 4–7. SRS mô tả hệ thống phục vụ khách hàng và nghiệp vụ nội bộ rạp; đây là mock project phục vụ học tập. Bảng dưới ánh xạ các use case ở mức chức năng. Các tiêu chí nghiệm thu được viết lại để kiểm thử được, không phải câu chữ nguyên văn của SRS.

## 2. Tác nhân theo SRS
| Tác nhân | Quyền/chức năng được mô tả trong SRS |
|---|---|
| Khách hàng chưa là thành viên | Xem phim, khuyến mãi, giá vé, lịch chiếu; đăng ký; đăng nhập |
| Khách hàng là thành viên | Các chức năng khách chưa là thành viên; đặt vé; quản lý tài khoản, vé và lịch sử điểm |
| Nhân viên | Quản lý bán vé, quản lý đặt vé, tìm kiếm thành viên; SRS cũng nêu quản lý phim và khuyến mãi trong mô tả phạm vi quyền |
| Quản lý/Admin | Quyền cao nhất; quản lý nhân viên, thành viên, phòng chiếu, phim, khuyến mãi và thống kê |

**Cần xác nhận:** SRS có khác biệt nhỏ giữa mô tả quyền ở phần tổng quan và các use case cụ thể. Phân quyền cuối cùng cần dựa trên bảng quyền được giảng viên/nhóm duyệt.

## 3. Mapping chức năng
| ID | Use case / khu vực SRS | Yêu cầu chức năng | Tiêu chí nghiệm thu đề xuất |
|---|---|---|---|
| REQ-AUTH-01 | 6.1 Đăng nhập | Xác thực bằng tài khoản và mật khẩu; kiểm tra trạng thái khóa; chuyển giao diện theo vai trò | Sai tài khoản/mật khẩu bị từ chối; tài khoản khóa bị từ chối; phản hồi không làm lộ mật khẩu; người dùng được chuyển tới khu vực phù hợp quyền |
| REQ-AUTH-02 | 6.3 Đăng ký | Khách tạo tài khoản thành viên | Dữ liệu bắt buộc được kiểm tra; tài khoản trùng bị từ chối; đăng ký hợp lệ tạo thành viên và thông báo thành công |
| REQ-MOV-01 | 6.5 Xem phim | Danh sách phim đang chiếu/sắp chiếu, tìm kiếm và chi tiết phim | Danh sách có tên, thể loại, ngày khởi chiếu, thời lượng; tìm kiếm trả kết quả phù hợp; trang chi tiết có diễn viên, đạo diễn, hãng, nội dung, trailer và lịch chiếu khi có dữ liệu |
| REQ-PROMO-01 | 6.6 Xem khuyến mãi | Khách xem danh sách/chi tiết chương trình khuyến mãi | Hiển thị thông tin khuyến mãi đang được công bố; nội dung và thời gian lấy từ dữ liệu hệ thống |
| REQ-SHOW-01 | 6.7 Xem lịch chiếu | Xem các suất chiếu theo phim/ngày | Lịch chiếu được lọc theo phim và ngày; không cho chọn suất không tồn tại |
| REQ-PRICE-01 | 6.8 Xem giá vé | Xem giá vé | Hiển thị mức giá hiện có; quy tắc tính giá theo loại ghế/phiên bản/phụ thu cần được xác nhận trước khi triển khai |
| REQ-BOOK-01 | 6.2 Đặt vé | Thành viên chọn phim, ngày, suất chiếu và ghế | Chỉ thành viên đã đăng nhập được đặt vé; suất chiếu và sơ đồ ghế tương ứng; ghế đã bán/đang được giữ hợp lệ không thể được đặt trùng |
| REQ-BOOK-02 | 6.4 Quản lý vé tài khoản | Thành viên xem vé đã đặt và vé đã hủy | Người dùng chỉ xem được vé thuộc tài khoản của mình; trạng thái vé hiển thị rõ |
| REQ-MEMBER-01 | 6.4 Quản lý tài khoản | Cập nhật thông tin, đổi mật khẩu, đăng xuất | Người dùng chỉ sửa tài khoản của mình; đổi mật khẩu yêu cầu xác minh mật khẩu hiện tại; thông tin hợp lệ được lưu |
| REQ-POINT-01 | 6.4 Lịch sử điểm | Xem lịch sử cộng/sử dụng điểm theo khoảng ngày | Có thể lọc theo ngày và loại lịch sử; tổng/chi tiết lấy từ giao dịch lưu trong hệ thống |
| REQ-SALE-01 | 6.9 Quản lý bán vé | Nhân viên bán vé tại quầy, tra cứu thành viên, tính tiền | Chỉ nhân viên/quản lý được truy cập; vé gắn với suất chiếu/ghế; không bán ghế đã bán |
| REQ-BOOK-03 | 6.10 Quản lý đặt vé | Nhân viên tra cứu và xử lý đặt vé theo quyền | Danh sách có thông tin và trạng thái đặt vé; hành động thay đổi trạng thái được kiểm tra quyền và lưu dấu vết |
| REQ-STAFF-01 | 6.11 Quản lý nhân viên | Quản lý xem/tìm kiếm/thêm/sửa/xóa nhân viên theo SRS | Chỉ quản lý/Admin thao tác; dữ liệu bắt buộc được kiểm tra; không để tài khoản mất liên kết sai với dữ liệu nghiệp vụ |
| REQ-ROOM-01 | 6.12 Quản lý phòng chiếu | Quản lý phòng và cấu hình ghế thường/ghế VIP | Chỉ quản lý/Admin thao tác; mỗi ghế thuộc một phòng; thay đổi sơ đồ không làm sai lệch vé/suất chiếu đã phát sinh |
| REQ-MEMBER-02 | 6.13 Quản lý thành viên | Quản lý xem danh sách và kiểm tra điểm thành viên | Chỉ vai trò được cấp quyền truy cập; tìm kiếm thành viên hoạt động; điểm và lịch sử có thể đối soát |
| REQ-MOV-02 | 6.14 Quản lý phim | Quản lý xem/thêm/sửa thông tin phim | Chỉ vai trò được cấp quyền thao tác; trường bắt buộc được kiểm tra; lịch chiếu liên quan không bị xóa/hỏng ngoài ý muốn |
| REQ-PROMO-02 | 6.15 Quản lý khuyến mãi | Quản lý tạo/sửa/xóa khuyến mãi | Chỉ vai trò được cấp quyền; dữ liệu thời gian và mức giảm hợp lệ; nếu khuyến mãi đã được sử dụng, xử lý xóa phải tuân theo quy tắc đã duyệt |
| REQ-REPORT-01 | 6.16 Thống kê | Quản lý xem thống kê kinh doanh | Chỉ quản lý/Admin; số liệu có khoảng thời gian và tiêu chí rõ; số liệu đối chiếu được với vé/doanh thu lưu trữ |

## 4. Yêu cầu phi chức năng cần đưa vào kiểm thử
SRS có các nhóm: hiệu năng, khả năng tăng cường, bảo mật, độ tin cậy, sao lưu/phục hồi, giao diện, hệ điều hành/phần mềm và ngôn ngữ. Nội dung cụ thể cần được đọc và chuyển thành ngưỡng kiểm thử trước khi chốt KPI. Không tự đặt ngưỡng tải/uptime khi chưa được nhóm duyệt.

| Nhóm | Tiêu chí nghiệm thu đề xuất |
|---|---|
| Hiệu năng | Ghi nhận ngưỡng phản hồi và số người dùng đồng thời theo đúng mục 7 SRS; chạy kiểm thử và lưu kết quả |
| Bảo mật | Phân quyền theo vai trò; endpoint quản trị không truy cập được bởi khách; mật khẩu không lưu dạng rõ; dữ liệu đầu vào được validate |
| Độ tin cậy | Lỗi nghiệp vụ trả thông báo rõ; không tạo hai vé hợp lệ cho cùng một ghế ở cùng suất |
| Sao lưu/phục hồi | Có quy trình backup/restore phù hợp gói database; kiểm tra phục hồi trước demo nếu phạm vi cho phép |
| Giao diện | Luồng chính sử dụng được trên desktop và mobile; trạng thái loading/empty/error rõ ràng |
| Khả năng tăng cường | Tách frontend/backend và cấu hình môi trường; có thể triển khai phiên bản mới mà không commit secrets |

## 5. Quyết định nhóm đã chốt và các quyết định còn mở
Đã chốt cho bản thiết kế (đây là quyết định của nhóm, không mặc nhiên là yêu cầu nguyên văn trong SRS):
- PostgreSQL được phép; dự kiến dùng Supabase PostgreSQL.
- Giữ ghế tạm 5 phút; nếu chưa thanh toán thành công khi hết hạn thì giải phóng ghế.
- Backend/PostgreSQL phải chống hai yêu cầu đồng thời chiếm cùng ghế trong cùng suất chiếu.
- Thanh toán mô phỏng, không dùng cổng thanh toán thật.
- Chỉ cộng điểm sau khi thanh toán thành công.
- Chỉ cho hủy vé trong một khoảng thời gian do nhóm quy định; chưa giả định chính sách hoàn tiền.

Còn mở — cần chốt trước khi implement đúng nghiệp vụ:
- Tỷ lệ/công thức cộng và sử dụng điểm, cùng giới hạn nếu có.
- Thời hạn hủy vé cụ thể và chính sách hoàn tiền/hoàn điểm (nếu có).
- Vai trò chính xác được phép quản lý phim/khuyến mãi và trạng thái tài khoản.
- Mức tối thiểu cần hoàn thành cho MVP so với toàn bộ SRS.
