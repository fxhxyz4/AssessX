package com.assessx.assessx.api;

import com.assessx.assessx.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class ApiClient {
    private static final ApiClient INSTANCE = new ApiClient();
    public static ApiClient get() { return INSTANCE; }

    private final String     baseUrl;
    private final HttpClient http;
    private final Gson       gson;

    private ApiClient() {
        this.baseUrl = System.getProperty("api.base", "http://localhost:8080");
        this.http    = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();
    }

    public void fetchMe() throws ApiException {
        JsonObject body = getJson("/auth/me");
        SessionManager s = SessionManager.get();
        s.setUserId(body.get("id").getAsLong());

        s.setGithubLogin(body.get("githubLogin").getAsString());
        s.setName(body.get("name").getAsString());
        s.setRole(body.get("role").getAsString());
    }

    public JsonObject completeRegistration(Map<String, Object> payload) throws ApiException {
        return postJson("/auth/complete-registration", payload);
    }

    // Tests
    public List<JsonObject> getTests() throws ApiException {
        return getJsonList("/api/tests");
    }

    public JsonObject getTest(long id) throws ApiException {
        return getJson("/api/tests/" + id);
    }

    public JsonObject createTest(JsonObject payload) throws ApiException {
        return postJsonRaw("/api/tests", payload.toString());
    }

    public JsonObject submitTest(long id, JsonObject payload) throws ApiException {
        return postJsonRaw("/api/tests/" + id + "/submit", payload.toString());
    }

    public void deleteTest(long id) throws ApiException {
        delete("/api/tests/" + id);
    }

    // Assignments
    public List<JsonObject> getMyAssignments() throws ApiException {
        return getJsonList("/api/assignments/my");
    }

    public List<JsonObject> getAllAssignments() throws ApiException {
        return getJsonList("/api/assignments");
    }

    public JsonObject createAssignment(Map<String, Object> payload) throws ApiException {
        return postJson("/api/assignments", payload);
    }

    public void deleteAssignment(long id) throws ApiException {
        delete("/api/assignments/" + id);
    }

    // Groups
    public List<JsonObject> getGroups() throws ApiException {
        return getJsonList("/api/groups");
    }

    public JsonObject createGroup(Map<String, Object> payload) throws ApiException {
        return postJson("/api/groups", payload);
    }

    public List<JsonObject> getGroupStudents(long groupId) throws ApiException {
        return getJsonList("/api/groups/" + groupId + "/students");
    }

    public JsonObject addStudentToGroup(long groupId, long userId) throws ApiException {
        return postJsonRaw("/api/groups/" + groupId + "/students?userId=" + userId, "");
    }

    public void removeStudentFromGroup(long groupId, long userId) throws ApiException {
        delete("/api/groups/" + groupId + "/students/" + userId);
    }

    // Code Practices
    public List<JsonObject> getPractices() throws ApiException {
        return getJsonList("/api/practices");
    }

    public JsonObject getPractice(long id) throws ApiException {
        return getJson("/api/practices/" + id);
    }

    public JsonObject createPractice(Map<String, Object> payload) throws ApiException {
        return postJson("/api/practices", payload);
    }

    public JsonObject submitPractice(long id, Map<String, Object> payload) throws ApiException {
        return postJson("/api/practices/" + id + "/submit", payload);
    }

    public void deletePractice(long id) throws ApiException {
        delete("/api/practices/" + id);
    }

    // Results
    public List<JsonObject> getMyResults() throws ApiException {
        return getJsonList("/api/results/my");
    }

    public List<JsonObject> getGroupResults(long groupId) throws ApiException {
        return getJsonList("/api/results/group/" + groupId);
    }

    public JsonObject getResult(long id) throws ApiException {
        return getJson("/api/results/" + id);
    }

    // Users
    public List<JsonObject> getAllUsers() throws ApiException {
        return getJsonList("/api/users");
    }

    public JsonObject getJson(String path) throws ApiException {
        HttpRequest req = authRequest(path).GET().build();
        return JsonParser.parseString(execute(req)).getAsJsonObject();
    }

    public List<JsonObject> getJsonList(String path) throws ApiException {
        HttpRequest req = authRequest(path).GET().build();
        String body = execute(req);
        Type listType = new TypeToken<List<JsonElement>>() {}.getType();

        List<JsonElement> elements = gson.fromJson(body, listType);
        return elements.stream().map(JsonElement::getAsJsonObject).toList();
    }

    public JsonObject postJson(String path, Object payload) throws ApiException {
        return postJsonRaw(path, gson.toJson(payload));
    }

    public JsonObject postJsonRaw(String path, String json) throws ApiException {
        HttpRequest req = authRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .header("Content-Type", "application/json; charset=utf-8")
            .build();

        String body = execute(req);
        if (body == null || body.isBlank()) return new JsonObject();

        return JsonParser.parseString(body).getAsJsonObject();
    }

    public void delete(String path) throws ApiException {
        HttpRequest req = authRequest(path).DELETE().build();
        execute(req);
    }

    private HttpRequest.Builder authRequest(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(60))
            .header("Accept", "application/json");
        String token = SessionManager.get().getToken();

        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }

        return b;
    }

    private String execute(HttpRequest req) throws ApiException {
        try {
            HttpResponse<String> resp = http.send(
                req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (resp.statusCode() >= 400) {
                throw new ApiException(resp.statusCode(), resp.body());
            }

            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new ApiException(0, e.getMessage());
        }
    }

    public String oauthGitHubUrl() {
        return baseUrl + "/auth/github";
    }
}
