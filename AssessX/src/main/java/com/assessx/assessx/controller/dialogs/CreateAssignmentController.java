package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAssignmentController extends BasePage {

    @FXML private ComboBox<GroupItem> groupCombo;
    @FXML private RadioButton         testRadio;
    @FXML private RadioButton         practiceRadio;
    @FXML private ComboBox<ItemEntry> testCombo;
    @FXML private ComboBox<ItemEntry> practiceCombo;
    @FXML private VBox                testBox;
    @FXML private VBox                practiceBox;
    @FXML private TextField           deadlineField;
    @FXML private ProgressIndicator   spinner;
    @FXML private Label               errorLabel;

    private List<JsonObject> tests     = new ArrayList<>();
    private List<JsonObject> practices = new ArrayList<>();

    @FXML
    public void initialize() {
        testRadio.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) {
                testBox.setVisible(true);
                testBox.setManaged(true);

                practiceBox.setVisible(false);
                practiceBox.setManaged(false);
            }
        });

        practiceRadio.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) {
                practiceBox.setVisible(true);
                practiceBox.setManaged(true);

                testBox.setVisible(false);
                testBox.setManaged(false);
            }
        });

        testRadio.setSelected(true);
        loadData();
    }

    private void loadData() {
        setSpinner(spinner, true);

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> groups = ApiClient.get().getGroups();
                tests     = ApiClient.get().getTests();
                practices = ApiClient.get().getPractices();

                Platform.runLater(() -> {
                    setSpinner(spinner, false);

                    groupCombo.getItems().clear();
                    for (JsonObject g : groups) {
                        groupCombo.getItems().add(new GroupItem(lng(g, "id"), str(g, "name")));
                    }
                    if (!groupCombo.getItems().isEmpty()) {
                        groupCombo.getSelectionModel().selectFirst();
                    }

                    // Tests
                    testCombo.getItems().clear();
                    for (JsonObject t : tests) {
                        testCombo.getItems().add(new ItemEntry(lng(t, "id"), str(t, "title")));
                    }
                    if (!testCombo.getItems().isEmpty()) {
                        testCombo.getSelectionModel().selectFirst();
                    }

                    // Practices
                    practiceCombo.getItems().clear();
                    for (JsonObject p : practices) {
                        practiceCombo.getItems().add(new ItemEntry(lng(p, "id"), str(p, "title")));
                    }
                    if (!practiceCombo.getItems().isEmpty()) {
                        practiceCombo.getSelectionModel().selectFirst();
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    @FXML
    protected void onCreate() {
        hideError(errorLabel);

        GroupItem group = groupCombo.getValue();
        if (group == null) { showError(errorLabel, "Виберіть групу"); return; }

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", group.id());

        if (testRadio.isSelected()) {
            ItemEntry item = testCombo.getValue();
            if (item == null) { showError(errorLabel, "Виберіть тест"); return; }

            payload.put("testId", item.id());
        } else {
            ItemEntry item = practiceCombo.getValue();
            if (item == null) { showError(errorLabel, "Виберіть практику"); return; }

            payload.put("practiceId", item.id());
        }

        String deadline = deadlineField.getText().trim();
        if (!deadline.isBlank()) {
            payload.put("deadline", deadline);
        }

        setSpinner(spinner, true);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createAssignment(payload);
                Platform.runLater(() -> ((Stage) groupCombo.getScene().getWindow()).close());
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
        ((Stage) groupCombo.getScene().getWindow()).close();
    }

    record GroupItem(long id, String name) {
        @Override public String toString() { return name; }
    }

    record ItemEntry(long id, String title) {
        @Override public String toString() { return title; }
    }
}
