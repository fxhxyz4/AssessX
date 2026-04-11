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

public class TestsPageController {

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
                List<JsonObject> tests = ApiClient.get().getTests();
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    if (tests.isEmpty()) {
                        emptyLabel.setVisible(true);
                        emptyLabel.setManaged(true);
                    } else {
                        tests.forEach(t -> listContainer.getChildren().add(buildCard(t)));
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

    private VBox buildCard(JsonObject t) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        long id    = t.get("id").getAsLong();
        String title = t.get("title").getAsString();

        int pts    = t.get("points").getAsInt();
        int timeSec = t.get("timeLimitSec").getAsInt();

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label ptsLabel = new Label(pts + " балів");
        ptsLabel.getStyleClass().addAll("badge", "badge-green");

        Label timeLabel = new Label(formatTime(timeSec));
        timeLabel.getStyleClass().add("card-subtitle");

        header.getChildren().addAll(titleLabel, spacer, timeLabel, ptsLabel);

        HBox actions = new HBox(8);
        if (SessionManager.get().isTeacher()) {
            Button delBtn = new Button("Видалити");
            delBtn.getStyleClass().add("btn-danger");

            delBtn.setOnAction(e -> deleteTest(id, card));
            actions.getChildren().add(delBtn);
        }

        card.getChildren().addAll(header, actions);
        return card;
    }

    private void deleteTest(long id, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити тест #" + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deleteTest(id);
                        Platform.runLater(() -> listContainer.getChildren().remove(card));
                    } catch (ApiException e) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                                e.getMessage(), ButtonType.OK).showAndWait());
                    }
                });
            }
        });
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dialogs/create_test.fxml"));
            Stage dialog = new Stage();

            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Новий тест");

            Scene scene = new Scene(loader.load(), 600, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dialog.setScene(scene);

            dialog.showAndWait();
            loadData();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private String formatTime(int sec) {
        if (sec < 60) return sec + " сек";
        return (sec / 60) + " хв";
    }
}
