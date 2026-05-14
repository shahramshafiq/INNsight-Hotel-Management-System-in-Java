package com.hotel.hotel_ai.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// GRASP: Creator - Reservation is assembled by the system when a booking is confirmed
// GRASP: Information Expert - Reservation calculates total nights and cost from its own data
// Polymorphism - extends BaseEntity, fulfilling the shared entity contract
public class Reservation extends BaseEntity {

    private int customerId;
    private int roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String reservationStatus; // "Confirmed", "Cancelled", "Pending"
    private String paymentStatus;     // "Paid", "Unpaid", "Refunded"

    // transient fields for display (not stored directly in DB)
    private String customerName;
    private int roomNumber;
    private double pricePerNight;

    public Reservation() {}

    public Reservation(int customerId, int roomId, LocalDate checkInDate, LocalDate checkOutDate,
                       String reservationStatus, String paymentStatus) {
        this.customerId = customerId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.reservationStatus = reservationStatus;
        this.paymentStatus = paymentStatus;
    }

    // Information Expert: Reservation calculates its own total nights
    public int calculateTotalNights() {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return (int) Math.max(nights, 0);
    }

    // Information Expert: Reservation calculates its own total cost
    public double calculateTotalCost() {
        return calculateTotalNights() * pricePerNight;
    }

    // Information Expert: Reservation validates its own booking data
    @Override
    public boolean isValid() {
        if (customerId <= 0) return false;
        if (roomId <= 0) return false;
        if (checkInDate == null || checkOutDate == null) return false;
        if (!checkOutDate.isAfter(checkInDate)) return false;
        return true;
    }

    @Override
    public String getValidationError() {
        if (customerId <= 0) return "Customer must be selected";
        if (roomId <= 0) return "Room must be selected";
        if (checkInDate == null) return "Check-in date is required";
        if (checkOutDate == null) return "Check-out date is required";
        if (!checkOutDate.isAfter(checkInDate)) return "Check-out must be after check-in";
        return null;
    }

    public boolean isActive() {
        return "Confirmed".equalsIgnoreCase(reservationStatus);
    }

    // delegates to inherited id field
    public int getReservationId() { return id; }
    public void setReservationId(int reservationId) { this.id = reservationId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public String getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
}
