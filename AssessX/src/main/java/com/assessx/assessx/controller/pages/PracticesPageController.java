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

public class PracticesPageController {

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
                List<JsonObject> practices = ApiClient.get().getPractices();
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    if (practices.isEmpty()) {
                        emptyLabel.setVisible(true);
                        emptyLabel.setManaged(true);
                    } else {
                        practices.forEach(p -> listContainer.getChildren().add(buildCard(p)));
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

    private VBox buildCard(JsonObject p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        long id     = p.get("id").getAsLong();
        String title = p.get("title").getAsString();

        String desc  = p.get("description").getAsString();
        int pts     = p.get("points").getAsInt();
        int timeSec = p.get("timeLimitSec").getAsInt();

        int tests   = p.get("unitTestCount").getAsInt();

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label ptsLabel = new Label(pts + " балів");
        ptsLabel.getStyleClass().addAll("badge", "badge-green");

        header.getChildren().addAll(titleLabel, spacer, ptsLabel);

        Label descLabel = new Label(desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);
        descLabel.getStyleClass().add("card-subtitle");

        descLabel.setWrapText(true);

        HBox meta = new HBox(16);
        String timeStr;
        
        if (timeSec < 60) {
            timeStr = timeSec + " с";
        } else if (timeSec % 60 == 0) {
            timeStr = timeSec / 60 + " хв";
        } else {
            timeStr = timeSec / 60 + " хв " + timeSec % 60 + " с";
        }
        
        Label timeLabel = new Label(timeStr);

        timeLabel.getStyleClass().add("card-subtitle");
        Label testCountLabel = new Label("🔬 " + tests + " unit-тестів");

        testCountLabel.getStyleClass().add("card-subtitle");
        meta.getChildren().addAll(timeLabel, testCountLabel);

        HBox actions = new HBox(8);

        if (SessionManager.get().isTeacher()) {
            Button delBtn = new Button("Видалити");
            delBtn.getStyleClass().add("btn-danger");

            delBtn.setOnAction(e -> deletePractice(id, card));
            actions.getChildren().add(delBtn);
        }

        card.getChildren().addAll(header, descLabel, meta, actions);
        return card;
    }

    private void deletePractice(long id, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити практику #" + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().deletePractice(id);
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
                    getClass().getResource("/fxml/dialogs/create_practice.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setTitle("Нова практика");
            Scene scene = new Scene(loader.load(), 650, 650);

            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            dialog.setScene(scene);
            dialog.showAndWait();

            loadData();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
