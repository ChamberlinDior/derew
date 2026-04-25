-- ============================================================
-- OVIRO – V4 : Table messages (chat chauffeur/passager)
-- ============================================================

CREATE TABLE IF NOT EXISTS messages (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    ride_id      CHAR(36)     NOT NULL,
    sender_id    CHAR(36)     NOT NULL,
    recipient_id CHAR(36)     NOT NULL,
    content      TEXT         NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      DATETIME     NULL,
    sent_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NULL,
    version      BIGINT       DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_msg_ride      FOREIGN KEY (ride_id)      REFERENCES rides(id),
    CONSTRAINT fk_msg_sender    FOREIGN KEY (sender_id)    REFERENCES users(id),
    CONSTRAINT fk_msg_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_messages_ride_id          ON messages(ride_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_recipient ON messages(ride_id, recipient_id, is_read);
