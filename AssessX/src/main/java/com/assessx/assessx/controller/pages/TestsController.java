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

public class TestsController extends BasePage {

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
                List<JsonObject> tests = ApiClient.get().getTests();
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    listContainer.getChildren().clear();

                    if (tests.isEmpty()) {
                        Label empty = new Label("Тестів немає");
                        empty.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 15px; -fx-padding: 24 0;");

                        listContainer.getChildren().add(empty);
                        return;
                    }
                    for (JsonObject t : tests) {
                        listContainer.getChildren().add(buildCard(t));
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

    private VBox buildCard(JsonObject t) {
        Label title = new Label(str(t, "title"));
        title.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 16px; -fx-font-weight: bold;");

        int timeSec = num(t, "timeLimitSec");
        int timeMin = timeSec / 60;

        HBox row1 = infoRow("Балів:", String.valueOf(num(t, "points")));
        HBox row2 = infoRow("Час:", timeMin + " хв " + (timeSec % 60) + " сек");

        HBox row3 = infoRow("Створено:", formatDate(t, "createdAt"));

        VBox info = new VBox(4, title, row1, row2, row3);

        HBox footer = new HBox(8);
        footer.setStyle("-fx-padding: 12 0 0 0;");

        if (SessionManager.get().isTeacher()) {
            Button deleteBtn = new Button("Видалити");
            deleteBtn.getStyleClass().add("btn-danger");

            deleteBtn.setOnAction(e -> deleteTest(lng(t, "id")));
            Region spacer = new Region();

            HBox.setHgrow(spacer, Priority.ALWAYS);
            footer.getChildren().addAll(spacer, deleteBtn);
        }

        return card(info, footer);
    }

    private void deleteTest(long id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити тест #" + id + "?", ButtonType.OK, ButtonType.CANCEL);

        confirm.setTitle("Підтвердження");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deleteTest(id);
                        Platform.runLater(this::load);
                    } catch (ApiException e) {
                        Platform.runLater(() -> showError(errorLabel, e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    protected void onCreateTest() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dialogs/create_test.fxml"));
            Scene scene = new Scene(loader.load(), 600, 580);

            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle("Новий тест");
            dialog.setScene(scene);

            dialog.showAndWait();
            load();
        } catch (IOException e) {
            showError(errorLabel, "Помилка: " + e.getMessage());
        }
    }
}
