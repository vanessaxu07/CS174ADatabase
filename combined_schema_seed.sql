-- ---------- Drop old tables safely ----------
BEGIN
    FOR t IN (
        SELECT table_name
        FROM user_tables
        WHERE table_name IN (
            'REPLENISHMENTORDER', 'WAREHOUSEORDER', 'SHIPMENT', 'SHIPPINGNOTICE',
            'CART_ITEMS', 'ORDER_ITEM', 'SHOPPING_CART', 'CUSTOMER_ORDERS',
            'PRODUCT_DESCRIPTION', 'PRODUCT_COMPATIBILITY', 'MANAGERS',
            'DISCOUNTRULES', 'CUSTOMER', 'INVENTORYPRODUCT', 'PRODUCTS'
        )
    ) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
END;
/

-- ---------- eMART tables ----------
CREATE TABLE Products (
    StockNumber CHAR(7),
    Category VARCHAR2(20),
    Manufacturer VARCHAR2(20) NOT NULL,
    ModelNumber VARCHAR2(20),
    Warranty INT NOT NULL,
    Price NUMBER(10, 2) NOT NULL,
    PRIMARY KEY (StockNumber),
    CONSTRAINT chk_prod_price CHECK (Price >= 0),
    CONSTRAINT chk_prod_stock CHECK (REGEXP_LIKE(StockNumber, '^[A-Z]{2}[0-9]{5}$'))
);

CREATE TABLE Product_Compatibility (
    StockNumber CHAR(7),
    CompatibleWithNumber CHAR(7),
    PRIMARY KEY (StockNumber, CompatibleWithNumber),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber) ON DELETE CASCADE,
    FOREIGN KEY (CompatibleWithNumber) REFERENCES Products(StockNumber) ON DELETE CASCADE
);

CREATE TABLE Product_Description (
    StockNumber CHAR(7) NOT NULL,
    AttributeName VARCHAR2(20),
    AttributeValue VARCHAR2(20),
    PRIMARY KEY (StockNumber, AttributeName),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber) ON DELETE CASCADE
);

CREATE TABLE Customer (
    Identifier VARCHAR2(20),
    Password VARCHAR2(20) NOT NULL,
    Name VARCHAR2(20) NOT NULL,
    Email VARCHAR2(20) ,
    Address VARCHAR2(60),
    Status VARCHAR2(20) DEFAULT 'New',
    PRIMARY KEY (Identifier)
);

CREATE TABLE Managers (
    Identifier VARCHAR2(20),
    Password VARCHAR2(20) NOT NULL,
    PRIMARY KEY (Identifier)
);

CREATE TABLE DiscountRules (
    status_name VARCHAR2(20) PRIMARY KEY,
    discount_rate NUMBER(4, 2),
    shipping_rate NUMBER(4, 2),
    shipping_threshold NUMBER(10, 2)
);

CREATE TABLE Shopping_Cart (
    CartId VARCHAR2(20),
    CreatedDate DATE DEFAULT SYSDATE NOT NULL,
    Identifier VARCHAR2(20) NOT NULL UNIQUE,
    PRIMARY KEY (CartId),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier) ON DELETE CASCADE
);

CREATE TABLE Customer_Orders (
    OrderNum VARCHAR2(30),
    Identifier VARCHAR2(20) NOT NULL,
    Subtotal NUMBER(10, 2) NOT NULL,
    OrderDate DATE DEFAULT SYSDATE NOT NULL,
    Discount NUMBER(10, 2),
    Shipping VARCHAR2(20),
    Total NUMBER(10, 2) NOT NULL,
    PRIMARY KEY (OrderNum),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier),
    CONSTRAINT chk_order_math CHECK (Total >= 0 AND Discount >= 0 AND Subtotal >= 0)
);

CREATE TABLE Cart_Items (
    StockNumber CHAR(7),
    CartId VARCHAR2(20),
    Quantity INT NOT NULL,
    PRIMARY KEY (StockNumber, CartId),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber) ON DELETE CASCADE,
    FOREIGN KEY (CartId) REFERENCES Shopping_Cart(CartId) ON DELETE CASCADE,
    CONSTRAINT chk_cart_qty CHECK (Quantity > 0)
);

CREATE TABLE Order_Item (
    StockNumber CHAR(7),
    OrderNum VARCHAR2(30),
    Quantity INT NOT NULL,
    SavedUnitPrice NUMBER(10, 2) NOT NULL,
    PRIMARY KEY (StockNumber, OrderNum),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber),
    FOREIGN KEY (OrderNum) REFERENCES Customer_Orders(OrderNum) ON DELETE CASCADE,
    CONSTRAINT chk_order_qty CHECK (Quantity > 0)
);

-- ---------- eDEPOT tables ----------
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
    CHECK (REGEXP_LIKE(location, '^[A-Za-z](0|[1-9][0-9]*)$'))
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
    order_number VARCHAR2(30) NOT NULL,
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

-- ---------- Seed data from SampleData.xlsx ----------
-- Demo starts with product catalog, inventory, customers, and managers only.
-- No carts, customer orders, order items, warehouse orders, shipments, or replenishment orders are preloaded.

-- Discount rules
INSERT INTO DiscountRules (status_name, discount_rate, shipping_rate, shipping_threshold) VALUES ('Gold', 0.10, 0.10, 100.00);
INSERT INTO DiscountRules (status_name, discount_rate, shipping_rate, shipping_threshold) VALUES ('Silver', 0.05, 0.10, 100.00);
INSERT INTO DiscountRules (status_name, discount_rate, shipping_rate, shipping_threshold) VALUES ('Green', 0.00, 0.10, 100.00);
INSERT INTO DiscountRules (status_name, discount_rate, shipping_rate, shipping_threshold) VALUES ('New', 0.10, 0.00, 0.00);

-- Products
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00101', 'Laptop', 'HP', 'A6111', 12, 1630.00);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00201', 'Desktop', 'Dell', 'B420', 12, 239.00);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00202', 'Desktop', 'eMachines', 'C3958', 12, 369.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00301', 'Monitor', 'Envision', 'D720', 36, 69.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00302', 'Monitor', 'Samsung', 'E712', 36, 279.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00401', 'Software', 'Symantec', 'F2005', 60, 19.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00402', 'Software', 'McAfee', 'G2005', 60, 19.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00403', 'Software', 'Oracle', 'H26', 12, 29.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00501', 'Printer', 'HP', 'J1320', 12, 299.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00601', 'Camera', 'HP', 'K435', 3, 119.99);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price) VALUES ('AA00602', 'Camera', 'Canon', 'L738', 1, 329.99);

-- Product compatibility
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00301', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00301', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00302', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00302', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00401', 'AA00101');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00401', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00401', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00402', 'AA00101');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00402', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00402', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00403', 'AA00101');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00403', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00403', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00501', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00501', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00601', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00601', 'AA00202');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00602', 'AA00201');
INSERT INTO Product_Compatibility (StockNumber, CompatibleWithNumber) VALUES ('AA00602', 'AA00202');

-- Product descriptions
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00101', 'Processor speed', '3.33Ghz');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00101', 'Ram size', '512 Mb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00101', 'Hard disk size', '100Gb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00101', 'Display Size', '17”');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00201', 'Processor speed', '2.53Ghz');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00201', 'Ram size', '256 Mb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00201', 'Hard disk size', '80Gb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00201', 'OS', 'none');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00202', 'Processor speed', '2.9Ghz');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00202', 'Ram size', '512 Mb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00202', 'Hard disk size', '80Gb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00301', 'Size', '17”');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00301', 'Weight', '25 lb.');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00302', 'Size', '17”');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00302', 'Weight', '9.6 lb.');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00401', 'Required disk size', '128 MB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00401', 'Required RAM size', '64 MB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00402', 'Required disk size', '128 MB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00402', 'Required RAM size', '64 MB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00403', 'Required disk size', '1 GB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00403', 'Required RAM size', '128 MB');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00501', 'Resolution', '1200 dpi');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00501', 'Sheet capacity', '500');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00501', 'Weight', '.4 lb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00601', 'Resolution', '3.1 Mp');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00601', 'Max zoom', '5 times');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00601', 'Weight', '24.7 lb');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00602', 'Resolution', '3.1 Mp');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00602', 'Max zoom', '5 times');
INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) VALUES ('AA00602', 'Weight', '24.7 lb');

-- Customers
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Lkim', 'Lkim', 'Linda Kim', 'lkim@cs', '45 Oak Ave, Santa Barbara, CA 93101', 'Gold');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Djones', 'Djones', 'Derek Jones', 'djones@cs', '88 Pine St, Goleta, CA 93117', 'Silver');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Mramirez', 'Mramirez', 'Maria Ramirez', 'mramirez@cs', '12 Maple Rd, Carpinteria, CA 93013', 'New');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Tpatel', 'Tpatel', 'Tariq Patel', 'tpatel@ce', '305 Elm Blvd, Ventura, CA 93001', 'New');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Swong', 'Swong', 'Sarah Wong', 'swong@ce', '77 Cedar Lane, Ojai, CA 93023', 'Green');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Bford', 'Bford', 'Blake Ford', 'bford@ce', '200 Spruce Ct, Oxnard, CA 93030', 'Green');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Tcodd', 'Tcodd', 'Ted Codd', 'tcodd@db', '123 Database St, Data, CA 93116', 'Gold');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Pchen', 'Pchen', 'Peter Chen', 'pchen@db', '456 Database Wy, Datum, CA 93117', 'Silver');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Jgray', 'Jgray', 'Jim Gray', 'jgray@db', '789 Database Rd, Datas, CA 93118', 'Green');
INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES ('Dknuth', 'Dknuth', 'Donald Knuth', 'dknuth@cs', '101 Compsci Ln, Comp, CA 94305', 'Gold');

-- Managers
INSERT INTO Managers (Identifier, Password) VALUES ('Swong', 'Swong');
INSERT INTO Managers (Identifier, Password) VALUES ('Tcodd', 'Tcodd');

-- Inventory products
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00101', 'HP', 'A6111', 2, 1, 2, 'A9', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00201', 'Dell', 'B420', 3, 2, 5, 'A7', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00202', 'eMachines', 'C3958', 4, 2, 5, 'B52', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00301', 'Envision', 'D720', 4, 3, 6, 'C27', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00302', 'Samsung', 'E712', 5, 3, 6, 'C13', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00401', 'Symantec', 'F2005', 7, 5, 9, 'D27', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00402', 'McAfee', 'G2005', 7, 5, 9, 'D15', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00403', 'Oracle', 'H26', 7, 5, 9, 'D3', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00501', 'HP', 'J1320', 3, 2, 4, 'E7', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00601', 'HP', 'K435', 3, 2, 5, 'F9', 0);
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) VALUES ('AA00602', 'Canon', 'L738', 3, 2, 5, 'F3', 0);

COMMIT;