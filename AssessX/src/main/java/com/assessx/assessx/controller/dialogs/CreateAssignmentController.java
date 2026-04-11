package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAssignmentController extends BasePage {

    @FXML private ComboBox<GroupItem>  groupCombo;
    @FXML private RadioButton          testRadio;
    @FXML private RadioButton          practiceRadio;
    @FXML private ComboBox<ItemEntry>  itemCombo;
    @FXML private Label                itemLabel;
    @FXML private DatePicker           deadlinePicker;
    @FXML private ProgressIndicator    spinner;
    @FXML private Label                errorLabel;

    private List<JsonObject> tests     = new ArrayList<>();
    private List<JsonObject> practices = new ArrayList<>();

    @FXML
    public void initialize() {
        // Toggle listener — reload item combo
        testRadio.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) {
                itemLabel.setText("Тест");
                populateItemCombo(true);
            }
        });

        practiceRadio.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) {
                itemLabel.setText("Практика");
                populateItemCombo(false);
            }
        });

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

                    populateItemCombo(true);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    private void populateItemCombo(boolean isTest) {
        itemCombo.getItems().clear();
        List<JsonObject> list = isTest ? tests : practices;

        for (JsonObject o : list) {
            itemCombo.getItems().add(new ItemEntry(lng(o, "id"), str(o, "title")));
        }

        if (!itemCombo.getItems().isEmpty()) {
            itemCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    protected void onCreate() {
        hideError(errorLabel);

        GroupItem group = groupCombo.getValue();
        ItemEntry item  = itemCombo.getValue();

        if (group == null) {
            showError(errorLabel, "Виберіть групу");
            return;
        }

        if (item  == null) {
            showError(errorLabel, "Виберіть завдання");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", group.id());

        if (testRadio.isSelected()) {
            payload.put("testId", item.id());
        } else {
            payload.put("practiceId", item.id());
        }

        LocalDate deadline = deadlinePicker.getValue();
        if (deadline != null) {
            payload.put("deadline",
                    deadline.atTime(23, 59, 0)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
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
