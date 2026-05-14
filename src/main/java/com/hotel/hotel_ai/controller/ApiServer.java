package com.hotel.hotel_ai.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hotel.hotel_ai.model.Room;
import com.hotel.hotel_ai.model.Reservation;
import com.hotel.hotel_ai.model.Customer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiServer {

    private static final int PORT = 8080;
    private HttpServer server;
    private final Gson gson;
    private final RoomController roomController;
    private final ReservationController reservationController;
    private final CustomerController customerController;

    public ApiServer() {
        this.gson = new Gson();
        this.roomController = new RoomController();
        this.reservationController = new ReservationController();
        this.customerController = new CustomerController();
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/rooms", this::handleRooms);
            server.createContext("/api/customers", this::handleCustomers);
            server.createContext("/api/reservations", this::handleReservations);
            server.createContext("/api/dashboard", this::handleDashboard);
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            System.out.println("INNsight API running on http://localhost:" + PORT);
        } catch (IOException e) {
            System.err.println("API server failed: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        ex.getResponseHeaders().add("Content-Type", "application/json");
    }

    private void sendResponse(HttpExchange ex, int code, Object data) throws IOException {
        addCorsHeaders(ex);
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void sendError(HttpExchange ex, int code, String message) throws IOException {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        err.put("success", "false");
        sendResponse(ex, code, err);
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String safeString(JsonObject json, String key) {
        try {
            if (json.has(key) && !json.get(key).isJsonNull()) {
                return json.get(key).getAsString().trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private int safeInt(JsonObject json, String key) {
        try {
            if (json.has(key) && !json.get(key).isJsonNull()) {
                JsonElement el = json.get(key);
                if (el.isJsonPrimitive()) {
                    String val = el.getAsString().trim();
                    if (!val.isEmpty()) return Integer.parseInt(val);
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void handleRooms(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(method)) { addCorsHeaders(ex); ex.sendResponseHeaders(204, -1); return; }
        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (path.endsWith("/available")) {
                    sendResponse(ex, 200, roomController.getAvailableRooms());
                } else {
                    sendResponse(ex, 200, roomController.getAllRooms());
                }
            } else {
                sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleCustomers(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) { addCorsHeaders(ex); ex.sendResponseHeaders(204, -1); return; }
        try {
            if ("GET".equalsIgnoreCase(method)) {
                sendResponse(ex, 200, customerController.getAllCustomers());
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String fullName = safeString(json, "fullName");
                String email = safeString(json, "email");
                String phone = safeString(json, "phone");
                if (fullName.isEmpty() || email.isEmpty()) {
                    sendError(ex, 400, "fullName and email are required. Received: fullName=" + fullName + " email=" + email);
                    return;
                }
                Customer customer = new Customer(fullName, email, phone.isEmpty() ? "0300-0000000" : phone);
                String error = customerController.addCustomer(customer);
                if (error != null) {
                    sendError(ex, 400, error);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Customer " + fullName + " added successfully");
                    result.put("customerId", customer.getCustomerId());
                    sendResponse(ex, 201, result);
                }
            } else {
                sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleReservations(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(method)) { addCorsHeaders(ex); ex.sendResponseHeaders(204, -1); return; }
        try {
            if ("GET".equalsIgnoreCase(method)) {
                sendResponse(ex, 200, reservationController.getAllReservations());
                return;
            }
            if (!"POST".equalsIgnoreCase(method)) {
                sendError(ex, 405, "Method not allowed");
                return;
            }

            String body = readBody(ex);
            System.out.println("Reservation request body: " + body);

            if (body == null || body.trim().isEmpty()) {
                sendError(ex, 400, "Request body is empty");
                return;
            }

            JsonObject json;
            try {
                json = JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                sendError(ex, 400, "Invalid JSON: " + e.getMessage());
                return;
            }

            // CANCEL
            if (path.endsWith("/cancel")) {
                int resId = safeInt(json, "reservationId");
                if (resId == 0) {
                    sendError(ex, 400, "reservationId is required and must be a number");
                    return;
                }
                String error = reservationController.cancelReservation(resId);
                if (error != null) {
                    sendError(ex, 400, error);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Reservation #" + resId + " cancelled successfully.");
                    sendResponse(ex, 200, result);
                }
                return;
            }

            // MODIFY
            if (path.endsWith("/modify")) {
                int resId = safeInt(json, "reservationId");
                String checkInStr = safeString(json, "checkIn");
                String checkOutStr = safeString(json, "checkOut");
                if (resId == 0 || checkInStr.isEmpty() || checkOutStr.isEmpty()) {
                    sendError(ex, 400, "reservationId, checkIn, checkOut are required. Got: resId=" + resId + " checkIn=" + checkInStr + " checkOut=" + checkOutStr);
                    return;
                }
                Reservation res = reservationController.getReservationById(resId);
                if (res == null) {
                    sendError(ex, 404, "Reservation #" + resId + " not found");
                    return;
                }
                try {
                    res.setCheckInDate(LocalDate.parse(checkInStr));
                    res.setCheckOutDate(LocalDate.parse(checkOutStr));
                } catch (Exception e) {
                    sendError(ex, 400, "Invalid date format. Use YYYY-MM-DD. Got: checkIn=" + checkInStr + " checkOut=" + checkOutStr);
                    return;
                }
                String error = reservationController.modifyReservation(res);
                if (error != null) {
                    sendError(ex, 400, error);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Reservation #" + resId + " updated to check-in " + checkInStr + " check-out " + checkOutStr);
                    sendResponse(ex, 200, result);
                }
                return;
            }

            // BOOK NEW RESERVATION
            String guestName = safeString(json, "guestName");
            String email = safeString(json, "email");
            String phone = safeString(json, "phone");
            int roomId = safeInt(json, "roomId");
            String checkInStr = safeString(json, "checkIn");
            String checkOutStr = safeString(json, "checkOut");
            String paymentStatus = safeString(json, "paymentStatus");

            System.out.println("Booking: name=" + guestName + " email=" + email + " roomId=" + roomId + " checkIn=" + checkInStr + " checkOut=" + checkOutStr);

            if (guestName.isEmpty() || email.isEmpty() || roomId == 0 || checkInStr.isEmpty() || checkOutStr.isEmpty()) {
                sendError(ex, 400, "Missing fields. Received: guestName='" + guestName + "' email='" + email + "' roomId=" + roomId + " checkIn='" + checkInStr + "' checkOut='" + checkOutStr + "'");
                return;
            }

            LocalDate checkIn, checkOut;
            try {
                checkIn = LocalDate.parse(checkInStr);
                checkOut = LocalDate.parse(checkOutStr);
            } catch (Exception e) {
                sendError(ex, 400, "Invalid date format. Use YYYY-MM-DD. Got: checkIn='" + checkInStr + "' checkOut='" + checkOutStr + "'");
                return;
            }

            if (!checkOut.isAfter(checkIn)) {
                sendError(ex, 400, "Check-out date must be after check-in date");
                return;
            }

            String error = reservationController.bookRoomWithCustomer(
                guestName, email,
                phone.isEmpty() ? "0300-0000000" : phone,
                roomId, checkIn, checkOut,
                paymentStatus.isEmpty() ? "Unpaid" : paymentStatus
            );

            if (error != null) {
                sendError(ex, 400, error);
            } else {
                Room room = roomController.getRoomById(roomId);
                long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                double total = room != null ? room.getPricePerNight() * nights : 0;
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Room booked for " + guestName);
                result.put("guestName", guestName);
                result.put("roomNumber", room != null ? room.getRoomNumber() : roomId);
                result.put("roomType", room != null ? room.getRoomType() : "Unknown");
                result.put("checkIn", checkInStr);
                result.put("checkOut", checkOutStr);
                result.put("nights", nights);
                result.put("totalCost", String.format("$%.2f", total));
                result.put("paymentStatus", paymentStatus.isEmpty() ? "Unpaid" : paymentStatus);
                try {
                    List<Reservation> allRes = reservationController.getAllReservations();
                    int newId = allRes.stream().mapToInt(r -> r.getReservationId()).max().orElse(0);
                    result.put("reservationId", newId);
                } catch (Exception ignored) {}
                sendResponse(ex, 201, result);
            }

        } catch (Exception e) {
            System.err.println("Reservation error: " + e.getMessage());
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleDashboard(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { addCorsHeaders(ex); ex.sendResponseHeaders(204, -1); return; }
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRooms", roomController.getTotalRoomCount());
            stats.put("availableRooms", roomController.getAvailableRoomCount());
            stats.put("activeReservations", reservationController.getActiveReservationCount());
            stats.put("totalCustomers", customerController.getCustomerCount());
            stats.put("totalRevenue", String.format("$%.2f", reservationController.getTotalRevenue()));
            sendResponse(ex, 200, stats);
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}
