-- Keep a single role code for cinema employees. Existing STAFF users retain
-- their role through reassignment before the legacy role is removed.
INSERT INTO roles (code, name)
VALUES ('EMPLOYEE', 'Nhân viên')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name;

UPDATE users
SET role_id = (SELECT id FROM roles WHERE code = 'EMPLOYEE')
WHERE role_id = (SELECT id FROM roles WHERE code = 'STAFF');

DELETE FROM roles
WHERE code = 'STAFF';
