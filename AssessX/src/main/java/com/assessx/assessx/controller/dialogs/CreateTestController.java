package com.assessx.assessx.controller.dialogs;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.assessx.assessx.controller.pages.BasePage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CreateTestController extends BasePage {

    @FXML private TextField titleField;
    @FXML private TextField pointsField;
    @FXML private TextField timeLimitField;
    @FXML private VBox      questionsContainer;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    private final List<QuestionRow> questions = new ArrayList<>();

    @FXML
    public void initialize() {
        addQuestion();
    }

    @FXML
    private void onAddQuestion() {
        addQuestion();
    }

    private void addQuestion() {
        QuestionRow row = new QuestionRow(questions.size() + 1, this::removeQuestion);
        questions.add(row);
        questionsContainer.getChildren().add(row.getNode());
        renumberQuestions();
    }

    private void removeQuestion(QuestionRow row) {
        questions.remove(row);
        questionsContainer.getChildren().remove(row.getNode());
        renumberQuestions();
    }

    private void renumberQuestions() {
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setNumber(i + 1);
        }
    }

    @FXML
    protected void onCreate() {
        hideError(errorLabel);

        String title    = titleField.getText().trim();
        String ptsStr   = pointsField.getText().trim();
        String timeStr  = timeLimitField.getText().trim();

        if (title.isBlank()) { showError(errorLabel, "Введіть назву"); return; }
        if (questions.isEmpty()) { showError(errorLabel, "Додайте хоча б одне питання"); return; }

        int points, timeLimit;
        try {
            points    = Integer.parseInt(ptsStr);
            timeLimit = Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            showError(errorLabel, "Балів і час мають бути числами");
            return;
        }

        JsonArray questionsJson = new JsonArray();
        JsonArray answersJson   = new JsonArray();

        for (int i = 0; i < questions.size(); i++) {
            QuestionRow q = questions.get(i);
            String qText = q.getText();
            List<String> opts = q.getOptions();
            int correct = q.getCorrectIndex();

            if (qText.isBlank()) {
                showError(errorLabel, "Питання " + (i + 1) + ": введіть текст");
                return;
            }
            if (opts.size() < 2) {
                showError(errorLabel, "Питання " + (i + 1) + ": мінімум 2 варіанти");
                return;
            }
            if (correct < 0) {
                showError(errorLabel, "Питання " + (i + 1) + ": оберіть правильну відповідь");
                return;
            }

            JsonObject qObj = new JsonObject();
            qObj.addProperty("text", qText);

            JsonArray optsArr = new JsonArray();
            opts.forEach(optsArr::add);
            qObj.add("options", optsArr);

            questionsJson.add(qObj);

            answersJson.add(correct);
        }

        setSpinner(spinner, true);

        JsonObject payload = new JsonObject();
        payload.addProperty("title", title);
        payload.add("questions", questionsJson);

        payload.add("answers",   answersJson);
        payload.addProperty("points",       points);
        payload.addProperty("timeLimitSec", timeLimit);

        System.out.println("Payload: " + payload);

        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().createTest(payload);
                Platform.runLater(() -> ((Stage) titleField.getScene().getWindow()).close());
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, e.getMessage());
                });
            }
        });
    }

    @FXML
    protected void onCancel() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    private static class QuestionRow {
        private final VBox node;
        private final TextField textField;
        private final VBox optionsBox;
        private final List<OptionRow> optionRows = new ArrayList<>();
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private Label numberLabel;

        interface RemoveCallback { void remove(QuestionRow row); }

        QuestionRow(int number, RemoveCallback onRemove) {
            node = new VBox(8);
            node.setStyle("-fx-background-color:#161b22;-fx-border-color:#30363d;-fx-border-radius:6;-fx-background-radius:6;-fx-border-width:1;-fx-padding:16;");

            // Header
            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            numberLabel = new Label("Питання " + number);
            numberLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color:#da3633;-fx-text-fill:white;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:2 8;-fx-cursor:hand;");
            removeBtn.setOnAction(e -> onRemove.remove(this));
            header.getChildren().addAll(numberLabel, spacer, removeBtn);

            // Text field
            textField = new TextField();
            textField.setPromptText("Текст питання...");
            textField.setStyle("-fx-background-color:#0d1117;-fx-border-color:#3d444d;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-text-fill:#e6edf3;-fx-prompt-text-fill:#545d68;-fx-font-size:14px;-fx-padding:10 14;-fx-pref-height:44;");

            // Options
            optionsBox = new VBox(6);
            Button addOptBtn = new Button("+ Додати варіант");
            addOptBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#388bfd;-fx-cursor:hand;-fx-font-size:13px;-fx-border-width:0;-fx-padding:4 0;");
            addOptBtn.setOnAction(e -> addOption());

            // Default 2 options
            addOption();
            addOption();

            node.getChildren().addAll(header, textField, optionsBox, addOptBtn);
        }

        private void addOption() {
            OptionRow opt = new OptionRow(toggleGroup, this::removeOption);
            optionRows.add(opt);
            optionsBox.getChildren().add(opt.getNode());
        }

        private void removeOption(OptionRow opt) {
            if (optionRows.size() <= 2) return;
            optionRows.remove(opt);
            optionsBox.getChildren().remove(opt.getNode());
        }

        void setNumber(int n) {
            numberLabel.setText("Питання " + n);
        }

        VBox getNode() { return node; }
        String getText() { return textField.getText().trim(); }

        List<String> getOptions() {
            List<String> list = new ArrayList<>();
            optionRows.forEach(o -> { if (!o.getText().isBlank()) list.add(o.getText()); });
            return list;
        }

        int getCorrectIndex() {
            List<String> opts = getOptions();

            for (int i = 0; i < optionRows.size(); i++) {
                if (optionRows.get(i).isSelected()) {
                    String val = optionRows.get(i).getText();
                    return opts.indexOf(val);
                }
            }

            return -1;
        }
    }


    private static class OptionRow {
        private final HBox node;
        private final TextField textField;
        private final RadioButton radio;

        interface RemoveCallback { void remove(OptionRow row); }

        OptionRow(ToggleGroup group, RemoveCallback onRemove) {
            node = new HBox(8);
            node.setAlignment(Pos.CENTER_LEFT);

            radio = new RadioButton();
            radio.setToggleGroup(group);
            radio.setStyle("-fx-cursor:hand;");

            textField = new TextField();
            textField.setPromptText("Варіант відповіді...");
            textField.setStyle("-fx-background-color:#0d1117;-fx-border-color:#3d444d;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-text-fill:#e6edf3;-fx-prompt-text-fill:#545d68;-fx-font-size:13px;-fx-padding:8 12;");
            HBox.setHgrow(textField, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#8b949e;-fx-cursor:hand;-fx-border-width:0;-fx-font-size:12px;-fx-padding:4 6;");
            removeBtn.setOnAction(e -> onRemove.remove(this));

            node.getChildren().addAll(radio, textField, removeBtn);
        }

        HBox getNode() { return node; }
        String getText() { return textField.getText().trim(); }
        boolean isSelected() { return radio.isSelected(); }
    }
}
