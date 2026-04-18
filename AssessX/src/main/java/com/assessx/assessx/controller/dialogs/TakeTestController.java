package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
    @FXML private Label metaLabel;
    @FXML private VBox  questionsContainer;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    private long testId;
    private long assignmentId;

    // questionIndex -> ToggleGroup (single) або List<CheckBox> (multi)
    private final Map<Integer, ToggleGroup>   singleGroups = new LinkedHashMap<>();
    private final Map<Integer, List<CheckBox>> multiGroups  = new LinkedHashMap<>();

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

        int pts  = num(test, "points");
        int mins = secondsLeft / 60;
        metaLabel.setText(pts + " балів · " + mins + " хв");

        startTimer();
        JsonArray questions;

        try {
            JsonElement qEl = test.get("questions");
            if (qEl.isJsonArray()) {
                questions = qEl.getAsJsonArray();
            } else {
                questions = JsonParser.parseString(qEl.getAsString()).getAsJsonArray();
            }
        } catch (Exception e) {
            showError(errorLabel, "Помилка парсингу питань");
            return;
        }

        JsonObject answersMap = null;
        try {
            JsonElement aEl = test.get("answers");
            if (aEl != null && !aEl.isJsonNull()) {
                if (aEl.isJsonObject()) {
                    answersMap = aEl.getAsJsonObject();
                } else {
                    answersMap = JsonParser.parseString(aEl.getAsString()).getAsJsonObject();
                }
            }
        } catch (Exception ignored) {}

        questionsContainer.getChildren().clear();
        singleGroups.clear();
        multiGroups.clear();

        for (int i = 0; i < questions.size(); i++) {
            JsonObject q = questions.get(i).getAsJsonObject();
            boolean isMulti = q.has("multiple") && !q.get("multiple").isJsonNull()
                && q.get("multiple").getAsBoolean();
            questionsContainer.getChildren().add(buildQuestion(i, q, isMulti));
        }
    }

    private VBox buildQuestion(int index, JsonObject q, boolean isMulti) {
        String questionText = q.has("text") ? q.get("text").getAsString()
            : q.has("q")    ? q.get("q").getAsString()
              : q.toString();

        Label qLabel = new Label((index + 1) + ". " + questionText);
        qLabel.setStyle("-fx-text-fill:#f0f6fc;-fx-font-size:15px;-fx-font-weight:bold;");
        qLabel.setWrapText(true);

        Label hint = new Label(isMulti ? "Оберіть всі правильні варіанти" : "Оберіть одну відповідь");
        hint.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12px;");

        VBox optionsBox = new VBox(8);
        optionsBox.setStyle("-fx-padding:8 0 0 16;");

        if (q.has("options")) {
            JsonArray options = q.get("options").getAsJsonArray();

            if (isMulti) {
                List<CheckBox> checkboxes = new ArrayList<>();
                for (JsonElement opt : options) {
                    String optText = opt.getAsString();
                    CheckBox cb = new CheckBox(optText);
                    cb.setUserData(optText);
                    cb.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:14px;");
                    cb.setWrapText(true);
                    checkboxes.add(cb);
                    optionsBox.getChildren().add(cb);
                }
                multiGroups.put(index, checkboxes);
            } else {
                ToggleGroup tg = new ToggleGroup();
                singleGroups.put(index, tg);
                for (JsonElement opt : options) {
                    String optText = opt.getAsString();
                    RadioButton rb = new RadioButton(optText);
                    rb.setToggleGroup(tg);
                    rb.setUserData(optText);
                    rb.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:14px;");
                    rb.setWrapText(true);
                    optionsBox.getChildren().add(rb);
                }
            }
        }

        VBox card = new VBox(6, qLabel, hint, optionsBox);
        card.getStyleClass().add("card");
        return card;
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
        timerLabel.setText(String.format("%02d:%02d", mins, secs));
        if (secondsLeft <= 60) {
            timerLabel.setStyle("-fx-text-fill:#f85149;-fx-font-size:20px;-fx-font-weight:bold;");
        }
    }

    @FXML
    protected void onSubmit() {
        if (timer != null) timer.stop();
        JsonObject answers = new JsonObject();

        singleGroups.forEach((idx, tg) -> {
            Toggle sel = tg.getSelectedToggle();
            if (sel != null) {
                answers.addProperty(String.valueOf(idx), (String) sel.getUserData());
            }
        });

        multiGroups.forEach((idx, checkboxes) -> {
            for (CheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    answers.addProperty(String.valueOf(idx), (String) cb.getUserData());
                    break;
                }
            }
        });

        setSpinner(spinner, true);

        JsonObject payload = new JsonObject();
        payload.add("answers", answers);
        if (assignmentId > 0) payload.addProperty("assignmentId", assignmentId);

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
        int earned  = num(result, "earnedPoints");
        int max     = num(result, "maxPoints");
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
