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
import com.assessx.assessx.controller.dialogs.TakeTestController;
import com.assessx.assessx.controller.dialogs.TakePracticeController;

import java.io.IOException;
import java.util.List;

public class AssignmentsPageController {

    @FXML private VBox   listContainer;
    @FXML private Label  emptyLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Button createBtn;

    @FXML
    public void initialize() {
        if (SessionManager.get().isTeacher()) {
            createBtn.setVisible(true);
            createBtn.setManaged(true);
        }
        loadData();
    }

    private void loadData() {
        spinner.setVisible(true);
        listContainer.getChildren().clear();

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> items = SessionManager.get().isTeacher()
                    ? ApiClient.get().getAllAssignments()
                    : ApiClient.get().getMyAssignments();

                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    if (items.isEmpty()) {
                        emptyLabel.setVisible(true);
                        emptyLabel.setManaged(true);
                    } else {
                        items.forEach(a -> listContainer.getChildren().add(buildCard(a)));
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    emptyLabel.setText("Помилка: " + e.getMessage());

                    emptyLabel.setVisible(true);
                    emptyLabel.setManaged(true);
                });
            }
        });
    }

    private VBox buildCard(JsonObject a) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        long id = a.get("id").getAsLong();
        boolean isTest = a.has("testId") && !a.get("testId").isJsonNull();

        String type = isTest ? "Тест" : "Практика";
        String deadline = a.has("deadline") && !a.get("deadline").isJsonNull()
            ? "Дедлайн: " + a.get("deadline").getAsString().substring(0, 16).replace("T", " ")
            : "Без дедлайну";

        String groupText = a.has("groupName") && !a.get("groupName").isJsonNull()
            ? a.get("groupName").getAsString()
            : "";

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().addAll("badge", isTest ? "badge-blue" : "badge-green");

        String title = isTest
            ? (a.has("testTitle") && !a.get("testTitle").isJsonNull()
               ? a.get("testTitle").getAsString() : "Тест #" + id)
            : (a.has("practiceTitle") && !a.get("practiceTitle").isJsonNull()
               ? a.get("practiceTitle").getAsString() : "Практика #" + id);

        Label idLabel = new Label(title);
        idLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#f0f6fc;");

        Label groupLabel = new Label(groupText + " н.г.");
        groupLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#8b949e; -fx-padding:0 0 0 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(typeLabel, idLabel, groupLabel, spacer);

        HBox actions = new HBox(8);

        if (isTest) {
            long testId = a.get("testId").getAsLong();
            Button startBtn = new Button("Розпочати тест");

            startBtn.getStyleClass().add("btn-primary");
            startBtn.setOnAction(e -> openTestDialog(testId, id));
            actions.getChildren().add(startBtn);
        } else {
            long practiceId = a.get("practiceId").getAsLong();
            Button startBtn = new Button("Відкрити практику");

            startBtn.getStyleClass().add("btn-primary");
            startBtn.setOnAction(e -> openPracticeDialog(practiceId, id));
            actions.getChildren().add(startBtn);
        }

        if (SessionManager.get().isTeacher()) {
            Button delBtn = new Button("Видалити");
            delBtn.getStyleClass().add("btn-danger");

            delBtn.setOnAction(e -> deleteAssignment(id, card));
            actions.getChildren().add(delBtn);
        }

        Label dlLabel = new Label(deadline);
        dlLabel.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(header, dlLabel, actions);
        return card;
    }

    private void openTestDialog(long testId, long assignmentId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/dialogs/take_test.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Тест");

            Scene scene = new Scene(loader.load(), 700, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            TakeTestController ctrl = loader.getController();

            ctrl.init(testId, assignmentId);
            dialog.setScene(scene);

            dialog.showAndWait();
            loadData();
        } catch (IOException e) {
            showError("Не вдалося відкрити тест: " + e.getMessage());
        }
    }

    private void openPracticeDialog(long practiceId, long assignmentId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/dialogs/take_practice.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Практика");

            Scene scene = new Scene(loader.load(), 900, 700);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            TakePracticeController ctrl = loader.getController();
            ctrl.init(practiceId, assignmentId);
            dialog.setScene(scene);

            dialog.showAndWait();
            loadData();
        } catch (IOException e) {
            showError("Не вдалося відкрити практику: " + e.getMessage());
        }
    }

    private void deleteAssignment(long id, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Видалити завдання #" + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deleteAssignment(id);
                        Platform.runLater(() -> listContainer.getChildren().remove(card));
                    } catch (ApiException e) {
                        Platform.runLater(() -> showError(e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/dialogs/create_assignment.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle("Нове завдання");
            Scene scene = new Scene(loader.load(), 500, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            dialog.setScene(scene);
            dialog.showAndWait();
            loadData();
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
