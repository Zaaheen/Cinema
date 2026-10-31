# Bộ tài liệu thiết kế — Website Quản lý rạp chiếu phim

**Nguồn nghiệp vụ chính:** `Quan ly rap chieu phim_SRS_v1.1_SE03.docx` (SRS 78 trang, phiên bản ghi trên bìa là 1.1; một số trang bên trong vẫn ghi V1.0).

## Trạng thái
Đây là **bản thiết kế khởi đầu** để thống nhất phạm vi, kiến trúc và cách triển khai trước khi code. Các yêu cầu được tóm tắt từ SRS; các quyết định chưa được SRS quy định được đánh dấu **TBD / cần xác nhận**, không coi là yêu cầu chính thức.

## Danh sách
- `REQUIREMENTS_MAPPING.md`: tác nhân, use case và tiêu chí nghiệm thu đề xuất.
- `ERD.md`: mô hình dữ liệu đề xuất và ràng buộc nghiệp vụ.
- `API_CONTRACT.md`: hợp đồng REST API khởi đầu.
- `PLAN.md`: phạm vi MVP, sprint và phụ thuộc.
- `DEPLOYMENT.md`: kiến trúc triển khai dự kiến.

## Kiến trúc đã chốt ở mức định hướng
- Frontend: React + Vite + JavaScript/JSX, triển khai Cloudflare Pages.
- Backend: Spring Boot, Java 21 (xác nhận môi trường trước khi khởi tạo), Gradle, Docker, triển khai Google Cloud Run.
- Database: Supabase PostgreSQL Free **chỉ khi giảng viên/đề bài không bắt buộc MySQL hoặc SQL Server**.
- Migration: Flyway; persistence: Spring Data JPA.
- Mục tiêu chi phí thấp cho đồ án/demo; không cam kết toàn bộ vận hành luôn miễn phí.

## Quy tắc sử dụng tài liệu
1. SRS là nguồn yêu cầu nghiệp vụ; đề xuất kỹ thuật trong các file này là thiết kế của dự án, không phải nội dung SRS.
2. Các quyết định nghiệp vụ đã chốt cho bản thiết kế: giữ ghế tạm 5 phút; backend/PostgreSQL chống đặt trùng ghế theo suất chiếu; thanh toán mô phỏng; chỉ cộng điểm sau khi thanh toán thành công; hủy vé chỉ trong thời hạn nhóm quy định.
3. Chưa tự đặt ra tỷ lệ cộng/đổi điểm, thời hạn hủy cụ thể hoặc chính sách hoàn tiền/hoàn điểm; các mục này vẫn TBD và cần nhóm xác nhận trước khi triển khai.
4. Không commit `.env`, mật khẩu, JWT secret, database URL hoặc credential.
5. Nếu yêu cầu môn học chỉ định công nghệ/database khác, cập nhật `DEPLOYMENT.md` và `ERD.md` trước khi tạo schema.
