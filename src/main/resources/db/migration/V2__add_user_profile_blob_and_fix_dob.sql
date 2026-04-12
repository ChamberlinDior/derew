-- ============================================================
-- OVIRO Backend – Migration V2 :
-- Rider profile photo in BLOB + date_of_birth as DATE
-- ============================================================

ALTER TABLE users
    ADD COLUMN profile_picture_data LONGBLOB NULL AFTER status,
    ADD COLUMN profile_picture_content_type VARCHAR(100) NULL AFTER profile_picture_data,
    ADD COLUMN profile_picture_file_name VARCHAR(255) NULL AFTER profile_picture_content_type,
    ADD COLUMN profile_picture_size BIGINT NULL AFTER profile_picture_file_name;

ALTER TABLE users
    MODIFY COLUMN date_of_birth DATE NULL;

ALTER TABLE users
    DROP COLUMN profile_picture_url;