-- ============================================================
-- OVIRO Backend – Migration V1 : Schéma initial (MySQL)
-- ============================================================

CREATE TABLE users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    profile_picture_url TEXT,
    date_of_birth DATETIME NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    last_login_at DATETIME NULL,
    failed_login_attempts INT DEFAULT 0,
    locked_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_phone ON users(phone_number);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_status ON users(status);

CREATE TABLE partner_profiles (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    company_registration_number VARCHAR(100),
    address VARCHAR(500),
    commission_rate DECIMAL(5,4) DEFAULT 0.1500,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_partner_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE vehicles (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    owner_id CHAR(36) NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    color VARCHAR(50),
    type VARCHAR(50),
    seats INT,
    insurance_document_url TEXT,
    insurance_expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_vehicle_owner FOREIGN KEY (owner_id) REFERENCES partner_profiles(id)
);

CREATE INDEX idx_vehicle_plate ON vehicles(plate_number);

CREATE TABLE driver_profiles (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL UNIQUE,
    partner_id CHAR(36) NULL,
    current_vehicle_id CHAR(36) NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    license_expiry_date DATE,
    national_id VARCHAR(50),
    national_id_document_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    current_latitude DECIMAL(10,7),
    current_longitude DECIMAL(10,7),
    rating DECIMAL(3,2) DEFAULT 5.00,
    total_rides INT DEFAULT 0,
    total_earnings DECIMAL(15,2) DEFAULT 0.00,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_driver_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id),
    CONSTRAINT fk_driver_vehicle FOREIGN KEY (current_vehicle_id) REFERENCES vehicles(id)
);

CREATE INDEX idx_driver_status ON driver_profiles(status);
CREATE INDEX idx_driver_partner ON driver_profiles(partner_id);

CREATE TABLE wallets (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL UNIQUE,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency CHAR(3) NOT NULL DEFAULT 'XAF',
    is_active BOOLEAN DEFAULT TRUE,
    daily_limit DECIMAL(15,2) DEFAULT 500000.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_wallet_user ON wallets(user_id);

CREATE TABLE rides (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(20) NOT NULL UNIQUE,
    client_id CHAR(36) NOT NULL,
    driver_id CHAR(36) NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_latitude DECIMAL(10,7) NOT NULL,
    pickup_longitude DECIMAL(10,7) NOT NULL,
    dropoff_address VARCHAR(500) NOT NULL,
    dropoff_latitude DECIMAL(10,7) NOT NULL,
    dropoff_longitude DECIMAL(10,7) NOT NULL,
    estimated_distance_km DECIMAL(8,3),
    estimated_duration_minutes INT,
    estimated_fare DECIMAL(12,2),
    actual_fare DECIMAL(12,2),
    commission_amount DECIMAL(12,2),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    requested_at DATETIME NULL,
    assigned_at DATETIME NULL,
    pickup_at DATETIME NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancellation_reason VARCHAR(500),
    client_rating INT,
    driver_rating INT,
    client_review VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_ride_client FOREIGN KEY (client_id) REFERENCES users(id),
    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles(id)
);

CREATE INDEX idx_ride_client ON rides(client_id);
CREATE INDEX idx_ride_driver ON rides(driver_id);
CREATE INDEX idx_ride_status ON rides(status);
CREATE INDEX idx_ride_reference ON rides(reference);
CREATE INDEX idx_ride_created ON rides(created_at);

CREATE TABLE transactions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(30) NOT NULL UNIQUE,
    wallet_id CHAR(36) NOT NULL,
    ride_id CHAR(36) NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    external_reference VARCHAR(100),
    payment_provider VARCHAR(100),
    completed_at DATETIME NULL,
    failure_reason VARCHAR(500),
    metadata TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_ride FOREIGN KEY (ride_id) REFERENCES rides(id)
);

CREATE INDEX idx_tx_wallet ON transactions(wallet_id);
CREATE INDEX idx_tx_type ON transactions(type);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_reference ON transactions(reference);

CREATE TABLE qr_code_payments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    ride_id CHAR(36) NOT NULL UNIQUE,
    token VARCHAR(256) NOT NULL UNIQUE,
    payload TEXT,
    qr_code_image LONGTEXT,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL,
    scanned_at DATETIME NULL,
    validated_at DATETIME NULL,
    scan_count INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_qr_ride FOREIGN KEY (ride_id) REFERENCES rides(id)
);

CREATE INDEX idx_qr_token ON qr_code_payments(token);
CREATE INDEX idx_qr_status ON qr_code_payments(status);

CREATE TABLE session_tokens (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    token LONGTEXT NOT NULL,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at DATETIME NULL,
    expires_at DATETIME NOT NULL,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_session_user ON session_tokens(user_id);

CREATE TABLE sos_alerts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    driver_id CHAR(36) NOT NULL,
    ride_id CHAR(36) NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    description VARCHAR(1000),
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at DATETIME NULL,
    resolved_by_admin_id CHAR(36) NULL,
    resolution_notes VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_sos_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles(id),
    CONSTRAINT fk_sos_ride FOREIGN KEY (ride_id) REFERENCES rides(id)
);

CREATE INDEX idx_sos_driver ON sos_alerts(driver_id);
CREATE INDEX idx_sos_resolved ON sos_alerts(resolved);

CREATE TABLE audit_logs (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    description VARCHAR(1000),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    success BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500),
    request_data LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    phone_number,
    password_hash,
    role,
    status,
    email_verified,
    phone_verified
) VALUES (
    UUID(),
    'Super',
    'Admin',
    'admin@oviro.com',
    '+24177000000',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2tpuHDDqFG',
    'ADMIN',
    'ACTIVE',
    TRUE,
    TRUE
);