package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class TakeTestController {

    @FXML private Label titleLabel;
    @FXML private Label infoLabel;
    @FXML private Label timerLabel;
    @FXML private Label resultLabel;
    @FXML private VBox  questionsBox;
    @FXML private ProgressIndicator spinner;
    @FXML private Button submitBtn;

    private long testId;
    private long assignmentId;
    private int  timeLeft;
    private Timeline timer;

    // questionIndex -> selectedAnswer
    private final Map<Integer, String> answers = new HashMap<>();
    private int totalQuestions = 0;

    public void init(long testId, long assignmentId) {
        this.testId       = testId;
        this.assignmentId = assignmentId;
        loadTest();
    }

    private void loadTest() {
        spinner.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject test = ApiClient.get().getTest(testId);
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);

                    titleLabel.setText(test.get("title").getAsString());
                    int pts  = test.get("points").getAsInt();

                    timeLeft = test.get("timeLimitSec").getAsInt();
                    infoLabel.setText(pts + " балів · " + timeLeft / 60 + " хв");

                    renderQuestions(test.get("questions").getAsString());
                    startTimer();

                    submitBtn.setDisable(false);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    resultLabel.setText("Помилка завантаження: " + e.getMessage());
                    resultLabel.setStyle("-fx-text-fill:#f85149;");
                });
            }
        });
    }

    private void renderQuestions(String questionsJson) {
        questionsBox.getChildren().clear();
        JsonArray questions = JsonParser.parseString(questionsJson).getAsJsonArray();
        totalQuestions = questions.size();

        for (int i = 0; i < questions.size(); i++) {
            JsonObject q = questions.get(i).getAsJsonObject();
            String text  = q.get("text").getAsString();

            JsonArray opts = q.getAsJsonArray("options");

            VBox qBox = new VBox(8);
            qBox.getStyleClass().add("card");

            Label qLabel = new Label((i + 1) + ". " + text);
            qLabel.setWrapText(true);

            qLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#f0f6fc;-fx-font-weight:bold;");
            qBox.getChildren().add(qLabel);

            ToggleGroup tg = new ToggleGroup();
            final int qIdx = i;

            for (JsonElement opt : opts) {
                String optText = opt.getAsString();
                RadioButton rb = new RadioButton(optText);

                rb.setToggleGroup(tg);
                rb.getStyleClass().add("role-radio");

                rb.setOnAction(e -> answers.put(qIdx, optText));
                qBox.getChildren().add(rb);
            }

            questionsBox.getChildren().add(qBox);
        }
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            int m = timeLeft / 60;

            int s = timeLeft % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));

            if (timeLeft <= 60) {
                timerLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#f85149;");
            }

            if (timeLeft <= 0) {
                timer.stop();
                submitTest();
            }
        }));

        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    @FXML
    private void onSubmit() {
        if (timer != null) timer.stop();
        submitTest();
    }

    private void submitTest() {
        submitBtn.setDisable(true);

        Map<String, String> stringAnswers = new HashMap<>();
        answers.forEach((k, v) -> stringAnswers.put(String.valueOf(k), v));

        Map<String, Object> payload = new HashMap<>();
        payload.put("assignmentId", assignmentId);
        payload.put("answers", stringAnswers);

        System.out.println("Submitting answers: " + stringAnswers);
        System.out.println("Payload: " + new com.google.gson.Gson().toJson(payload));

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject result = ApiClient.get().submitTest(testId, payload);
                Platform.runLater(() -> {
                    int earned  = result.get("earnedPoints").getAsInt();
                    int max     = result.get("maxPoints").getAsInt();

                    int correct = result.get("correctAnswers").getAsInt();
                    int total   = result.get("totalQuestions").getAsInt();

                    resultLabel.setText(String.format(
                            "✅ Результат: %d/%d балів · %d/%d правильних відповідей",
                            earned, max, correct, total));

                    double pct = max > 0 ? (double) earned / max : 0;
                    String color = pct >= 0.5 ? "#3fb950" : "#f85149";
                    resultLabel.setStyle("-fx-font-size:14px;-fx-text-fill:" + color + ";-fx-font-weight:bold;");

                    timerLabel.setText("Завершено");
                    timerLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#8b949e;");
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    resultLabel.setText("Помилка: " + e.getMessage());
                    resultLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#f85149;");
                    submitBtn.setDisable(false);
                });
            }
        });
    }
}
