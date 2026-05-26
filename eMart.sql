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
    Shipping VARCHAR(20),
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

INSERT INTO DiscountRules VALUES ('gold',   0.10, 0.10, 100);
INSERT INTO DiscountRules VALUES ('new',    0.10, 0.00, 0);
INSERT INTO DiscountRules VALUES ('silver', 0.05, 0.10, 100);
INSERT INTO DiscountRules VALUES ('green',  0.00, 0.10, 100);
COMMIT;


DROP TABLE Order_Item CASCADE CONSTRAINTS;
DROP TABLE Cart_Items CASCADE CONSTRAINTS;
DROP TABLE Product_Description CASCADE CONSTRAINTS;
DROP TABLE Shopping_Cart CASCADE CONSTRAINTS;
DROP TABLE Customer_Orders CASCADE CONSTRAINTS;
DROP TABLE Products CASCADE CONSTRAINTS;
DROP TABLE Customer CASCADE CONSTRAINTS;


-- Clear out any existing rows before seeding to avoid primary key conflicts
DELETE FROM Order_Item;
DELETE FROM Cart_Items;
DELETE FROM Product_Description;
DELETE FROM Product_Compatibility;
DELETE FROM Shopping_Cart;
DELETE FROM Customer_Orders;
DELETE FROM Customer;
DELETE FROM Managers;
DELETE FROM Products;
DELETE FROM DiscountRules;
DELETE FROM InventoryProduct;
COMMIT;

--- ====================================================================
--- 1. SEED SYSTEM RULES (DiscountRules Map)
--- ====================================================================
INSERT INTO DiscountRules VALUES ('gold',   0.10, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('silver', 0.05, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('green',  0.00, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('new',    0.10, 0.00, 0.00);
INSERT INTO DiscountRules VALUES ('Gold',   0.10, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('Silver', 0.05, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('Green',  0.00, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('New',    0.10, 0.00, 0.00);

--- ====================================================================
--- 2. SEED CORE PRODUCT CATALOG (eMART Database)
--- ====================================================================
INSERT INTO Products VALUES ('AA00101', 'Laptop',   'HP',        'A6111', 12, 1630.00);
INSERT INTO Products VALUES ('AA00201', 'Desktop',  'Dell',      'B420',  12, 239.00);
INSERT INTO Products VALUES ('AA00202', 'Desktop',  'eMachines', 'C3958', 12, 369.99);
INSERT INTO Products VALUES ('AA00301', 'Monitor',  'Envision',  'D720',  36, 69.99);
INSERT INTO Products VALUES ('AA00302', 'Monitor',  'Samsung',   'E712',  36, 279.99);
INSERT INTO Products VALUES ('AA00401', 'Software', 'Symantec',  'F2005', 60, 19.99);
INSERT INTO Products VALUES ('AA00402', 'Software', 'McAfee',    'G2005', 60, 19.99);
INSERT INTO Products VALUES ('AA00403', 'Software', 'Oracle',    'H26',   12, 29.99);
INSERT INTO Products VALUES ('AA00501', 'Printer',  'HP',        'J1320', 12, 299.99);
INSERT INTO Products VALUES ('AA00601', 'Camera',   'HP',        'K435',  3,  119.99);
INSERT INTO Products VALUES ('AA00602', 'Camera',   'Canon',     'L738',  1,  329.99);

--- ====================================================================
--- 3. SEED PRODUCT COMPATIBILITY LISTS (Link Table Model)
--- ====================================================================
-- Monitors are compatible with desktops
INSERT INTO Product_Compatibility VALUES ('AA00301', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00301', 'AA00202');
INSERT INTO Product_Compatibility VALUES ('AA00302', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00302', 'AA00202');

-- Software packages are compatible with hardware systems
INSERT INTO Product_Compatibility VALUES ('AA00401', 'AA00101');
INSERT INTO Product_Compatibility VALUES ('AA00401', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00401', 'AA00202');
INSERT INTO Product_Compatibility VALUES ('AA00402', 'AA00101');
INSERT INTO Product_Compatibility VALUES ('AA00402', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00402', 'AA00202');
INSERT INTO Product_Compatibility VALUES ('AA00403', 'AA00101');
INSERT INTO Product_Compatibility VALUES ('AA00403', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00403', 'AA00202');

-- Printers and cameras are compatible with desktops
INSERT INTO Product_Compatibility VALUES ('AA00501', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00501', 'AA00202');
INSERT INTO Product_Compatibility VALUES ('AA00601', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00601', 'AA00202');
INSERT INTO Product_Compatibility VALUES ('AA00602', 'AA00201');
INSERT INTO Product_Compatibility VALUES ('AA00602', 'AA00202');

--- ====================================================================
--- 4. SEED DETAILED KEY-VALUE ATTRIBUTES (Product_Description)
--- ====================================================================
INSERT INTO Product_Description VALUES ('AA00101', 'Processor speed', '3.33Ghz');
INSERT INTO Product_Description VALUES ('AA00101', 'Ram size', '512 MB');
INSERT INTO Product_Description VALUES ('AA00101', 'Hard disk size', '100Gb');
INSERT INTO Product_Description VALUES ('AA00101', 'Display Size', '17\"');

INSERT INTO Product_Description VALUES ('AA00201', 'Processor speed', '2.53Ghz');
INSERT INTO Product_Description VALUES ('AA00201', 'Ram size', '256 Mb');
INSERT INTO Product_Description VALUES ('AA00201', 'Hard disk size', '80Gb');
INSERT INTO Product_Description VALUES ('AA00201', 'OS', 'none');

INSERT INTO Product_Description VALUES ('AA00202', 'Processor speed', '2.9Ghz');
INSERT INTO Product_Description VALUES ('AA00202', 'Ram size', '512 Mb');
INSERT INTO Product_Description VALUES ('AA00202', 'Hard disk size', '80Gb');

INSERT INTO Product_Description VALUES ('AA00301', 'Size', '17\"');
INSERT INTO Product_Description VALUES ('AA00301', 'Weight', '25 lb.');

INSERT INTO Product_Description VALUES ('AA00302', 'Size', '17\"');
INSERT INTO Product_Description VALUES ('AA00302', 'Weight', '9.6 lb.');

INSERT INTO Product_Description VALUES ('AA00401', 'Required disk size', '128 MB');
INSERT INTO Product_Description VALUES ('AA00401', 'Required RAM size', '64 MB');

INSERT INTO Product_Description VALUES ('AA00402', 'Required disk size', '128 MB');
INSERT INTO Product_Description VALUES ('AA00402', 'Required RAM size', '64 MB');

INSERT INTO Product_Description VALUES ('AA00403', 'Required disk size', '1 GB');
INSERT INTO Product_Description VALUES ('AA00403', 'Required RAM size', '128 MB');

INSERT INTO Product_Description VALUES ('AA00501', 'Resolution', '1200 dpi');
INSERT INTO Product_Description VALUES ('AA00501', 'Sheet capacity', '500');
INSERT INTO Product_Description VALUES ('AA00501', 'Weight', '.4 lb');

INSERT INTO Product_Description VALUES ('AA00601', 'Resolution', '3.1 Mp');
INSERT INTO Product_Description VALUES ('AA00601', 'Max zoom', '5 times');
INSERT INTO Product_Description VALUES ('AA00601', 'Weight', '24.7 lb');

INSERT INTO Product_Description VALUES ('AA00602', 'Resolution', '3.1 Mp');
INSERT INTO Product_Description VALUES ('AA00602', 'Max zoom', '5 times');
INSERT INTO Product_Description VALUES ('AA00602', 'Weight', '24.7 lb');

--- ====================================================================
--- 5. SEED ACCOUNT PROFILES (Customers & Managers Data)
--- ====================================================================
-- Standard customer accounts
INSERT INTO Customer VALUES ('C001', 'pass1', 'Alice Smith', 'alice@gmail.com', '123 Main St', 'gold');
INSERT INTO Customer VALUES ('C002', 'pass2', 'Bob Jones', 'bob@gmail.com', '456 Oak Ave', 'silver');
INSERT INTO Customer VALUES ('C003', 'pass3', 'Carol White', 'carol@gmail.com', '789 Pine Rd', 'new');
INSERT INTO Customer VALUES ('C004', 'pass4', 'David Brown', 'david@gmail.com', '321 Elm St', 'green');

-- Spreadsheet sample accounts
INSERT INTO Customer VALUES ('Lkim', 'Lkim', 'Linda Kim', 'lkim@cs', '45 Oak Ave, Santa Barbara, CA 93101', 'Gold');
INSERT INTO Customer VALUES ('Djones', 'Djones', 'Derek Jones', 'djones@cs', '88 Pine St, Goleta, CA 93117', 'Silver');
INSERT INTO Customer VALUES ('Mramirez', 'Mramirez', 'Maria Ramirez', 'mramirez@cs', '12 Maple Rd, Carpinteria, CA 93013', 'New');
INSERT INTO Customer VALUES ('Tpatel', 'Tpatel', 'Tariq Patel', 'tpatel@ce', '305 Elm Blvd, Ventura, CA 93001', 'New');
INSERT INTO Customer VALUES ('Bford', 'Bford', 'Blake Ford', 'bford@ce', '200 Spruce Ct, Oxnard, CA 93030', 'Green');
INSERT INTO Customer VALUES ('Pchen', 'Pchen', 'Peter Chen', 'pchen@db', '456 Database Wy, Datum, CA 93117', 'Silver');
INSERT INTO Customer VALUES ('Jgray', 'Jgray', 'Jim Gray', 'jgray@db', '789 Database Rd, Datas, CA 93118', 'Green');
INSERT INTO Customer VALUES ('Dknuth', 'Dknuth', 'Donald Knuth', 'dknuth@cs', '101 Compsci Ln, Comp, CA 94305', 'Gold');
INSERT INTO Customer VALUES ('Swong', 'Swong', 'Sarah Wong', 'swong@ce', '77 Cedar Lane, Ojai, CA 93023', 'Green');
INSERT INTO Customer VALUES ('Tcodd', 'Tcodd', 'Ted Codd', 'tcodd@db', '123 Database St, Data, CA 93116', 'Gold');

-- Administration Authority Maps
INSERT INTO Managers VALUES ('M001', 'admin1');
INSERT INTO Managers VALUES ('M002', 'admin2');
INSERT INTO Managers VALUES ('Swong', 'Swong');
INSERT INTO Managers VALUES ('Tcodd', 'Tcodd');

--- ====================================================================
--- 6. SEED SIMULATED BASKETS (Active Carts)
--- ====================================================================
INSERT INTO Shopping_Cart VALUES ('CART001', TO_DATE('2026-05-01', 'YYYY-MM-DD'), 'C001');
INSERT INTO Shopping_Cart VALUES ('CART002', TO_DATE('2026-05-02', 'YYYY-MM-DD'), 'C002');
INSERT INTO Shopping_Cart VALUES ('CART003', TO_DATE('2026-05-03', 'YYYY-MM-DD'), 'C003');

INSERT INTO Cart_Items VALUES ('AA00101', 'CART001', 1);
INSERT INTO Cart_Items VALUES ('AA00201', 'CART001', 2);
INSERT INTO Cart_Items VALUES ('AA00301', 'CART002', 1);
INSERT INTO Cart_Items VALUES ('AA00401', 'CART003', 3);

--- ====================================================================
--- 7. SEED EXACT PHYSICAL INVENTORY LOGISTICS (eDEPOT Database)
--- ====================================================================


DELETE FROM ReplenishmentOrder;
DELETE FROM WarehouseOrder;
DELETE FROM Shipment;
DELETE FROM ShippingNotice;
DELETE FROM InventoryProduct;
COMMIT;
-- Clean out any failed entries first
DELETE FROM InventoryProduct;
COMMIT;

--- ====================================================================
--- SEED INVENTORYPRODUCT (eDEPOT Database) - FULL 8 COLUMNS
--- ====================================================================
INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00101', 'HP', 'A6111', 2, 1, 2, 'A9', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00201', 'Dell', 'B420', 3, 2, 5, 'A7', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00202', 'eMachines', 'C3958', 4, 2, 5, 'B52', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00301', 'Envision', 'D720', 4, 3, 6, 'C27', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00302', 'Samsung', 'E712', 5, 3, 6, 'C13', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00401', 'Symantec', 'F2005', 7, 5, 9, 'D27', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00402', 'McAfee', 'G2005', 7, 5, 9, 'D15', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00403', 'Oracle', 'H26', 7, 5, 9, 'D3', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00501', 'HP', 'J1320', 3, 2, 4, 'E7', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00601', 'HP', 'K435', 3, 2, 5, 'F9', 0);

INSERT INTO InventoryProduct (stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment) 
VALUES ('AA00602', 'Canon', 'L738', 3, 2, 5, 'F3', 0);

COMMIT;

-- export DB_USER=ADMIN export DB_PASSWORD=Vicecreamlover*1 export TNS_ADMIN=/Users/vanessaxu/Downloads/Wallet_CS174AShoppingDatabase