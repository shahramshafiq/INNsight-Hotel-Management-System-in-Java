package com.hotel.hotel_ai.controller;

import com.hotel.hotel_ai.model.Customer;
import com.hotel.hotel_ai.repository.ICustomerRepository;
import com.hotel.hotel_ai.repository.CustomerRepository;
import com.hotel.hotel_ai.repository.IReservationRepository;
import com.hotel.hotel_ai.repository.ReservationRepository;
import java.sql.SQLException;
import java.util.List;

// GRASP: Controller - handles customer-related use cases
// Use Cases: Provide Booking Details (UC9)
public class CustomerController {

    private final ICustomerRepository customerRepository;
    private final IReservationRepository reservationRepository;

    public CustomerController() {
        this.customerRepository = new CustomerRepository();
        this.reservationRepository = new ReservationRepository();
    }

    // UC9: Register or update customer when booking details are provided
    public String addCustomer(Customer customer) {
        try {
            String error = customer.getValidationError();
            if (error != null) {
                return error;
            }

            // Check for duplicate email
            Customer existing = customerRepository.findByEmail(customer.getEmail());
            if (existing != null) {
                return "A customer with email " + customer.getEmail() + " already exists";
            }

            customerRepository.save(customer);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    public String updateCustomer(Customer customer) {
        try {
            String error = customer.getValidationError();
            if (error != null) {
                return error;
            }
            customerRepository.update(customer);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    public String deleteCustomer(int customerId) {
        try {
            // Prevent deletion if customer has active reservations
            var reservations = reservationRepository.findByCustomerId(customerId);
            boolean hasActive = reservations.stream()
                .anyMatch(r -> "Confirmed".equalsIgnoreCase(r.getReservationStatus()));
            if (hasActive) {
                return "Cannot delete customer with active reservations";
            }
            customerRepository.delete(customerId);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(int id) throws SQLException {
        return customerRepository.findById(id);
    }

    public int getCustomerCount() throws SQLException {
        return customerRepository.count();
    }
}
