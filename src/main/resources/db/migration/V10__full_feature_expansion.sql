-- ============================================================
-- OVIRO Backend – Migration V10 : Expansion complète des services
-- Livraison, Food, Parrainage, Promo, Support, Objets perdus,
-- Zones de tarification, Bonus chauffeur, Multi-arrêts, Planification
-- ============================================================

-- 1. Colonnes sur rides (paiement, pourboire, planification, partage, surge, promo)
ALTER TABLE rides
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'WALLET',
    ADD COLUMN IF NOT EXISTS tip_amount DECIMAL(12,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS promo_code VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS promo_discount_amount DECIMAL(12,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS scheduled_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS share_token VARCHAR(100) NULL UNIQUE,
    ADD COLUMN IF NOT EXISTS surge_multiplier DECIMAL(5,2) DEFAULT 1.00,
    ADD COLUMN IF NOT EXISTS service_type VARCHAR(20) DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS for_someone_else TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS sender_note VARCHAR(500) NULL;

-- (service_type, for_someone_else etc. sont déjà là depuis V7, on ignore les erreurs via IF NOT EXISTS)

-- 2. Colonnes sur users (code parrainage)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(20) NULL UNIQUE,
    ADD COLUMN IF NOT EXISTS referred_by_code VARCHAR(20) NULL;

-- 3. Arrêts intermédiaires (multi-stop)
CREATE TABLE IF NOT EXISTS ride_stops (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    ride_id CHAR(36) NOT NULL,
    stop_order INT NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    note VARCHAR(200) NULL,
    reached TINYINT(1) DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_stop_ride FOREIGN KEY (ride_id) REFERENCES rides(id)
);
CREATE INDEX IF NOT EXISTS idx_stop_ride ON ride_stops(ride_id);

-- 4. Livraisons moto
CREATE TABLE IF NOT EXISTS deliveries (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(20) NOT NULL UNIQUE,
    sender_id CHAR(36) NOT NULL,
    driver_id CHAR(36) NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_latitude DECIMAL(10,7) NOT NULL,
    pickup_longitude DECIMAL(10,7) NOT NULL,
    dropoff_address VARCHAR(500) NOT NULL,
    dropoff_latitude DECIMAL(10,7) NOT NULL,
    dropoff_longitude DECIMAL(10,7) NOT NULL,
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    package_type VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    package_description VARCHAR(500) NULL,
    package_weight_kg DECIMAL(6,2) NULL,
    fragile TINYINT(1) DEFAULT 0,
    sender_note VARCHAR(500) NULL,
    payment_method VARCHAR(20) DEFAULT 'WALLET',
    estimated_distance_km DECIMAL(8,3) NULL,
    estimated_fare DECIMAL(12,2) NULL,
    actual_fare DECIMAL(12,2) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    delivery_pin VARCHAR(10) NULL,
    delivery_photo_url VARCHAR(500) NULL,
    delivery_photo_data LONGBLOB NULL,
    delivery_photo_content_type VARCHAR(100) NULL,
    requested_at DATETIME NULL,
    assigned_at DATETIME NULL,
    picked_up_at DATETIME NULL,
    delivered_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancellation_reason VARCHAR(500) NULL,
    sender_rating INT NULL,
    sender_review VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_delivery_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_delivery_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles(id)
);
CREATE INDEX IF NOT EXISTS idx_delivery_sender ON deliveries(sender_id);
CREATE INDEX IF NOT EXISTS idx_delivery_driver ON deliveries(driver_id);
CREATE INDEX IF NOT EXISTS idx_delivery_status ON deliveries(status);
CREATE INDEX IF NOT EXISTS idx_delivery_reference ON deliveries(reference);

-- 5. Codes promo
CREATE TABLE IF NOT EXISTS promo_codes (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(300) NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    max_discount_amount DECIMAL(12,2) NULL,
    min_ride_amount DECIMAL(12,2) DEFAULT 0.00,
    max_uses INT NULL,
    uses_per_user INT DEFAULT 1,
    total_used INT DEFAULT 0,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    active TINYINT(1) DEFAULT 1,
    applies_to_service_types VARCHAR(200) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_promo_code ON promo_codes(code);

-- 6. Utilisations des codes promo
CREATE TABLE IF NOT EXISTS promo_code_usages (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    promo_code_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    ride_id CHAR(36) NULL,
    delivery_id CHAR(36) NULL,
    discount_applied DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_promo_usage_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id)
);
CREATE INDEX IF NOT EXISTS idx_promo_usage_user ON promo_code_usages(user_id);
CREATE INDEX IF NOT EXISTS idx_promo_usage_code ON promo_code_usages(promo_code_id);

-- 7. Parrainage
CREATE TABLE IF NOT EXISTS referrals (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    referrer_id CHAR(36) NOT NULL,
    referred_id CHAR(36) NOT NULL UNIQUE,
    referral_code VARCHAR(20) NOT NULL,
    referrer_bonus DECIMAL(12,2) DEFAULT 2000.00,
    referred_bonus DECIMAL(12,2) DEFAULT 1000.00,
    bonus_credited TINYINT(1) DEFAULT 0,
    credited_at DATETIME NULL,
    first_ride_completed TINYINT(1) DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_referral_referrer ON referrals(referrer_id);
CREATE INDEX IF NOT EXISTS idx_referral_referred ON referrals(referred_id);
CREATE INDEX IF NOT EXISTS idx_referral_code ON referrals(referral_code);

-- 8. Tickets de support
CREATE TABLE IF NOT EXISTS support_tickets (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(20) NOT NULL UNIQUE,
    user_id CHAR(36) NOT NULL,
    assigned_agent_id CHAR(36) NULL,
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    related_ride_id CHAR(36) NULL,
    related_delivery_id CHAR(36) NULL,
    related_order_id CHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ticket_user ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_status ON support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_ticket_reference ON support_tickets(reference);

-- 9. Messages de support
CREATE TABLE IF NOT EXISTS support_messages (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    ticket_id CHAR(36) NOT NULL,
    sender_id CHAR(36) NOT NULL,
    sender_name VARCHAR(200) NULL,
    content TEXT NOT NULL,
    is_from_agent TINYINT(1) DEFAULT 0,
    attachment_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_smsg_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_smsg_ticket ON support_messages(ticket_id);

-- 10. Objets perdus
CREATE TABLE IF NOT EXISTS lost_item_reports (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reporter_id CHAR(36) NOT NULL,
    ride_id CHAR(36) NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    item_color VARCHAR(100) NULL,
    photo_url VARCHAR(500) NULL,
    contact_phone VARCHAR(20) NULL,
    resolved TINYINT(1) DEFAULT 0,
    resolution_notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_lost_ride FOREIGN KEY (ride_id) REFERENCES rides(id)
);
CREATE INDEX IF NOT EXISTS idx_lost_ride ON lost_item_reports(ride_id);
CREATE INDEX IF NOT EXISTS idx_lost_reporter ON lost_item_reports(reporter_id);

-- 11. Zones de tarification / Surge pricing
CREATE TABLE IF NOT EXISTS pricing_zones (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300) NULL,
    center_latitude DECIMAL(10,7) NOT NULL,
    center_longitude DECIMAL(10,7) NOT NULL,
    radius_km DECIMAL(8,3) NOT NULL,
    surge_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    base_fare_override DECIMAL(12,2) NULL,
    active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_zone_name ON pricing_zones(name);

-- 12. Objectifs bonus chauffeur
CREATE TABLE IF NOT EXISTS driver_bonus_targets (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    driver_id CHAR(36) NOT NULL,
    target_date DATE NOT NULL,
    required_rides INT NOT NULL,
    completed_rides INT DEFAULT 0,
    bonus_amount DECIMAL(12,2) NOT NULL,
    achieved TINYINT(1) DEFAULT 0,
    bonus_credited TINYINT(1) DEFAULT 0,
    description VARCHAR(300) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bonus_driver ON driver_bonus_targets(driver_id);
CREATE INDEX IF NOT EXISTS idx_bonus_date ON driver_bonus_targets(target_date);

-- 13. Profils restaurants
CREATE TABLE IF NOT EXISTS restaurant_profiles (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    owner_id CHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500) NULL,
    phone VARCHAR(20) NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    cuisine_type VARCHAR(100) NULL,
    opening_time VARCHAR(10) NULL,
    closing_time VARCHAR(10) NULL,
    delivery_radius_km DECIMAL(6,2) DEFAULT 5.00,
    min_order_amount DECIMAL(12,2) DEFAULT 1000.00,
    delivery_fee DECIMAL(12,2) DEFAULT 500.00,
    average_prep_minutes INT DEFAULT 20,
    rating DECIMAL(3,2) DEFAULT 5.00,
    total_orders INT DEFAULT 0,
    active TINYINT(1) DEFAULT 1,
    verified TINYINT(1) DEFAULT 0,
    logo_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_restaurant_owner ON restaurant_profiles(owner_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_active ON restaurant_profiles(active);

-- 14. Articles du menu
CREATE TABLE IF NOT EXISTS menu_items (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    restaurant_id CHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500) NULL,
    category VARCHAR(100) NULL,
    price DECIMAL(12,2) NOT NULL,
    photo_url VARCHAR(500) NULL,
    available TINYINT(1) DEFAULT 1,
    is_popular TINYINT(1) DEFAULT 0,
    preparation_minutes INT DEFAULT 10,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_menu_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant_profiles(id)
);
CREATE INDEX IF NOT EXISTS idx_menu_restaurant ON menu_items(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_menu_available ON menu_items(available);

-- 15. Commandes food
CREATE TABLE IF NOT EXISTS food_orders (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(20) NOT NULL UNIQUE,
    client_id CHAR(36) NOT NULL,
    restaurant_id CHAR(36) NOT NULL,
    driver_id CHAR(36) NULL,
    delivery_address VARCHAR(500) NOT NULL,
    delivery_latitude DECIMAL(10,7) NOT NULL,
    delivery_longitude DECIMAL(10,7) NOT NULL,
    items_total DECIMAL(12,2) NOT NULL,
    delivery_fee DECIMAL(12,2) DEFAULT 500.00,
    discount_amount DECIMAL(12,2) DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) DEFAULT 'WALLET',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    special_instructions VARCHAR(500) NULL,
    estimated_delivery_minutes INT NULL,
    confirmed_at DATETIME NULL,
    ready_at DATETIME NULL,
    picked_up_at DATETIME NULL,
    delivered_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancellation_reason VARCHAR(300) NULL,
    client_rating INT NULL,
    client_review VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_order_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant_profiles(id)
);
CREATE INDEX IF NOT EXISTS idx_order_client ON food_orders(client_id);
CREATE INDEX IF NOT EXISTS idx_order_restaurant ON food_orders(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON food_orders(status);
CREATE INDEX IF NOT EXISTS idx_order_reference ON food_orders(reference);

-- 16. Lignes de commande food
CREATE TABLE IF NOT EXISTS food_order_items (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    order_id CHAR(36) NOT NULL,
    menu_item_id CHAR(36) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    note VARCHAR(200) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES food_orders(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_order_item_order ON food_order_items(order_id);

-- 17. Générer des codes de parrainage pour les utilisateurs existants
UPDATE users SET referral_code = CONCAT('OVIRO', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 6)))
WHERE referral_code IS NULL;
