INSERT INTO roles (code, name)
VALUES ('ADMIN', 'Quản trị viên'),
       ('STAFF', 'Nhân viên'),
       ('MEMBER', 'Thành viên')
ON CONFLICT (code) DO NOTHING;
