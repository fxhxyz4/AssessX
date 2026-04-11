package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class TakePracticeController {

    @FXML private Label    titleLabel;
    @FXML private Label    descLabel;
    @FXML private Label    timerLabel;
    @FXML private Label    resultLabel;
    @FXML private Label    outputLabel;
    @FXML private TextArea codeEditor;
    @FXML private ProgressIndicator spinner;
    @FXML private Button   submitBtn;

    private long practiceId;
    private long assignmentId;

    private int  timeLeft;
    private Timeline timer;

    public void init(long practiceId, long assignmentId) {
        this.practiceId   = practiceId;
        this.assignmentId = assignmentId;

        loadPractice();
    }

    private void loadPractice() {
        spinner.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject practice = ApiClient.get().getPractice(practiceId);
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);

                    titleLabel.setText(practice.get("title").getAsString());
                    descLabel.setText(practice.get("description").getAsString());

                    timeLeft = practice.get("timeLimitSec").getAsInt();
                    int pts  = practice.get("points").getAsInt();
                    int tests = practice.get("unitTestCount").getAsInt();

                    descLabel.setText(practice.get("description").getAsString()
                            + "\n\n⚡ " + pts + " балів · ⏱ " + timeLeft / 60 + " хв · 🔬 " + tests + " unit-тестів");

                    codeEditor.setText(
                            "public class Solution {\n" +
                            "    public static void main(String[] args) {\n" +
                            "        // ваш код тут\n" +
                            "    }\n" +
                            "}\n"
                    );

                    startTimer();
                    submitBtn.setDisable(false);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    resultLabel.setText("Помилка завантаження: " + e.getMessage());
                });
            }
        });
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            int m = timeLeft / 60;

            int s = timeLeft % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));

            if (timeLeft <= 300) {
                timerLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#d29922;");
            }

            if (timeLeft <= 60) {
                timerLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#f85149;");
            }

            if (timeLeft <= 0) {
                timer.stop();
                submitCode();
            }
        }));

        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    @FXML
    private void onSubmit() {
        if (timer != null) timer.stop();
        submitCode();
    }

    private void submitCode() {
        submitBtn.setDisable(true);
        resultLabel.setText("Перевіряємо код...");

        resultLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#8b949e;");
        outputLabel.setText("");

        String code = codeEditor.getText();
        Map<String, Object> payload = new HashMap<>();

        payload.put("assignmentId", assignmentId);
        payload.put("code", code);

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject result = ApiClient.get().submitPractice(practiceId, payload);
                Platform.runLater(() -> {
                    int passed = result.get("passedTests").getAsInt();

                    int total  = result.get("totalTests").getAsInt();
                    int pts    = result.has("earnedPoints") ? result.get("earnedPoints").getAsInt() : 0;

                    boolean allPassed = passed == total;
                    resultLabel.setText(String.format(
                            "%s Пройдено: %d/%d тестів · %d балів",
                            allPassed ? "✅" : "⚠️", passed, total, pts));
                    resultLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"
                            + (allPassed ? "#3fb950" : "#d29922") + ";");

                    if (result.has("testOutput") && !result.get("testOutput").isJsonNull()) {
                        String out = result.get("testOutput").getAsString();
                        outputLabel.setText(out.length() > 500 ? out.substring(0, 500) + "..." : out);
                    }

                    if (!allPassed) {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Спробувати ще раз");
                    } else {
                        timerLabel.setText("Завершено ✅");
                        timerLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#3fb950;");
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    resultLabel.setText("Помилка: " + e.getMessage());
                    resultLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#f85149;");
                    submitBtn.setDisable(false);
                });
            }
        });
    }
}
