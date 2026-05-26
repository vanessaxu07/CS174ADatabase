-- ============================================================
-- Combined eMART + eDEPOT schema and seed data
-- Run this once in the shared Oracle account.
-- eMART and eDEPOT stay as separate table groups, but share one DB.
-- ============================================================

-- ---------- Drop old tables safely ----------
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ReplenishmentOrder CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE WarehouseOrder CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Shipment CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ShippingNotice CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Cart_Items CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Order_Item CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Shopping_Cart CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Customer_Orders CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Product_Description CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Product_Compatibility CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Managers CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE DiscountRules CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Customer CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE InventoryProduct CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Products CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

-- ---------- eMART tables ----------
CREATE TABLE Products (
    StockNumber CHAR(7),
    Category VARCHAR2(20),
    Manufacturer VARCHAR2(20),
    ModelNumber VARCHAR2(20),
    Warranty INT,
    Price NUMBER(10, 2),
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
    Email VARCHAR2(20),
    Address VARCHAR2(60),
    Status VARCHAR2(20) DEFAULT 'new',
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
    CreatedDate DATE DEFAULT SYSDATE,
    Identifier VARCHAR2(20) NOT NULL UNIQUE,
    PRIMARY KEY (CartId),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier) ON DELETE CASCADE
);

CREATE TABLE Customer_Orders (
    OrderNum VARCHAR2(30),
    Identifier VARCHAR2(20) NOT NULL,
    Subtotal NUMBER(10, 2),
    OrderDate DATE DEFAULT SYSDATE,
    Discount NUMBER(10, 2),
    Shipping VARCHAR2(20),
    Total NUMBER(10, 2),
    PRIMARY KEY (OrderNum),
    FOREIGN KEY (Identifier) REFERENCES Customer(Identifier),
    CONSTRAINT chk_order_math CHECK (Total >= 0 AND Discount >= 0 AND Subtotal >= 0)
);

CREATE TABLE Cart_Items (
    StockNumber CHAR(7),
    CartId VARCHAR2(20),
    Quantity INT,
    PRIMARY KEY (StockNumber, CartId),
    FOREIGN KEY (StockNumber) REFERENCES Products(StockNumber) ON DELETE CASCADE,
    FOREIGN KEY (CartId) REFERENCES Shopping_Cart(CartId) ON DELETE CASCADE,
    CONSTRAINT chk_cart_qty CHECK (Quantity > 0)
);

CREATE TABLE Order_Item (
    StockNumber CHAR(7),
    OrderNum VARCHAR2(30),
    Quantity INT,
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

-- ---------- Seed data ----------
--- ====================================================================
--- 1. SEED SYSTEM RULES (DiscountRules Map)
--- ====================================================================
INSERT INTO DiscountRules VALUES ('gold',   0.10, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('silver', 0.05, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('green',  0.00, 0.10, 100.00);
INSERT INTO DiscountRules VALUES ('new',    0.10, 0.00, 0.00);