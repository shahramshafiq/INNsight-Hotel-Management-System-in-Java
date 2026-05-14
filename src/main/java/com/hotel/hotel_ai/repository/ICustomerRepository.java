package com.hotel.hotel_ai.repository;

import com.hotel.hotel_ai.model.Customer;
import java.sql.SQLException;

// GRASP: Protected Variation - extends IRepository with customer-specific operations
// Controllers depend on this interface, not on the concrete CustomerRepository class
public interface ICustomerRepository extends IRepository<Customer> {

    Customer findByEmail(String email) throws SQLException;

    int count() throws SQLException;
}
