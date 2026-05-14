package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// GRASP: Pure Fabrication - artificial service class for Room DB operations
// GRASP: Protected Variation - implements IRepository so callers never depend on concrete class
public class RoomRepository implements IRoomRepository {

    private final DatabaseManager dbManager;

    public RoomRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Room save(Room room) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO Rooms (room_number, room_type, capacity, price_per_night, status) VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, room.getRoomNumber());
        ps.setString(2, room.getRoomType());
        ps.setInt(3, room.getCapacity());
        ps.setDouble(4, room.getPricePerNight());
        ps.setString(5, room.getStatus());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            room.setRoomId(keys.getInt(1));
        }
        keys.close();
        ps.close();
        return room;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Rooms ORDER BY room_number");

        while (rs.next()) {
            Room r = mapRow(rs);
            rooms.add(r);
        }
        rs.close();
        stmt.close();
        return rooms;
    }

    public List<Room> findAvailable() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM Rooms WHERE status = 'Available' ORDER BY room_number"
        );
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            rooms.add(mapRow(rs));
        }
        rs.close();
        ps.close();
        return rooms;
    }

    public Room findById(int id) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Rooms WHERE room_id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        Room r = null;
        if (rs.next()) {
            r = mapRow(rs);
        }
        rs.close();
        ps.close();
        return r;
    }

    public Room findByRoomNumber(int roomNumber) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Rooms WHERE room_number = ?");
        ps.setInt(1, roomNumber);
        ResultSet rs = ps.executeQuery();

        Room r = null;
        if (rs.next()) {
            r = mapRow(rs);
        }
        rs.close();
        ps.close();
        return r;
    }

    public void update(Room room) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Rooms SET room_number = ?, room_type = ?, capacity = ?, price_per_night = ?, status = ? WHERE room_id = ?"
        );
        ps.setInt(1, room.getRoomNumber());
        ps.setString(2, room.getRoomType());
        ps.setInt(3, room.getCapacity());
        ps.setDouble(4, room.getPricePerNight());
        ps.setString(5, room.getStatus());
        ps.setInt(6, room.getRoomId());
        ps.executeUpdate();
        ps.close();
    }

    public void updateStatus(int roomId, String status) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("UPDATE Rooms SET status = ? WHERE room_id = ?");
        ps.setString(1, status);
        ps.setInt(2, roomId);
        ps.executeUpdate();
        ps.close();
    }

    public void delete(int roomId) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM Rooms WHERE room_id = ?");
        ps.setInt(1, roomId);
        ps.executeUpdate();
        ps.close();
    }

    public int count() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Rooms");
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
        return count;
    }

    public int countAvailable() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Rooms WHERE status = 'Available'");
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
        return count;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("room_id"),
            rs.getInt("room_number"),
            rs.getString("room_type"),
            rs.getInt("capacity"),
            rs.getDouble("price_per_night"),
            rs.getString("status")
        );
    }
}
