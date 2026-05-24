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
        con = DriverManager.getConnection(
            "jdbc:oracle:thin:@CS174AShoppingDatabase_low?TNS_ADMIN=/Users/vanessaxu/Downloads/Wallet_CS174AShoppingDatabase",
            "ADMIN",
            "Vicecreamlover*1"
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
        else if (choice == 3)  viewCart();
        else if (choice == 4)  removeFromCart();
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
    System.out.print("Enter Cart ID: ");      String cartId     = scanner.nextLine();
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

    static void viewCart() throws SQLException {
    System.out.print("Enter Cart ID: "); String cartId = scanner.nextLine();

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

static void removeFromCart() throws SQLException {
    System.out.print("Enter Cart ID: ");      String cartId   = scanner.nextLine();
    System.out.print("Enter Stock Number: "); String stockNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "DELETE FROM Cart_Items " +
        "WHERE TRIM(CartId) = TRIM(?) AND TRIM(StockNumber) = TRIM(?)"
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
    String orderNum = "ORD-" + System.currentTimeMillis();
    System.out.print("Enter Cart ID: ");      
    String cartId = scanner.nextLine();
    System.out.print("Enter Stock Number: "); 
    String stockNum = scanner.nextLine();
    System.out.print("Enter Quantity: ");     
    int quantity = Integer.parseInt(scanner.nextLine());
    System.out.print("Enter Shipping Method: "); 
    String shippingMethod = scanner.nextLine();
    placeOrder(customerId, orderNum, stockNum, quantity, shippingMethod, cartId);
}
static void placeOrder(String customerId, String orderNum, String stockNum, int quantity, String shippingMethod, String cartId) throws SQLException {

    PreparedStatement custCheck = con.prepareStatement(
        "SELECT Identifier, Status FROM Customer WHERE TRIM(Identifier) = TRIM(?)"
    );
    custCheck.setString(1, customerId);
    ResultSet custRs = custCheck.executeQuery();
    if (!custRs.next()) {
        System.out.println("Customer not found!");
        custRs.close(); 
        custCheck.close(); 
        return;
    }
    String status = custRs.getString("Status").trim().toLowerCase();
    custRs.close(); 
    custCheck.close();

    PreparedStatement cartCheck = con.prepareStatement(
        "SELECT COUNT(*) FROM Cart_Items WHERE TRIM(CartId) = TRIM(?)"
    );
    cartCheck.setString(1, cartId);
    ResultSet cartRs = cartCheck.executeQuery();
    cartRs.next();
    int itemCount = cartRs.getInt(1);
    cartRs.close(); cartCheck.close();
    if (itemCount == 0) {
        System.out.println("Cart is empty! Please add items before placing an order.");
        return;
    }

    PreparedStatement prodCheck = con.prepareStatement(
        "SELECT Price FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
    );
    prodCheck.setString(1, stockNum);
    ResultSet prodRs = prodCheck.executeQuery();
    if (!prodRs.next()) {
        System.out.println("Product not found!");
        prodRs.close(); 
        prodCheck.close(); 
        return;
    }
    double price = prodRs.getDouble("Price");
    prodRs.close(); 
    prodCheck.close();

    PreparedStatement invCheck = con.prepareStatement(
        "SELECT quantity FROM InventoryProduct WHERE TRIM(stock_number) = TRIM(?)"
    );
    invCheck.setString(1, stockNum);
    ResultSet invRs = invCheck.executeQuery();
    if (!invRs.next()) {
        System.out.println("Product not in inventory!");
        invRs.close(); 
        invCheck.close(); 
        return;
    }
    int currentQty = invRs.getInt("quantity");
    invRs.close(); invCheck.close();
    if (currentQty < quantity) {
        System.out.println("Not enough inventory! Available: " + currentQty);
        return;
    }

    double discountRate = 0.0;
    if (status.equals("gold") || status.equals("new")) {
        discountRate = 0.10;
    } else if (status.equals("silver")) {
        discountRate = 0.05;
    }

    double subtotal = price * quantity;
    double discountAmt = subtotal * discountRate;
    double afterDiscount = subtotal - discountAmt;

    double shippingFee = 0.0;
    if (afterDiscount <= 100 && !status.equals("new")) {
        shippingFee = afterDiscount * 0.10;
    }

    double total = afterDiscount + shippingFee;

    System.out.println("\n--- Order Summary ---");
    System.out.println("Customer Status: " + status);
    System.out.println("Subtotal:        $" + subtotal);
    System.out.println("Discount (" + (int)(discountRate * 100) + "%): -$" + discountAmt);
    System.out.println("Shipping:        $" + shippingFee);
    System.out.println("Total:           $" + total);
    System.out.print("Confirm order? (y/n): ");
    String confirm = scanner.nextLine();
    if (!confirm.equalsIgnoreCase("y")) {
        System.out.println("Order cancelled.");
        return;
    }

    PreparedStatement ps1 = con.prepareStatement(
        "INSERT INTO Customer_Orders (OrderNum, Subtotal, OrderDate, Discount, Shipping, Total, Identifier) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );
    ps1.setString(1, orderNum);
    ps1.setDouble(2, subtotal);
    ps1.setString(3, java.time.LocalDate.now().toString());
    ps1.setDouble(4, discountAmt);
    ps1.setString(5, shippingMethod);
    ps1.setDouble(6, total);
    ps1.setString(7, customerId);
    ps1.executeUpdate();
    ps1.close();

    PreparedStatement ps2 = con.prepareStatement(
        "INSERT INTO Order_Item (StockNumber, OrderNum, Quantity) VALUES (?, ?, ?)"
    );
    ps2.setString(1, stockNum);
    ps2.setString(2, orderNum);
    ps2.setInt(3, quantity);
    ps2.executeUpdate();
    ps2.close();

    PreparedStatement ps3 = con.prepareStatement(
        "UPDATE InventoryProduct SET quantity = quantity - ? " +
        "WHERE TRIM(stock_number) = TRIM(?)"
    );
    ps3.setInt(1, quantity);
    ps3.setString(2, stockNum);
    ps3.executeUpdate();
    ps3.close();

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
    statusRs.close(); 
    statusUpdate.close();

    String newStatus;
    if (last3Total > 500) {
        newStatus = "gold";
    } else if (last3Total > 100) {
        newStatus = "silver";
    } else if (last3Total > 0) {
        newStatus = "green";
    } else {
        newStatus = "new";
    }

    PreparedStatement updateStatus = con.prepareStatement(
        "UPDATE Customer SET Status = ? WHERE TRIM(Identifier) = TRIM(?)"
    );
    PreparedStatement clearCart = con.prepareStatement(
    "DELETE FROM Cart_Items WHERE TRIM(CartId) = TRIM(?)"
);
    clearCart.setString(1, cartId);
    clearCart.executeUpdate();
    clearCart.close();

    updateStatus.setString(1, newStatus);
    updateStatus.setString(2, customerId);
    updateStatus.executeUpdate();
    updateStatus.close();

    con.commit();
    System.out.println("\nOrder placed! Order#: " + orderNum + " | Total: $" + total);
    System.out.println("Customer status updated to: " + newStatus);
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
    System.out.print("Enter Order Number to re-run: "); String oldOrderNum = scanner.nextLine();

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

    PreparedStatement cartFind = con.prepareStatement(
        "SELECT CartId FROM Shopping_Cart WHERE TRIM(Identifier) = TRIM(?)"
    );
    cartFind.setString(1, customerId);
    ResultSet cartRs = cartFind.executeQuery();
    if (!cartRs.next()) {
        System.out.println("No cart found! Please create a cart first.");
        cartRs.close(); 
        cartFind.close(); 
        return;
    }
    String cartId = cartRs.getString("CartId").trim();
    cartRs.close(); 
    cartFind.close();

    String newOrderNum = "ORD-" + System.currentTimeMillis();
    System.out.println("Re-running order with Stock#: " + stockNum + " Qty: " + quantity);
    placeOrder(customerId, newOrderNum, stockNum, quantity, shipping, cartId);
}
static void monthlySummary() throws SQLException { 

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
static void sendOrderToManufacturer() throws SQLException { 

}
static void changePrice() throws SQLException {

 }
static void deleteTransactions() throws SQLException { 

}
}