package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Reservation;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

// GRASP: Protected Variation - extends IRepository with reservation-specific operations
// Controllers depend on this interface, not on the concrete ReservationRepository class
public interface IReservationRepository extends IRepository<Reservation> {

    List<Reservation> findByCustomerId(int customerId) throws SQLException;

    List<Reservation> findActiveByRoomId(int roomId) throws SQLException;

    void cancelReservation(int reservationId) throws SQLException;

    boolean hasConflict(int roomId, LocalDate checkIn, LocalDate checkOut, int excludeReservationId) throws SQLException;

    double getTotalRevenue() throws SQLException;

    int count() throws SQLException;

    int countActive() throws SQLException;
}
