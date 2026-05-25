package org.ivc.dbms.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
                    "jdbc:oracle:thin:@CS174AShoppingDatabase_low",
                    dbUser,
                    dbPassword
            );

            con.setAutoCommit(false);
            System.out.println("Connected!");

            boolean running = true;
            while (running) {
                System.out.println("\n--- Welcome to eMART ---");
                System.out.println("1. Customer Login");
                System.out.println("2. Manager Login");
                System.out.println("3. Exit");
                System.out.print("Choose: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                if (choice == 1) customerLogin();
                else if (choice == 2) managerLogin();
                else if (choice == 3) running = false;
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
        System.out.print("Enter Customer ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();

        PreparedStatement checkPs = con.prepareStatement(
                "SELECT Status FROM Customer " +
                        "WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?)) " +
                        "AND TRIM(Password) = TRIM(?)"
        );

        checkPs.setString(1, id);
        checkPs.setString(2, pass);

        ResultSet rs = checkPs.executeQuery();

        if (rs.next()) {
            String status = rs.getString("Status");
            System.out.println("Welcome back!");
            System.out.println("Customer status: " + status);

        } else {
            System.out.println("New customer detected. Please enter your information.");

            System.out.print("Enter Name: ");
            String name = scanner.nextLine();

            System.out.print("Enter Email: ");
            String email = scanner.nextLine();

            System.out.print("Enter Address: ");
            String address = scanner.nextLine();

            String status = "new";

            PreparedStatement insertPs = con.prepareStatement(
                    "INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) VALUES (?, ?, ?, ?, ?, ?)"
            );

            insertPs.setString(1, id);
            insertPs.setString(2, pass);
            insertPs.setString(3, name);
            insertPs.setString(4, email);
            insertPs.setString(5, address);
            insertPs.setString(6, status);

            insertPs.executeUpdate();
            insertPs.close();

            System.out.println("New customer account created!");

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
            System.out.println("Cart created for customer: " + cartId);
        }

        rs.close();
        checkPs.close();
        runCustomerMenu(id);
    }

    static void managerLogin() throws SQLException {
        System.out.print("Enter Manager ID: ");
        String id = scanner.nextLine();

        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();

        PreparedStatement ps = con.prepareStatement(
                "SELECT Identifier FROM Managers " +
                        "WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?)) " +
                        "AND TRIM(Password) = TRIM(?)"
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
            System.out.println("\n--- Customer Menu ---");
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
            System.out.println("11. Logout");
            System.out.print("Choose: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

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
            else if (choice == 11) running = false;
        }
    }

    static void runManagerMenu() throws SQLException {
        boolean running = true;

        while (running) {
            System.out.println("\n--- Manager Menu ---");
            System.out.println("1. Print monthly sales summary");
            System.out.println("2. Adjust customer status");
            System.out.println("3. Send order to manufacturer");
            System.out.println("4. Change price of item");
            System.out.println("5. Delete old sales transactions");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) monthlySummary();
            else if (choice == 2) adjustCustomerStatus();
            else if (choice == 3) sendOrderToManufacturer();
            else if (choice == 4) changePrice();
            else if (choice == 5) deleteTransactions();
            else if (choice == 6) running = false;
        }
    }

    static void viewProducts() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT StockNumber, Category, Manufacturer, Price FROM Products"
        );

        while (rs.next()) {
            System.out.printf("%s %s %s %.2f%n",
                    rs.getString("StockNumber"),
                    rs.getString("Category"),
                    rs.getString("Manufacturer"),
                    rs.getDouble("Price"));
        }

        rs.close();
        stmt.close();
    }

    static void searchProduct() throws SQLException {
    System.out.println("\n--- Search Products ---");
    System.out.println("1. By stock number");
    System.out.println("2. By manufacturer");
    System.out.println("3. By model number");
    System.out.println("4. By category");
    System.out.println("5. By description attribute");
    System.out.println("6. By compatible item");
    System.out.print("Choose: ");

    int choice = Integer.parseInt(scanner.nextLine());
    PreparedStatement ps = null;

    if (choice == 1) {
        System.out.print("Enter Stock Number: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
        );
        ps.setString(1, val);

    } else if (choice == 2) {
        System.out.print("Enter Manufacturer: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE LOWER(TRIM(Manufacturer)) = LOWER(TRIM(?))"
        );
        ps.setString(1, val);

    } else if (choice == 3) {
        System.out.print("Enter Model Number: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE LOWER(TRIM(ModelNumber)) = LOWER(TRIM(?))"
        );
        ps.setString(1, val);

    } else if (choice == 4) {
        System.out.print("Enter Category: ");
        String val = scanner.nextLine();
        ps = con.prepareStatement(
            "SELECT StockNumber, Category, Manufacturer, ModelNumber, Price, Warranty " +
            "FROM Products WHERE LOWER(TRIM(Category)) = LOWER(TRIM(?))"
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
            "WHERE LOWER(TRIM(p.StockNumber)) = LOWER(TRIM(pd.StockNumber)) " +
            "AND LOWER(TRIM(pd.AttributeName)) = LOWER(TRIM(?)) " +
            "AND LOWER(TRIM(pd.AttributeValue)) = LOWER(TRIM(?))"
        );
        ps.setString(1, attr);
        ps.setString(2, attrVal);

    } else if (choice == 6) {
        System.out.print("Enter Stock Number to find compatible items: ");
        String val = scanner.nextLine();

        ps = con.prepareStatement(
            "SELECT p.StockNumber, p.Category, p.Manufacturer, p.ModelNumber, p.Price, p.Warranty " +
            "FROM Products p " +
            "JOIN Product_Compatibility pc ON p.StockNumber = pc.StockNumber " +
            "WHERE LOWER(TRIM(pc.CompatibleWithNumber)) = LOWER(TRIM(?))"
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
        "SELECT CartId FROM Shopping_Cart WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
    );
    getCart.setString(1, customerId);
    ResultSet rs1 = getCart.executeQuery();

    if (!rs1.next()) {
        System.out.println("No cart found for customer!");
        return;
    }

    String cartId = rs1.getString("CartId").trim();
    rs1.close();
    getCart.close();

    System.out.print("Enter Stock Number: ");
    String stockNum = scanner.nextLine();

    System.out.print("Enter Quantity: ");
    int quantity = Integer.parseInt(scanner.nextLine());

    PreparedStatement customerCheck = con.prepareStatement(
        "SELECT Identifier FROM Customer WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
        "SELECT StockNumber FROM Products WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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

    PreparedStatement check = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE LOWER(TRIM(CartId)) = LOWER(TRIM(?))"
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

    PreparedStatement ps2 = con.prepareStatement(
        "INSERT INTO Cart_Items (StockNumber, CartId, Quantity) VALUES (?, ?, ?)"
    );

    ps2.setString(1, stockNum);
    ps2.setString(2, cartId);
    ps2.setInt(3, quantity);

    ps2.executeUpdate();

    con.commit();
    System.out.println("Item added to cart!");
    ps2.close();
}

   static void viewCart(String customerId) throws SQLException {
    PreparedStatement getCart = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
        "WHERE LOWER(TRIM(ci.StockNumber)) = LOWER(TRIM(p.StockNumber)) " +
        "AND LOWER(TRIM(ci.CartId)) = LOWER(TRIM(?))"
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
        "SELECT CartId FROM Shopping_Cart WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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

    System.out.print("Enter Stock Number: ");
    String stockNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "DELETE FROM Cart_Items " +
        "WHERE LOWER(TRIM(CartId)) = LOWER(TRIM(?)) " +
        "AND LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
        "SELECT CartId FROM Shopping_Cart WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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

    String orderNum = "ORD-" + System.currentTimeMillis();

    PreparedStatement ps = con.prepareStatement(
        "SELECT ci.StockNumber, ci.Quantity, p.Price " +
        "FROM Cart_Items ci, Products p " +
        "WHERE LOWER(TRIM(ci.CartId)) = LOWER(TRIM(?)) " +
        "AND LOWER(TRIM(ci.StockNumber)) = LOWER(TRIM(p.StockNumber))"
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
        "SELECT Status FROM Customer WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
        "WHERE LOWER(TRIM(ci.CartId)) = LOWER(TRIM(?)) " +
        "AND LOWER(TRIM(ci.StockNumber)) = LOWER(TRIM(p.StockNumber))"
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

    PreparedStatement clear = con.prepareStatement(
        "DELETE FROM Cart_Items WHERE LOWER(TRIM(CartId)) = LOWER(TRIM(?))"
    );

    clear.setString(1, cartId);
    clear.executeUpdate();
    clear.close();

    con.commit();

    System.out.println("Order placed!");
    System.out.println("Order#: " + orderNum);
    System.out.println("Total: " + total);
}
static void viewOrderHistory(String customerId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT OrderNum, OrderDate, Subtotal, Discount, Total, Shipping " +
        "FROM Customer_Orders WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
    System.out.print("Enter Stock Number: ");
    String stockNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "SELECT AttributeName, AttributeValue " +
        "FROM Product_Description " +
        "WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
        "WHERE LOWER(TRIM(o.OrderNum)) = LOWER(TRIM(oi.OrderNum)) " +
        "AND LOWER(TRIM(oi.StockNumber)) = LOWER(TRIM(p.StockNumber)) " +
        "AND LOWER(TRIM(o.OrderNum)) = LOWER(TRIM(?))"
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
        "SELECT Shipping FROM Customer_Orders WHERE LOWER(TRIM(OrderNum)) = LOWER(TRIM(?)) " +
        "AND LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
        "SELECT StockNumber, Quantity FROM Order_Item WHERE LOWER(TRIM(OrderNum)) = LOWER(TRIM(?))"
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
            "SELECT Price FROM Products WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
            "SELECT quantity FROM InventoryProduct WHERE LOWER(TRIM(stock_number)) = LOWER(TRIM(?))"
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
        "SELECT Status FROM Customer WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
            "UPDATE InventoryProduct SET quantity = quantity - ? WHERE LOWER(TRIM(stock_number)) = LOWER(TRIM(?))"
        );

        invUp.setInt(1, quantities.get(i));
        invUp.setString(2, stockNumbers.get(i));

        invUp.executeUpdate();
        invUp.close();
    }

    con.commit();

    System.out.println("Order re-run successful! New Order#: " + newOrderNum);
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
        "JOIN Order_Item oi ON LOWER(TRIM(p.StockNumber)) = LOWER(TRIM(oi.StockNumber)) " +
        "JOIN Customer_Orders co ON LOWER(TRIM(oi.OrderNum)) = LOWER(TRIM(co.OrderNum)) " +
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
        "WHERE LOWER(TRIM(p.StockNumber)) = LOWER(TRIM(oi.StockNumber)) " +
        "AND LOWER(TRIM(oi.OrderNum)) = LOWER(TRIM(co.OrderNum)) " +
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
        "WHERE LOWER(TRIM(c.Identifier)) = LOWER(TRIM(co.Identifier)) " +
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
        "SELECT Identifier FROM Customer WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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
    System.out.print("Choose: ");

    int choice = Integer.parseInt(scanner.nextLine());

    String newStatus;

    if (choice == 1) {

        PreparedStatement statusUpdate = con.prepareStatement(
            "SELECT SUM(Total) FROM (" +
            "SELECT Total FROM Customer_Orders " +
            "WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?)) " +
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
        "UPDATE Customer SET Status = ? WHERE LOWER(TRIM(Identifier)) = LOWER(TRIM(?))"
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

    System.out.print("Enter Stock Number: ");
    String stockNum = scanner.nextLine();

    System.out.print("Enter Quantity to order: ");
    int quantity = Integer.parseInt(scanner.nextLine());

    PreparedStatement prodCheck = con.prepareStatement(
        "SELECT StockNumber, Manufacturer, ModelNumber, Price " +
        "FROM Products " +
        "WHERE LOWER(TRIM(Manufacturer)) = LOWER(TRIM(?)) " +
        "AND LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
        "WHERE LOWER(TRIM(stock_number)) = LOWER(TRIM(?))"
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
    System.out.print("Enter Stock Number: ");
    String stockNum = scanner.nextLine();

    System.out.print("Enter new price: ");
    double newPrice = Double.parseDouble(scanner.nextLine());

    while (newPrice < 0) {
        System.out.println("Price cannot be negative! Enter a new price");
        newPrice = Double.parseDouble(scanner.nextLine());
    }

    PreparedStatement check = con.prepareStatement(
        "SELECT StockNumber FROM Products WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
        "UPDATE Products SET Price = ? WHERE LOWER(TRIM(StockNumber)) = LOWER(TRIM(?))"
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
        "SELECT OrderNum FROM Customer_Orders WHERE LOWER(TRIM(OrderNum)) = LOWER(TRIM(?))"
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
        "DELETE FROM Order_Item WHERE LOWER(TRIM(OrderNum)) = LOWER(TRIM(?))"
    );
    ps1.setString(1, orderNum);
    ps1.executeUpdate();
    ps1.close();

    PreparedStatement ps2 = con.prepareStatement(
        "DELETE FROM Customer_Orders WHERE LOWER(TRIM(OrderNum)) = LOWER(TRIM(?))"
    );
    ps2.setString(1, orderNum);
    ps2.executeUpdate();
    ps2.close();

    con.commit();
    System.out.println("Order " + orderNum + " deleted!");
}
}