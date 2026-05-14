package com.hotel.hotel_ai.model;

// GRASP: Information Expert - Room knows its own data and availability
// Polymorphism - extends BaseEntity, fulfilling the shared entity contract
public class Room extends BaseEntity {

    private int roomNumber;
    private String roomType;
    private int capacity;
    private double pricePerNight;
    private String status; // "Available", "Booked", "Maintenance"

    public Room() {}
    //static polymorphism
    public Room(int roomNumber, String roomType, int capacity, double pricePerNight, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public Room(int roomId, int roomNumber, String roomType, int capacity, double pricePerNight, String status) {
        this.id = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    // Information Expert: Room knows if it is available
    public boolean isAvailable() {
        return "Available".equalsIgnoreCase(status);
    }

    // Information Expert: Room validates its own data
    @Override
    public boolean isValid() {
        if (roomNumber <= 0) return false;
        if (roomType == null || roomType.trim().isEmpty()) return false;
        if (capacity <= 0) return false;
        if (pricePerNight <= 0) return false;
        if (status == null || status.trim().isEmpty()) return false;
        return true;
    }

    @Override
    public String getValidationError() {
        if (roomNumber <= 0) return "Room number must be positive";
        if (roomType == null || roomType.trim().isEmpty()) return "Room type is required";
        if (capacity <= 0) return "Capacity must be positive";
        if (pricePerNight <= 0) return "Price per night must be positive";
        if (status == null || status.trim().isEmpty()) return "Status is required";
        return null;
    }

    // delegates to inherited id field
    public int getRoomId() { return id; }
    public void setRoomId(int roomId) { this.id = roomId; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + roomType + ") - $" + pricePerNight + "/night";
    }
}
