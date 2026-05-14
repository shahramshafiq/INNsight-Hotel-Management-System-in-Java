package com.hotel.hotel_ai.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.hotel.hotel_ai.model.Room;
import com.hotel.hotel_ai.repository.IRoomRepository;
import com.hotel.hotel_ai.repository.RoomRepository;
import com.hotel.hotel_ai.repository.IReservationRepository;
import com.hotel.hotel_ai.repository.ReservationRepository;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

// GRASP: Indirection - mediates between UI and external Botpress AI service
// GRASP: Protected Variation - implements IAIService so any AI provider can be swapped in
// Use Cases: Initiate Conversation (UC3), Interpret User Intent (UC4),
//            Process Natural Language Input (UC8), Extract Booking Info (UC2),
//            Request Information (UC10)
public class AIEngine implements IAIService {

    private String botpressWebhookUrl;
    private final Gson gson;
    private boolean isConnected;
    private final IRoomRepository roomRepo;
    private final IReservationRepository resRepo;

    public AIEngine() {
        this.gson = new Gson();
        this.botpressWebhookUrl = "";
        this.isConnected = false;
        this.roomRepo = new RoomRepository();
        this.resRepo = new ReservationRepository();
    }

    public void setBotpressWebhookUrl(String url) {
        this.botpressWebhookUrl = url;
        this.isConnected = url != null && !url.trim().isEmpty();
    }

    public String getBotpressWebhookUrl() {
        return botpressWebhookUrl;
    }

    public boolean isBotpressConnected() {
        return isConnected;
    }

    // UC8: Process Natural Language Input -> UC4: Interpret User Intent
    // Sends message to Botpress and gets AI response
    public String sendMessageToAI(String userMessage) {
        if (!isConnected || botpressWebhookUrl.isEmpty()) {
            return getOfflineResponse(userMessage);
        }

        try {
            return callBotpressAPI(userMessage);
        } catch (Exception e) {
            System.err.println("Botpress API error: " + e.getMessage());
            return getOfflineResponse(userMessage);
        }
    }

    // Calls Hotel API endpoints based on intent (matches class diagram)
    public void callHotelAPI() {
        // This method is invoked when the AI determines it needs to query hotel data
        // In practice, the controllers handle the actual DB queries
        // This is the integration point between AI and business logic
    }

    // UC4: Analyze text for intent detection
    public String analyzeText(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "empty";
        }

        String lower = message.toLowerCase().trim();

        if (lower.contains("book") || lower.contains("reserve") || lower.contains("room")) {
            return "booking";
        }
        if (lower.contains("cancel") || lower.contains("modify") || lower.contains("change")) {
            return "modification";
        }
        if (lower.contains("price") || lower.contains("cost") || lower.contains("rate")) {
            return "pricing";
        }
        if (lower.contains("available") || lower.contains("vacancy") || lower.contains("free")) {
            return "availability";
        }
        if (lower.contains("help") || lower.contains("info") || lower.contains("what can")) {
            return "help";
        }
        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey")) {
            return "greeting";
        }

        return "general";
    }

    // Calls Botpress webhook API
    private String callBotpressAPI(String message) throws Exception {
        URL url = new URL(botpressWebhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        // Prepare request body
        JsonObject body = new JsonObject();
        body.addProperty("text", message);
        body.addProperty("userId", "innsight_user");

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse Botpress response
            JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
            if (jsonResponse.has("responses")) {
                JsonArray responses = jsonResponse.getAsJsonArray("responses");
                if (responses.size() > 0) {
                    JsonObject first = responses.get(0).getAsJsonObject();
                    if (first.has("text")) {
                        return first.get("text").getAsString();
                    }
                }
            }
            return response.toString();
        } else {
            return "I'm having trouble connecting right now. Please try again.";
        }
    }

    // Offline responses with live data from database
    private String getOfflineResponse(String message) {
        String intent = analyzeText(message);

        switch (intent) {
            case "greeting":
                return "Welcome to INNsight! I'm your AI receptionist. " +
                       "I can help you with room bookings, check availability, and more. " +
                       "What would you like to do today?";
            case "booking":
                return "I'd love to help you book a room! Please use the Reservations tab " +
                       "to select your preferred room, check-in and check-out dates. " +
                       "You can also ask me about available rooms or prices!";
            case "modification":
                return "To modify or cancel a reservation, go to the Reservations tab, " +
                       "select the booking you'd like to change, then click Modify or Cancel Booking.";
            case "pricing":
                try {
                    List<Room> rooms = roomRepo.findAll();
                    StringBuilder sb = new StringBuilder("Here are our current room rates:\n\n");
                    for (Room r : rooms) {
                        sb.append("Room ").append(r.getRoomNumber())
                          .append(" (").append(r.getRoomType()).append(") - $")
                          .append(String.format("%.0f", r.getPricePerNight()))
                          .append("/night [").append(r.getStatus()).append("]\n");
                    }
                    return sb.toString();
                } catch (Exception e) {
                    return "I couldn't load pricing right now. Check the Rooms tab for details.";
                }
            case "availability":
                try {
                    List<Room> available = roomRepo.findAvailable();
                    if (available.isEmpty()) {
                        return "Sorry, all rooms are currently booked. Please check back later!";
                    }
                    StringBuilder sb = new StringBuilder("We have " + available.size() + " room(s) available:\n\n");
                    for (Room r : available) {
                        sb.append("Room ").append(r.getRoomNumber())
                          .append(" - ").append(r.getRoomType())
                          .append(" (").append(r.getCapacity()).append(" guests)")
                          .append(" - $").append(String.format("%.0f", r.getPricePerNight()))
                          .append("/night\n");
                    }
                    sb.append("\nGo to Reservations tab to book one!");
                    return sb.toString();
                } catch (Exception e) {
                    return "I couldn't check availability. Please visit the Rooms tab.";
                }
            case "help":
                return "Here's what I can help you with:\n\n" +
                       "1. Book a room - type 'book' or 'reserve'\n" +
                       "2. Check availability - type 'available' or 'vacancy'\n" +
                       "3. View prices - type 'price' or 'rate'\n" +
                       "4. Modify/cancel booking - type 'modify' or 'cancel'\n" +
                       "5. General info - just ask!\n\n" +
                       "Try asking: 'What rooms are available?'";
            default:
                return "I understand you need assistance. You can ask me about:\n" +
                       "- Room availability (\"what's available?\")\n" +
                       "- Prices (\"what are the rates?\")\n" +
                       "- Bookings (\"I want to book a room\")\n" +
                       "- Help (\"help\")\n\n" +
                       "Or use the sidebar to navigate directly!";
        }
    }
}