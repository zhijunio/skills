CREATE TABLE stock (
    sku varchar(40) PRIMARY KEY,
    available integer NOT NULL CHECK (available >= 0)
);

CREATE TABLE reservations (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku varchar(40) NOT NULL REFERENCES stock (sku),
    quantity integer NOT NULL CHECK (quantity > 0)
);
