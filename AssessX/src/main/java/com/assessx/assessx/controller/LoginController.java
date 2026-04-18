package com.assessx.assessx.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private Label loginTitle;

    @FXML
    protected void onGithubLogin() {
        navigateTo("/fxml/github_auth.fxml");
    }

    @FXML
    protected void onGoogleLogin() {
        loginTitle.setText("Google login ще не підтримується");
    }

    @FXML
    protected void onRegisterClick() {
        navigateTo("/fxml/sign_up.fxml", "/styles/sign_up.css");
    }

    private void navigateTo(String fxmlPath, String... extraCss) {
        try {
            Stage stage = (Stage) loginTitle.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            for (String css : extraCss) {
                scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
            }

            stage.setScene(scene);

            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
