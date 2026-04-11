package com.assessx.assessx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        loadEnv();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/fxml/login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainApplication.class.getResource("/styles/login.css").toExternalForm());

        stage.setTitle("AssessX");
        stage.setMinWidth(1200);
        stage.setMinHeight(1200);

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private void loadEnv() {
        File envFile = new File(".env");

        if (!envFile.exists()) {
            envFile = new File("../.env");
        }

        if (!envFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq < 1) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'")  && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[AssessX] Could not read .env: " + e.getMessage());
        }

        if (System.getProperty("api.base") == null) {
            String url = System.getProperty("API_URL", "localhost");

            String port = System.getProperty("API_PORT",
                System.getProperty("PORT", "8080"));

            System.setProperty("api.base", url + ":" + port);
        }
    }
}
