 package org.ivc.dbms.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CS174AShoppingDatabase {

    static Connection con = null;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");

            String tnsAdmin = System.getenv("TNS_ADMIN");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            String dbService = System.getenv("DB_SERVICE");

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
            if (dbService == null || dbService.isBlank()) {
                dbService = "emartdepot_low";
            }

            con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@" + dbService,
                    dbUser,
                    dbPassword
            );

            con.setAutoCommit(false);
            System.out.println("Connected!");

            boolean running = true;
            while (running) {
                System.out.println("\n------ Main Menu ------");
                System.out.println("1. eMART Customer Login");
                System.out.println("2. eMART Manager Login");
                System.out.println("3. eDEPOT Warehouse Menu");
                System.out.println("4. Exit Program");

                int choice = readIntSafe("Choose: ");

                if (choice == 1) customerLogin();
                else if (choice == 2) managerLogin();
                else if (choice == 3) runDepotMenu();
                else if (choice == 4) running = false;
                else System.out.println("Invalid Choice");
            }

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

    static void customerLogin() throws SQLException {

    while (true) {

        System.out.print("Enter Customer ID: ");
        String id = scanner.nextLine();

        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();

        PreparedStatement checkPs = con.prepareStatement(
                "SELECT Status FROM Customer " +
                        "WHERE Identifier = ? " +
                        "AND Password = ?"
        );

        checkPs.setString(1, id);
        checkPs.setString(2, pass);

        ResultSet rs = checkPs.executeQuery();

        // =========================
        // EXISTING CUSTOMER LOGIN
        // =========================
        if (rs.next()) {

            String status = rs.getString("Status");

            rs.close();
            checkPs.close();

            System.out.println("Welcome back!");
            System.out.println("Customer status: " + status);

            ensureCartExists(id);

            runCustomerMenu(id);
            return;
        }

        rs.close();
        checkPs.close();

        // =========================
        // NEW CUSTOMER FLOW
        // =========================
        System.out.println("Customer not found or incorrect password.");
        System.out.println("1. Try again");
        System.out.println("2. Create new account");
        System.out.println("0. Back to Main Menu");
        System.out.print("Choose: ");

        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Try again.\n");
            continue;
        }

        if (choice == 1) {
            continue; // reprompt login
        }

        if (choice == 0) {
            return; // back to main menu
        }

        if (choice != 2) {
            System.out.println("Invalid choice.\n");
            continue;
        }

        // =========================
        // CREATE NEW CUSTOMER
        // =========================
        System.out.println("New customer detected. Please enter your information.");

        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Address: ");
        String address = scanner.nextLine();

        String status = "new";

        try {
            // pre-check to avoid ORA-00001
            PreparedStatement preCheck = con.prepareStatement(
                    "SELECT 1 FROM Customer WHERE Identifier = ?"
            );
            preCheck.setString(1, id);
            ResultSet exists = preCheck.executeQuery();

            if (exists.next()) {
                System.out.println("Customer ID already exists. Please log in instead.\n");
                exists.close();
                preCheck.close();
                continue;
            }

            exists.close();
            preCheck.close();

            PreparedStatement insertPs = con.prepareStatement(
                    "INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)"
            );

            insertPs.setString(1, id);
            insertPs.setString(2, pass);
            insertPs.setString(3, name);
            insertPs.setString(4, email);
            insertPs.setString(5, address);
            insertPs.setString(6, status);

            insertPs.executeUpdate();
            insertPs.close();

            String cartId = "CART-" + System.currentTimeMillis();

            PreparedStatement cartInsert = con.prepareStatement(
                    "INSERT INTO Shopping_Cart (CartId, CreatedDate, Identifier) VALUES (?, ?, ?)"
            );

            cartInsert.setString(1, cartId);
            cartInsert.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
            cartInsert.setString(3, id);

            cartInsert.executeUpdate();
            cartInsert.close();

            con.commit();

            System.out.println("New customer account created!");
            System.out.println("Cart created for customer: " + cartId);

            runCustomerMenu(id);
            return;

        } catch (SQLException e) {

            con.rollback();

            if (e.getErrorCode() == 1) {
                System.out.println("Customer ID already exists (constraint). Try again.\n");
            } else {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }
}

    static void ensureCartExists(String customerId) throws SQLException {
        PreparedStatement check = con.prepareStatement(
                "SELECT CartId FROM Shopping_Cart WHERE Identifier = ?"
        );
        check.setString(1, customerId);
        ResultSet rs = check.executeQuery();

        if (rs.next()) {
            rs.close();
            check.close();
            return;
        }

        rs.close();
        check.close();

        String cartId = "CART-" + System.currentTimeMillis();

        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO Shopping_Cart (CartId, CreatedDate, Identifier) VALUES (?, ?, ?)"
        );
        insert.setString(1, cartId);
        insert.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
        insert.setString(3, customerId);
        insert.executeUpdate();
        insert.close();

        con.commit();
    }

    static void managerLogin() throws SQLException {
        System.out.print("Enter Manager ID: ");
        String id = scanner.nextLine();

        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();

        PreparedStatement ps = con.prepareStatement(
                "SELECT Identifier FROM Managers " +
                        "WHERE Identifier = ? " +
                        "AND Password = ?"
        );

        ps.setString(1, id);
        ps.setString(2, pass);

        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("Invalid ID or password!");
            rs.close();
            ps.close();
            return;
        }

        rs.close();
        ps.close();

        System.out.println("Welcome, Manager " + id + "!");
        runManagerMenu();
    }

    static void runCustomerMenu(String customerId) throws SQLException {
        boolean running = true;

        while (running) {
            System.out.println("\n------- Customer Menu -------");
            System.out.println("1. View all products");
            System.out.println("2. Search product");
            System.out.println("3. View cart");
            System.out.println("4. Remove item from cart");
            System.out.println("5. Place order");
            System.out.println("6. View order history");
            System.out.println("7. View product description");
            System.out.println("8. Add to cart");
            System.out.println("9. View order by order number");
            System.out.println("10. Re-run previous order");
            System.out.println("0. Back to Main Menu");

            int choice = readIntSafe("Choose: ");

            if (choice == 1) viewProducts();
            else if (choice == 2) searchProduct();
            else if (choice == 3) viewCart(customerId);
            else if (choice == 4) removeFromCart(customerId);
            else if (choice == 5) placeOrder(customerId);
            else if (choice == 6) viewOrderHistory(customerId);
            else if (choice == 7) viewProductDescription();
            else if (choice == 8) addToCart(customerId);
            else if (choice == 9) viewOrderByNumber();
            else if (choice == 10) rerunOrder(customerId);
            else if (choice == 0) running = false;
            else System.out.println("Invalid Choice");
        }
    }

    static void runManagerMenu() throws SQLException {
        boolean running = true;

        while (running) {
            System.out.println("\n--------- Manager Menu ---------");
            System.out.println("1. Print monthly sales summary");
            System.out.println("2. Adjust customer status");
            System.out.println("3. Send order to manufacturer");
            System.out.println("4. Change price of item");
            System.out.println("5. Delete old sales transactions");
            System.out.println("0. Back to Main Menu");

            int choice = readIntSafe("Choose: ");

            if (choice == 1) monthlySummary();
            else if (choice == 2) adjustCustomerStatus();
            else if (choice == 3) sendOrderToManufacturer();
            else if (choice == 4) changePrice();
            else if (choice == 5) deleteTransactions();
            else if (choice == 0) running = false;
            else System.out.println("Invalid Choice");
        }
    }

    static void viewProducts() throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products ORDER BY StockNumber"
        );

        ResultSet rs = ps.executeQuery();

        System.out.println("\n" + String.format(
            "%-12s %-12s %-15s %-15s %-10s %s",
            "Stock#", "Category", "Manufacturer", "Model", "Price", "Warranty"
        ));

        System.out.println("-----------------------------------------------------------------------------");

        boolean found = false;

        while (rs.next()) {
            found = true;
            System.out.printf(
                "%-12s %-12s %-15s %-15s $%-9.2f %d months%n",
                rs.getString("StockNumber").trim(),
                rs.getString("Category").trim(),
                rs.getString("Manufacturer").trim(),
                rs.getString("ModelNumber").trim(),
                rs.getDouble("Price"),
                rs.getInt("Warranty")
            );
        }

        if (!found) {
            System.out.println("No products found.");
        }

        rs.close();
        ps.close();
    }

    static void searchProduct() throws SQLException {
    System.out.println("\n--- Search Products ---");
    System.out.println("1. By stock number");
    System.out.println("2. By manufacturer");
    System.out.println("3. By model number");
    System.out.println("4. By category");
    System.out.println("5. By description attribute");
    System.out.println("6. By compatible item");

    int choice = readIntSafe("Choose: ");
    PreparedStatement ps = null;

    if (choice == 1) {
        String val = readStockNumber("Enter Stock Number: ");
        if (val == null) return;

        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
        );
        ps.setString(1, val);

    } else if (choice == 2) {
        System.out.print("Enter Manufacturer: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE Manufacturer = ?"
        );
        ps.setString(1, val);

    } else if (choice == 3) {
        System.out.print("Enter Model Number: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE ModelNumber = ?"
        );
        ps.setString(1, val);

    } else if (choice == 4) {
        System.out.print("Enter Category: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE Category = ?"
        );
        ps.setString(1, val);

    } else if (choice == 5) {
        System.out.print("Enter Attribute Name: ");
        String attr = scanner.nextLine();
        System.out.print("Enter Attribute Value: ");
        String attrVal = scanner.nextLine();

        ps = con.prepareStatement(
            "SELECT p.StockNumber, p.Category, p.Manufacturer, p.ModelNumber, p.Price, p.Warranty " +
            "FROM Products p, Product_Description pd " +
            "WHERE TRIM(p.StockNumber) = TRIM(pd.StockNumber) " +
            "AND LOWER(TRIM(pd.AttributeName)) = LOWER(TRIM(?)) " +
            "AND LOWER(TRIM(pd.AttributeValue)) = LOWER(TRIM(?))"
        );
        ps.setString(1, attr);
        ps.setString(2, attrVal);

    } else if (choice == 6) {
        String val = readStockNumber("Enter Stock Number to find compatible items: ");
        if (val == null) return;

        ps = con.prepareStatement(
            "SELECT p.StockNumber, p.Category, p.Manufacturer, p.ModelNumber, p.Price, p.Warranty " +
            "FROM Products p " +
            "JOIN Product_Compatibility pc ON TRIM(p.StockNumber) = TRIM(pc.StockNumber) " +
            "WHERE TRIM(pc.CompatibleWithNumber) = TRIM(?)"
        );
        ps.setString(1, val);
    } else {
        System.out.println("Invalid choice.");
        return;
    }

    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-12s %-12s %-15s %-15s %-10s %s",
        "Stock#", "Category", "Manufacturer", "Model", "Price", "Warranty"));

    System.out.println("--------------------------------------------------------------------------");

    while (rs.next()) {
        System.out.printf("%-12s %-12s %-15s %-15s %-10.2f %d months%n",
            rs.getString("StockNumber"),
            rs.getString("Category"),
            rs.getString("Manufacturer"),
            rs.getString("ModelNumber"),
            rs.getDouble("Price"),
            rs.getInt("Warranty")
        );
    }

    rs.close();
    ps.close();
}
static void addToCart(String customerId) throws SQLException {
    PreparedStatement getCart = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE Identifier = ?"
    );
    getCart.setString(1, customerId);
    ResultSet rs1 = getCart.executeQuery();

    if (!rs1.next()) {
        System.out.println("No cart found for customer!");
        rs1.close();
        getCart.close();
        return;
    }

    String cartId = rs1.getString("CartId").trim();
    rs1.close();
    getCart.close();

    String stockNum = readStockNumber("Enter Stock Number: ");
    if (stockNum == null) return;

    int quantity = readIntSafe("Enter Quantity: ");
    if (quantity <= 0) {
        System.out.println("Quantity must be greater than 0.");
        return;
    }

    PreparedStatement customerCheck = con.prepareStatement(
        "SELECT Identifier FROM Customer WHERE Identifier = ?"
    );
    customerCheck.setString(1, customerId);
    ResultSet customerRs = customerCheck.executeQuery();

    if (!customerRs.next()) {
        System.out.println("Customer not found! Please add the customer first.");
        customerRs.close();
        customerCheck.close();
        return;
    }
    customerRs.close();
    customerCheck.close();

    PreparedStatement productCheck = con.prepareStatement(
        "SELECT StockNumber FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
    );
    productCheck.setString(1, stockNum);
    ResultSet productRs = productCheck.executeQuery();

    if (!productRs.next()) {
        System.out.println("Product not found! Please add the product first.");
        productRs.close();
        productCheck.close();
        return;
    }
    productRs.close();
    productCheck.close();

    int inventoryQty = getInventoryQuantity(stockNum);
    if (inventoryQty < 0) {
        System.out.println("Product exists in catalog, but it is not available in inventory.");
        return;
    }

    int currentCartQty = getCartQuantityForStock(cartId, stockNum);
    if (currentCartQty + quantity > inventoryQty) {
        System.out.println("Sorry, there are only " + inventoryQty + " left in stock right now.");
        return;
    }

    PreparedStatement check = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE CartId = ?"
    );
    check.setString(1, cartId);
    ResultSet rs = check.executeQuery();

    if (!rs.next()) {
        System.out.println("Cart not found! Please create a cart first.");
        rs.close();
        check.close();
        return;
    }

    rs.close();
    check.close();

    PreparedStatement updateItem = con.prepareStatement(
        "UPDATE Cart_Items SET Quantity = Quantity + ? " +
        "WHERE TRIM(StockNumber) = TRIM(?) AND CartId = ?"
    );
    updateItem.setInt(1, quantity);
    updateItem.setString(2, stockNum);
    updateItem.setString(3, cartId);
    int rows = updateItem.executeUpdate();
    updateItem.close();

    if (rows == 0) {
        PreparedStatement insertItem = con.prepareStatement(
            "INSERT INTO Cart_Items (StockNumber, CartId, Quantity) VALUES (?, ?, ?)"
        );
        insertItem.setString(1, stockNum);
        insertItem.setString(2, cartId);
        insertItem.setInt(3, quantity);
        insertItem.executeUpdate();
        insertItem.close();
    }

    con.commit();
    System.out.println("Item added to cart!");
}

   static void viewCart(String customerId) throws SQLException {
    PreparedStatement getCart = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE Identifier = ?"
    );
    getCart.setString(1, customerId);
    ResultSet rs2 = getCart.executeQuery();

    if (!rs2.next()) {
        System.out.println("No cart found.");
        return;
    }

    String cartId = rs2.getString("CartId").trim();
    rs2.close();
    getCart.close();

    PreparedStatement ps = con.prepareStatement(
        "SELECT ci.StockNumber, p.Manufacturer, p.Price, ci.Quantity " +
        "FROM Cart_Items ci, Products p " +
        "WHERE TRIM(ci.StockNumber) = TRIM(p.StockNumber) " +
        "AND ci.CartId = ?"
    );

    ps.setString(1, cartId);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-12s %-15s %-10s %s", "Stock#", "Manufacturer", "Price", "Qty"));
    System.out.println("--------------------------------------------------");

    boolean found = false;

    while (rs.next()) {
        found = true;
        System.out.printf("%-12s %-15s %-10.2f %d%n",
            rs.getString("StockNumber"),
            rs.getString("Manufacturer"),
            rs.getDouble("Price"),
            rs.getInt("Quantity")
        );
    }

    if (!found) System.out.println("Cart is empty or not found.");

    rs.close();
    ps.close();
}

static void removeFromCart(String customerId) throws SQLException {
    PreparedStatement getCart = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE Identifier = ?"
    );
    getCart.setString(1, customerId);
    ResultSet cartRs = getCart.executeQuery();

    if (!cartRs.next()) {
        System.out.println("No cart found!");
        cartRs.close();
        getCart.close();
        return;
    }

    String cartId = cartRs.getString("CartId").trim();
    cartRs.close();
    getCart.close();

    String stockNum = readStockNumber("Enter Stock Number: ");
    if (stockNum == null) return;

    PreparedStatement ps = con.prepareStatement(
        "DELETE FROM Cart_Items " +
        "WHERE CartId = ? " +
        "AND TRIM(StockNumber) = TRIM(?)"
    );

    ps.setString(1, cartId);
    ps.setString(2, stockNum);

    int rows = ps.executeUpdate();

    if (rows > 0) {
        con.commit();
        System.out.println("Item removed from cart!");
    } else {
        System.out.println("Item not found in cart.");
    }

    ps.close();
}

    static void placeOrder(String customerId) throws SQLException {
    PreparedStatement getCart = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE Identifier = ?"
    );
    getCart.setString(1, customerId);
    ResultSet cartRs = getCart.executeQuery();

    if (!cartRs.next()) {
        System.out.println("No cart found!");
        cartRs.close();
        getCart.close();
        return;
    }

    String cartId = cartRs.getString("CartId").trim();
    cartRs.close();
    getCart.close();

    System.out.print("Enter Shipping Method: ");
    String shippingMethod = scanner.nextLine();

    placeOrder(customerId, cartId, shippingMethod);
}
static void placeOrder(String customerId, String cartId, String shippingMethod) throws SQLException {

    if (!cartHasEnoughInventory(cartId)) {
        System.out.println("Order cannot be placed because inventory is not enough.");
        return;
    }

    String orderNum = "ORD-" + System.currentTimeMillis();

    PreparedStatement ps = con.prepareStatement(
        "SELECT ci.StockNumber, ci.Quantity, p.Price " +
        "FROM Cart_Items ci, Products p " +
        "WHERE ci.CartId = ? " +
        "AND TRIM(ci.StockNumber) = TRIM(p.StockNumber)"
    );

    ps.setString(1, cartId);
    ResultSet rs = ps.executeQuery();

    double subtotal = 0;
    boolean hasItems = false;

    while (rs.next()) {
        hasItems = true;
        subtotal += rs.getDouble("Price") * rs.getInt("Quantity");
    }

    rs.close();
    ps.close();

    if (!hasItems) {
        System.out.println("Cart is empty!");
        return;
    }

    PreparedStatement cust = con.prepareStatement(
        "SELECT Status FROM Customer WHERE Identifier = ?"
    );
    cust.setString(1, customerId);
    ResultSet crs = cust.executeQuery();

    crs.next();
    String status = crs.getString(1).trim();

    crs.close();
    cust.close();

    PreparedStatement rulePs = con.prepareStatement(
        "SELECT discount_rate, shipping_rate, shipping_threshold " +
        "FROM DiscountRules WHERE LOWER(TRIM(status_name)) = LOWER(TRIM(?))"
    );

    rulePs.setString(1, status);
    ResultSet ruleRs = rulePs.executeQuery();
    ruleRs.next();

    double discountRate = ruleRs.getDouble("discount_rate");
    double shippingRate = ruleRs.getDouble("shipping_rate");
    double shippingThreshold = ruleRs.getDouble("shipping_threshold");

    ruleRs.close();
    rulePs.close();

    double discount = subtotal * discountRate;
    double afterDiscount = subtotal - discount;

    double shippingFee;
    if (afterDiscount > shippingThreshold) {
        shippingFee = 0;
    } else {
        shippingFee = afterDiscount * shippingRate;
    }

    double total = afterDiscount + shippingFee;

    PreparedStatement order = con.prepareStatement(
        "INSERT INTO Customer_Orders " +
        "(OrderNum, Subtotal, OrderDate, Discount, Shipping, Total, Identifier) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );

    order.setString(1, orderNum);
    order.setDouble(2, subtotal);
    order.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
    order.setDouble(4, discount);
    order.setString(5, shippingMethod);
    order.setDouble(6, total);
    order.setString(7, customerId);

    order.executeUpdate();
    order.close();

    ps = con.prepareStatement(
        "SELECT ci.StockNumber, ci.Quantity, p.Price " +
        "FROM Cart_Items ci, Products p " +
        "WHERE ci.CartId = ? " +
        "AND TRIM(ci.StockNumber) = TRIM(p.StockNumber)"
    );

    ps.setString(1, cartId);
    rs = ps.executeQuery();

    while (rs.next()) {

        PreparedStatement oi = con.prepareStatement(
            "INSERT INTO Order_Item (StockNumber, OrderNum, Quantity, SavedUnitPrice) VALUES (?, ?, ?, ?)"
        );

        oi.setString(1, rs.getString("StockNumber"));
        oi.setString(2, orderNum);
        oi.setInt(3, rs.getInt("Quantity"));
        oi.setDouble(4, rs.getDouble("Price"));

        oi.executeUpdate();
        oi.close();
    }

    rs.close();
    ps.close();

    deductInventoryForCart(cartId);
    insertWarehouseOrderFromCart(orderNum, cartId);

    PreparedStatement clear = con.prepareStatement(
        "DELETE FROM Cart_Items WHERE CartId = ?"
    );

    clear.setString(1, cartId);
    clear.executeUpdate();
    clear.close();

    updateCustomerStatus(customerId);
    con.commit();

    System.out.println("Order placed!");
    System.out.println("Order#: " + orderNum);
    System.out.println("Total: " + total);
}
static void viewOrderHistory(String customerId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT OrderNum, OrderDate, Subtotal, Discount, Total, Shipping " +
        "FROM Customer_Orders WHERE Identifier = ?"
    );

    ps.setString(1, customerId);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format(
        "%-12s %-15s %-12s %-12s %-10s %s",
        "Order#", "Date", "Subtotal", "Discount", "Total", "Shipping"
    ));

    System.out.println("------------------------------------------------------------------------");

    boolean found = false;

    while (rs.next()) {
        found = true;
        System.out.printf("%-12s %-15s %-12.2f %-12.2f %-10.2f %s%n",
            rs.getString("OrderNum"),
            rs.getString("OrderDate"),
            rs.getDouble("Subtotal"),
            rs.getDouble("Discount"),
            rs.getDouble("Total"),
            rs.getString("Shipping")
        );
    }

    if (!found) System.out.println("No orders found for this customer.");

    rs.close();
    ps.close();
}

static void viewProductDescription() throws SQLException {
    String stockNum = readStockNumber("Enter Stock Number: ");
    if (stockNum == null) return;

    PreparedStatement ps = con.prepareStatement(
        "SELECT AttributeName, AttributeValue " +
        "FROM Product_Description " +
        "WHERE TRIM(StockNumber) = TRIM(?)"
    );

    ps.setString(1, stockNum);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-20s %s", "Attribute", "Value"));
    System.out.println("----------------------------------");

    boolean found = false;

    while (rs.next()) {
        found = true;
        System.out.printf("%-20s %s%n",
            rs.getString("AttributeName"),
            rs.getString("AttributeValue")
        );
    }

    if (!found) System.out.println("No description found for this product.");

    rs.close();
    ps.close();
}
static void viewOrderByNumber() throws SQLException {
    System.out.print("Enter Order Number: ");
    String orderNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "SELECT o.OrderNum, o.OrderDate, o.Subtotal, o.Discount, o.Total, o.Shipping, " +
        "oi.StockNumber, p.Manufacturer, p.Price, oi.Quantity " +
        "FROM Customer_Orders o, Order_Item oi, Products p " +
        "WHERE o.OrderNum = oi.OrderNum " +
        "AND TRIM(oi.StockNumber) = TRIM(p.StockNumber) " +
        "AND o.OrderNum = ?"
    );

    ps.setString(1, orderNum);
    ResultSet rs = ps.executeQuery();

    boolean found = false;

    while (rs.next()) {
        if (!found) {
            System.out.println("\nOrder#:   " + rs.getString("OrderNum"));
            System.out.println("Date:     " + rs.getString("OrderDate"));
            System.out.println("Discount: $" + rs.getDouble("Discount"));
            System.out.println("Shipping: " + rs.getString("Shipping"));
            System.out.println("Total:    $" + rs.getDouble("Total"));

            System.out.println("\n" + String.format(
                "%-12s %-15s %-10s %s",
                "Stock#", "Manufacturer", "Price", "Qty"
            ));

            System.out.println("--------------------------------------------------");
        }

        found = true;

        System.out.printf("%-12s %-15s %-10.2f %d%n",
            rs.getString("StockNumber"),
            rs.getString("Manufacturer"),
            rs.getDouble("Price"),
            rs.getInt("Quantity")
        );
    }

    if (!found) System.out.println("Order not found.");

    rs.close();
    ps.close();
}

static void rerunOrder(String customerId) throws SQLException {
    System.out.print("Enter Order Number to re-run: ");
    String oldOrderNum = scanner.nextLine();

    PreparedStatement orderCheck = con.prepareStatement(
        "SELECT Shipping FROM Customer_Orders WHERE OrderNum = ? " +
        "AND Identifier = ?"
    );

    orderCheck.setString(1, oldOrderNum);
    orderCheck.setString(2, customerId);

    ResultSet orderRs = orderCheck.executeQuery();

    if (!orderRs.next()) {
        System.out.println("Order not found!");
        orderRs.close();
        orderCheck.close();
        return;
    }

    String shippingMethod = orderRs.getString("Shipping");

    orderRs.close();
    orderCheck.close();

    PreparedStatement itemsPs = con.prepareStatement(
        "SELECT StockNumber, Quantity FROM Order_Item WHERE OrderNum = ?"
    );

    itemsPs.setString(1, oldOrderNum);
    ResultSet itemsRs = itemsPs.executeQuery();

    java.util.List<String> stockNumbers = new java.util.ArrayList<>();
    java.util.List<Integer> quantities = new java.util.ArrayList<>();
    java.util.List<Double> prices = new java.util.ArrayList<>();

    double subtotal = 0;
    boolean stockAvailable = true;

    while (itemsRs.next()) {
        String stockNum = itemsRs.getString("StockNumber");
        int qty = itemsRs.getInt("Quantity");

        PreparedStatement prodPs = con.prepareStatement(
            "SELECT Price FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
        );

        prodPs.setString(1, stockNum);
        ResultSet prodRs = prodPs.executeQuery();

        if (!prodRs.next()) {
            System.out.println("Product " + stockNum + " no longer exists!");
            stockAvailable = false;
            prodRs.close();
            prodPs.close();
            break;
        }

        double price = prodRs.getDouble("Price");

        prodRs.close();
        prodPs.close();

        PreparedStatement invPs = con.prepareStatement(
            "SELECT quantity FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
        );

        invPs.setString(1, stockNum);
        ResultSet invRs = invPs.executeQuery();

        if (!invRs.next() || invRs.getInt("quantity") < qty) {
            System.out.println("Not enough inventory for product " + stockNum);
            stockAvailable = false;
            invRs.close();
            invPs.close();
            break;
        }

        invRs.close();
        invPs.close();

        stockNumbers.add(stockNum);
        quantities.add(qty);
        prices.add(price);

        subtotal += price * qty;
    }

    itemsRs.close();
    itemsPs.close();

    if (!stockAvailable || stockNumbers.isEmpty()) return;

    PreparedStatement custPs = con.prepareStatement(
        "SELECT Status FROM Customer WHERE Identifier = ?"
    );

    custPs.setString(1, customerId);
    ResultSet custRs = custPs.executeQuery();

    custRs.next();
    String status = custRs.getString("Status");

    custRs.close();
    custPs.close();

    PreparedStatement rulePs = con.prepareStatement(
        "SELECT discount_rate, shipping_rate, shipping_threshold " +
        "FROM DiscountRules WHERE LOWER(TRIM(status_name)) = LOWER(TRIM(?))"
    );

    rulePs.setString(1, status);
    ResultSet ruleRs = rulePs.executeQuery();

    ruleRs.next();

    double discountRate = ruleRs.getDouble("discount_rate");
    double shippingRate = ruleRs.getDouble("shipping_rate");
    double shippingThreshold = ruleRs.getDouble("shipping_threshold");

    ruleRs.close();
    rulePs.close();

    double discount = subtotal * discountRate;
    double afterDiscount = subtotal - discount;

    double shippingFee = 0;
    if (afterDiscount <= shippingThreshold) {
        shippingFee = afterDiscount * shippingRate;
    }

    double total = afterDiscount + shippingFee;

    System.out.println("\n--- Re-run Order Summary ---");
    System.out.println("Customer Status: " + status);
    System.out.printf("Subtotal: $%.2f%n", subtotal);
    System.out.printf("Total: $%.2f%n", total);

    System.out.print("Confirm order? (y/n): ");
    String confirm = scanner.nextLine();

    if (!confirm.equalsIgnoreCase("y")) {
        System.out.println("Order cancelled.");
        return;
    }

    String newOrderNum = "ORD-" + System.currentTimeMillis();

    PreparedStatement orderPs = con.prepareStatement(
        "INSERT INTO Customer_Orders (OrderNum, Subtotal, OrderDate, Discount, Shipping, Total, Identifier) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );

    orderPs.setString(1, newOrderNum);
    orderPs.setDouble(2, subtotal);
    orderPs.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
    orderPs.setDouble(4, discount);
    orderPs.setString(5, shippingMethod);
    orderPs.setDouble(6, total);
    orderPs.setString(7, customerId);

    orderPs.executeUpdate();
    orderPs.close();

    for (int i = 0; i < stockNumbers.size(); i++) {
        PreparedStatement oiPs = con.prepareStatement(
            "INSERT INTO Order_Item (StockNumber, OrderNum, Quantity, SavedUnitPrice) VALUES (?, ?, ?, ?)"
        );

        oiPs.setString(1, stockNumbers.get(i));
        oiPs.setString(2, newOrderNum);
        oiPs.setInt(3, quantities.get(i));
        oiPs.setDouble(4, prices.get(i));

        oiPs.executeUpdate();
        oiPs.close();

        PreparedStatement invUp = con.prepareStatement(
            "UPDATE InventoryProduct SET quantity = quantity - ? WHERE TRIM(stock_number) = TRIM(?)"
        );

        invUp.setInt(1, quantities.get(i));
        invUp.setString(2, stockNumbers.get(i));

        invUp.executeUpdate();
        invUp.close();
    }

    insertWarehouseOrderFromOrderItems(newOrderNum);
    updateCustomerStatus(customerId);
    con.commit();

    System.out.println("Order re-run successful! New Order#: " + newOrderNum);
}

static void updateCustomerStatus(String customerId) throws SQLException {
    // Status is based on the sum of the customer's most recent three orders.
    PreparedStatement statusPs = con.prepareStatement(
        "SELECT NVL(SUM(Total), 0) FROM (" +
        "SELECT Total FROM Customer_Orders " +
        "WHERE Identifier = ? " +
        "ORDER BY OrderDate DESC, OrderNum DESC FETCH FIRST 3 ROWS ONLY)"
    );

    statusPs.setString(1, customerId);
    ResultSet statusRs = statusPs.executeQuery();
    statusRs.next();

    double last3Total = statusRs.getDouble(1);

    statusRs.close();
    statusPs.close();

    String newStatus;
    if (last3Total > 500) newStatus = "gold";
    else if (last3Total > 100) newStatus = "silver";
    else if (last3Total > 0) newStatus = "green";
    else newStatus = "new";

    PreparedStatement updatePs = con.prepareStatement(
        "UPDATE Customer SET Status = ? WHERE Identifier = ?"
    );

    updatePs.setString(1, newStatus);
    updatePs.setString(2, customerId);
    updatePs.executeUpdate();
    updatePs.close();
}


static int getInventoryQuantity(String stockNum) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT quantity FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
    );
    ps.setString(1, stockNum);
    ResultSet rs = ps.executeQuery();

    int quantity = -1;
    if (rs.next()) {
        quantity = rs.getInt("quantity");
    }

    rs.close();
    ps.close();
    return quantity;
}

static int getCartQuantityForStock(String cartId, String stockNum) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT NVL(SUM(Quantity), 0) FROM Cart_Items " +
        "WHERE CartId = ? " +
        "AND TRIM(StockNumber) = TRIM(?)"
    );
    ps.setString(1, cartId);
    ps.setString(2, stockNum);
    ResultSet rs = ps.executeQuery();

    int quantity = 0;
    if (rs.next()) {
        quantity = rs.getInt(1);
    }

    rs.close();
    ps.close();
    return quantity;
}

static boolean cartHasEnoughInventory(String cartId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT q.StockNumber, q.CartQty, NVL(ip.quantity, 0) AS InventoryQty " +
        "FROM (" +
        "    SELECT TRIM(StockNumber) AS StockNumber, SUM(Quantity) AS CartQty " +
        "    FROM Cart_Items " +
        "    WHERE CartId = ? " +
        "    GROUP BY TRIM(StockNumber)" +
        ") q LEFT JOIN InventoryProduct ip " +
        "ON TRIM(ip.stock_number) = q.StockNumber " +
        "WHERE ip.stock_number IS NULL OR ip.quantity < q.CartQty"
    );
    ps.setString(1, cartId);
    ResultSet rs = ps.executeQuery();

    boolean enough = true;
    while (rs.next()) {
        enough = false;
        System.out.println("Not enough inventory for product " + rs.getString("StockNumber").trim() +
                ". Requested: " + rs.getInt("CartQty") +
                ", available: " + rs.getInt("InventoryQty"));
    }

    rs.close();
    ps.close();
    return enough;
}

static void deductInventoryForCart(String cartId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "UPDATE InventoryProduct ip " +
        "SET quantity = quantity - (" +
        "    SELECT SUM(ci.Quantity) FROM Cart_Items ci " +
        "    WHERE ci.CartId = ? " +
        "    AND TRIM(ci.StockNumber) = TRIM(ip.stock_number)" +
        ") " +
        "WHERE EXISTS (" +
        "    SELECT 1 FROM Cart_Items ci " +
        "    WHERE ci.CartId = ? " +
        "    AND TRIM(ci.StockNumber) = TRIM(ip.stock_number)" +
        ")"
    );
    ps.setString(1, cartId);
    ps.setString(2, cartId);
    ps.executeUpdate();
    ps.close();
}

static void insertWarehouseOrderFromCart(String orderNum, String cartId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "INSERT INTO WarehouseOrder (order_number, stock_number, quantity_ordered, order_date, status) " +
        "SELECT ?, TRIM(StockNumber), SUM(Quantity), SYSDATE, 'filled' " +
        "FROM Cart_Items " +
        "WHERE CartId = ? " +
        "GROUP BY TRIM(StockNumber)"
    );
    ps.setString(1, orderNum);
    ps.setString(2, cartId);
    ps.executeUpdate();
    ps.close();
}

static void insertWarehouseOrderFromOrderItems(String orderNum) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "INSERT INTO WarehouseOrder (order_number, stock_number, quantity_ordered, order_date, status) " +
        "SELECT TRIM(OrderNum), TRIM(StockNumber), SUM(Quantity), SYSDATE, 'filled' " +
        "FROM Order_Item " +
        "WHERE OrderNum = ? " +
        "GROUP BY TRIM(OrderNum), TRIM(StockNumber)"
    );
    ps.setString(1, orderNum);
    ps.executeUpdate();
    ps.close();
}


static void monthlySummary() throws SQLException {

    System.out.print("Enter month (MM): ");
    String month = scanner.nextLine();

    System.out.print("Enter year (YYYY): ");
    String year = scanner.nextLine();

    System.out.println("\n======================================");
    System.out.println("MONTHLY SALES SUMMARY");
    System.out.println("======================================");

    System.out.println("\n--- Sales Per Product ---");

    PreparedStatement productPs = con.prepareStatement(
        "SELECT p.StockNumber, p.Category, " +
        "SUM(oi.Quantity) AS TotalQty, " +
        "SUM(oi.Quantity * oi.SavedUnitPrice) AS TotalSales " +
        "FROM Products p " +
        "JOIN Order_Item oi ON TRIM(p.StockNumber) = TRIM(oi.StockNumber) " +
        "JOIN Customer_Orders co ON oi.OrderNum = co.OrderNum " +
        "WHERE TO_CHAR(co.OrderDate, 'MM') = ? " +
        "AND TO_CHAR(co.OrderDate, 'YYYY') = ? " +
        "GROUP BY p.StockNumber, p.Category"
    );

    productPs.setString(1, month);
    productPs.setString(2, year);

    ResultSet productRs = productPs.executeQuery();

    System.out.printf("%-12s %-15s %-12s %s%n",
        "Stock#", "Category", "Quantity", "Sales");

    System.out.println("------------------------------------------------");

    while (productRs.next()) {
        System.out.printf("%-12s %-15s %-12d %.2f%n",
            productRs.getString("StockNumber"),
            productRs.getString("Category"),
            productRs.getInt("TotalQty"),
            productRs.getDouble("TotalSales")
        );
    }

    productRs.close();
    productPs.close();

    System.out.println("\n--- Sales Per Category ---");

    PreparedStatement categoryPs = con.prepareStatement(
        "SELECT p.Category, " +
        "SUM(oi.Quantity) AS TotalQty, " +
        "SUM(oi.Quantity * p.Price) AS TotalSales " +
        "FROM Products p, Order_Item oi, Customer_Orders co " +
        "WHERE TRIM(p.StockNumber) = TRIM(oi.StockNumber) " +
        "AND oi.OrderNum = co.OrderNum " +
        "AND TO_CHAR(co.OrderDate, 'YYYY-MM') = ? " +
        "GROUP BY p.Category " +
        "ORDER BY TotalSales DESC"
    );

    categoryPs.setString(1, year + "-" + month);

    ResultSet categoryRs = categoryPs.executeQuery();

    System.out.printf("%-15s %-12s %s%n",
        "Category", "Quantity", "Sales");

    System.out.println("-------------------------------------------");

    while (categoryRs.next()) {
        System.out.printf("%-15s %-12d %.2f%n",
            categoryRs.getString("Category"),
            categoryRs.getInt("TotalQty"),
            categoryRs.getDouble("TotalSales")
        );
    }

    categoryRs.close();
    categoryPs.close();

    System.out.println("\n--- Top Customer ---");

    PreparedStatement topCustomerPs = con.prepareStatement(
        "SELECT * FROM (" +
        "SELECT c.Identifier, c.Name, " +
        "SUM(co.Total) AS TotalSpent " +
        "FROM Customer c, Customer_Orders co " +
        "WHERE c.Identifier = co.Identifier " +
        "AND TO_CHAR(co.OrderDate, 'YYYY-MM') = ? " +
        "GROUP BY c.Identifier, c.Name " +
        "ORDER BY TotalSpent DESC" +
        ") WHERE ROWNUM = 1"
    );

    topCustomerPs.setString(1, year + "-" + month);

    ResultSet topRs = topCustomerPs.executeQuery();

    if (topRs.next()) {
        System.out.println("Customer ID: " + topRs.getString("Identifier"));
        System.out.println("Customer Name: " + topRs.getString("Name"));
        System.out.printf("Total Purchases: %.2f%n", topRs.getDouble("TotalSpent"));
    } else {
        System.out.println("No sales found for this month.");
    }

    topRs.close();
    topCustomerPs.close();
}

static void adjustCustomerStatus() throws SQLException {

    System.out.print("Enter Customer ID: ");
    String customerId = scanner.nextLine();

    PreparedStatement check = con.prepareStatement(
        "SELECT Identifier FROM Customer WHERE Identifier = ?"
    );

    check.setString(1, customerId);
    ResultSet rs = check.executeQuery();

    if (!rs.next()) {
        System.out.println("Customer not found!");
        rs.close();
        check.close();
        return;
    }

    rs.close();
    check.close();

    System.out.println("1. Auto-calculate from sales");
    System.out.println("2. Set manually");

    int choice = readIntSafe("Choose: ");

    String newStatus;

    if (choice == 1) {

        PreparedStatement statusUpdate = con.prepareStatement(
            "SELECT SUM(Total) FROM (" +
            "SELECT Total FROM Customer_Orders " +
            "WHERE Identifier = ? " +
            "ORDER BY OrderDate DESC FETCH FIRST 3 ROWS ONLY)"
        );

        statusUpdate.setString(1, customerId);
        ResultSet statusRs = statusUpdate.executeQuery();
        statusRs.next();

        double last3Total = statusRs.getDouble(1);

        statusRs.close();
        statusUpdate.close();

        if (last3Total > 500) newStatus = "gold";
        else if (last3Total > 100) newStatus = "silver";
        else if (last3Total > 0) newStatus = "green";
        else newStatus = "new";

    } else {

        System.out.print("Enter new status (gold/silver/green/new): ");
        newStatus = scanner.nextLine();

        if (!newStatus.equalsIgnoreCase("gold") &&
            !newStatus.equalsIgnoreCase("silver") &&
            !newStatus.equalsIgnoreCase("green") &&
            !newStatus.equalsIgnoreCase("new")) {

            System.out.println("Invalid status!");
            return;
        }
    }

    PreparedStatement ps = con.prepareStatement(
        "UPDATE Customer SET Status = ? WHERE Identifier = ?"
    );

    ps.setString(1, newStatus);
    ps.setString(2, customerId);

    ps.executeUpdate();
    con.commit();

    System.out.println("Customer " + customerId + " status updated to: " + newStatus);

    ps.close();
}
static void sendOrderToManufacturer() throws SQLException {
    System.out.print("Enter Manufacturer name: ");
    String mfr = scanner.nextLine();

    String stockNum = readStockNumber("Enter Stock Number: ");
    if (stockNum == null) return;

    int quantity = readIntSafe("Enter Quantity to order: ");

    PreparedStatement prodCheck = con.prepareStatement(
        "SELECT StockNumber, Manufacturer, ModelNumber, Price " +
        "FROM Products " +
        "WHERE Manufacturer = ? " +
        "AND TRIM(StockNumber) = TRIM(?)"
    );

    prodCheck.setString(1, mfr);
    prodCheck.setString(2, stockNum);

    ResultSet rs = prodCheck.executeQuery();

    if (!rs.next()) {
        System.out.println("Product not found for that manufacturer!");
        rs.close();
        prodCheck.close();
        return;
    }

    String replenishmentId = "REP-" + System.currentTimeMillis();

    PreparedStatement insertRep = con.prepareStatement(
        "INSERT INTO ReplenishmentOrder " +
        "(replenishment_order_id, stock_number, quantity_ordered, replenishment_order_date, status) " +
        "VALUES (?, ?, ?, ?, ?)"
    );

    insertRep.setString(1, replenishmentId);
    insertRep.setString(2, stockNum);
    insertRep.setInt(3, quantity);
    insertRep.setDate(4, java.sql.Date.valueOf(java.time.LocalDate.now()));
    insertRep.setString(5, "ordered");

    insertRep.executeUpdate();
    insertRep.close();

    PreparedStatement updateRep = con.prepareStatement(
        "UPDATE InventoryProduct " +
        "SET replenishment = replenishment + ? " +
        "WHERE TRIM(stock_number) = TRIM(?)"
    );

    updateRep.setInt(1, quantity);
    updateRep.setString(2, stockNum);

    updateRep.executeUpdate();
    updateRep.close();

    System.out.println("\n--- Order to Manufacturer ---");
    System.out.println("Manufacturer: " + rs.getString("Manufacturer"));
    System.out.println("Stock#:       " + rs.getString("StockNumber"));
    System.out.println("Model:        " + rs.getString("ModelNumber"));
    System.out.println("Quantity:     " + quantity);
    System.out.println("Order Date:   " + java.time.LocalDate.now());
    System.out.println("--- Order sent to manufacturer! ---");

    rs.close();
    prodCheck.close();
}

static void changePrice() throws SQLException {
    String stockNum = readStockNumber("Enter Stock Number: ");
    if (stockNum == null) return;

    double newPrice = readDoubleSafe("Enter new price: ");

    while (newPrice < 0) {
        newPrice = readDoubleSafe("Price cannot be negative! Enter a new price: ");
    }

    PreparedStatement check = con.prepareStatement(
        "SELECT StockNumber FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
    );

    check.setString(1, stockNum);
    ResultSet rs = check.executeQuery();

    if (!rs.next()) {
        System.out.println("Product not found!");
        rs.close();
        check.close();
        return;
    }

    rs.close();
    check.close();

    PreparedStatement ps = con.prepareStatement(
        "UPDATE Products SET Price = ? WHERE TRIM(StockNumber) = TRIM(?)"
    );

    ps.setDouble(1, newPrice);
    ps.setString(2, stockNum);

    ps.executeUpdate();
    con.commit();

    System.out.printf("Price updated to $%.2f for stock %s%n", newPrice, stockNum);

    ps.close();
}
static void deleteTransactions() throws SQLException {
    System.out.print("Enter Order Number to delete: "); 
    String orderNum = scanner.nextLine();

    PreparedStatement check = con.prepareStatement(
        "SELECT OrderNum FROM Customer_Orders WHERE OrderNum = ?"
    );
    check.setString(1, orderNum);
    ResultSet rs = check.executeQuery();

    if (!rs.next()) {
        System.out.println("Order not found!");
        rs.close(); 
        check.close(); 
        return;
    }

    rs.close(); 
    check.close();

    PreparedStatement ps1 = con.prepareStatement(
        "DELETE FROM Order_Item WHERE OrderNum = ?"
    );
    ps1.setString(1, orderNum);
    ps1.executeUpdate();
    ps1.close();

    PreparedStatement ps2 = con.prepareStatement(
        "DELETE FROM Customer_Orders WHERE OrderNum = ?"
    );
    ps2.setString(1, orderNum);
    ps2.executeUpdate();
    ps2.close();

    con.commit();
    System.out.println("Order " + orderNum + " deleted!");
}
static int readIntSafe(String prompt) {
    while (true) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Please enter digits only.");
        }
    }
}

static double readDoubleSafe(String prompt) {
    while (true) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Please enter a valid decimal value.");
        }
    }
}

    // =========================
    // eDEPOT Warehouse Methods
    // =========================

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
            System.out.println("0. Back to Main Menu");

            int choice = readIntSafe("Choose: ");

            if (choice == 1) viewInventory();
            else if (choice == 2) checkItemQuantity();
            else if (choice == 3) receiveShippingNotice();
            else if (choice == 4) receiveShipment();
            else if (choice == 5) fillEmartOrder();
            else if (choice == 6) showManufacturersNeedingReplenishment();
            else if (choice == 7) generateReplenishmentOrders();
            else if (choice == 0) running = false;
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
        int replenishmentToAdd = 0;

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

            // New product has no existing replenishment yet.
            replenishmentToAdd = quantity;

        } else {
            System.out.println("Existing product found. Stock Number: " + stockNum);

            if (!canAddReplenishment(stockNum, quantity)) return false;

            // Only add the extra amount not already covered by current replenishment.
            replenishmentToAdd = getReplenishmentToAddForNotice(stockNum, quantity);
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

            if (replenishmentToAdd > 0) {
                PreparedStatement ps2 = con.prepareStatement(
                        "UPDATE InventoryProduct " +
                        "SET replenishment = replenishment + ? " +
                        "WHERE TRIM(stock_number) = TRIM(?)"
                );
                ps2.setInt(1, replenishmentToAdd);
                ps2.setString(2, stockNum);
                ps2.executeUpdate();
                ps2.close();

                System.out.println("Shipping notice item received. Replenishment increased by "
                        + replenishmentToAdd + " for " + stockNum + ".");
            } else {
                System.out.println("Shipping notice item received. Existing replenishment already covers "
                        + stockNum + ".");
            }

            con.commit();
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
                    "SELECT oi.OrderNum, TRIM(oi.StockNumber), SUM(oi.Quantity), SYSDATE, 'pending' " +
                    "FROM Order_Item oi " +
                    "WHERE oi.OrderNum = ? " +
                    "GROUP BY oi.OrderNum, TRIM(oi.StockNumber)"
            );
            ps1.setString(1, orderNum);
            int inserted = ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE InventoryProduct ip " +
                    "SET quantity = quantity - (" +
                    "    SELECT SUM(oi.Quantity) " +
                    "    FROM Order_Item oi " +
                    "    WHERE oi.OrderNum = ? " +
                    "    AND TRIM(oi.StockNumber) = TRIM(ip.stock_number)" +
                    ") " +
                    "WHERE EXISTS (" +
                    "    SELECT 1 FROM Order_Item oi " +
                    "    WHERE oi.OrderNum = ? " +
                    "    AND TRIM(oi.StockNumber) = TRIM(ip.stock_number)" +
                    ")"
            );
            ps2.setString(1, orderNum);
            ps2.setString(2, orderNum);
            ps2.executeUpdate();
            ps2.close();

            PreparedStatement ps3 = con.prepareStatement(
                    "UPDATE WarehouseOrder SET status = 'filled' " +
                    "WHERE order_number = ?"
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
                        "WHERE manufacturer_name = ? " +
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
                        "    WHERE ro.replenishment_order_id = ? " +
                        "    AND TRIM(ro.stock_number) = TRIM(ip.stock_number)" +
                        ") " +
                        "WHERE EXISTS (" +
                        "    SELECT 1 FROM ReplenishmentOrder ro " +
                        "    WHERE ro.replenishment_order_id = ? " +
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
                "WHERE manufacturer_name = ? " +
                "AND model_number = ?"
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
                "SELECT notice_id FROM ShippingNotice WHERE notice_id = ?"
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
                "SELECT shipping_company_name FROM ShippingNotice WHERE notice_id = ?"
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

        int openNoticeQuantity = getOpenShippingNoticeQuantity(stockNum);
        int neededIncoming = openNoticeQuantity + quantityToAdd;

        // If current replenishment already covers the notice quantity,
        // do not count the notice again.
        int extraReplenishmentNeeded = Math.max(0, neededIncoming - replenishment);

        if (quantity + replenishment + extraReplenishmentNeeded > maxStock) {
            System.out.println("This notice would exceed the maximum stock level.");
            System.out.println("Current quantity: " + quantity +
                    ", current replenishment: " + replenishment +
                    ", open notice quantity: " + openNoticeQuantity +
                    ", new notice quantity: " + quantityToAdd +
                    ", max: " + maxStock);
            return false;
        }

        return true;
    }

    static int getOpenShippingNoticeQuantity(String stockNum) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT NVL(SUM(sn.quantity), 0) " +
                "FROM ShippingNotice sn " +
                "WHERE TRIM(sn.stock_number) = TRIM(?) " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM Shipment sh " +
                "    WHERE sh.notice_id = sn.notice_id " +
                "    AND TRIM(sh.stock_number) = TRIM(sn.stock_number)" +
                ")"
        );

        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        int total = 0;
        if (rs.next()) {
            total = rs.getInt(1);
        }

        rs.close();
        ps.close();

        return total;
    }

    static int getReplenishmentToAddForNotice(String stockNum, int noticeQuantityToAdd) throws SQLException {
        int openNoticeQuantity = getOpenShippingNoticeQuantity(stockNum);
        int currentReplenishment = getReplenishment(stockNum);

        int neededIncoming = openNoticeQuantity + noticeQuantityToAdd;

        return Math.max(0, neededIncoming - currentReplenishment);
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
                "WHERE notice_id = ? AND TRIM(stock_number) = TRIM(?)"
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
                "WHERE notice_id = ? AND TRIM(stock_number) = TRIM(?)"
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
                "WHERE notice_id = ? AND TRIM(stock_number) = TRIM(?)"
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
                "WHERE shipment_id = ? AND TRIM(stock_number) = TRIM(?)"
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
                "WHERE notice_id = ? AND TRIM(stock_number) = TRIM(?)"
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
                "SELECT OrderNum FROM Order_Item WHERE OrderNum = ?"
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
                "SELECT order_number FROM WarehouseOrder WHERE order_number = ?"
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
                "    WHERE OrderNum = ? " +
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
                "WHERE replenishment_order_id = ?"
        );
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
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
        if (!location.matches("^[A-Za-z](0|[1-9][0-9]*)$")) {
            System.out.println("Location must be a letter followed by 0 or a number without leading zeros, for example A0 or A12.");
            return false;
        }
        return true;
    }
}