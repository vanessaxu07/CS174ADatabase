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
                System.out.println("\n--- Shopping Database ---");
                System.out.println("1. View all products");
                System.out.println("2. Search product by stock number");
                System.out.println("3. Add a customer");
                System.out.println("4. Add item to cart");
                System.out.println("5. Add Product");

                System.out.println("6. View cart");
                System.out.println("7. Remove item from cart");
                System.out.println("8. Place order");
                System.out.println("9. View order history");
                System.out.println("10. Add product description");
                System.out.println("11. View product description");
                System.out.println("12. Exit");
                System.out.print("Choose: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); 

                if(choice == 1) viewProducts();
                else if(choice == 2) searchProduct();
                else if(choice == 3) addCustomer(); 
                else if(choice == 4) addToCart();
                else if(choice == 5) addProduct();
                else if(choice == 6) viewCart();
                else if(choice == 7) removeFromCart();
                else if(choice == 8) placeOrder();
                else if(choice == 9) viewOrderHistory();
                else if(choice == 10) viewProductDescription();
                else if(choice == 11) addProductDescription();
                else if(choice == 12) running = false;
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
    static void addProduct() throws SQLException {
    System.out.print("Enter Stock Number: ");    
    String stockNum = scanner.nextLine();
    System.out.print("Enter Category: ");        
    String category = scanner.nextLine();
    System.out.print("Enter Manufacturer: ");    
    String mfr = scanner.nextLine();
    System.out.print("Enter Model Number: ");    
    String modelNum = scanner.nextLine();
    System.out.print("Enter Warranty: ");        
    int warranty = Integer.parseInt(scanner.nextLine());
    System.out.print("Enter Price: ");           
    double price = Double.parseDouble(scanner.nextLine());
    System.out.print("Enter Compatible Stock Number (or press Enter to skip): "); 
    String compat = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "INSERT INTO Products (StockNumber, Category, Manufacturer, ModelNumber, Warranty, Price, CompatibleWithNumber) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );
    ps.setString(1, stockNum);
    ps.setString(2, category);
    ps.setString(3, mfr);
    ps.setString(4, modelNum);
    ps.setInt(5, warranty);
    ps.setDouble(6, price);

    if (compat.isEmpty()) {
        ps.setNull(7, java.sql.Types.CHAR);
    } else {
        ps.setString(7, compat);
    }

    ps.executeUpdate();
    con.commit();
    System.out.println("Product added!");
    ps.close();
}
    static void viewProducts() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT StockNumber, Category, Manufacturer, Price FROM Products");

        System.out.println("\nStock#\t\tCategory\tManufacturer\tPrice");
        System.out.println("-------------------------------------------------------");
        while (rs.next()) {
            System.out.println(
                rs.getString("StockNumber").trim() + "\t" +
                rs.getString("Category").trim()    + "\t" +
                rs.getString("Manufacturer").trim()+ "\t" +
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

static void addToCart() throws SQLException {
    System.out.print("Enter Customer ID: ");  String customerId = scanner.nextLine();
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
        PreparedStatement ps1 = con.prepareStatement(
            "INSERT INTO Shopping_Cart (CartId, CreatedDate, Identifier) VALUES (?, ?, ?)"
        );
        ps1.setString(1, cartId);
        ps1.setString(2, java.time.LocalDate.now().toString());
        ps1.setString(3, customerId);
        ps1.executeUpdate();
        ps1.close();
    }
    rs.close();
    check.close();

    // Insert into Cart_Items
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
    static void addCustomer() throws SQLException {
        System.out.print("Enter ID: ");       String id   = scanner.nextLine();
        System.out.print("Enter Name: ");     String name = scanner.nextLine();
        System.out.print("Enter Email: ");    String email= scanner.nextLine();
        System.out.print("Enter Address: ");  String addr = scanner.nextLine();
        System.out.print("Enter Password: "); String pass = scanner.nextLine();

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO Customer (Identifier, Password, Name, Email, Address, Status) " +
            "VALUES (?, ?, ?, ?, ?, 'active')"
        );
        ps.setString(1, id);
        ps.setString(2, pass);
        ps.setString(3, name);
        ps.setString(4, email);
        ps.setString(5, addr);

        ps.executeUpdate();
        con.commit();
        System.out.println("Customer added!");
        ps.close();
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

    System.out.println("\nStock#\t\tManufacturer\t\tPrice\tQty");
    System.out.println("--------------------------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.println(
            rs.getString("StockNumber").trim() + "\t" +
            rs.getString("Manufacturer").trim() + "\t" +
            rs.getDouble("Price") + "\t" +
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

static void placeOrder() throws SQLException {
    System.out.print("Enter Customer ID: ");  String customerId = scanner.nextLine();
    System.out.print("Enter Order Number: "); String orderNum   = scanner.nextLine();
    System.out.print("Enter Cart ID: ");      String cartId     = scanner.nextLine();
    System.out.print("Enter Stock Number: "); String stockNum   = scanner.nextLine();
    System.out.print("Enter Quantity: ");     int quantity      = Integer.parseInt(scanner.nextLine());
    System.out.print("Enter Shipping: ");     String shipping   = scanner.nextLine();
    System.out.print("Enter Discount: ");     double discount   = Double.parseDouble(scanner.nextLine());

    PreparedStatement custCheck = con.prepareStatement(
        "SELECT Identifier FROM Customer WHERE TRIM(Identifier) = TRIM(?)"
    );
    custCheck.setString(1, customerId);
    ResultSet custRs = custCheck.executeQuery();
    if (!custRs.next()) {
        System.out.println("Customer not found!");
        custRs.close(); 
        custCheck.close(); 
        return;
    }
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
    invRs.close(); 
    invCheck.close();

    if (currentQty < quantity) {
        System.out.println("Not enough inventory! Available: " + currentQty);
        return;
    }

    double subtotal = price * quantity;
    double total    = subtotal - discount;

    PreparedStatement ps1 = con.prepareStatement(
        "INSERT INTO Customer_Orders (OrderNum, Subtotal, OrderDate, Discount, Shipping, Total, Identifier) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );
    ps1.setString(1, orderNum);
    ps1.setDouble(2, subtotal);
    ps1.setString(3, java.time.LocalDate.now().toString());
    ps1.setDouble(4, discount);
    ps1.setString(5, shipping);
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

    con.commit();
    System.out.println("Order placed! Total: $" + total);
}
static void viewOrderHistory() throws SQLException {
    System.out.print("Enter Customer ID: "); String customerId = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "SELECT OrderNum, OrderDate, Subtotal, Discount, Total, Shipping " +
        "FROM Customer_Orders WHERE TRIM(Identifier) = TRIM(?)"
    );
    ps.setString(1, customerId);
    ResultSet rs = ps.executeQuery();

    System.out.println("\nOrder#\t\tDate\t\tSubtotal\tDiscount\tTotal\tShipping");
    System.out.println("------------------------------------------------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.println(
            rs.getString("OrderNum").trim() + "\t" +
            rs.getString("OrderDate").trim() + "\t" +
            rs.getDouble("Subtotal") + "\t" +
            rs.getDouble("Discount") + "\t" +
            rs.getDouble("Total") + "\t" +
            rs.getString("Shipping").trim()
        );
    }
    if (!found) System.out.println("No orders found for this customer.");
    rs.close();
    ps.close();
}
static void viewProductDescription() throws SQLException {
    System.out.print("Enter Stock Number: "); String stockNum = scanner.nextLine();

    PreparedStatement ps = con.prepareStatement(
        "SELECT AttributeName, AttributeValue " +
        "FROM Product_Description WHERE TRIM(StockNumber) = TRIM(?)"
    );
    ps.setString(1, stockNum);
    ResultSet rs = ps.executeQuery();

    System.out.println("\nAttribute\t\tValue");
    System.out.println("----------------------------------");
    boolean found = false;
    while (rs.next()) {
        found = true;
        System.out.println(
            rs.getString("AttributeName").trim() + "\t\t" +
            rs.getString("AttributeValue").trim()
        );
    }
    if (!found) System.out.println("No description found for this product.");
    rs.close();
    ps.close();
}

static void addProductDescription() throws SQLException {
    System.out.print("Enter Stock Number: ");    String stockNum  = scanner.nextLine();
    System.out.print("Enter Attribute Name: ");  String attrName  = scanner.nextLine();
    System.out.print("Enter Attribute Value: "); String attrValue = scanner.nextLine();

    // Check product exists first
    PreparedStatement prodCheck = con.prepareStatement(
        "SELECT StockNumber FROM Products WHERE TRIM(StockNumber) = TRIM(?)"
    );
    prodCheck.setString(1, stockNum);
    ResultSet prodRs = prodCheck.executeQuery();
    if (!prodRs.next()) {
        System.out.println("Product not found! Please add the product first.");
        prodRs.close(); 
        prodCheck.close();
        return;
    }
    prodRs.close(); 
    prodCheck.close();

    PreparedStatement ps = con.prepareStatement(
        "INSERT INTO Product_Description (StockNumber, AttributeName, AttributeValue) " +
        "VALUES (?, ?, ?)"
    );
    ps.setString(1, stockNum);
    ps.setString(2, attrName);
    ps.setString(3, attrValue);
    ps.executeUpdate();
    con.commit();
    System.out.println("Description added!");
    ps.close();
}
}