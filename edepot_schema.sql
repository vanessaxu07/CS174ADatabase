CREATE TABLE InventoryProduct (
    stock_num VARCHAR(20) PRIMARY KEY,
    model_num VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    min_stock_level INTEGER NOT NULL,
    max_stock_level INTEGER NOT NULL,
    replenishment_quantity INTEGER NOT NULL,
    manufacturer VARCHAR(20) NOT NULL,
    location VARCHAR(20) NOT NULL
);

CREATE TABLE ShippingNotice (
    notice_id VARCHAR(20) PRIMARY KEY,
    notice_date DATE NOT NULL,
    shipping_company_name VARCHAR(20) NOT NULL,
    manufacturerID VARCHAR(20) NOT NULL
);

CREATE TABLE ProductShipment (
    shipment_id VARCHAR(20) PRIMARY KEY,
    arrival_date DATE NOT NULL,
); 

CREATE TABLE ReplenishmentOrder (
    replenish_order_id VARCHAR(20) PRIMARY KEY,
    replenish_order_date DATE NOT NULL,
    manufacturerID VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (manufacturerID) REFERENCES Manufacturer(manufacturerID)
);

