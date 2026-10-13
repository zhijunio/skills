CREATE TABLE product (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    sku VARCHAR(32) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_sku UNIQUE (sku)
);

CREATE INDEX idx_product_created_at_id ON product (created_at DESC, id DESC);
