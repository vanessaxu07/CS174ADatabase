package org.ivc.dbms.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EdepotApp {

    static Connection con = null;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");

            String tnsAdmin = System.getenv("TNS_ADMIN");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (tnsAdmin != null && !tnsAdmin.isBlank()) {
                System.setProperty("oracle.net.tns_admin", tnsAdmin);
            }
            if (dbUser == null || dbUser.isBlank()) {
                dbUser = "ADMIN";
            }
            if (dbPassword == null || dbPassword.isBlank()) {
                System.out.println("Missing DB_PASSWORD environment variable.");
                return;
            }

            con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@emartdepot_low",
                    dbUser,
                    dbPassword
            );
            con.setAutoCommit(false);
            System.out.println("Connected!");

            runDepotMenu();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found: " + e.getMessage());
        } finally {
            try {
                if (con != null) con.close();
                System.out.println("Connection closed.");
            } catch (SQLException e) {
                System.out.println("Error closing: " + e.getMessage());
            }
        }
    }

    static void runDepotMenu() throws SQLException {
        boolean running = true;
        while (running) {
            System.out.println("\n--------------- eDEPOT Menu ---------------");
            System.out.println("1. View inventory");
            System.out.println("2. Check item quantity");
            System.out.println("3. Receive shipping notice");
            System.out.println("4. Receive shipment");
            System.out.println("5. Fill eMART order");
            System.out.println("6. Show manufacturers needing replenishment");
            System.out.println("7. Generate replenishment orders");
            System.out.println("8. Exit");
            System.out.print("Choose: ");

            int choice = readMenuChoice();

            if (choice == 1) viewInventory();
            else if (choice == 2) checkItemQuantity();
            else if (choice == 3) receiveShippingNotice();
            else if (choice == 4) receiveShipment();
            else if (choice == 5) fillEmartOrder();
            else if (choice == 6) showManufacturersNeedingReplenishment();
            else if (choice == 7) generateReplenishmentOrders();
            else if (choice == 8) running = false;
            else System.out.println("Invalid Choice");
        }
    }

    static void viewInventory() throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT stock_number, manufacturer_name, model_number, quantity, " +
                "min_stock_level, max_stock_level, location, replenishment " +
                "FROM InventoryProduct ORDER BY stock_number"
        );
        ResultSet rs = ps.executeQuery();

        System.out.println("\n" + String.format("%-10s %-15s %-15s %-8s %-8s %-8s %-10s %s",
                "Stock#", "Manufacturer", "Model", "Qty", "Min", "Max", "Location", "Repl"));
        System.out.println("--------------------------------------------------------------------------------");

        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.printf("%-10s %-15s %-15s %-8d %-8d %-8d %-10s %d%n",
                    rs.getString("stock_number").trim(),
                    rs.getString("manufacturer_name").trim(),
                    rs.getString("model_number").trim(),
                    rs.getInt("quantity"),
                    rs.getInt("min_stock_level"),
                    rs.getInt("max_stock_level"),
                    rs.getString("location").trim(),
                    rs.getInt("replenishment")
            );
        }
        if (!found) System.out.println("No inventory products found.");

        rs.close();
        ps.close();
    }

    static void checkItemQuantity() throws SQLException {
        String stockNum = readStockNumber("Enter Stock Number: ");
        if (stockNum == null) return;

        PreparedStatement ps = con.prepareStatement(
                "SELECT stock_number, manufacturer_name, model_number, quantity, replenishment " +
                "FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("Stock Number: " + rs.getString("stock_number").trim());
            System.out.println("Manufacturer: " + rs.getString("manufacturer_name").trim());
            System.out.println("Model Number: " + rs.getString("model_number").trim());
            System.out.println("Quantity: " + rs.getInt("quantity"));
            System.out.println("Replenishment: " + rs.getInt("replenishment"));
        } else {
            System.out.println("No product found in inventory.");
        }

        rs.close();
        ps.close();
    }

    static void receiveShippingNotice() throws SQLException {
        String noticeId = readText("Enter Notice ID: ");
        if (!validText(noticeId, "Notice ID")) return;

        String shippingCompany;
        if (noticeIdExists(noticeId)) {
            shippingCompany = getShippingCompanyForNotice(noticeId);
            System.out.println("This notice ID already exists. Adding items to the existing notice.");
            System.out.println("Shipping Company: " + shippingCompany);
        } else {
            shippingCompany = readText("Enter Shipping Company: ");
            if (!validText(shippingCompany, "Shipping company")) return;
        }

        boolean addingItems = true;
        while (addingItems) {
            boolean added = receiveOneShippingNoticeItem(noticeId, shippingCompany);

            if (!added) {
                System.out.print("Try another item for this notice? (y/n): ");
            } else {
                System.out.print("Add another item to this same notice? (y/n): ");
            }
            addingItems = scanner.nextLine().trim().equalsIgnoreCase("y");
        }
    }

    static boolean receiveOneShippingNoticeItem(String noticeId, String shippingCompany) throws SQLException {
        String manufacturer = readText("Enter Manufacturer: ");
        String modelNum = readText("Enter Model Number: ");
        int quantity = readPositiveInt("Enter Quantity: ");

        if (!validText(manufacturer, "Manufacturer")) return false;
        if (!validText(modelNum, "Model number")) return false;
        if (quantity <= 0) return false;

        String stockNum = findStockNumber(manufacturer, modelNum);
        boolean newProduct = false;
        int minStock = 0;
        int maxStock = 0;
        String location = null;

        if (stockNum == null) {
            newProduct = true;
            System.out.println("New product detected. Please assign inventory information.");
            stockNum = readStockNumber("Enter New Stock Number, for example AA00001: ");
            if (stockNum == null) return false;

            if (inventoryProductExists(stockNum)) {
                System.out.println("That stock number already exists. Please use a new unique stock number.");
                return false;
            }

            minStock = readNonNegativeInt("Enter Minimum Stock Level: ");
            maxStock = readNonNegativeInt("Enter Maximum Stock Level: ");
            location = readText("Enter Warehouse Location, for example A1: ");

            if (minStock < 0 || maxStock < 0) return false;
            if (maxStock < minStock) {
                System.out.println("Maximum stock level must be greater than or equal to minimum stock level.");
                return false;
            }
            if (!validLocation(location)) return false;
            location = location.toUpperCase();

            if (quantity > maxStock) {
                System.out.println("Quantity in notice would exceed the maximum stock level for this new product.");
                return false;
            }
        } else {
            System.out.println("Existing product found. Stock Number: " + stockNum);
            if (!canAddReplenishment(stockNum, quantity)) return false;
        }

        if (shippingNoticeExists(noticeId, stockNum)) {
            if (shipmentNoticeItemAlreadyReceived(noticeId, stockNum)) {
                System.out.println("This notice item has already been received, so its quantity cannot be changed.");
                return false;
            }

            try {
                addQuantityToShippingNoticeItem(noticeId, stockNum, quantity);
                con.commit();
                System.out.println("This product was already in the notice. Added quantity to the existing notice item.");
                return true;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Could not update existing notice item: " + e.getMessage());
                return false;
            }
        }

        try {
            if (newProduct) {
                insertInventoryProduct(stockNum, manufacturer, modelNum, minStock, maxStock, location);
            }

            PreparedStatement ps1 = con.prepareStatement(
                    "INSERT INTO ShippingNotice " +
                    "(notice_id, stock_number, quantity, shipping_company_name, notice_date) " +
                    "VALUES (?, ?, ?, ?, SYSDATE)"
            );
            ps1.setString(1, noticeId);
            ps1.setString(2, stockNum);
            ps1.setInt(3, quantity);
            ps1.setString(4, shippingCompany);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE InventoryProduct " +
                    "SET replenishment = replenishment + ? " +
                    "WHERE TRIM(stock_number) = TRIM(?)"
            );
            ps2.setInt(1, quantity);
            ps2.setString(2, stockNum);
            ps2.executeUpdate();
            ps2.close();

            con.commit();
            System.out.println("Shipping notice item received. Replenishment updated for " + stockNum + ".");
            return true;
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Shipping notice failed: " + e.getMessage());
            return false;
        }
    }

    static void receiveShipment() throws SQLException {
        String shipmentId = readText("Enter Shipment ID: ");
        if (!validText(shipmentId, "Shipment ID")) return;

        String noticeId = readText("Enter Notice ID: ");
        if (!validText(noticeId, "Notice ID")) return;

        String stockNum = readStockNumber("Enter Stock Number: ");
        if (stockNum == null) return;

        int quantityReceived = readPositiveInt("Enter Quantity Received: ");
        if (quantityReceived <= 0) return;

        int noticeQuantity = getNoticeQuantity(noticeId, stockNum);
        if (noticeQuantity == -1) {
            System.out.println("No matching shipping notice found.");
            return;
        }

        if (shipmentIdStockExists(shipmentId, stockNum)) {
            System.out.println("This shipment ID already has this product.");
            return;
        }

        if (shipmentNoticeItemAlreadyReceived(noticeId, stockNum)) {
            System.out.println("This notice item has already been received.");
            return;
        }

        if (quantityReceived != noticeQuantity) {
            System.out.println("Quantity received must match the shipping notice quantity: " + noticeQuantity);
            return;
        }

        int replenishment = getReplenishment(stockNum);
        if (replenishment < quantityReceived) {
            System.out.println("Not enough replenishment recorded. Current replenishment: " + replenishment);
            return;
        }

        if (!canReceiveShipment(stockNum, quantityReceived)) return;

        try {
            PreparedStatement ps1 = con.prepareStatement(
                    "INSERT INTO Shipment " +
                    "(shipment_id, notice_id, stock_number, quantity_received, arrival_date) " +
                    "VALUES (?, ?, ?, ?, SYSDATE)"
            );
            ps1.setString(1, shipmentId);
            ps1.setString(2, noticeId);
            ps1.setString(3, stockNum);
            ps1.setInt(4, quantityReceived);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE InventoryProduct " +
                    "SET quantity = quantity + ?, replenishment = replenishment - ? " +
                    "WHERE TRIM(stock_number) = TRIM(?) AND replenishment >= ?"
            );
            ps2.setInt(1, quantityReceived);
            ps2.setInt(2, quantityReceived);
            ps2.setString(3, stockNum);
            ps2.setInt(4, quantityReceived);
            int rows = ps2.executeUpdate();
            ps2.close();

            if (rows == 0) {
                con.rollback();
                System.out.println("Shipment failed. Inventory was not updated.");
                return;
            }

            con.commit();
            System.out.println("Shipment item received. Inventory quantity updated for " + stockNum + ".");
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Shipment failed: " + e.getMessage());
        }
    }

    static void fillEmartOrder() throws SQLException {
        String orderNum = readText("Enter eMART Order Number: ");
        if (!validText(orderNum, "Order number")) return;

        if (!orderHasItems(orderNum)) {
            System.out.println("Order not found or order has no items.");
            return;
        }

        if (warehouseOrderExists(orderNum)) {
            System.out.println("This order has already been processed by eDEPOT.");
            return;
        }

        if (printOrderShortages(orderNum)) {
            System.out.println("Order cannot be filled because inventory is not enough.");
            return;
        }

        try {
            PreparedStatement ps1 = con.prepareStatement(
                    "INSERT INTO WarehouseOrder " +
                    "(order_number, stock_number, quantity_ordered, order_date, status) " +
                    "SELECT TRIM(oi.OrderNum), TRIM(oi.StockNumber), SUM(oi.Quantity), SYSDATE, 'pending' " +
                    "FROM Order_Item oi " +
                    "WHERE TRIM(oi.OrderNum) = TRIM(?) " +
                    "GROUP BY TRIM(oi.OrderNum), TRIM(oi.StockNumber)"
            );
            ps1.setString(1, orderNum);
            int inserted = ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE InventoryProduct ip " +
                    "SET quantity = quantity - (" +
                    "    SELECT SUM(oi.Quantity) " +
                    "    FROM Order_Item oi " +
                    "    WHERE TRIM(oi.OrderNum) = TRIM(?) " +
                    "    AND TRIM(oi.StockNumber) = TRIM(ip.stock_number)" +
                    ") " +
                    "WHERE EXISTS (" +
                    "    SELECT 1 FROM Order_Item oi " +
                    "    WHERE TRIM(oi.OrderNum) = TRIM(?) " +
                    "    AND TRIM(oi.StockNumber) = TRIM(ip.stock_number)" +
                    ")"
            );
            ps2.setString(1, orderNum);
            ps2.setString(2, orderNum);
            ps2.executeUpdate();
            ps2.close();

            PreparedStatement ps3 = con.prepareStatement(
                    "UPDATE WarehouseOrder SET status = 'filled' " +
                    "WHERE TRIM(order_number) = TRIM(?)"
            );
            ps3.setString(1, orderNum);
            ps3.executeUpdate();
            ps3.close();

            con.commit();
            System.out.println("Order filled. Warehouse order rows inserted: " + inserted);

            generateReplenishmentOrders();
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Fill order failed: " + e.getMessage());
        }
    }

    static void showManufacturersNeedingReplenishment() throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT manufacturer_name, COUNT(*) AS item_count " +
                "FROM InventoryProduct " +
                "WHERE quantity < min_stock_level " +
                "GROUP BY manufacturer_name " +
                "HAVING COUNT(*) >= 3"
        );
        ResultSet rs = ps.executeQuery();

        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.println(rs.getString("manufacturer_name").trim() +
                    " needs replenishment. Low items: " + rs.getInt("item_count"));
        }
        if (!found) System.out.println("No manufacturer currently needs replenishment.");

        rs.close();
        ps.close();
    }

    static void generateReplenishmentOrders() throws SQLException {
        List<String> manufacturers = new ArrayList<>();

        PreparedStatement findPs = con.prepareStatement(
                "SELECT manufacturer_name " +
                "FROM InventoryProduct " +
                "WHERE quantity < min_stock_level " +
                "GROUP BY manufacturer_name " +
                "HAVING COUNT(*) >= 3"
        );
        ResultSet rs = findPs.executeQuery();
        while (rs.next()) {
            manufacturers.add(rs.getString("manufacturer_name").trim());
        }
        rs.close();
        findPs.close();

        if (manufacturers.isEmpty()) {
            System.out.println("No replenishment order needed.");
            return;
        }

        try {
            for (String manufacturer : manufacturers) {
                String replenishmentOrderId = nextReplenishmentOrderId();

                PreparedStatement insertPs = con.prepareStatement(
                        "INSERT INTO ReplenishmentOrder " +
                        "(replenishment_order_id, stock_number, quantity_ordered, replenishment_order_date, status) " +
                        "SELECT ?, stock_number, max_stock_level - quantity - replenishment, SYSDATE, 'pending' " +
                        "FROM InventoryProduct " +
                        "WHERE LOWER(TRIM(manufacturer_name)) = LOWER(TRIM(?)) " +
                        "AND quantity < max_stock_level " +
                        "AND max_stock_level - quantity - replenishment > 0"
                );
                insertPs.setString(1, replenishmentOrderId);
                insertPs.setString(2, manufacturer);
                int rows = insertPs.executeUpdate();
                insertPs.close();

                if (rows == 0) {
                    System.out.println("No new replenishment rows needed for " + manufacturer + ".");
                    continue;
                }

                PreparedStatement updatePs = con.prepareStatement(
                        "UPDATE InventoryProduct ip " +
                        "SET replenishment = replenishment + (" +
                        "    SELECT ro.quantity_ordered " +
                        "    FROM ReplenishmentOrder ro " +
                        "    WHERE TRIM(ro.replenishment_order_id) = TRIM(?) " +
                        "    AND TRIM(ro.stock_number) = TRIM(ip.stock_number)" +
                        ") " +
                        "WHERE EXISTS (" +
                        "    SELECT 1 FROM ReplenishmentOrder ro " +
                        "    WHERE TRIM(ro.replenishment_order_id) = TRIM(?) " +
                        "    AND TRIM(ro.stock_number) = TRIM(ip.stock_number)" +
                        ")"
                );
                updatePs.setString(1, replenishmentOrderId);
                updatePs.setString(2, replenishmentOrderId);
                updatePs.executeUpdate();
                updatePs.close();

                System.out.println("Replenishment order created: " + replenishmentOrderId +
                        " for " + manufacturer + " | rows: " + rows);
            }

            con.commit();
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Generate replenishment order failed: " + e.getMessage());
        }
    }

    static String findStockNumber(String manufacturer, String modelNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT stock_number FROM InventoryProduct " +
                "WHERE LOWER(TRIM(manufacturer_name)) = LOWER(TRIM(?)) " +
                "AND LOWER(TRIM(model_number)) = LOWER(TRIM(?))"
        );
        ps.setString(1, manufacturer);
        ps.setString(2, modelNum);
        ResultSet rs = ps.executeQuery();

        String stockNum = null;
        if (rs.next()) {
            stockNum = rs.getString("stock_number").trim();
        }

        rs.close();
        ps.close();
        return stockNum;
    }

    static boolean noticeIdExists(String noticeId) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT notice_id FROM ShippingNotice WHERE TRIM(notice_id) = TRIM(?)"
        );
        ps.setString(1, noticeId);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static String getShippingCompanyForNotice(String noticeId) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT shipping_company_name FROM ShippingNotice WHERE TRIM(notice_id) = TRIM(?)"
        );
        ps.setString(1, noticeId);
        ResultSet rs = ps.executeQuery();

        String company = "";
        if (rs.next()) {
            company = rs.getString("shipping_company_name").trim();
        }

        rs.close();
        ps.close();
        return company;
    }

    static boolean canAddReplenishment(String stockNum, int quantityToAdd) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT quantity, replenishment, max_stock_level FROM InventoryProduct " +
                "WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            rs.close();
            ps.close();
            System.out.println("Product not found in inventory.");
            return false;
        }

        int quantity = rs.getInt("quantity");
        int replenishment = rs.getInt("replenishment");
        int maxStock = rs.getInt("max_stock_level");

        rs.close();
        ps.close();

        if (quantity + replenishment + quantityToAdd > maxStock) {
            System.out.println("This notice would exceed the maximum stock level.");
            System.out.println("Current quantity: " + quantity + ", current replenishment: " + replenishment + ", max: " + maxStock);
            return false;
        }
        return true;
    }

    static boolean canReceiveShipment(String stockNum, int quantityReceived) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT quantity, max_stock_level FROM InventoryProduct " +
                "WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            rs.close();
            ps.close();
            System.out.println("Product not found in inventory.");
            return false;
        }

        int quantity = rs.getInt("quantity");
        int maxStock = rs.getInt("max_stock_level");

        rs.close();
        ps.close();

        if (quantity + quantityReceived > maxStock) {
            System.out.println("Receiving this shipment would exceed the maximum stock level.");
            System.out.println("Current quantity: " + quantity + ", received: " + quantityReceived + ", max: " + maxStock);
            return false;
        }
        return true;
    }

    static void addQuantityToShippingNoticeItem(String noticeId, String stockNum, int quantity) throws SQLException {
        PreparedStatement ps1 = con.prepareStatement(
                "UPDATE ShippingNotice SET quantity = quantity + ? " +
                "WHERE TRIM(notice_id) = TRIM(?) AND TRIM(stock_number) = TRIM(?)"
        );
        ps1.setInt(1, quantity);
        ps1.setString(2, noticeId);
        ps1.setString(3, stockNum);
        ps1.executeUpdate();
        ps1.close();

        PreparedStatement ps2 = con.prepareStatement(
                "UPDATE InventoryProduct SET replenishment = replenishment + ? " +
                "WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps2.setInt(1, quantity);
        ps2.setString(2, stockNum);
        ps2.executeUpdate();
        ps2.close();
    }

    static void insertInventoryProduct(String stockNum, String manufacturer, String modelNum,
                                       int minStock, int maxStock, String location) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO InventoryProduct " +
                "(stock_number, manufacturer_name, model_number, quantity, min_stock_level, " +
                "max_stock_level, location, replenishment) " +
                "VALUES (?, ?, ?, 0, ?, ?, ?, 0)"
        );
        ps.setString(1, stockNum);
        ps.setString(2, manufacturer);
        ps.setString(3, modelNum);
        ps.setInt(4, minStock);
        ps.setInt(5, maxStock);
        ps.setString(6, location);
        ps.executeUpdate();
        ps.close();
    }

    static boolean inventoryProductExists(String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT stock_number FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static boolean shippingNoticeExists(String noticeId, String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT notice_id FROM ShippingNotice " +
                "WHERE TRIM(notice_id) = TRIM(?) AND TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, noticeId);
        ps.setString(2, stockNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static int getNoticeQuantity(String noticeId, String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT quantity FROM ShippingNotice " +
                "WHERE TRIM(notice_id) = TRIM(?) AND TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, noticeId);
        ps.setString(2, stockNum);
        ResultSet rs = ps.executeQuery();

        int quantity = -1;
        if (rs.next()) {
            quantity = rs.getInt("quantity");
        }

        rs.close();
        ps.close();
        return quantity;
    }

    static boolean shipmentIdStockExists(String shipmentId, String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT shipment_id FROM Shipment " +
                "WHERE TRIM(shipment_id) = TRIM(?) AND TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, shipmentId);
        ps.setString(2, stockNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static boolean shipmentNoticeItemAlreadyReceived(String noticeId, String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT shipment_id FROM Shipment " +
                "WHERE TRIM(notice_id) = TRIM(?) AND TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, noticeId);
        ps.setString(2, stockNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static int getReplenishment(String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT replenishment FROM InventoryProduct " +
                "WHERE TRIM(stock_number) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        int replenishment = 0;
        if (rs.next()) {
            replenishment = rs.getInt("replenishment");
        }

        rs.close();
        ps.close();
        return replenishment;
    }

    static boolean orderHasItems(String orderNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT OrderNum FROM Order_Item WHERE TRIM(OrderNum) = TRIM(?)"
        );
        ps.setString(1, orderNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static boolean warehouseOrderExists(String orderNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT order_number FROM WarehouseOrder WHERE TRIM(order_number) = TRIM(?)"
        );
        ps.setString(1, orderNum);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static boolean printOrderShortages(String orderNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT q.stock_number, q.quantity_ordered, NVL(ip.quantity, 0) AS inventory_quantity " +
                "FROM (" +
                "    SELECT TRIM(StockNumber) AS stock_number, SUM(Quantity) AS quantity_ordered " +
                "    FROM Order_Item " +
                "    WHERE TRIM(OrderNum) = TRIM(?) " +
                "    GROUP BY TRIM(StockNumber)" +
                ") q LEFT JOIN InventoryProduct ip " +
                "ON TRIM(ip.stock_number) = q.stock_number " +
                "WHERE ip.stock_number IS NULL OR ip.quantity < q.quantity_ordered"
        );
        ps.setString(1, orderNum);
        ResultSet rs = ps.executeQuery();

        boolean hasShortage = false;
        while (rs.next()) {
            hasShortage = true;
            System.out.println("Shortage for Stock Number: " + rs.getString("stock_number").trim() +
                    " | Ordered: " + rs.getInt("quantity_ordered") +
                    " | Available: " + rs.getInt("inventory_quantity"));
        }

        rs.close();
        ps.close();
        return hasShortage;
    }

    static String nextReplenishmentOrderId() throws SQLException {
        String id;
        int suffix = 0;
        do {
            id = "R" + (System.currentTimeMillis() % 1000000000L) + suffix;
            suffix++;
        } while (replenishmentOrderIdExists(id));
        return id;
    }

    static boolean replenishmentOrderIdExists(String id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT replenishment_order_id FROM ReplenishmentOrder " +
                "WHERE TRIM(replenishment_order_id) = TRIM(?)"
        );
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    static int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    static String readStockNumber(String prompt) {
        String stockNum = readText(prompt);
        if (!stockNum.matches("^[A-Z]{2}[0-9]{5}$")) {
            System.out.println("Stock number must have two uppercase letters followed by five digits, for example AA00001.");
            return null;
        }
        return stockNum;
    }

    static int readPositiveInt(String prompt) {
        try {
            int value = Integer.parseInt(readText(prompt));
            if (value <= 0) {
                System.out.println("Value must be greater than 0.");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    static int readNonNegativeInt(String prompt) {
        try {
            int value = Integer.parseInt(readText(prompt));
            if (value < 0) {
                System.out.println("Value cannot be negative.");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    static boolean validText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            System.out.println(fieldName + " cannot be empty.");
            return false;
        }
        if (text.length() > 20) {
            System.out.println(fieldName + " must be at most 20 characters.");
            return false;
        }
        return true;
    }

    static boolean validLocation(String location) {
        if (!validText(location, "Location")) return false;
        if (!location.matches("^[A-Za-z][1-9][0-9]*$")) {
            System.out.println("Location must be a letter followed by a positive number without leading zeros, for example A1.");
            return false;
        }
        return true;
    }
}