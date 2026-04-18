package com.assessx.assessx.controller;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.session.SessionManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;

public class GitHubOAuthController {

    @FXML private WebView  webView;
    @FXML private Label    statusLabel;
    @FXML private ProgressIndicator spinner;

    private boolean tokenReceived = false;

    @FXML
    public void initialize() {
        WebEngine engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                spinner.setVisible(true);
                statusLabel.setText("Завантаження...");
            } else if (newState == Worker.State.SUCCEEDED) {
                spinner.setVisible(false);
                checkForToken(engine);
            } else if (newState == Worker.State.FAILED) {
                spinner.setVisible(false);
                statusLabel.setText("Помилка завантаження. Перевірте з'єднання з сервером.");
            }
        });

        statusLabel.setText("Відкриваємо GitHub...");
        engine.load(ApiClient.get().oauthGitHubUrl());
    }

    private void checkForToken(WebEngine engine) {
        if (tokenReceived) return;

        String url = engine.getLocation();

        if (shouldClose(url)) {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Scene scene = new Scene(loader.load());
                    scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
                    ((Stage) webView.getScene().getWindow()).setScene(scene);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        String pageText = (String) engine.executeScript(
            "document.body ? document.body.innerText : ''"
        );
        if (pageText == null || pageText.isBlank()) return;

        try {
            String trimmed = pageText.trim();
            if (!trimmed.startsWith("{")) return;
            JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
            if (json.has("token")) {
                tokenReceived = true;
                handleTokenReceived(json.get("token").getAsString());
            }
        } catch (Exception ignored) {}
    }

    private boolean shouldClose(String url) {
        if (url == null) return false;
        if (url.contains("localhost")) return false;
        if (url.contains("github.com/login")) return false;
        if (url.contains("github.com/sessions")) return false;
        if (url.contains("github.com/oauth")) return false;
        if (url.contains("github.com/authorize")) return false;
        return url.contains("github.com");
    }

    private void handleTokenReceived(String token) {
        statusLabel.setText("Авторизація успішна! Завантажуємо профіль...");
        spinner.setVisible(true);
        webView.setVisible(false);
        SessionManager.get().setToken(token);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().fetchMe();
                Platform.runLater(this::navigateNext);
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    statusLabel.setText("Помилка отримання профілю: " + e.getMessage());
                });
            }
        });
    }

    private void navigateNext() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/complete_registration.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/sign_up.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            ((Stage) statusLabel.getScene().getWindow()).setScene(scene);
        } catch (IOException e) {
            statusLabel.setText("Помилка навігації: " + e.getMessage());
        }
    }
}
