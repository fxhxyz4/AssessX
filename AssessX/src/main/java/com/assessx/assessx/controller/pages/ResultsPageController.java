package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ResultsPageController {

    @FXML private VBox  listContainer;
    @FXML private Label emptyLabel;
    @FXML private ProgressIndicator spinner;

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        spinner.setVisible(true);
        listContainer.getChildren().clear();

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> results = ApiClient.get().getMyResults();
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    if (results.isEmpty()) {
                        emptyLabel.setVisible(true);
                        emptyLabel.setManaged(true);
                    } else {
                        results.forEach(r -> listContainer.getChildren().add(buildCard(r)));
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

    private VBox buildCard(JsonObject r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        int pts    = r.get("points").getAsInt();
        int maxPts = r.get("maxPoints").getAsInt();

        int pct    = maxPts > 0 ? pts * 100 / maxPts : 0;
        int attempt = r.get("attemptNumber").getAsInt();

        boolean isTest = r.has("testId") && !r.get("testId").isJsonNull();
        String type = isTest ? "Тест" : "Практика";

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().addAll("badge", isTest ? "badge-blue" : "badge-green");

        Label scoreLabel = new Label(pts + " / " + maxPts + " балів");
        scoreLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String badgeStyle = pct >= 70 ? "badge-green" : pct >= 40 ? "badge-orange" : "badge";
        Label pctLabel = new Label(pct + "%");
        pctLabel.getStyleClass().addAll("badge", badgeStyle);

        header.getChildren().addAll(typeLabel, scoreLabel, spacer, pctLabel);

        HBox meta = new HBox(16);
        Label attemptLabel = new Label("Спроба #" + attempt);
        attemptLabel.getStyleClass().add("card-subtitle");

        String submitted = r.has("submittedAt") && !r.get("submittedAt").isJsonNull()
                ? r.get("submittedAt").getAsString().substring(0, 16).replace("T", " ")
                : "Не завершено";
        Label submittedLabel = new Label("Здано: " + submitted);
        submittedLabel.getStyleClass().add("card-subtitle");

        meta.getChildren().addAll(attemptLabel, submittedLabel);
        card.getChildren().addAll(header, meta);

        return card;
    }
}
