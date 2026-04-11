package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.session.SessionManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class HomeController extends BasePage {

    @FXML private Label greetingLabel;
    @FXML private Label roleLabel;
    @FXML private Label assignmentsCount;
    @FXML private Label testsCount;
    @FXML private Label practicesCount;
    @FXML private Label resultsCount;
    @FXML private VBox  recentAssignments;
    @FXML private VBox  recentResults;
    @FXML private Label noAssignmentsLabel;
    @FXML private Label noResultsLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        SessionManager s = SessionManager.get();
        greetingLabel.setText("Привіт, " + s.getName() + "!");
        roleLabel.setText(s.isTeacher() ? "Роль: Викладач" : "Роль: Студент");

        loadData();
    }

    private void loadData() {
        runAsync(() -> {
            try {
                ApiClient api = ApiClient.get();

                List<JsonObject> assignments = s().isTeacher()
                        ? api.getAllAssignments()
                        : api.getMyAssignments();
                List<JsonObject> tests      = api.getTests();

                List<JsonObject> practices  = api.getPractices();
                List<JsonObject> results    = api.getMyResults();

                Platform.runLater(() -> {
                    setSpinner(spinner, false);

                    assignmentsCount.setText(String.valueOf(assignments.size()));
                    testsCount.setText(String.valueOf(tests.size()));

                    practicesCount.setText(String.valueOf(practices.size()));
                    resultsCount.setText(String.valueOf(results.size()));

                    recentAssignments.getChildren().clear();
                    List<JsonObject> recent = assignments.stream().limit(5).toList();

                    if (recent.isEmpty()) {
                        noAssignmentsLabel.setVisible(true);
                        noAssignmentsLabel.setManaged(true);
                    } else {
                        for (JsonObject a : recent) {
                            recentAssignments.getChildren().add(buildAssignmentRow(a, tests, practices));
                        }
                    }

                    recentResults.getChildren().clear();
                    List<JsonObject> recentRes = results.stream().limit(5).toList();

                    if (recentRes.isEmpty()) {
                        noResultsLabel.setVisible(true);
                        noResultsLabel.setManaged(true);
                    } else {
                        for (JsonObject r : recentRes) {
                            recentResults.getChildren().add(buildResultRow(r));
                        }
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

    private SessionManager s() { return SessionManager.get(); }

    private HBox buildAssignmentRow(JsonObject a,
                                    List<JsonObject> tests,
                                    List<JsonObject> practices) {
        long testId     = lng(a, "testId");
        long practiceId = lng(a, "practiceId");

        String typeLabel;
        String titleText;

        if (testId != 0) {
            typeLabel  = "Тест";
            titleText  = findTitle(tests, testId);
        } else {
            typeLabel  = "Практика";
            titleText  = findTitle(practices, practiceId);
        }

        Label type  = badge(typeLabel, testId != 0 ? "badge-blue" : "badge-green");
        Label title = new Label(titleText);

        title.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 14px; -fx-padding: 0 0 0 10;");

        String deadline = formatDate(a, "deadline");
        Label dl = new Label(deadline.equals("—") ? "Без дедлайну" : "до " + deadline);

        dl.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(type, title, spacer, dl);
        row.getStyleClass().add("list-row");

        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildResultRow(JsonObject r) {
        int points    = num(r, "points");
        int maxPoints = num(r, "maxPoints");

        String scoreText = points + " / " + maxPoints + " балів";
        double pct = maxPoints > 0 ? (double) points / maxPoints : 0;

        String badgeClass = pct >= 0.7 ? "badge-green" : pct >= 0.4 ? "badge-orange" : "badge-danger";

        Label score = badge(scoreText, "badge-blue");
        Label attempt = new Label("Спроба #" + num(r, "attemptNumber"));

        attempt.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 14px; -fx-padding: 0 0 0 10;");

        String submitted = formatDate(r, "submittedAt");
        Label date = new Label(submitted);

        date.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(score, attempt, spacer, date);
        row.getStyleClass().add("list-row");

        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private String findTitle(List<JsonObject> items, long id) {
        return items.stream()
                .filter(o -> lng(o, "id") == id)
                .map(o -> str(o, "title"))
                .findFirst()
                .orElse("#" + id);
    }
}
