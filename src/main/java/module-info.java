module com.hotel.hotel_ai {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires com.google.gson;
    requires jdk.httpserver;
    requires java.desktop;

    opens com.hotel.hotel_ai to javafx.fxml;
    opens com.hotel.hotel_ai.model to com.google.gson;
    exports com.hotel.hotel_ai;
    exports com.hotel.hotel_ai.model;
    exports com.hotel.hotel_ai.repository;
    exports com.hotel.hotel_ai.controller;
    exports com.hotel.hotel_ai.ui;
}
