CREATE TABLE InventoryProduct (
    stock_number CHAR(7) PRIMARY KEY,
    manufacturer_name VARCHAR2(20) NOT NULL,
    model_number VARCHAR2(20) NOT NULL,
    quantity NUMBER NOT NULL,
    min_stock_level NUMBER NOT NULL,
    max_stock_level NUMBER NOT NULL,
    location VARCHAR2(20) NOT NULL,
    replenishment NUMBER NOT NULL,

    UNIQUE (manufacturer_name, model_number),

    CHECK (REGEXP_LIKE(stock_number, '^[A-Z]{2}[0-9]{5}$')),
    CHECK (quantity >= 0),
    CHECK (min_stock_level >= 0),
    CHECK (max_stock_level >= min_stock_level),
    CHECK (replenishment >= 0),
    CHECK (REGEXP_LIKE(location, '^[A-Za-z][1-9][0-9]*$'))
);

CREATE TABLE ShippingNotice (
    notice_id VARCHAR2(20) NOT NULL,
    stock_number CHAR(7) NOT NULL,
    quantity NUMBER NOT NULL,
    shipping_company_name VARCHAR2(20) NOT NULL,
    notice_date DATE NOT NULL,

    PRIMARY KEY (notice_id, stock_number),
    FOREIGN KEY (stock_number) REFERENCES InventoryProduct(stock_number),

    CHECK (quantity > 0)
);

CREATE TABLE Shipment (
    shipment_id VARCHAR2(20) NOT NULL,
    notice_id VARCHAR2(20) NOT NULL,
    stock_number CHAR(7) NOT NULL,
    quantity_received NUMBER NOT NULL,
    arrival_date DATE NOT NULL,

    PRIMARY KEY (shipment_id, stock_number),
    UNIQUE (notice_id, stock_number),
    FOREIGN KEY (notice_id, stock_number) REFERENCES ShippingNotice(notice_id, stock_number),

    CHECK (quantity_received > 0)
);

CREATE TABLE WarehouseOrder (
    order_number VARCHAR2(20) NOT NULL,
    stock_number CHAR(7) NOT NULL,
    quantity_ordered NUMBER NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR2(20) NOT NULL,

    PRIMARY KEY (order_number, stock_number),
    FOREIGN KEY (stock_number) REFERENCES InventoryProduct(stock_number),

    CHECK (quantity_ordered > 0),
    CHECK (LOWER(status) IN ('pending', 'filled', 'cancelled'))
);

CREATE TABLE ReplenishmentOrder (
    replenishment_order_id VARCHAR2(20) NOT NULL,
    stock_number CHAR(7) NOT NULL,
    quantity_ordered NUMBER NOT NULL,
    replenishment_order_date DATE NOT NULL,
    status VARCHAR2(20) NOT NULL,

    PRIMARY KEY (replenishment_order_id, stock_number),
    FOREIGN KEY (stock_number) REFERENCES InventoryProduct(stock_number),

    CHECK (quantity_ordered > 0),
    CHECK (LOWER(status) IN ('pending', 'ordered', 'received', 'cancelled'))
);