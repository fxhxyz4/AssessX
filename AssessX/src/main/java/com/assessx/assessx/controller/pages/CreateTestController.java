package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.ui.CodeMirrorEditor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreateTestController {

    @FXML private TextField titleField;
    @FXML private VBox      questionsContainer;
    @FXML private VBox      answersContainer;
    @FXML private TextField pointsField;
    @FXML private TextField timeField;
    @FXML private Label     errorLabel;
    @FXML private ProgressIndicator spinner;

    private CodeMirrorEditor questionsEditor;
    private CodeMirrorEditor answersEditor;

    @FXML
    public void initialize() {
        questionsEditor = new CodeMirrorEditor();
        questionsEditor.setPrefHeight(180);
        questionsContainer.getChildren().add(questionsEditor);

        answersEditor = new CodeMirrorEditor();
        answersEditor.setPrefHeight(120);
        answersContainer.getChildren().add(answersEditor);
    }

    @FXML
    private void onCreate() {
        String title     = titleField.getText().trim();
        String questions = questionsEditor.getValue().trim();

        String answers   = answersEditor.getValue().trim();
        String ptsStr    = pointsField.getText().trim();
        String timeStr   = timeField.getText().trim();

        if (title.isBlank() || questions.isBlank() || answers.isBlank()
            || ptsStr.isBlank() || timeStr.isBlank()) {
            showError("Заповніть всі поля");
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

        try { JsonParser.parseString(questions); }
        catch (Exception e) { showError("Питання: невалідний JSON — " + e.getMessage()); return; }

        try { JsonParser.parseString(answers); }
        catch (Exception e) { showError("Відповіді: невалідний JSON — " + e.getMessage()); return; }

        errorLabel.setVisible(false);
        spinner.setVisible(true);
        spinner.setManaged(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("title", title);
        payload.add("questions", JsonParser.parseString(questions));
        payload.add("answers",   JsonParser.parseString(answers));
        payload.addProperty("points",       pts);
        payload.addProperty("timeLimitSec", time);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createTest(payload);
                Platform.runLater(() -> ((Stage) titleField.getScene().getWindow()).close());
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
