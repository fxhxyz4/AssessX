package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class AllResultsController extends BasePage {

    @FXML private ComboBox<GroupItem> groupCombo;
    @FXML private VBox  listContainer;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        loadGroups();
    }

    private void loadGroups() {
        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> groups = ApiClient.get().getGroups();
                Platform.runLater(() -> {
                    groupCombo.getItems().clear();

                    for (JsonObject g : groups) {
                        groupCombo.getItems().add(
                                new GroupItem(lng(g, "id"), str(g, "name")));
                    }

                    if (!groupCombo.getItems().isEmpty()) {
                        groupCombo.getSelectionModel().selectFirst();
                        onGroupSelected();
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> showError(errorLabel, e.getMessage()));
            }
        });
    }

    @FXML
    public void onGroupSelected() {
        GroupItem selected = groupCombo.getValue();
        if (selected == null) return;

        runAsync(() -> {
            try {
                List<JsonObject> results = ApiClient.get().getGroupResults(selected.id());
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    listContainer.getChildren().clear();

                    if (results.isEmpty()) {
                        Label empty = new Label("Результатів для групи " + selected.name() + " немає");
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

        Label typeLabel = badge(
                lng(r, "testId") != 0 ? "Тест" : "Практика",
                lng(r, "testId") != 0 ? "badge-blue" : "badge-green"
        );

        Label userLabel = new Label("Студент ID: " + lng(r, "userId"));
        userLabel.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(8, typeLabel, userLabel);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String scoreText = points + " / " + maxPoints + " балів (" + Math.round(pct * 100) + "%)";

        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(pct);
        bar.setMaxWidth(Double.MAX_VALUE);

        bar.setStyle(pct >= 0.7 ? "-fx-accent: #3fb950;"
                : pct >= 0.4 ? "-fx-accent: #d29922;" : "-fx-accent: #f85149;");

        HBox scoreRow   = infoRow("Результат:", scoreText);
        HBox attemptRow = infoRow("Спроба:", "#" + num(r, "attemptNumber"));

        HBox subRow     = infoRow("Здано:", formatDate(r, "submittedAt"));
        return card(titleRow, bar, scoreRow, attemptRow, subRow);
    }

    public record GroupItem(long id, String name) {
        @Override public String toString() { return name; }
    }
}
