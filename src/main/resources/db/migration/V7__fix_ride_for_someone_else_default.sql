UPDATE rides
SET for_someone_else = FALSE
WHERE for_someone_else IS NULL;

ALTER TABLE rides
    MODIFY COLUMN for_someone_else BOOLEAN NOT NULL DEFAULT FALSE;
