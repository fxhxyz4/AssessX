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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class HomePageController {

    @FXML private Label greetingLabel;
    @FXML private Label roleLabel;
    @FXML private Label statAssignments;
    @FXML private Label statResults;
    @FXML private Label statAvg;
    @FXML private VBox  assignmentsList;
    @FXML private Label assignmentsEmpty;
    @FXML private VBox  resultsList;
    @FXML private Label resultsEmpty;
    @FXML private ProgressIndicator spinner;

    @FXML
    public void initialize() {
        SessionManager s = SessionManager.get();
        greetingLabel.setText("Привіт, " + s.getName() + "!");

        roleLabel.setText(s.isTeacher() ? "Ви увійшли як Викладач" : "Ви увійшли як Студент");
        loadData();
    }

    private void loadData() {
        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> assignments = ApiClient.get().getMyAssignments();
                List<JsonObject> results     = ApiClient.get().getMyResults();

                double avg = 0;

                if (!results.isEmpty()) {
                    double sum = 0;
                    for (JsonObject r : results) {
                        int pts    = r.get("points").getAsInt();
                        int maxPts = r.get("maxPoints").getAsInt();
                        sum += maxPts > 0 ? (pts * 100.0 / maxPts) : 0;
                    }

                    avg = sum / results.size();
                }

                final double finalAvg = avg;

                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);

                    statAssignments.setText(String.valueOf(assignments.size()));
                    statResults.setText(String.valueOf(results.size()));
                    statAvg.setText(results.isEmpty() ? "—" : String.format("%.0f%%", finalAvg));

                    if (assignments.isEmpty()) {
                        assignmentsEmpty.setVisible(true);
                        assignmentsEmpty.setManaged(true);
                    } else {
                        assignments.stream().limit(5).forEach(a -> {
                            assignmentsList.getChildren().add(buildAssignmentRow(a));
                        });
                    }

                    if (results.isEmpty()) {
                        resultsEmpty.setVisible(true);
                        resultsEmpty.setManaged(true);
                    } else {
                        results.stream().limit(5).forEach(r -> {
                            resultsList.getChildren().add(buildResultRow(r));
                        });
                    }
                });

            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);

                    assignmentsEmpty.setText("Помилка завантаження: " + e.getMessage());
                    assignmentsEmpty.setVisible(true);
                    assignmentsEmpty.setManaged(true);
                });
            }
        });
    }

    private HBox buildAssignmentRow(JsonObject a) {
        HBox row = new HBox(12);
        row.getStyleClass().add("list-row");

        String type = a.has("testId") && !a.get("testId").isJsonNull() ? "Тест" : "Практика";
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().addAll("badge", "badge-blue");

        String deadline = a.has("deadline") && !a.get("deadline").isJsonNull()
                ? "до " + a.get("deadline").getAsString().substring(0, 10)
                : "Без дедлайну";

        Label dlLabel = new Label(deadline);
        dlLabel.getStyleClass().add("card-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label idLabel = new Label("#" + a.get("id").getAsLong());
        idLabel.getStyleClass().add("card-subtitle");

        row.getChildren().addAll(typeLabel, dlLabel, spacer, idLabel);
        return row;
    }

    private HBox buildResultRow(JsonObject r) {
        HBox row = new HBox(12);
        row.getStyleClass().add("list-row");

        int pts    = r.get("points").getAsInt();
        int maxPts = r.get("maxPoints").getAsInt();

        int pct    = maxPts > 0 ? pts * 100 / maxPts : 0;

        Label scoreLabel = new Label(pts + " / " + maxPts);
        scoreLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#f0f6fc;");

        String badgeClass = pct >= 70 ? "badge-green" : pct >= 40 ? "badge-orange" : "badge";
        Label pctLabel = new Label(pct + "%");
        pctLabel.getStyleClass().addAll("badge", badgeClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label attempt = new Label("Спроба #" + r.get("attemptNumber").getAsInt());
        attempt.getStyleClass().add("card-subtitle");

        row.getChildren().addAll(scoreLabel, pctLabel, spacer, attempt);
        return row;
    }
}
