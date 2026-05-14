package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// GRASP: Pure Fabrication - artificial service class for Reservation DB operations
// GRASP: Protected Variation - implements IRepository so callers never depend on concrete class
public class ReservationRepository implements IReservationRepository {

    private final DatabaseManager dbManager;

    public ReservationRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Reservation save(Reservation reservation) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO Reservations (customer_id, room_id, check_in, check_out, reservation_status, payment_status) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, reservation.getCustomerId());
        ps.setInt(2, reservation.getRoomId());
        ps.setString(3, reservation.getCheckInDate().toString());
        ps.setString(4, reservation.getCheckOutDate().toString());
        ps.setString(5, reservation.getReservationStatus());
        ps.setString(6, reservation.getPaymentStatus());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            reservation.setReservationId(keys.getInt(1));
        }
        keys.close();
        ps.close();
        return reservation;
    }

    public List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT r.*, c.full_name AS customer_name, rm.room_number, rm.price_per_night " +
            "FROM Reservations r " +
            "JOIN Customers c ON r.customer_id = c.customer_id " +
            "JOIN Rooms rm ON r.room_id = rm.room_id " +
            "ORDER BY r.check_in DESC"
        );

        while (rs.next()) {
            list.add(mapRow(rs));
        }
        rs.close();
        stmt.close();
        return list;
    }

    public Reservation findById(int id) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT r.*, c.full_name AS customer_name, rm.room_number, rm.price_per_night " +
            "FROM Reservations r " +
            "JOIN Customers c ON r.customer_id = c.customer_id " +
            "JOIN Rooms rm ON r.room_id = rm.room_id " +
            "WHERE r.reservation_id = ?"
        );
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        Reservation reservation = null;
        if (rs.next()) {
            reservation = mapRow(rs);
        }
        rs.close();
        ps.close();
        return reservation;
    }

    public List<Reservation> findByCustomerId(int customerId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT r.*, c.full_name AS customer_name, rm.room_number, rm.price_per_night " +
            "FROM Reservations r " +
            "JOIN Customers c ON r.customer_id = c.customer_id " +
            "JOIN Rooms rm ON r.room_id = rm.room_id " +
            "WHERE r.customer_id = ? ORDER BY r.check_in DESC"
        );
        ps.setInt(1, customerId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(mapRow(rs));
        }
        rs.close();
        ps.close();
        return list;
    }

    public List<Reservation> findActiveByRoomId(int roomId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT r.*, c.full_name AS customer_name, rm.room_number, rm.price_per_night " +
            "FROM Reservations r " +
            "JOIN Customers c ON r.customer_id = c.customer_id " +
            "JOIN Rooms rm ON r.room_id = rm.room_id " +
            "WHERE r.room_id = ? AND r.reservation_status = 'Confirmed'"
        );
        ps.setInt(1, roomId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(mapRow(rs));
        }
        rs.close();
        ps.close();
        return list;
    }

    public void update(Reservation reservation) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Reservations SET customer_id = ?, room_id = ?, check_in = ?, check_out = ?, " +
            "reservation_status = ?, payment_status = ? WHERE reservation_id = ?"
        );
        ps.setInt(1, reservation.getCustomerId());
        ps.setInt(2, reservation.getRoomId());
        ps.setString(3, reservation.getCheckInDate().toString());
        ps.setString(4, reservation.getCheckOutDate().toString());
        ps.setString(5, reservation.getReservationStatus());
        ps.setString(6, reservation.getPaymentStatus());
        ps.setInt(7, reservation.getReservationId());
        ps.executeUpdate();
        ps.close();
    }

    public void cancelReservation(int reservationId) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Reservations SET reservation_status = 'Cancelled', payment_status = 'Refunded' " +
            "WHERE reservation_id = ?"
        );
        ps.setInt(1, reservationId);
        ps.executeUpdate();
        ps.close();
    }

    public void delete(int reservationId) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM Reservations WHERE reservation_id = ?");
        ps.setInt(1, reservationId);
        ps.executeUpdate();
        ps.close();
    }

    public int count() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Reservations");
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
        return count;
    }

    public int countActive() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Reservations WHERE reservation_status = 'Confirmed'");
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
        return count;
    }

    // Check if room has conflicting reservation for given dates
    public boolean hasConflict(int roomId, LocalDate checkIn, LocalDate checkOut, int excludeReservationId) throws SQLException {
        Connection conn = dbManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM Reservations WHERE room_id = ? AND reservation_status = 'Confirmed' " +
            "AND reservation_id != ? AND NOT (check_out <= ? OR check_in >= ?)"
        );
        ps.setInt(1, roomId);
        ps.setInt(2, excludeReservationId);
        ps.setString(3, checkIn.toString());
        ps.setString(4, checkOut.toString());
        ResultSet rs = ps.executeQuery();
        boolean conflict = rs.next() && rs.getInt(1) > 0;
        rs.close();
        ps.close();
        return conflict;
    }

    public double getTotalRevenue() throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT COALESCE(SUM(rm.price_per_night * " +
            "(CAST(julianday(r.check_out) - julianday(r.check_in) AS INTEGER))), 0) " +
            "FROM Reservations r JOIN Rooms rm ON r.room_id = rm.room_id " +
            "WHERE r.reservation_status = 'Confirmed'"
        );
        double revenue = rs.next() ? rs.getDouble(1) : 0;
        rs.close();
        stmt.close();
        return revenue;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setRoomId(rs.getInt("room_id"));
        r.setCheckInDate(LocalDate.parse(rs.getString("check_in")));
        r.setCheckOutDate(LocalDate.parse(rs.getString("check_out")));
        r.setReservationStatus(rs.getString("reservation_status"));
        r.setPaymentStatus(rs.getString("payment_status"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setRoomNumber(rs.getInt("room_number"));
        r.setPricePerNight(rs.getDouble("price_per_night"));
        return r;
    }
}
