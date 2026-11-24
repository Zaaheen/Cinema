-- Development baseline for the existing Cinema UI.  The guards preserve any
-- room already configured by an operator and make the migration repeat-safe.
INSERT INTO rooms (name, status)
SELECT seed.name, 'ACTIVE'
FROM (VALUES
    ('Phòng 01 - IMAX Laser 3D'),
    ('Phòng 02 - Dolby Atmos'),
    ('Phòng 03 - CineLounge VIP'),
    ('Phòng 04 - Standard 2D'),
    ('Phòng 05 - Standard 2D')
) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1 FROM rooms existing WHERE existing.name = seed.name
);
