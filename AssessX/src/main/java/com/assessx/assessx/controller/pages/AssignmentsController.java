package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.session.SessionManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssignmentsController extends BasePage {

    @FXML private VBox   listContainer;
    @FXML private HBox   teacherBar;
    @FXML private ProgressIndicator spinner;
    @FXML private Label  errorLabel;

    private List<JsonObject> tests     = new ArrayList<>();
    private List<JsonObject> practices = new ArrayList<>();

    @FXML
    public void initialize() {
        if (SessionManager.get().isTeacher()) {
            teacherBar.setVisible(true);
            teacherBar.setManaged(true);
        }

        load();
    }

    private void load() {
        runAsync(() -> {
            try {
                ApiClient api = ApiClient.get();

                List<JsonObject> assignments = SessionManager.get().isTeacher()
                        ? api.getAllAssignments()
                        : api.getMyAssignments();
                tests     = api.getTests();
                practices = api.getPractices();

                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    listContainer.getChildren().clear();

                    if (assignments.isEmpty()) {
                        Label empty = new Label("Завдань немає");
                        empty.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 15px; -fx-padding: 24 0;");

                        listContainer.getChildren().add(empty);
                        return;
                    }

                    for (JsonObject a : assignments) {
                        listContainer.getChildren().add(buildCard(a));
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        }, spinner, errorLabel);
    }

    private VBox buildCard(JsonObject a) {
        long testId     = lng(a, "testId");
        long practiceId = lng(a, "practiceId");

        boolean isTest  = testId != 0;
        String typeStr  = isTest ? "Тест" : "Практика";

        long   itemId   = isTest ? testId : practiceId;
        String title    = findTitle(itemId, isTest);

        Label typeLabel = badge(typeStr, isTest ? "badge-blue" : "badge-green");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(8, typeLabel, titleLabel);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String deadline = formatDate(a, "deadline");
        HBox deadlineRow = infoRow("Дедлайн:", deadline.equals("—") ? "Без дедлайну" : deadline);
        HBox idRow       = infoRow("ID завдання:", String.valueOf(lng(a, "id")));

        VBox info = new VBox(4, titleRow, deadlineRow, idRow);

        Button actionBtn = new Button(isTest ? "Пройти тест" : "Виконати задачу");
        actionBtn.getStyleClass().add("btn-primary");
        actionBtn.setOnAction(e -> openItem(itemId, isTest, lng(a, "id")));

        HBox footer = new HBox(actionBtn);
        footer.setStyle("-fx-padding: 12 0 0 0;");

        if (SessionManager.get().isTeacher()) {
            Button deleteBtn = new Button("Видалити");
            deleteBtn.getStyleClass().add("btn-danger");

            deleteBtn.setOnAction(e -> deleteAssignment(lng(a, "id")));
            Region spacer = new Region();

            HBox.setHgrow(spacer, Priority.ALWAYS);
            footer.getChildren().addAll(spacer, deleteBtn);
        }

        return card(info, footer);
    }

    private void openItem(long itemId, boolean isTest, long assignmentId) {
        try {
            String fxml = isTest ? "/fxml/dialogs/take_test.fxml" : "/fxml/dialogs/take_practice.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));

            Scene scene = new Scene(loader.load(), 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            if (isTest) {
                com.assessx.assessx.controller.dialogs.TakeTestController ctrl = loader.getController();
                ctrl.init(itemId, assignmentId);
            } else {
                com.assessx.assessx.controller.dialogs.TakePracticeController ctrl = loader.getController();
                ctrl.init(itemId, assignmentId);
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle(isTest ? "Тест" : "Практична задача");
            dialog.setScene(scene);

            dialog.showAndWait();

            load();
        } catch (IOException e) {
            showError(errorLabel, "Помилка відкриття: " + e.getMessage());
        }
    }

    private void deleteAssignment(long id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити завдання #" + id + "?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Підтвердження");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deleteAssignment(id);
                        Platform.runLater(this::load);
                    } catch (ApiException e) {
                        Platform.runLater(() -> showError(errorLabel, e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    protected void onCreateAssignment() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dialogs/create_assignment.fxml"));
            Scene scene = new Scene(loader.load(), 500, 500);

            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle("Нове завдання");
            dialog.setScene(scene);

            dialog.showAndWait();
            load();
        } catch (IOException e) {
            showError(errorLabel, "Помилка: " + e.getMessage());
        }
    }

    private String findTitle(long id, boolean isTest) {
        List<JsonObject> list = isTest ? tests : practices;

        return list.stream()
                .filter(o -> lng(o, "id") == id)
                .map(o -> str(o, "title"))
                .findFirst()
                .orElse(isTest ? "Тест #" + id : "Практика #" + id);
    }
}
