package com.hotel.hotel_ai.controller;

// GRASP: Indirection - decouples UI from any specific AI provider
// GRASP: Protected Variation - swapping Botpress for another AI doesn't affect callers
public interface IAIService {

    String sendMessageToAI(String message);

    String analyzeText(String message);

    boolean isBotpressConnected();
}
