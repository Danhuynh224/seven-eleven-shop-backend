-- Users table
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('ADMIN', 'CUSTOMER')),
    full_name   VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Products table (soft delete via deleted_at)
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)    NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2)  NOT NULL CHECK (price >= 0),
    stock       INT             NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url   VARCHAR(500),
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount NUMERIC(12, 2)  NOT NULL CHECK (total_amount >= 0),
    status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','CONFIRMED','SHIPPED','COMPLETED','CANCELLED')),
    note         TEXT,
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order items table
CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT          NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity   INT             NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2)  NOT NULL CHECK (unit_price >= 0)
);

-- Indexes
CREATE INDEX idx_products_category   ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_name       ON products(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_user         ON orders(user_id);
CREATE INDEX idx_orders_status       ON orders(status);
CREATE INDEX idx_orders_created_at   ON orders(created_at DESC);
CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
