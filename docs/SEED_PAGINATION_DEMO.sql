-- Chỉ chạy thủ công trên database được duyệt có Flyway V1–V9 của nhánh PR #9.
-- Không chạy trên Supabase Cinema cũ có lineage V4–V7 khác nhánh này.
-- Dữ liệu mẫu: 20 phim (16 đang chiếu, 4 sắp chiếu) và 12 ưu đãi ACTIVE.
-- Chạy lại không tạo bản sao; không cập nhật hay xóa dữ liệu đã có.
-- Các ưu đãi ACTIVE là công khai và có thể ảnh hưởng thanh toán khi luồng đặt vé sử dụng khuyến mãi.
-- Không tạo lịch chiếu, poster, banner hoặc ảnh khuyến mãi giả.

BEGIN;

DO $guard$
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NULL THEN
        RAISE EXCEPTION 'Thiếu lịch sử Flyway của PR #9; chưa thêm dữ liệu mẫu';
    END IF;
    IF (SELECT count(*) FROM (VALUES
            ('4', 'V4__add_home_catalog_media.sql'),
            ('5', 'V5__add_stitch_content_staging.sql'),
            ('6', 'V6__add_admin_seat_price_management.sql'),
            ('7', 'V7__add_movie_genre_compatibility.sql'),
            ('8', 'V8__protect_showtime_room_overlap.sql'),
            ('9', 'V9__seed_cinema_rooms.sql')
        ) AS expected(version, script)
        JOIN public.flyway_schema_history history
          ON history.version = expected.version AND history.script = expected.script AND history.success) <> 6 THEN
        RAISE EXCEPTION 'Flyway V4–V9 không khớp nhánh PR #9; chưa thêm dữ liệu mẫu';
    END IF;
    IF (SELECT count(*) FROM public.genres
        WHERE is_selectable = true
          AND name IN ('Hành động', 'Hài kịch', 'Kinh dị', 'Khoa học viễn tưởng', 'Tình cảm', 'Phiêu lưu')) <> 6 THEN
        RAISE EXCEPTION 'Thiếu thể loại chuẩn của Flyway V7; chưa thêm dữ liệu mẫu';
    END IF;
END
$guard$;

WITH seed(n, title, genre) AS (
    VALUES
        (1,  'Thành Phố Sau Mưa',        'Hành động'),
        (2,  'Vệt Sáng Cuối Trời',       'Tình cảm'),
        (3,  'Hành Trình Xuyên Đêm',     'Phiêu lưu'),
        (4,  'Tín Hiệu Từ Sao Xa',       'Khoa học viễn tưởng'),
        (5,  'Tiếng Cười Bên Kia Phố',   'Hài kịch'),
        (6,  'Căn Phòng Số 17',          'Kinh dị'),
        (7,  'Người Giữ Ngọn Hải Đăng',  'Phiêu lưu'),
        (8,  'Mật Mã Bình Minh',         'Hành động'),
        (9,  'Bản Nhạc Mùa Hè',          'Tình cảm'),
        (10, 'Chuyến Tàu Nửa Đêm',      'Kinh dị'),
        (11, 'Vùng Trời Mới',            'Khoa học viễn tưởng'),
        (12, 'Một Ngày Rất Khác',       'Hài kịch'),
        (13, 'Cánh Cửa Trong Sương',    'Kinh dị'),
        (14, 'Bên Kia Dãy Núi',         'Phiêu lưu'),
        (15, 'Nhịp Đập Thành Phố',      'Hành động'),
        (16, 'Lời Hẹn Tháng Mười Hai',  'Tình cảm'),
        (17, 'Khi Sao Rơi',             'Khoa học viễn tưởng'),
        (18, 'Bữa Tiệc Bất Ngờ',        'Hài kịch'),
        (19, 'Hòn Đảo Không Tên',       'Phiêu lưu'),
        (20, 'Ký Ức Trong Gió',         'Tình cảm')
), inserted AS (
INSERT INTO public.movies
    (source_key, title, genre, release_date, duration_minutes, version, synopsis, status)
SELECT
    'seed-pagination-v1-movie-' || lpad(n::text, 2, '0'),
    title,
    genre,
    CASE WHEN n <= 16 THEN current_date - ((16 - n) % 12)
         ELSE current_date + ((n - 16) * 7) END,
    90 + ((n % 7) * 12),
    CASE WHEN n % 5 = 0 THEN 'IMAX' ELSE '2D' END,
    'Phim dữ liệu mẫu số ' || n || ' để kiểm tra danh sách và phân trang; không phải thông tin phát hành thật.',
    CASE WHEN n <= 16 THEN 'NOW_SHOWING' ELSE 'COMING_SOON' END
FROM seed
ON CONFLICT DO NOTHING
RETURNING id, genre
)
INSERT INTO public.movie_genres (movie_id, genre_id)
SELECT inserted.id, g.id
FROM inserted
JOIN public.genres g ON g.name = (inserted.genre COLLATE "C") AND g.is_selectable = true
ON CONFLICT (movie_id, genre_id) DO NOTHING;

WITH seed(n) AS (SELECT generate_series(1, 12))
INSERT INTO public.promotions
    (source_key, title, description, discount_type, discount_value, starts_at, ends_at, status)
SELECT
    'seed-pagination-v1-promotion-' || lpad(n::text, 2, '0'),
    'Ưu đãi mẫu ' || lpad(n::text, 2, '0') ||
        CASE WHEN n % 2 = 1 THEN ' · Giảm 20.000đ' ELSE ' · Giảm 10%' END,
    'Dữ liệu thử nghiệm để xem giao diện nhiều khuyến mãi. Đây là ưu đãi mẫu đang công khai.',
    CASE WHEN n % 2 = 1 THEN 'FIXED' ELSE 'PERCENT' END,
    CASE WHEN n % 2 = 1 THEN 20000.00 ELSE 10.00 END,
    now() - interval '1 day',
    now() + interval '30 days',
    'ACTIVE'
FROM seed
ON CONFLICT DO NOTHING;

COMMIT;

-- Kiểm tra sau khi chạy (trong lần đầu mong đợi 20 phim, 12 khuyến mãi mẫu).
SELECT
    (SELECT count(*) FROM public.movies WHERE source_key LIKE 'seed-pagination-v1-movie-%') AS phim_mau,
    (SELECT count(*) FROM public.movies WHERE source_key LIKE 'seed-pagination-v1-movie-%' AND status = 'NOW_SHOWING') AS phim_dang_chieu_mau,
    (SELECT count(*) FROM public.movie_genres mg JOIN public.movies m ON m.id = mg.movie_id
        WHERE m.source_key LIKE 'seed-pagination-v1-movie-%') AS lien_ket_the_loai,
    (SELECT count(*) FROM public.promotions WHERE source_key LIKE 'seed-pagination-v1-promotion-%') AS khuyen_mai_mau,
    (SELECT count(*) FROM public.promotions WHERE source_key LIKE 'seed-pagination-v1-promotion-%'
        AND status = 'ACTIVE' AND starts_at <= now() AND ends_at >= now()) AS khuyen_mai_cong_khai;
