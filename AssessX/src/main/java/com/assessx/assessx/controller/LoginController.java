package com.assessx.assessx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML
    private Label loginTitle;

    @FXML
    protected void onGithubLogin() {
        System.out.println("GitHub login");
    }

    @FXML
    protected void onGoogleLogin() {
        System.out.println("Google login");
    }

    @FXML
    protected void onRegisterClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sign_up.fxml"));
            Scene scene = new Scene(loader.load());
           
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/sign_up.css").toExternalForm());

            Stage stage = (Stage) loginTitle.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
