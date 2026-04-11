package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class TakeTestController extends BasePage {

    @FXML private Label testTitleLabel;
    @FXML private Label timerLabel;
    @FXML private VBox  questionsContainer;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    private long testId;
    private long assignmentId;

    // questionIndex -> selected answer string
    private final Map<String, ToggleGroup> toggleGroups = new LinkedHashMap<>();

    private Timeline timer;
    private int secondsLeft;

    public void init(long testId, long assignmentId) {
        this.testId       = testId;
        this.assignmentId = assignmentId;

        loadTest();
    }

    private void loadTest() {
        setSpinner(spinner, true);
        Thread.ofVirtual().start(() -> {
            try {
                JsonObject test = ApiClient.get().getTest(testId);
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    renderTest(test);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    private void renderTest(JsonObject test) {
        testTitleLabel.setText(str(test, "title"));
        secondsLeft = num(test, "timeLimitSec");

        startTimer();

        String questionsJson = str(test, "questions");
        JsonArray questions;

        try {
            questions = JsonParser.parseString(questionsJson).getAsJsonArray();
        } catch (Exception e) {
            showError(errorLabel, "Помилка парсингу питань: " + e.getMessage());
            return;
        }

        questionsContainer.getChildren().clear();

        for (int i = 0; i < questions.size(); i++) {
            JsonObject q = questions.get(i).getAsJsonObject();
            questionsContainer.getChildren().add(buildQuestion(i, q));
        }
    }

    private VBox buildQuestion(int index, JsonObject q) {
        String questionText = q.has("q") ? q.get("q").getAsString() : q.toString();
        Label qLabel = new Label((index + 1) + ". " + questionText);

        qLabel.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 15px; -fx-font-weight: bold; -fx-wrap-text: true;");
        qLabel.setWrapText(true);

        ToggleGroup tg = new ToggleGroup();
        toggleGroups.put(String.valueOf(index), tg);

        VBox optionsBox = new VBox(8);
        optionsBox.setStyle("-fx-padding: 8 0 0 16;");

        if (q.has("options")) {
            JsonArray options = q.get("options").getAsJsonArray();
            for (int j = 0; j < options.size(); j++) {
                String optText = options.get(j).getAsString();
                RadioButton rb = new RadioButton(optText);

                rb.setToggleGroup(tg);
                rb.setUserData(optText);

                rb.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 14px;");
                rb.setWrapText(true);

                optionsBox.getChildren().add(rb);
            }
        } else {
            // Free text fallback
            TextField tf = new TextField();
            tf.setPromptText("Ваша відповідь...");
            tf.getStyleClass().add("input-field");

            // Store in toggle group via Label trick — use TextField tag
            tf.setId("q_" + index);
            optionsBox.getChildren().add(tf);
        }

        VBox card = new VBox(8, qLabel, optionsBox);
        card.getStyleClass().add("card");

        return card;
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            updateTimerLabel();
            if (secondsLeft <= 0) {
                timer.stop();
                onSubmit(); // auto-submit
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        updateTimerLabel();
    }

    private void updateTimerLabel() {
        int mins = secondsLeft / 60;
        int secs = secondsLeft % 60;

        String text = String.format("⏱ %02d:%02d", mins, secs);
        timerLabel.setText(text);

        if (secondsLeft <= 60) {
            timerLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 16px; -fx-font-weight: bold;");
        }
    }

    @FXML
    protected void onSubmit() {
        if (timer != null) timer.stop();

        // Collect answers
        Map<String, String> answers = new LinkedHashMap<>();
        for (Map.Entry<String, ToggleGroup> entry : toggleGroups.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();
            if (selected != null) {
                answers.put(entry.getKey(), (String) selected.getUserData());
            }
        }

        // Also collect text fields
        questionsContainer.lookupAll(".input-field").forEach(node -> {
            if (node instanceof TextField tf && tf.getId() != null && tf.getId().startsWith("q_")) {
                String idx = tf.getId().substring(2);
                if (!tf.getText().isBlank()) {
                    answers.put(idx, tf.getText().trim());
                }
            }
        });

        setSpinner(spinner, true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("answers", answers);

        if (assignmentId > 0) payload.put("assignmentId", assignmentId);

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject result = ApiClient.get().submitTest(testId, payload);
                Platform.runLater(() -> showResult(result));
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    private void showResult(JsonObject result) {
        setSpinner(spinner, false);
        int earned = num(result, "earnedPoints");

        int max    = num(result, "maxPoints");
        int correct = num(result, "correctAnswers");

        int total   = num(result, "totalQuestions");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Результат");

        alert.setHeaderText("Тест здано!");
        alert.setContentText(String.format(
                "Правильних відповідей: %d / %d%n" +
                "Отримано балів: %d / %d",
                correct, total, earned, max));
        alert.showAndWait();

        ((Stage) testTitleLabel.getScene().getWindow()).close();
    }
}
