CREATE TABLE Products(
    StockNumber CHAR(20),
    Category CHAR(20),
    Manufacturer CHAR(20),
    ModelNumber CHAR(20),
    Warranty INT,
    Price REAL,
    CompatibleWithNumber CHAR(20),
    PRIMARY KEY(StockNumber),
    FOREIGN KEY (CompatibleWithNumber) REFERENCES Products(StockNumber),
    CHECK (Price >= 0),
    CHECK (REGEXP_LIKE(TRIM(StockNumber), '^[A-Z]{2}[0-9]{5}$'))
);
CREATE TABLE Customer(
    Identifier CHAR(20),
    Password CHAR(20),
    Name CHAR(20),
    Email CHAR(20),
    Address CHAR(60),
    Status CHAR(20),
    PRIMARY KEY (Identifier)
);
CREATE TABLE Product_Description(
    StockNumber CHAR(20) NOT NULL,
    AttributeName CHAR(20),
    AttributeValue CHAR(20),
    PRIMARY KEY (StockNumber, AttributeName),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber),
    CHECK (REGEXP_LIKE(TRIM(StockNumber), '^[A-Z]{2}[0-9]{5}$'))
);
CREATE TABLE Shopping_Cart(
    CartId CHAR(20),
    CreatedDate CHAR(20),
    Identifier CHAR(20) NOT NULL,
    PRIMARY KEY (CartId),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier)
);

CREATE TABLE Customer_Orders(
    OrderNum CHAR(20),
    Subtotal REAL,
    OrderDate CHAR(20),
    Discount REAL,
    Shipping CHAR(20),
    Total REAL,
    Identifier CHAR(20) NOT NULL,
    PRIMARY KEY (OrderNum),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier),
    CHECK (Total >= 0 AND Discount >= 0 AND Subtotal >= 0)
);
CREATE TABLE Cart_Items(
    StockNumber CHAR(20),
    CartId CHAR(20),
    Quantity INTEGER,
    PRIMARY KEY (StockNumber, CartId),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber),
    FOREIGN KEY (CartId) REFERENCES Shopping_Cart(CartId), 
    CHECK (Quantity >= 0),
    CHECK (REGEXP_LIKE(TRIM(StockNumber), '^[A-Z]{2}[0-9]{5}$'))
);
CREATE TABLE Order_Item(
    StockNumber CHAR(20),
    OrderNum CHAR(20),
    Quantity INTEGER,
    PRIMARY KEY (StockNumber, OrderNum),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber),
    FOREIGN KEY (OrderNum) REFERENCES Customer_Orders(OrderNum),
    CHECK (Quantity >= 0),
    CHECK (REGEXP_LIKE(TRIM(StockNumber), '^[A-Z]{2}[0-9]{5}$'))

);
CREATE TABLE Managers (
    Identifier CHAR(20),
    Password CHAR(20),
    PRIMARY KEY (Identifier)
);

DROP TABLE Order_Item CASCADE CONSTRAINTS;
DROP TABLE Cart_Items CASCADE CONSTRAINTS;
DROP TABLE Product_Description CASCADE CONSTRAINTS;
DROP TABLE Shopping_Cart CASCADE CONSTRAINTS;
DROP TABLE Customer_Orders CASCADE CONSTRAINTS;
DROP TABLE Products CASCADE CONSTRAINTS;
DROP TABLE Customer CASCADE CONSTRAINTS;





-- Products
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00101', 'Laptop', 'HP', 'A6111', 12, 1630.00, NULL);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00201', 'Desktop', 'Dell', 'B420', 12, 239.00, NULL);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00202', 'Desktop', 'eMachines', 'C3958', 12, 369.99, NULL);
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00301', 'Monitor', 'Envision', 'D720', 36, 69.99, 'AA00201');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00302', 'Monitor', 'Samsung', 'E712', 36, 279.99, 'AA00201');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00401', 'Software', 'Symantec', 'F2005', 60, 19.99, 'AA00101');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00402', 'Software', 'McAfee', 'G2005', 60, 19.99, 'AA00101');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00403', 'Software', 'Oracle', 'H26', 12, 29.99, 'AA00101');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00501', 'Printer', 'HP', 'J1320', 12, 299.99, 'AA00201');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00601', 'Camera', 'HP', 'K435', 3, 119.99, 'AA00201');
INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) VALUES ('AA00602', 'Camera', 'Canon', 'L738', 1, 329.99, 'AA00201');

-- Product Descriptions
INSERT INTO Product_Description VALUES ('AA00101', 'Processor', '3.33Ghz');
INSERT INTO Product_Description VALUES ('AA00101', 'RAM', '512Mb');
INSERT INTO Product_Description VALUES ('AA00101', 'HardDisk', '100Gb');
INSERT INTO Product_Description VALUES ('AA00101', 'Display', '17in');
INSERT INTO Product_Description VALUES ('AA00201', 'Processor', '2.53Ghz');
INSERT INTO Product_Description VALUES ('AA00201', 'RAM', '256Mb');
INSERT INTO Product_Description VALUES ('AA00201', 'HardDisk', '80Gb');
INSERT INTO Product_Description VALUES ('AA00201', 'OS', 'none');
INSERT INTO Product_Description VALUES ('AA00202', 'Processor', '2.9Ghz');
INSERT INTO Product_Description VALUES ('AA00202', 'RAM', '512Mb');
INSERT INTO Product_Description VALUES ('AA00202', 'HardDisk', '80Gb');
INSERT INTO Product_Description VALUES ('AA00301', 'Size', '17in');
INSERT INTO Product_Description VALUES ('AA00301', 'Weight', '25lb');
INSERT INTO Product_Description VALUES ('AA00302', 'Size', '17in');
INSERT INTO Product_Description VALUES ('AA00302', 'Weight', '9.6lb');
INSERT INTO Product_Description VALUES ('AA00401', 'DiskSize', '128MB');
INSERT INTO Product_Description VALUES ('AA00401', 'RAMSize', '64MB');
INSERT INTO Product_Description VALUES ('AA00402', 'DiskSize', '128MB');
INSERT INTO Product_Description VALUES ('AA00402', 'RAMSize', '64MB');
INSERT INTO Product_Description VALUES ('AA00403', 'DiskSize', '1GB');
INSERT INTO Product_Description VALUES ('AA00403', 'RAMSize', '128MB');
INSERT INTO Product_Description VALUES ('AA00501', 'Resolution', '1200dpi');
INSERT INTO Product_Description VALUES ('AA00501', 'SheetCap', '500');
INSERT INTO Product_Description VALUES ('AA00501', 'Weight', '0.4lb');
INSERT INTO Product_Description VALUES ('AA00601', 'Resolution', '3.1Mp');
INSERT INTO Product_Description VALUES ('AA00601', 'MaxZoom', '5times');
INSERT INTO Product_Description VALUES ('AA00601', 'Weight', '24.7lb');
INSERT INTO Product_Description VALUES ('AA00602', 'Resolution', '3.1Mp');
INSERT INTO Product_Description VALUES ('AA00602', 'MaxZoom', '5times');
INSERT INTO Product_Description VALUES ('AA00602', 'Weight', '24.7lb');

-- Customers
INSERT INTO Customer VALUES ('C001', 'pass1', 'Alice Smith', 'alice@gmail.com', '123 Main St', 'gold');
INSERT INTO Customer VALUES ('C002', 'pass2', 'Bob Jones', 'bob@gmail.com', '456 Oak Ave', 'silver');
INSERT INTO Customer VALUES ('C003', 'pass3', 'Carol White', 'carol@gmail.com', '789 Pine Rd', 'new');
INSERT INTO Customer VALUES ('C004', 'pass4', 'David Brown', 'david@gmail.com', '321 Elm St', 'green');

-- Managers
INSERT INTO Managers VALUES ('M001', 'admin1');
INSERT INTO Managers VALUES ('M002', 'admin2');

-- Shopping Carts
INSERT INTO Shopping_Cart VALUES ('CART001', '2026-05-01', 'C001');
INSERT INTO Shopping_Cart VALUES ('CART002', '2026-05-02', 'C002');
INSERT INTO Shopping_Cart VALUES ('CART003', '2026-05-03', 'C003');

-- Cart Items
INSERT INTO Cart_Items VALUES ('AA00101', 'CART001', 1);
INSERT INTO Cart_Items VALUES ('AA00201', 'CART001', 2);
INSERT INTO Cart_Items VALUES ('AA00301', 'CART002', 1);
INSERT INTO Cart_Items VALUES ('AA00401', 'CART003', 3);

-- Customers (non-managers)
INSERT INTO Customer VALUES ('Lkim', 'Lkim', 'Linda Kim', 'lkim@cs', '45 Oak Ave, Santa Barbara, CA 93101', 'Gold');
INSERT INTO Customer VALUES ('Djones', 'Djones', 'Derek Jones', 'djones@cs', '88 Pine St, Goleta, CA 93117', 'Silver');
INSERT INTO Customer VALUES ('Mramirez', 'Mramirez', 'Maria Ramirez', 'mramirez@cs', '12 Maple Rd, Carpinteria, CA 93013', 'New');
INSERT INTO Customer VALUES ('Tpatel', 'Tpatel', 'Tariq Patel', 'tpatel@ce', '305 Elm Blvd, Ventura, CA 93001', 'New');
INSERT INTO Customer VALUES ('Bford', 'Bford', 'Blake Ford', 'bford@ce', '200 Spruce Ct, Oxnard, CA 93030', 'Green');
INSERT INTO Customer VALUES ('Pchen', 'Pchen', 'Peter Chen', 'pchen@db', '456 Database Wy, Datum, CA 93117', 'Silver');
INSERT INTO Customer VALUES ('Jgray', 'Jgray', 'Jim Gray', 'jgray@db', '789 Database Rd, Datas, CA 93118', 'Green');
INSERT INTO Customer VALUES ('Dknuth', 'Dknuth', 'Donald Knuth', 'dknuth@cs', '101 Compsci Ln, Comp, CA 94305', 'Gold');

-- Managers (Swong and Tcodd have MANAGER = TRUE)
INSERT INTO Managers VALUES ('Swong', 'Swong');
INSERT INTO Managers VALUES ('Tcodd', 'Tcodd');

-- Also insert managers as customers since they still have customer details
INSERT INTO Customer VALUES ('Swong', 'Swong', 'Sarah Wong', 'swong@ce', '77 Cedar Lane, Ojai, CA 93023', 'Green');
INSERT INTO Customer VALUES ('Tcodd', 'Tcodd', 'Ted Codd', 'tcodd@db', '123 Database St, Data, CA 93116', 'Gold');

COMMIT;