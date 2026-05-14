package com.hotel.hotel_ai.model;

// GRASP: Information Expert - Customer knows its own data and validates itself
// Polymorphism - extends BaseEntity, fulfilling the shared entity contract
public class Customer extends BaseEntity {

    private String fullName;
    private String email;
    private String phone;

    public Customer() {}

    public Customer(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public Customer(int customerId, String fullName, String email, String phone) {
        this.id = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    // Information Expert: Customer validates its own data
    @Override
    public boolean isValid() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        if (email == null || !email.contains("@")) {
            return false;
        }
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String getValidationError() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name is required";
        }
        if (email == null || !email.contains("@")) {
            return "Valid email is required";
        }
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number is required";
        }
        return null;
    }

    // delegates to inherited id field
    public int getCustomerId() { return id; }
    public void setCustomerId(int customerId) { this.id = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}
