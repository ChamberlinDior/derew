-- ============================================================
-- OVIRO – V6 : Location de véhicule + Abonnements
-- ============================================================

CREATE TABLE IF NOT EXISTS vehicle_rentals (
    id               CHAR(36)       NOT NULL DEFAULT (UUID()),
    passenger_id     CHAR(36)       NOT NULL,
    driver_id        CHAR(36),
    vehicle_id       CHAR(36),
    start_time       DATETIME,
    end_time         DATETIME,
    duration_hours   INT            NOT NULL,
    price_per_hour   DECIMAL(12,2),
    total_amount     DECIMAL(12,2),
    status           VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED',
    pickup_address   VARCHAR(500),
    notes            VARCHAR(1000),
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME,
    version          BIGINT         DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_rental_passenger FOREIGN KEY (passenger_id) REFERENCES users(id),
    CONSTRAINT fk_rental_driver    FOREIGN KEY (driver_id)    REFERENCES driver_profiles(id),
    CONSTRAINT fk_rental_vehicle   FOREIGN KEY (vehicle_id)   REFERENCES vehicles(id)
);

CREATE INDEX IF NOT EXISTS idx_rental_passenger ON vehicle_rentals(passenger_id);
CREATE INDEX IF NOT EXISTS idx_rental_driver    ON vehicle_rentals(driver_id);
CREATE INDEX IF NOT EXISTS idx_rental_status    ON vehicle_rentals(status);

CREATE TABLE IF NOT EXISTS subscription_plans (
    id              CHAR(36)       NOT NULL DEFAULT (UUID()),
    name            VARCHAR(100)   NOT NULL,
    description     VARCHAR(500),
    duration_days   INT            NOT NULL,
    included_trips  INT            NOT NULL,
    price           DECIMAL(12,2)  NOT NULL,
    bonus_ut        INT            DEFAULT 0,
    is_active       BOOLEAN        DEFAULT TRUE,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    version         BIGINT         DEFAULT 0,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_subscriptions (
    id              CHAR(36)       NOT NULL DEFAULT (UUID()),
    user_id         CHAR(36)       NOT NULL,
    plan_id         CHAR(36)       NOT NULL,
    start_date      DATETIME       NOT NULL,
    end_date        DATETIME       NOT NULL,
    trips_remaining INT            NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    version         BIGINT         DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);

CREATE INDEX IF NOT EXISTS idx_sub_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_sub_status  ON user_subscriptions(status);

-- Données de démo pour les plans
INSERT IGNORE INTO subscription_plans (id, name, description, duration_days, included_trips, price, bonus_ut, is_active)
VALUES
    (UUID(), 'Starter',    '10 trajets pour le mois',          30, 10,  10000.00, 2,  TRUE),
    (UUID(), 'Essentiel',  '25 trajets + 5 UT bonus',          30, 25,  22000.00, 5,  TRUE),
    (UUID(), 'Premium',    '50 trajets + 10 UT bonus',         30, 50,  40000.00, 10, TRUE),
    (UUID(), 'Business',   '100 trajets + 20 UT bonus',        30, 100, 75000.00, 20, TRUE);
