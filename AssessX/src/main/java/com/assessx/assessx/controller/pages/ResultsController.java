package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ResultsController extends BasePage {

    @FXML private VBox  listContainer;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        load();
    }

    private void load() {
        runAsync(() -> {
            try {
                List<JsonObject> results = ApiClient.get().getMyResults();
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    listContainer.getChildren().clear();

                    if (results.isEmpty()) {
                        Label empty = new Label("Результатів ще немає. Виконайте завдання!");
                        empty.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 15px; -fx-padding: 24 0;");

                        listContainer.getChildren().add(empty);
                        return;
                    }
                    for (JsonObject r : results) {
                        listContainer.getChildren().add(buildCard(r));
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

    private VBox buildCard(JsonObject r) {
        int points    = num(r, "points");
        int maxPoints = num(r, "maxPoints");
        double pct    = maxPoints > 0 ? (double) points / maxPoints : 0;

        String badgeClass = pct >= 0.7 ? "badge-green" : pct >= 0.4 ? "badge-orange" : "badge-blue";
        String scoreText  = points + " / " + maxPoints + " балів";

        Label scoreLabel  = badge(scoreText, badgeClass);
        Label typeLabel   = badge(
                lng(r, "testId") != 0 ? "Тест" : "Практика",
                lng(r, "testId") != 0 ? "badge-blue" : "badge-green"
        );

        Label attemptLabel = new Label("Спроба #" + num(r, "attemptNumber"));
        attemptLabel.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(8, typeLabel, attemptLabel);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox scoreRow   = infoRow("Результат:", scoreText + "  (" + Math.round(pct * 100) + "%)");
        HBox startedRow = infoRow("Розпочато:", formatDate(r, "startedAt"));

        HBox subRow     = infoRow("Здано:", formatDate(r, "submittedAt"));
        HBox asgRow     = infoRow("ID завдання:", String.valueOf(lng(r, "assignmentId")));

        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(pct);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle(pct >= 0.7
                ? "-fx-accent: #3fb950;"
                : pct >= 0.4
                ? "-fx-accent: #d29922;"
                : "-fx-accent: #f85149;");

        return card(titleRow, bar, scoreRow, startedRow, subRow, asgRow);
    }
}
