package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Customer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// GRASP: Pure Fabrication - artificial service class for DB operations
// GRASP: Protected Variation - implements IRepository so callers never depend on concrete class
// Provides CRUD operations for Customer entity
public class CustomerRepository implements ICustomerRepository {

    private final DatabaseManager dbManager;

    public CustomerRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Customer save(Customer customer) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO Customers (full_name, email, phone) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, customer.getFullName());
        ps.setString(2, customer.getEmail());
        ps.setString(3, customer.getPhone());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            customer.setCustomerId(keys.getInt(1));
        }
        keys.close();
        ps.close();
        return customer;
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customers ORDER BY full_name");

        while (rs.next()) {
            Customer c = new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone")
            );
            customers.add(c);
        }
        rs.close();
        stmt.close();
        return customers;
    }

    public Customer findById(int id) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Customers WHERE customer_id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        Customer c = null;
        if (rs.next()) {
            c = new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone")
            );
        }
        rs.close();
        ps.close();
        return c;
    }

    public Customer findByEmail(String email) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Customers WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        Customer c = null;
        if (rs.next()) {
            c = new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone")
            );
        }
        rs.close();
        ps.close();
        return c;
    }

    public void update(Customer customer) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Customers SET full_name = ?, email = ?, phone = ? WHERE customer_id = ?"
        );
        ps.setString(1, customer.getFullName());
        ps.setString(2, customer.getEmail());
        ps.setString(3, customer.getPhone());
        ps.setInt(4, customer.getCustomerId());
        ps.executeUpdate();
        ps.close();
    }

    public void delete(int customerId) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM Customers WHERE customer_id = ?");
        ps.setInt(1, customerId);
        ps.executeUpdate();
        ps.close();
    }

    public int count() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Customers");
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
        return count;
    }
}
