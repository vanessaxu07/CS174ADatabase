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
                System.out.println("6. Exit");
                System.out.print("Choose: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); 

                if(choice == 1) viewProducts();
                else if(choice == 2) searchProduct();
                else if(choice == 3) addCustomer();
                else if(choice == 4) addToCart();
                else if(choice == 5) addProduct();
                else if(choice == 6) running = false;
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
}