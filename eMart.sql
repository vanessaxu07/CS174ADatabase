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
    CHECK (REGEXP_LIKE(StockNumber, '^[A-Z]{2}[0-9]{5}$'))
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
    CHECK (REGEXP_LIKE(StockNumber, '^[A-Z]{2}[0-9]{5}$'))

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
    CHECK (REGEXP_LIKE(StockNumber, '^[A-Z]{2}[0-9]{5}$'))

);
CREATE TABLE Order_Item(
    StockNumber CHAR(20),
    OrderNum CHAR(20),
    Quantity INTEGER,
    PRIMARY KEY (StockNumber, OrderNum),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber),
    FOREIGN KEY (OrderNum) REFERENCES Customer_Orders(OrderNum),
    CHECK (Quantity >= 0),
    CHECK (REGEXP_LIKE(StockNumber, '^[A-Z]{2}[0-9]{5}$'))

);

DROP TABLE Order_Item CASCADE CONSTRAINTS;
DROP TABLE Products CASCADE CONSTRAINTS;
DROP TABLE Product_Description CASCADE CONSTRAINTS;
DROP TABLE Cart_Items CASCADE CONSTRAINTS;



