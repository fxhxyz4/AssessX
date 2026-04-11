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
import java.util.List;

public class PracticesController extends BasePage {

    @FXML private VBox   listContainer;
    @FXML private Button createBtn;
    @FXML private ProgressIndicator spinner;
    @FXML private Label  errorLabel;

    @FXML
    public void initialize() {
        if (SessionManager.get().isTeacher()) {
            createBtn.setVisible(true);
            createBtn.setManaged(true);
        }

        load();
    }

    private void load() {
        runAsync(() -> {
            try {
                List<JsonObject> practices = ApiClient.get().getPractices();
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    listContainer.getChildren().clear();

                    if (practices.isEmpty()) {
                        Label empty = new Label("Практичних задач немає");
                        empty.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 15px; -fx-padding: 24 0;");

                        listContainer.getChildren().add(empty);
                        return;
                    }
                    for (JsonObject p : practices) {
                        listContainer.getChildren().add(buildCard(p));
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

    private VBox buildCard(JsonObject p) {
        Label title = new Label(str(p, "title"));
        title.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label desc = new Label(str(p, "description"));
        desc.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");

        desc.setWrapText(true);

        int timeSec = num(p, "timeLimitSec");
        int timeMin = timeSec / 60;

        HBox row1 = infoRow("Балів:", String.valueOf(num(p, "points")));
        HBox row2 = infoRow("Час:", timeMin + " хв " + (timeSec % 60) + " сек");

        HBox row3 = infoRow("Unit-тестів:", String.valueOf(num(p, "unitTestCount")));
        HBox row4 = infoRow("Створено:", formatDate(p, "createdAt"));

        VBox info = new VBox(4, title, desc, row1, row2, row3, row4);

        HBox footer = new HBox(8);
        footer.setStyle("-fx-padding: 12 0 0 0;");

        if (SessionManager.get().isTeacher()) {
            Button deleteBtn = new Button("Видалити");
            deleteBtn.getStyleClass().add("btn-danger");

            deleteBtn.setOnAction(e -> deletePractice(lng(p, "id")));
            Region spacer = new Region();

            HBox.setHgrow(spacer, Priority.ALWAYS);
            footer.getChildren().addAll(spacer, deleteBtn);
        }

        return card(info, footer);
    }

    private void deletePractice(long id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити задачу #" + id + "?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Підтвердження");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deletePractice(id);
                        Platform.runLater(this::load);
                    } catch (ApiException e) {
                        Platform.runLater(() -> showError(errorLabel, e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    protected void onCreatePractice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dialogs/create_practice.fxml"));
            Scene scene = new Scene(loader.load(), 620, 600);

            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle("Нова практична задача");
            dialog.setScene(scene);

            dialog.showAndWait();
            load();
        } catch (IOException e) {
            showError(errorLabel, "Помилка: " + e.getMessage());
        }
    }
}
