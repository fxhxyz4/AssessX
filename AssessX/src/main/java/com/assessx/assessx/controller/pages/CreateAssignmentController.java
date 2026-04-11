package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAssignmentController {

    @FXML private ComboBox<IdNameItem> groupCombo;
    @FXML private ComboBox<IdNameItem> testCombo;
    @FXML private ComboBox<IdNameItem> practiceCombo;
    @FXML private RadioButton testRadio;
    @FXML private RadioButton practiceRadio;
    @FXML private VBox testBox;
    @FXML private VBox practiceBox;
    @FXML private TextField deadlineField;
    @FXML private Label     errorLabel;
    @FXML private ProgressIndicator spinner;

    @FXML
    public void initialize() {
        testRadio.selectedProperty().addListener((obs, old, sel) -> {
            testBox.setVisible(sel);
            testBox.setManaged(sel);
            practiceBox.setVisible(!sel);
            practiceBox.setManaged(!sel);
        });
        testRadio.setSelected(true);
        loadCombos();
    }

    private void loadCombos() {
        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> groups    = ApiClient.get().getGroups();
                List<JsonObject> tests     = ApiClient.get().getTests();
                List<JsonObject> practices = ApiClient.get().getPractices();

                Platform.runLater(() -> {
                    groups.forEach(g -> groupCombo.getItems().add(
                            new IdNameItem(g.get("id").getAsLong(), g.get("name").getAsString())));
                    tests.forEach(t -> testCombo.getItems().add(
                            new IdNameItem(t.get("id").getAsLong(), t.get("title").getAsString())));
                    practices.forEach(p -> practiceCombo.getItems().add(
                            new IdNameItem(p.get("id").getAsLong(), p.get("title").getAsString())));
                });
            } catch (ApiException ignored) {}
        });
    }

    @FXML
    private void onCreate() {
        if (groupCombo.getValue() == null) { showError("Виберіть групу"); return; }

        boolean isTest = testRadio.isSelected();

        if (isTest && testCombo.getValue() == null) { showError("Виберіть тест"); return; }
        if (!isTest && practiceCombo.getValue() == null) { showError("Виберіть практику"); return; }

        errorLabel.setVisible(false);
        spinner.setVisible(true);

        spinner.setManaged(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", groupCombo.getValue().id());

        if (isTest)  payload.put("testId",     testCombo.getValue().id());
        else         payload.put("practiceId", practiceCombo.getValue().id());

        String dl = deadlineField.getText().trim();
        if (!dl.isBlank()) payload.put("deadline", dl);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createAssignment(payload);
                Platform.runLater(() -> ((Stage) groupCombo.getScene().getWindow()).close());
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

    public record IdNameItem(long id, String name) {
        @Override public String toString() { return name; }
    }
}
