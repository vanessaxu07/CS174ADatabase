/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.ivc.dbms.Main;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
/**
 *
 * @author vanessaxu
 */
public class CS174AShoppingDatabase {


    //ENSURE THE SPECIFIC FORMAT VIOLATIONS (like stock number) ARE HANDLED

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

//I THINK WE SHOULD AUTO GENERATE EACH NEW CUSTOMER'S CARTID FROM THE BEGINNING 
// AND PUT IT INTO SHOPPING CART (BASICALLY CREATE THE SHOPPING CART WHEN A NEW 
// CUSTOMER IS ADDED including created date and id of customer). 
// ORDERNUM ID SHOULD ALR BE SET TO AUTOGENERATE.
static void customerLogin() throws SQLException {
    System.out.print("Enter Customer ID: "); String id   = scanner.nextLine();
    System.out.print("Enter Password: ");    String pass = scanner.nextLine();
    PreparedStatement checkPs = con.prepareStatement(
    "SELECT Status FROM Customer " +
    "WHERE TRIM(Identifier)=TRIM(?) AND TRIM(Password)=TRIM(?)"
);

    checkPs.setString(1, id);
    checkPs.setString(2, pass);

    ResultSet rs = checkPs.executeQuery();

    if (rs.next()) {
        String status = rs.getString("Status");
        System.out.println("Welcome back!");
        System.out.println("Customer status: " + status);

    } 
    else {
        System.out.println("New customer detected. Please enter your information.");
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Address: ");
        String address = scanner.nextLine();
        String status = "new";
        PreparedStatement insertPs = con.prepareStatement(
            "INSERT INTO Customer " +
            "(Identifier, Password, Name, Email, Address, Status) " +
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

    System.out.println("Cart created for customer: " + cartId);
    }

    rs.close();
    checkPs.close();
    runCustomerMenu(id);
}

static void managerLogin() throws SQLException {
    System.out.print("Enter Manager ID: "); String id   = scanner.nextLine();
    System.out.print("Enter Password: ");   String pass = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "SELECT Identifier FROM Managers WHERE TRIM(Identifier) = TRIM(?) AND TRIM(Password) = TRIM(?)"
    );
    ps.setString(1, id);
    ps.setString(2, pass);
    ResultSet rs = ps.executeQuery();

    if (!rs.next()) {
        System.out.println("Invalid ID or password!");
        rs.close(); ps.close(); return;
    }
    rs.close(); ps.close();

    System.out.println("Welcome, Manager " + id + "!");
    runManagerMenu();
}

static void runCustomerMenu(String customerId) throws SQLException {
    boolean running = true;
    while (running) {
        System.out.println("\n--- Customer Menu ---");
        System.out.println("1. View all products");
        System.out.println("2. Search product by stock number");
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

        if (choice == 1)       viewProducts();
        else if (choice == 2)  searchProduct();
        else if (choice == 3)  viewCart(customerId);
        else if (choice == 4)  removeFromCart(customerId);
        else if (choice == 5)  placeOrder(customerId);
        else if (choice == 6)  viewOrderHistory(customerId);
        else if (choice == 7)  viewProductDescription();
        else if (choice == 8)  addToCart(customerId);
        else if (choice == 9)  viewOrderByNumber();
        else if (choice == 10) rerunOrder(customerId);
        else if (choice == 11) running = false;
        else System.out.println("Invalid Choice");
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

        if (choice == 1)      monthlySummary();
        else if (choice == 2) adjustCustomerStatus();
        else if (choice == 3) sendOrderToManufacturer();
        else if (choice == 4) changePrice();
        else if (choice == 5) deleteTransactions();
        else if (choice == 6) running = false;
        else System.out.println("Invalid Choice");
    }
}
    
    static void viewProducts() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT StockNumber, Category, Manufacturer, Price FROM Products");

        System.out.println("\n" + String.format("%-12s %-12s %-15s %s", "Stock#", "Category", "Manufacturer", "Price"));
        System.out.println("-------------------------------------------------------");
        while (rs.next()) {
            System.out.printf("%-12s %-12s %-15s %.2f%n",
            rs.getString("StockNumber").trim(),
            rs.getString("Category").trim(),
            rs.getString("Manufacturer").trim(),
            rs.getDouble("Price")
        );
        }
        rs.close();
        stmt.close();
    }
    //MATCH THE VARIABLE TYPES APPROPRIATELY

    static void searchProduct() throws SQLException {
        System.out.print("Enter stock number: ");
        String stockNum = scanner.nextLine();

        PreparedStatement ps = con.prepareStatement(
        "SELECT * FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("Found: " + rs.getString("Manufacturer").trim() +
                               " | Price: $" + rs.getDouble("Price") +
                               " | Warranty: " + rs.getInt("Warranty"));
        } else {
            System.out.println("No product found with that stock number.");
        }
        rs.close();
        ps.close();
    }

static void addToCart(String customerId) throws SQLException {
PreparedStatement getCart = con.prepareStatement(
    "SELECT CartId FROM Shopping_Cart WHERE TRIM(Identifier) = TRIM(?)"
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
    System.out.print("Enter Stock Number: "); String stockNum   = scanner.nextLine();
    System.out.print("Enter Quantity: ");     int quantity      = Integer.parseInt(scanner.nextLine());

    PreparedStatement customerCheck = con.prepareStatement(
        "SELECT Identifier FROM Customer WHERE TRIM(Identifier) = TRIM(?)"
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

   PreparedStatement check = con.prepareStatement(
    "SELECT CartId FROM Shopping_Cart WHERE TRIM(CartId) = TRIM(?)"
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
        "SELECT CartId FROM Shopping_Cart WHERE TRIM(Identifier)=TRIM(?)"
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
        "AND TRIM(ci.CartId) = TRIM(?)"
    );
    ps.setString(1, cartId);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-12s %-15s %-10s %s", "Stock#", "Manufacturer", "Price", "Qty"));
    System.out.println("--------------------------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.printf("%-12s %-15s %-10.2f %d%n",
            rs.getString("StockNumber").trim(),
            rs.getString("Manufacturer").trim(),
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
        "SELECT CartId FROM Shopping_Cart WHERE TRIM(Identifier) = TRIM(?)"
    );
    getCart.setString(1, customerId);
    ResultSet cartRs = getCart.executeQuery();
    if (!cartRs.next()) {
        System.out.println("No cart found!");
        cartRs.close(); getCart.close(); return;
    }
    String cartId = cartRs.getString("CartId").trim();
    cartRs.close(); getCart.close();

    System.out.print("Enter Stock Number: "); String stockNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "DELETE FROM Cart_Items WHERE TRIM(CartId) = TRIM(?) AND TRIM(StockNumber) = TRIM(?)"
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
        "SELECT CartId FROM Shopping_Cart WHERE TRIM(Identifier) = TRIM(?)"
    );
    getCart.setString(1, customerId);
    ResultSet cartRs = getCart.executeQuery();
    if (!cartRs.next()) {
        System.out.println("No cart found!");
        cartRs.close(); 
        getCart.close(); return;
    }
    String cartId = cartRs.getString("CartId").trim();
    cartRs.close(); getCart.close();

    System.out.print("Enter Shipping Method: "); String shippingMethod = scanner.nextLine();
    placeOrder(customerId, cartId, shippingMethod);
}
static void placeOrder(String customerId, String cartId, String shippingMethod) throws SQLException {

    String orderNum = "ORD-" + System.currentTimeMillis();

    PreparedStatement ps = con.prepareStatement(
        "SELECT ci.StockNumber, ci.Quantity, p.Price " +
        "FROM Cart_Items ci, Products p " +
        "WHERE TRIM(ci.CartId)=TRIM(?) AND TRIM(ci.StockNumber)=TRIM(p.StockNumber)"
    );

    ps.setString(1, cartId);
    ResultSet rs = ps.executeQuery();

    double subtotal = 0;

    boolean hasItems = false;

    while (rs.next()) {
        hasItems = true;

        String stockNum = rs.getString("StockNumber").trim();
        int qty = rs.getInt("Quantity");
        double price = rs.getDouble("Price");

        subtotal += price * qty;

        PreparedStatement oi = con.prepareStatement(
            "INSERT INTO Order_Item (StockNumber, OrderNum, Quantity) VALUES (?, ?, ?)"
        );
        oi.setString(1, stockNum);
        oi.setString(2, orderNum);
        oi.setInt(3, qty);
        oi.executeUpdate();
        oi.close();

        PreparedStatement inv = con.prepareStatement(
            "UPDATE InventoryProduct SET quantity = quantity - ? WHERE TRIM(stock_number)=TRIM(?)"
        );
        inv.setInt(1, qty);
        inv.setString(2, stockNum);
        inv.executeUpdate();
        inv.close();
    }

    rs.close();
    ps.close();

    if (!hasItems) {
        System.out.println("Cart is empty!");
        return;
    }

    PreparedStatement cust = con.prepareStatement(
        "SELECT Status FROM Customer WHERE TRIM(Identifier)=TRIM(?)"
    );
    cust.setString(1, customerId);
    ResultSet crs = cust.executeQuery();
    crs.next();
    String status = crs.getString(1).trim();
    crs.close();
    cust.close();

    double discountRate = 
    switch (status.toLowerCase()) {
        case "gold", "new" -> 0.10;
        case "silver" -> 0.05;
        default -> 0.0;
    };

    double discount = subtotal * discountRate;
    double afterDiscount = subtotal - discount;

    double shippingFee = 0;
    if (afterDiscount <= 100 && !status.equalsIgnoreCase("new")) {
        shippingFee = afterDiscount * 0.10;
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

    PreparedStatement clear = con.prepareStatement(
        "DELETE FROM Cart_Items WHERE TRIM(CartId)=TRIM(?)"
    );
    clear.setString(1, cartId);
    clear.executeUpdate();
    clear.close();

    con.commit();

    System.out.println("\nOrder placed!");
    System.out.println("Order#: " + orderNum);
    System.out.println("Total: $" + total);
}

static void viewOrderHistory(String customerId) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "SELECT OrderNum, OrderDate, Subtotal, Discount, Total, Shipping " +
        "FROM Customer_Orders WHERE TRIM(Identifier) = TRIM(?)"
    );
    ps.setString(1, customerId);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-12s %-15s %-12s %-12s %-10s %s", "Order#", "Date", "Subtotal", "Discount", "Total", "Shipping"));
    System.out.println("------------------------------------------------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.printf("%-12s %-15s %-12.2f %-12.2f %-10.2f %s%n",
        rs.getString("OrderNum").trim(),
        rs.getString("OrderDate").trim(),
        rs.getDouble("Subtotal"),
        rs.getDouble("Discount"),
        rs.getDouble("Total"),
        rs.getString("Shipping").trim()
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
        "FROM Product_Description WHERE TRIM(StockNumber) = TRIM(?)"
    );
    ps.setString(1, stockNum);
    ResultSet rs = ps.executeQuery();

    System.out.println("\n" + String.format("%-20s %s", "Attribute", "Value"));
    System.out.println("----------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.printf("%-20s %s%n",
        rs.getString("AttributeName").trim(),
        rs.getString("AttributeValue").trim()
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
        "WHERE TRIM(o.OrderNum) = TRIM(oi.OrderNum) " +
        "AND TRIM(oi.StockNumber) = TRIM(p.StockNumber) " +
        "AND TRIM(o.OrderNum) = TRIM(?)"
    );
    ps.setString(1, orderNum);
    ResultSet rs = ps.executeQuery();

    boolean found = false;
    while (rs.next()) {
        if (!found) {
            System.out.println("\nOrder#:   " + rs.getString("OrderNum").trim());
            System.out.println("Date:     " + rs.getString("OrderDate").trim());
            System.out.println("Discount: $" + rs.getDouble("Discount"));
            System.out.println("Shipping: " + rs.getString("Shipping").trim());
            System.out.println("Total:    $" + rs.getDouble("Total"));
            System.out.println("\n" + String.format("%-12s %-15s %-10s %s", "Stock#", "Manufacturer", "Price", "Qty"));
            System.out.println("--------------------------------------------------");
        }
        found = true;
        System.out.printf("%-12s %-15s %-10.2f %d%n",
            rs.getString("StockNumber").trim(),
            rs.getString("Manufacturer").trim(),
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

    PreparedStatement ps = con.prepareStatement(
        "SELECT o.Shipping, oi.StockNumber, oi.Quantity " +
        "FROM Customer_Orders o, Order_Item oi " +
        "WHERE TRIM(o.OrderNum) = TRIM(oi.OrderNum) " +
        "AND TRIM(o.Identifier) = TRIM(?) " +
        "AND TRIM(o.OrderNum) = TRIM(?)"
    );
    ps.setString(1, customerId);
    ps.setString(2, oldOrderNum);
    ResultSet rs = ps.executeQuery();

    if (!rs.next()) {
        System.out.println("Order not found!");
        rs.close(); ps.close(); return;
    }

    String shipping = rs.getString("Shipping").trim();
    String stockNum = rs.getString("StockNumber").trim();
    int quantity    = rs.getInt("Quantity");
    rs.close(); ps.close();

    PreparedStatement pricePs = con.prepareStatement(
        "SELECT Price FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
    );
    pricePs.setString(1, stockNum);
    ResultSet priceRs = pricePs.executeQuery();
    if (!priceRs.next()) {
        System.out.println("Product no longer exists!");
        priceRs.close(); pricePs.close(); return;
    }
    double price = priceRs.getDouble("Price");
    priceRs.close(); pricePs.close();

    PreparedStatement invCheck = con.prepareStatement(
        "SELECT quantity FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
    );
    invCheck.setString(1, stockNum);
    ResultSet invRs = invCheck.executeQuery();
    if (!invRs.next()) {
        System.out.println("Product not in inventory!");
        invRs.close(); invCheck.close(); return;
    }
    int currentQty = invRs.getInt("quantity");
    invRs.close(); invCheck.close();
    if (currentQty < quantity) {
        System.out.println("Not enough inventory! Available: " + currentQty);
        return;
    }

    PreparedStatement custPs = con.prepareStatement(
        "SELECT Status FROM Customer WHERE TRIM(Identifier) = TRIM(?)"
    );
    custPs.setString(1, customerId);
    ResultSet custRs = custPs.executeQuery();
    custRs.next();
    String status = custRs.getString("Status").trim();
    custRs.close(); custPs.close();

    double discountRate = switch (status.toLowerCase()) {
        case "gold", "new" -> 0.10;
        case "silver"      -> 0.05;
        default            -> 0.0;
    };

    double subtotal      = price * quantity;
    double discount      = subtotal * discountRate;
    double afterDiscount = subtotal - discount;
    double shippingFee   = 0;
    if (afterDiscount <= 100 && !status.equalsIgnoreCase("new")) {
        shippingFee = afterDiscount * 0.10;
    }
    double total = afterDiscount + shippingFee;

    System.out.println("\n--- Re-run Order Summary ---");
    System.out.println("Customer Status: " + status);
    System.out.printf("Subtotal:        $%.2f%n", subtotal);
    System.out.printf("Discount (%d%%):   -$%.2f%n", (int)(discountRate * 100), discount);
    System.out.printf("Shipping:        $%.2f%n", shippingFee);
    System.out.printf("Total:           $%.2f%n", total);
    System.out.print("Confirm order? (y/n): ");
    String confirm = scanner.nextLine();
    if (!confirm.equalsIgnoreCase("y")) {
        System.out.println("Order cancelled.");
        return;
    }

    String newOrderNum = "ORD-" + System.currentTimeMillis();

    PreparedStatement orderPs = con.prepareStatement(
        "INSERT INTO Customer_Orders " +
        "(OrderNum, Subtotal, OrderDate, Discount, Shipping, Total, Identifier) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );
    orderPs.setString(1, newOrderNum);
    orderPs.setDouble(2, subtotal);
    orderPs.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
    orderPs.setDouble(4, discount);
    orderPs.setString(5, shipping);
    orderPs.setDouble(6, total);
    orderPs.setString(7, customerId);
    orderPs.executeUpdate();
    orderPs.close();

    PreparedStatement oiPs = con.prepareStatement(
        "INSERT INTO Order_Item (StockNumber, OrderNum, Quantity) VALUES (?, ?, ?)"
    );
    oiPs.setString(1, stockNum);
    oiPs.setString(2, newOrderNum);
    oiPs.setInt(3, quantity);
    oiPs.executeUpdate();
    oiPs.close();

    PreparedStatement invPs = con.prepareStatement(
        "UPDATE InventoryProduct SET quantity = quantity - ? WHERE TRIM(stock_number) = TRIM(?)"
    );
    invPs.setInt(1, quantity);
    invPs.setString(2, stockNum);
    invPs.executeUpdate();
    invPs.close();

    con.commit();
    System.out.println("\nOrder re-run! Order#: " + newOrderNum + " | Total: $" + total);
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
        "SUM(oi.Quantity * p.Price) AS TotalSales " +
        "FROM Products p, Order_Item oi, Customer_Orders co " +
        "WHERE TRIM(p.StockNumber) = TRIM(oi.StockNumber) " +
        "AND TRIM(oi.OrderNum) = TRIM(co.OrderNum) " +
        "AND SUBSTR(co.OrderDate,1,7) = ? " +
        "GROUP BY p.StockNumber, p.Category " +
        "ORDER BY TotalSales DESC"
    );

    productPs.setString(1, year + "-" + month);

    ResultSet productRs = productPs.executeQuery();

    System.out.printf("%-12s %-15s %-12s %s%n",
        "Stock#", "Category", "Quantity", "Sales");

    System.out.println("------------------------------------------------");

    while (productRs.next()) {
        System.out.printf("%-12s %-15s %-12d %.2f%n",
            productRs.getString("StockNumber").trim(),
            productRs.getString("Category").trim(),
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
        "AND TRIM(oi.OrderNum) = TRIM(co.OrderNum) " +
        "AND SUBSTR(co.OrderDate,1,7) = ? " +
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
            categoryRs.getString("Category").trim(),
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
        "WHERE TRIM(c.Identifier) = TRIM(co.Identifier) " +
        "AND SUBSTR(co.OrderDate,1,7) = ? " +
        "GROUP BY c.Identifier, c.Name " +
        "ORDER BY TotalSpent DESC" +
        ") WHERE ROWNUM = 1"
    );

    topCustomerPs.setString(1, year + "-" + month);

    ResultSet topRs = topCustomerPs.executeQuery();

    if (topRs.next()) {
        System.out.println("Customer ID: " +
            topRs.getString("Identifier").trim());

        System.out.println("Customer Name: " +
            topRs.getString("Name").trim());

        System.out.printf("Total Purchases: %.2f%n",
            topRs.getDouble("TotalSpent"));
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
        "SELECT Identifier FROM Customer WHERE TRIM(Identifier) = TRIM(?)"
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
            "WHERE TRIM(Identifier) = TRIM(?) " +
            "ORDER BY OrderDate DESC FETCH FIRST 3 ROWS ONLY)"
        );
        statusUpdate.setString(1, customerId);
        ResultSet statusRs = statusUpdate.executeQuery();
        statusRs.next();
        double last3Total = statusRs.getDouble(1);
        statusRs.close(); statusUpdate.close();

        if (last3Total > 500)      newStatus = "gold";
        else if (last3Total > 100) newStatus = "silver";
        else if (last3Total > 0)   newStatus = "green";
        else                       newStatus = "new";
    } else {
        System.out.print("Enter new status (gold/silver/green/new): ");
        newStatus = scanner.nextLine();
        if (!newStatus.equalsIgnoreCase("gold") && !newStatus.equalsIgnoreCase("silver") &&
            !newStatus.equalsIgnoreCase("green") && !newStatus.equalsIgnoreCase("new")) {
            System.out.println("Invalid status!");
            return;
        }
    }

    PreparedStatement ps = con.prepareStatement(
        "UPDATE Customer SET Status = ? WHERE TRIM(Identifier) = TRIM(?)"
    );
    ps.setString(1, newStatus);
    ps.setString(2, customerId);
    ps.executeUpdate();
    con.commit();
    System.out.println("Customer " + customerId + " status updated to: " + newStatus);
    ps.close();
}

//need to send the order
static void sendOrderToManufacturer() throws SQLException {
    System.out.print("Enter Manufacturer name: "); 
    String mfr = scanner.nextLine();
    System.out.print("Enter Stock Number: ");      
    String stockNum  = scanner.nextLine();
    System.out.print("Enter Quantity to order: ");  
    int quantity = Integer.parseInt(scanner.nextLine());

    PreparedStatement prodCheck = con.prepareStatement(
        "SELECT StockNumber, Manufacturer, ModelNumber, Price FROM Products WHERE TRIM(Manufacturer) = TRIM(?) AND TRIM(StockNumber) = TRIM(?)"
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
    "WHERE TRIM(stock_number)=TRIM(?)"
);

    updateRep.setInt(1, quantity);
    updateRep.setString(2, stockNum);
    updateRep.executeUpdate();
    updateRep.close();
    System.out.println("\n--- Order to Manufacturer ---");
    System.out.println("Manufacturer: " + rs.getString("Manufacturer").trim());
    System.out.println("Stock#:       " + rs.getString("StockNumber").trim());
    System.out.println("Model:        " + rs.getString("ModelNumber").trim());
    System.out.println("Quantity:     " + quantity);
    System.out.println("Order Date:   " + java.time.LocalDate.now().toString());
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
        "SELECT OrderNum FROM Customer_Orders WHERE TRIM(OrderNum) = TRIM(?)"
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
        "DELETE FROM Order_Item WHERE TRIM(OrderNum) = TRIM(?)"
    );
    ps1.setString(1, orderNum);
    ps1.executeUpdate();
    ps1.close();

    PreparedStatement ps2 = con.prepareStatement(
        "DELETE FROM Customer_Orders WHERE TRIM(OrderNum) = TRIM(?)"
    );
    ps2.setString(1, orderNum);
    ps2.executeUpdate();
    ps2.close();

    con.commit();
    System.out.println("Order " + orderNum + " deleted!");
}
}