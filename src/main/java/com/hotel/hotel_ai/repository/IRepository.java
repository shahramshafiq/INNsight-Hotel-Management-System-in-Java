package com.hotel.hotel_ai.repository;

import java.sql.SQLException;
import java.util.List;

// GRASP: Protected Variation - stable interface shields callers from DB implementation changes
// GRASP: Polymorphism - any concrete repo can be swapped behind this contract
public interface IRepository<T> {

    T save(T entity) throws SQLException;

    List<T> findAll() throws SQLException;

    T findById(int id) throws SQLException;

    void update(T entity) throws SQLException;

    void delete(int id) throws SQLException;
}
