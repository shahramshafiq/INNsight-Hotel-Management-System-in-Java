package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Room;
import java.sql.SQLException;
import java.util.List;

// GRASP: Protected Variation - extends IRepository with room-specific operations
// Controllers depend on this interface, not on the concrete RoomRepository class
public interface IRoomRepository extends IRepository<Room> {

    List<Room> findAvailable() throws SQLException;

    Room findByRoomNumber(int roomNumber) throws SQLException;

    void updateStatus(int roomId, String status) throws SQLException;

    int count() throws SQLException;

    int countAvailable() throws SQLException;
}
