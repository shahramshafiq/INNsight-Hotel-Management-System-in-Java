package com.hotel.hotel_ai.controller;

import com.hotel.hotel_ai.model.Room;
import com.hotel.hotel_ai.repository.IRoomRepository;
import com.hotel.hotel_ai.repository.RoomRepository;
import com.hotel.hotel_ai.repository.IReservationRepository;
import com.hotel.hotel_ai.repository.ReservationRepository;
import java.sql.SQLException;
import java.util.List;

// GRASP: Controller - handles system events for room-related use cases
// Use Cases: Manage Room Inventory (UC6)
public class RoomController {

    private final IRoomRepository roomRepository;
    private final IReservationRepository reservationRepository;

    // GRASP: Low Coupling - depends on repositories, not on UI
    public RoomController() {
        this.roomRepository = new RoomRepository();
        this.reservationRepository = new ReservationRepository();
    }

    // UC6: Get all rooms for display
    public List<Room> getAllRooms() throws SQLException {
        return roomRepository.findAll();
    }

    // UC6: Get available rooms only
    public List<Room> getAvailableRooms() throws SQLException {
        return roomRepository.findAvailable();
    }

    // UC6: Add a new room to inventory
    public String addRoom(Room room) {
        try {
            // Information Expert: Room validates itself
            String error = room.getValidationError();
            if (error != null) {
                return error;
            }

            // Check if room number already exists
            Room existing = roomRepository.findByRoomNumber(room.getRoomNumber());
            if (existing != null) {
                return "Room number " + room.getRoomNumber() + " already exists";
            }

            roomRepository.save(room);
            return null; // null means success
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC6: Update room details
    public String updateRoom(Room room) {
        try {
            String error = room.getValidationError();
            if (error != null) {
                return error;
            }

            // Check if another room has the same number
            Room existing = roomRepository.findByRoomNumber(room.getRoomNumber());
            if (existing != null && existing.getRoomId() != room.getRoomId()) {
                return "Room number " + room.getRoomNumber() + " is already used by another room";
            }

            roomRepository.update(room);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC6: Remove room from inventory (prevents if booked)
    public String deleteRoom(int roomId) {
        try {
            // Prevent deletion if room has active reservations
            var activeReservations = reservationRepository.findActiveByRoomId(roomId);
            if (!activeReservations.isEmpty()) {
                return "Cannot delete room - it has " + activeReservations.size() + " active reservation(s)";
            }

            roomRepository.delete(roomId);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    public Room getRoomById(int roomId) throws SQLException {
        return roomRepository.findById(roomId);
    }

    public int getTotalRoomCount() throws SQLException {
        return roomRepository.count();
    }

    public int getAvailableRoomCount() throws SQLException {
        return roomRepository.countAvailable();
    }
}
