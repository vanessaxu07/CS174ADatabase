-- check item quantity
SELECT stock_number, manufacturer_name, model_number, quantity, replenishment
FROM InventoryProduct
WHERE stock_number = :stock_number;

-- insert new inventory product if needed
INSERT INTO InventoryProduct (
  stock_number, manufacturer_name, model_number, quantity, min_stock_level, max_stock_level, location, replenishment
)
SELECT :stock_number, :manufacturer_name, :model_number, 0, :min_stock_level, :max_stock_level, :location, 0
FROM dual
WHERE NOT EXISTS (
  SELECT 1
  FROM InventoryProduct
  WHERE stock_number = :stock_number
);

-- receive shipping notice
INSERT INTO ShippingNotice (
  notice_id, stock_number, quantity, shipping_company_name, notice_date
)
VALUES (
  :notice_id, :stock_number, :quantity, :shipping_company_name, SYSDATE
);

UPDATE InventoryProduct
SET replenishment = replenishment + :quantity
WHERE stock_number = :stock_number;

-- receive shipment
INSERT INTO Shipment (
    shipment_id, notice_id, stock_number, quantity_received, arrival_date
)
VALUES (
    :shipment_id, :notice_id, :stock_number, :quantity_received, SYSDATE
);

UPDATE InventoryProduct
SET quantity = quantity + :quantity_received,
    replenishment = replenishment - :quantity_received
WHERE stock_number = :stock_number
  AND replenishment >= :quantity_received;

-- check whether an eMART order can be filled
SELECT oi.OrderNum,
       oi.StockNumber,
       oi.Quantity AS quantity_ordered,
       ip.quantity AS inventory_quantity
FROM Order_Item oi
LEFT JOIN InventoryProduct ip
    ON ip.stock_number = TRIM(oi.StockNumber)
WHERE oi.OrderNum = :order_number
  AND (ip.stock_number IS NULL OR ip.quantity < oi.Quantity);

-- fill eMART order
-- first run the check above; only fill the order if it returns no rows

INSERT INTO WarehouseOrder (
    order_number, stock_number, quantity_ordered, order_date, status
)
SELECT oi.OrderNum, TRIM(oi.StockNumber), oi.Quantity, SYSDATE, 'pending'
FROM Order_Item oi
WHERE oi.OrderNum = :order_number
  AND NOT EXISTS (
      SELECT 1
      FROM WarehouseOrder wo
      WHERE wo.order_number = oi.OrderNum
        AND wo.stock_number = TRIM(oi.StockNumber)
  );

UPDATE InventoryProduct ip
SET quantity = quantity - (
    SELECT SUM(oi.Quantity)
    FROM Order_Item oi
    WHERE oi.OrderNum = :order_number
      AND TRIM(oi.StockNumber) = ip.stock_number
)
WHERE EXISTS (
    SELECT 1
    FROM Order_Item oi
    WHERE oi.OrderNum = :order_number
      AND TRIM(oi.StockNumber) = ip.stock_number
)
AND ip.quantity >= (
    SELECT SUM(oi.Quantity)
    FROM Order_Item oi
    WHERE oi.OrderNum = :order_number
      AND TRIM(oi.StockNumber) = ip.stock_number
);

UPDATE WarehouseOrder
SET status = 'filled'
WHERE order_number = :order_number;

-- find manufacturers that need replenishment
SELECT manufacturer_name
FROM InventoryProduct
WHERE quantity < min_stock_level
GROUP BY manufacturer_name
HAVING COUNT(*) >= 3;

-- generate replenishment order
INSERT INTO ReplenishmentOrder (
    replenishment_order_id, stock_number,
    quantity_ordered, replenishment_order_date, status
)
SELECT :replenishment_order_id,
       stock_number,
       max_stock_level - quantity - replenishment,
       SYSDATE,
       'pending'
FROM InventoryProduct
WHERE manufacturer_name = :manufacturer_name
  AND quantity < max_stock_level
  AND max_stock_level - quantity - replenishment > 0
  AND manufacturer_name IN (
      SELECT manufacturer_name
      FROM InventoryProduct
      WHERE quantity < min_stock_level
      GROUP BY manufacturer_name
      HAVING COUNT(*) >= 3
  );

UPDATE InventoryProduct ip
SET replenishment = replenishment + (
    SELECT ro.quantity_ordered
    FROM ReplenishmentOrder ro
    WHERE ro.replenishment_order_id = :replenishment_order_id
      AND ro.stock_number = ip.stock_number
)
WHERE EXISTS (
    SELECT 1
    FROM ReplenishmentOrder ro
    WHERE ro.replenishment_order_id = :replenishment_order_id
      AND ro.stock_number = ip.stock_number
);