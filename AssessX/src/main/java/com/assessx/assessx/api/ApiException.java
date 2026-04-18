package com.assessx.assessx.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ApiException extends Exception {
    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(parseMessage(message));
        this.statusCode = statusCode;
    }

    private static String parseMessage(String raw) {
        if (raw == null) return "Невідома помилка";
        try {
            JsonObject json = JsonParser.parseString(raw.trim()).getAsJsonObject();
            if (json.has("error")) return json.get("error").getAsString();
            if (json.has("message")) return json.get("message").getAsString();
        } catch (Exception ignored) {}
        return raw;
    }

    public int getStatusCode() { return statusCode; }
    public boolean isUnauthorized() { return statusCode == 401; }
    public boolean isNotFound() { return statusCode == 404; }
}
