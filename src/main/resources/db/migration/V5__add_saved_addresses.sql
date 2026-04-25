-- ============================================================
-- OVIRO – V5 : Adresses sauvegardées + champs ride
-- ============================================================

CREATE TABLE IF NOT EXISTS saved_addresses (
    id           CHAR(36)       NOT NULL DEFAULT (UUID()),
    user_id      CHAR(36)       NOT NULL,
    label        VARCHAR(100)   NOT NULL,
    address_name VARCHAR(255),
    latitude     DOUBLE         NOT NULL,
    longitude    DOUBLE         NOT NULL,
    type         VARCHAR(20)    NOT NULL DEFAULT 'OTHER',
    is_default   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NULL,
    version      BIGINT         DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_saved_addresses_user_id ON saved_addresses(user_id);

-- Ajouter les colonnes serviceType et "pour quelqu'un d'autre" dans rides
ALTER TABLE rides
    ADD COLUMN IF NOT EXISTS service_type   VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS for_someone_else BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS recipient_phone  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS recipient_name   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sender_note      VARCHAR(500);

-- Ajouter le PIN de transfert dans users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS transfer_pin_hash VARCHAR(255);
