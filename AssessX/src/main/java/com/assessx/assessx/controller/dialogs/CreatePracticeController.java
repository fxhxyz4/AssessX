package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePracticeController extends BasePage {

    @FXML private TextField titleField;
    @FXML private TextArea  descArea;
    @FXML private TextField pointsField;
    @FXML private TextField timeLimitField;
    @FXML private TextArea  unitTestsArea;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    @FXML
    protected void onCreate() {
        hideError(errorLabel);

        String title     = titleField.getText().trim();
        String desc      = descArea.getText().trim();

        String pointsStr = pointsField.getText().trim();
        String timeStr   = timeLimitField.getText().trim();
        String testsText = unitTestsArea.getText().trim();

        if (title.isBlank())  {
            showError(errorLabel, "Введіть назву");
            return;
        }

        if (desc.isBlank())   {
            showError(errorLabel, "Введіть опис");
            return;
        }

        if (testsText.isBlank()) {
            showError(errorLabel, "Додайте хоча б один unit-тест");
            return;
        }

        int points, timeLimit;

        try {
            points    = Integer.parseInt(pointsStr);
            timeLimit = Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            showError(errorLabel, "Балів і час мають бути числами");
            return;
        }

        List<Map<String, String>> unitTests = new ArrayList<>();
        for (String line : testsText.split("\n")) {
            String trimmed = line.trim();

            if (!trimmed.isBlank()) {
                Map<String, String> t = new HashMap<>();
                t.put("testCode", trimmed);

                unitTests.add(t);
            }
        }

        setSpinner(spinner, true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", desc);

        payload.put("points", points);
        payload.put("timeLimitSec", timeLimit);
        payload.put("unitTests", unitTests);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createPractice(payload);
                Platform.runLater(() -> ((Stage) titleField.getScene().getWindow()).close());
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    @FXML
    protected void onCancel() {
        ((Stage) titleField.getScene().getWindow()).close();
    }
}
