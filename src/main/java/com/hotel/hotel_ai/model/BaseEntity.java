package com.hotel.hotel_ai.model;

// GRASP: Polymorphism - base class enforces a common contract on all domain models
// All entities must know how to validate themselves and expose their primary key
public abstract class BaseEntity {

    // protected so subclasses can read/write directly
    protected int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Subclasses implement their own validation logic
    public abstract boolean isValid();

    public abstract String getValidationError();
}
