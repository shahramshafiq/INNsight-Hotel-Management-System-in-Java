package com.hotel.hotel_ai.controller;

import com.hotel.hotel_ai.model.Customer;
import com.hotel.hotel_ai.model.Reservation;
import com.hotel.hotel_ai.model.Room;
import com.hotel.hotel_ai.repository.ICustomerRepository;
import com.hotel.hotel_ai.repository.CustomerRepository;
import com.hotel.hotel_ai.repository.IReservationRepository;
import com.hotel.hotel_ai.repository.ReservationRepository;
import com.hotel.hotel_ai.repository.IRoomRepository;
import com.hotel.hotel_ai.repository.RoomRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

// GRASP: Controller - coordinates use cases for reservations
// Use Cases: Make Room Reservation (UC5), Modify or Cancel Reservation (UC1),
//            Monitor Booking Records (UC7), Update Reservation Database (UC11),
//            Validate Booking Information (UC12)
public class ReservationController {

    private final IReservationRepository reservationRepository;
    private final IRoomRepository roomRepository;
    private final ICustomerRepository customerRepository;

    public ReservationController() {
        this.reservationRepository = new ReservationRepository();
        this.roomRepository = new RoomRepository();
        this.customerRepository = new CustomerRepository();
    }

    // UC5: Make Room Reservation (books a room for a customer)
    // UC12: Validate Booking Information (validates before saving)
    // UC11: Update Reservation Database (saves to DB)
    public String bookRoom(Reservation reservation) {
        try {
            // UC12: Validate booking information
            String validationError = reservation.getValidationError();
            if (validationError != null) {
                return validationError;
            }

            // Check if room exists and is available
            Room room = roomRepository.findById(reservation.getRoomId());
            if (room == null) {
                return "Selected room does not exist";
            }

            // Check for date conflicts with existing reservations
            boolean hasConflict = reservationRepository.hasConflict(
                reservation.getRoomId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                0 // no reservation to exclude
            );
            if (hasConflict) {
                return "Room is already booked for the selected dates. Please choose different dates.";
            }

            // UC11: Save the reservation
            reservation.setReservationStatus("Confirmed");
            if (reservation.getPaymentStatus() == null) {
                reservation.setPaymentStatus("Unpaid");
            }
            reservationRepository.save(reservation);

            // Update room status to Booked
            roomRepository.updateStatus(reservation.getRoomId(), "Booked");

            return null; // success
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC5 variant: Book with customer info (creates customer if needed)
    public String bookRoomWithCustomer(String guestName, String email, String phone,
                                        int roomId, LocalDate checkIn, LocalDate checkOut,
                                        String paymentStatus) {
        try {
            // Find or create customer
            Customer customer = customerRepository.findByEmail(email);
            if (customer == null) {
                customer = new Customer(guestName, email, phone);
                String custError = customer.getValidationError();
                if (custError != null) {
                    return custError;
                }
                // GRASP: Creator - controller creates customer when it has the data
                customerRepository.save(customer);
            }

            Reservation reservation = new Reservation(
                customer.getCustomerId(), roomId, checkIn, checkOut,
                "Confirmed", paymentStatus
            );

            return bookRoom(reservation);
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC1: Modify reservation
    public String modifyReservation(Reservation reservation) {
        try {
            String validationError = reservation.getValidationError();
            if (validationError != null) {
                return validationError;
            }

            // Check for conflicts excluding this reservation itself
            boolean hasConflict = reservationRepository.hasConflict(
                reservation.getRoomId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getReservationId()
            );
            if (hasConflict) {
                return "Room is already booked for the selected dates";
            }

            reservationRepository.update(reservation);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC1: Cancel reservation
    public String cancelReservation(int reservationId) {
        try {
            Reservation r = reservationRepository.findById(reservationId);
            if (r == null) {
                return "Reservation not found";
            }
            if ("Cancelled".equalsIgnoreCase(r.getReservationStatus())) {
                return "Reservation is already cancelled";
            }

            reservationRepository.cancelReservation(reservationId);

            // Free up the room
            roomRepository.updateStatus(r.getRoomId(), "Available");

            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // UC7: Monitor booking records
    public List<Reservation> getAllReservations() throws SQLException {
        return reservationRepository.findAll();
    }

    public Reservation getReservationById(int id) throws SQLException {
        return reservationRepository.findById(id);
    }

    public List<Reservation> getReservationsByCustomer(int customerId) throws SQLException {
        return reservationRepository.findByCustomerId(customerId);
    }

    public int getActiveReservationCount() throws SQLException {
        return reservationRepository.countActive();
    }

    public int getTotalReservationCount() throws SQLException {
        return reservationRepository.count();
    }

    public double getTotalRevenue() throws SQLException {
        return reservationRepository.getTotalRevenue();
    }
}
