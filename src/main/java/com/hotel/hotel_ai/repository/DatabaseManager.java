package com.hotel.hotel_ai.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// GRASP: Protected Variation - shields the rest of the system from DB changes
// GRASP: Pure Fabrication - artificial class to manage DB lifecycle
public class DatabaseManager {

    private static final String DB_URL;
    static {
        String dbFolder = System.getProperty("user.home") + java.io.File.separator + "INNsight";
        new java.io.File(dbFolder).mkdirs();
        DB_URL = "jdbc:sqlite:" + dbFolder + java.io.File.separator + "innsight.db";
    }
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    // Singleton to ensure one DB connection
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public void initializeDatabase() {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();

            // Create Customers table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Customers (" +
                "customer_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "full_name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "phone TEXT NOT NULL" +
                ")"
            );

            // Create Rooms table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Rooms (" +
                "room_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "room_number INTEGER NOT NULL UNIQUE, " +
                "room_type TEXT NOT NULL, " +
                "capacity INTEGER NOT NULL, " +
                "price_per_night REAL NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'Available'" +
                ")"
            );

            // Create Reservations table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Reservations (" +
                "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER NOT NULL, " +
                "room_id INTEGER NOT NULL, " +
                "check_in TEXT NOT NULL, " +
                "check_out TEXT NOT NULL, " +
                "reservation_status TEXT NOT NULL DEFAULT 'Confirmed', " +
                "payment_status TEXT NOT NULL DEFAULT 'Unpaid', " +
                "FOREIGN KEY (customer_id) REFERENCES Customers(customer_id), " +
                "FOREIGN KEY (room_id) REFERENCES Rooms(room_id)" +
                ")"
            );

            // Create Admin table for login
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Admins (" +
                "admin_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "full_name TEXT NOT NULL" +
                ")"
            );

            // Insert default admin if not exists
            stmt.executeUpdate(
                "INSERT OR IGNORE INTO Admins (username, password, full_name) " +
                "VALUES ('admin', 'admin123', 'System Administrator')"
            );

            // Insert sample rooms if the table is empty
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM Rooms");
            if (rs.next() && rs.getInt(1) == 0) {
                insertSampleData(stmt);
            }
            rs.close();

            stmt.close();
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertSampleData(Statement stmt) throws SQLException {
        // Sample rooms
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (101, 'Single', 1, 75.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (102, 'Single', 1, 75.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (201, 'Double', 2, 120.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (202, 'Double', 2, 120.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (301, 'Suite', 4, 250.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (302, 'Suite', 4, 250.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (401, 'Deluxe', 3, 180.0, 'Available')");
        stmt.executeUpdate("INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (402, 'Penthouse', 5, 500.0, 'Available')");

        // Sample customers
        stmt.executeUpdate("INSERT INTO Customers (full_name, email, phone) VALUES ('Ali Khan', 'ali.khan@email.com', '0300-1234567')");
        stmt.executeUpdate("INSERT INTO Customers (full_name, email, phone) VALUES ('Sara Ahmed', 'sara.ahmed@email.com', '0321-9876543')");
        stmt.executeUpdate("INSERT INTO Customers (full_name, email, phone) VALUES ('Usman Malik', 'usman.m@email.com', '0333-5551234')");
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // Validates admin credentials
    public boolean validateAdmin(String username, String password) {
        try {
            var conn = getConnection();
            var ps = conn.prepareStatement("SELECT COUNT(*) FROM Admins WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            var rs = ps.executeQuery();
            boolean valid = rs.next() && rs.getInt(1) > 0;
            rs.close();
            ps.close();
            return valid;
        } catch (SQLException e) {
            System.err.println("Login validation error: " + e.getMessage());
            return false;
        }
    }
}
