-- ============================================================
-- OVIRO Backend – Migration V3 : Notifications & FCM tokens
-- ============================================================

-- FCM device token on users (one active token per user)
ALTER TABLE users
    ADD COLUMN fcm_token VARCHAR(500) NULL;

-- Notifications history
CREATE TABLE notifications (
    id           CHAR(36)     NOT NULL PRIMARY KEY DEFAULT (UUID()),
    user_id      CHAR(36)     NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    data         TEXT         NULL COMMENT 'JSON metadata (rideId, amount, …)',
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      DATETIME     NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user_id    ON notifications (user_id);
CREATE INDEX idx_notif_type       ON notifications (type);
CREATE INDEX idx_notif_is_read    ON notifications (user_id, is_read);
CREATE INDEX idx_notif_created_at ON notifications (created_at DESC);
