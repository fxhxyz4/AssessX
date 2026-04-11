package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class TakePracticeController extends BasePage {

    @FXML private Label    titleLabel;
    @FXML private Label    descLabel;
    @FXML private Label    timerLabel;
    @FXML private TextArea codeArea;
    @FXML private TextArea outputArea;
    @FXML private Label    resultLabel;
    @FXML private ProgressIndicator spinner;

    private long practiceId;
    private long assignmentId;

    private Timeline timer;
    private int secondsLeft;

    public void init(long practiceId, long assignmentId) {
        this.practiceId   = practiceId;
        this.assignmentId = assignmentId;

        loadPractice();
    }

    private void loadPractice() {
        setSpinner(spinner, true);
        Thread.ofVirtual().start(() -> {
            try {
                JsonObject p = ApiClient.get().getPractice(practiceId);
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    titleLabel.setText(str(p, "title"));
                    descLabel.setText(str(p, "description"));
                    secondsLeft = num(p, "timeLimitSec");
                    startTimer();

                    codeArea.setText(
                            "public class Solution {\n" +
                            "    public static Object solve(Object input) {\n" +
                            "        // Ваш код тут\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "}\n"
                    );
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    outputArea.setText("Помилка завантаження: " + e.getMessage());
                });
            }
        });
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            updateTimerLabel();

            if (secondsLeft <= 0) {
                timer.stop();
                onSubmit();
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        updateTimerLabel();
    }

    private void updateTimerLabel() {
        int mins = secondsLeft / 60;
        int secs = secondsLeft % 60;

        timerLabel.setText(String.format("⏱ %02d:%02d", mins, secs));

        if (secondsLeft <= 60) {
            timerLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 16px; -fx-font-weight: bold;");
        }
    }

    @FXML
    protected void onSubmit() {
        if (timer != null) timer.stop();

        String code = codeArea.getText();

        if (code.isBlank()) {
            outputArea.setText("Код не може бути порожнім!");
            return;
        }

        setSpinner(spinner, true);
        resultLabel.setText("Відправляємо на перевірку...");

        outputArea.setText("");

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);

        if (assignmentId > 0) payload.put("assignmentId", assignmentId);

        Thread.ofVirtual().start(() -> {
            try {
                JsonObject result = ApiClient.get().submitPractice(practiceId, payload);
                Platform.runLater(() -> showResult(result));
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    resultLabel.setText("");
                    outputArea.setText("Помилка: " + e.getMessage());
                });
            }
        });
    }

    private void showResult(JsonObject result) {
        setSpinner(spinner, false);
        int passed = num(result, "passedTests");

        int total  = num(result, "totalTests");
        String output = str(result, "output");

        outputArea.setText(output);

        boolean allPassed = passed == total && total > 0;
        resultLabel.setText(String.format("%d / %d тестів пройдено", passed, total));
        resultLabel.setStyle(allPassed
                ? "-fx-text-fill: #3fb950; -fx-font-size: 14px; -fx-font-weight: bold;"
                : "-fx-text-fill: #f85149; -fx-font-size: 14px; -fx-font-weight: bold;");

        if (allPassed) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Чудово!");

            alert.setHeaderText("Всі тести пройдено!");
            alert.setContentText("Результат: " + passed + " / " + total);

            alert.showAndWait();
            ((Stage) titleLabel.getScene().getWindow()).close();
        }
    }
}
