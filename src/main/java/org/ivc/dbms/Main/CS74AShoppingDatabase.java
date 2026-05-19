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
public class CS74AShoppingDatabase {

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

            // Simple menu
            boolean running = true;
            while (running) {
                System.out.println("\n--- Shopping Database ---");
                System.out.println("1. View all products");
                System.out.println("2. Search product by stock number");
                System.out.println("3. Add a customer");
                System.out.println("4. Exit");
                System.out.print("Choose: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); 

                if(choice == 1) viewProducts();
                if(choice == 2) searchProduct();
                if(choice == 3) addCustomer();
                if(choice == 4) running = false;
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

    // 1. View all products
    static void viewProducts() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT StockNumber, Category, Manufacturer, Price FROM Products");

        System.out.println("\nStock#\t\tCategory\tManufacturer\tPrice");
        System.out.println("-------------------------------------------------------");
        while (rs.next()) {
            System.out.println(
                rs.getString("StockNumber") + "\t" +
                rs.getString("Category")    + "\t" +
                rs.getString("Manufacturer")+ "\t" +
                rs.getDouble("Price")
            );
        }
        rs.close();
        stmt.close();
    }

    // 2. Search product by stock number (PreparedStatement with ? parameter)
    static void searchProduct() throws SQLException {
        System.out.print("Enter stock number: ");
        String stockNum = scanner.nextLine();

        PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM Products WHERE StockNumber = ?"
        );
        ps.setString(1, stockNum);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("Found: " + rs.getString("Manufacturer") +
                               " | Price: $" + rs.getDouble("Price") +
                               " | Warranty: " + rs.getString("Warranty"));
        } else {
            System.out.println("No product found with that stock number.");
        }
        rs.close();
        ps.close();
    }

    // 3. Add a customer (INSERT with parameters)
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