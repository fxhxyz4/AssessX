package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePracticeController {

    @FXML private TextField titleField;
    @FXML private TextArea  descField;
    @FXML private TextArea  unitTestsField;
    @FXML private TextField pointsField;
    @FXML private TextField timeField;
    @FXML private Label     errorLabel;
    @FXML private ProgressIndicator spinner;

    @FXML
    private void onCreate() {
        String title     = titleField.getText().trim();
        String desc      = descField.getText().trim();

        String unitTests = unitTestsField.getText().trim();
        String ptsStr    = pointsField.getText().trim();
        String timeStr   = timeField.getText().trim();

        if (title.isBlank() || desc.isBlank() || ptsStr.isBlank() || timeStr.isBlank()) {
            showError("Заповніть всі обов'язкові поля");
            return;
        }

        int pts, time;
        try {
            pts  = Integer.parseInt(ptsStr);
            time = Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            showError("Бали та час мають бути числами");
            return;
        }

        List<String> tests = unitTests.isBlank()
                ? List.of()
                : Arrays.stream(unitTests.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();

        errorLabel.setVisible(false);
        spinner.setVisible(true);
        spinner.setManaged(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("title",        title);

        payload.put("description",  desc);
        payload.put("unitTests",    tests);

        payload.put("points",       pts);
        payload.put("timeLimitSec", time);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createPractice(payload);
                Platform.runLater(() -> {
                    Stage stage = (Stage) titleField.getScene().getWindow();
                    stage.close();
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    showError("Помилка: " + e.getMessage());
                });
            }
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
