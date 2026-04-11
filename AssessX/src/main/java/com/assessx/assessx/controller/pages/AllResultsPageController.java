package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class AllResultsPageController {

    @FXML private ComboBox<GroupItem> groupCombo;
    @FXML private VBox  listContainer;
    @FXML private Label emptyLabel;
    @FXML private ProgressIndicator spinner;

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
                    groups.forEach(g -> groupCombo.getItems().add(
                            new GroupItem(g.get("id").getAsLong(), g.get("name").getAsString())));
                });
            } catch (ApiException ignored) {}
        });
    }

    @FXML
    private void onGroupSelected() {
        GroupItem selected = groupCombo.getValue();
        if (selected == null) return;

        spinner.setVisible(true);
        spinner.setManaged(true);

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        listContainer.getChildren().clear();

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> results = ApiClient.get().getGroupResults(selected.id());
                List<JsonObject> students = ApiClient.get().getGroupStudents(selected.id());

                java.util.Map<Long, JsonObject> userMap = new java.util.HashMap<>();
                students.forEach(s -> userMap.put(s.get("id").getAsLong(), s));

                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);

                    if (results.isEmpty()) {
                        emptyLabel.setText("Результатів немає для цієї групи");
                        emptyLabel.setVisible(true);

                        emptyLabel.setManaged(true);
                    } else {
                        results.forEach(r -> listContainer.getChildren().add(buildCard(r, userMap)));
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

    private VBox buildCard(JsonObject r, java.util.Map<Long, JsonObject> userMap) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        int pts    = r.get("points").getAsInt();
        int maxPts = r.get("maxPoints").getAsInt();

        int pct    = maxPts > 0 ? pts * 100 / maxPts : 0;

        boolean isTest = r.has("testId") && !r.get("testId").isJsonNull();
        String type = isTest ? "Тест" : "Практика";

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        long userId = r.get("userId").getAsLong();
        JsonObject user = userMap.get(userId);

        String displayName = user != null
                ? user.get("name").getAsString() + " (@" + user.get("githubLogin").getAsString() + ")"
                : "User #" + userId;

        Label userLabel = new Label(displayName);
        userLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().addAll("badge", isTest ? "badge-blue" : "badge-green");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreLabel = new Label(pts + "/" + maxPts + " (" + pct + "%)");
        String c = pct >= 70 ? "#3fb950" : pct >= 40 ? "#d29922" : "#f85149";

        scoreLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + c + ";");

        header.getChildren().addAll(userLabel, typeLabel, spacer, scoreLabel);

        String submitted = r.has("submittedAt") && !r.get("submittedAt").isJsonNull()
                ? r.get("submittedAt").getAsString().substring(0, 16).replace("T", " ")
                : "Не завершено";

        Label meta = new Label("Спроба #" + r.get("attemptNumber").getAsInt() + " · " + submitted);
        meta.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(header, meta);
        return card;
    }

    public record GroupItem(long id, String name) {
        @Override public String toString() { return name; }
    }
}
