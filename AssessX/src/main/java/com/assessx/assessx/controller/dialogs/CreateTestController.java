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

        String title   = titleField.getText().trim();
        String ptsStr  = pointsField.getText().trim();
        String timeStr = timeLimitField.getText().trim();

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
        JsonObject answersJson  = new JsonObject();

        for (int i = 0; i < questions.size(); i++) {
            QuestionRow q = questions.get(i);
            String qText = q.getText();

            List<String> opts = q.getOptions();
            List<String> correct = q.getCorrectOptions();

            if (qText.isBlank()) {
                showError(errorLabel, "Питання " + (i+1) + ": введіть текст"); return;
            }
            if (opts.size() < 2) {
                showError(errorLabel, "Питання " + (i+1) + ": мінімум 2 варіанти"); return;
            }
            if (correct.isEmpty()) {
                showError(errorLabel, "Питання " + (i+1) + ": оберіть правильну відповідь"); return;
            }

            JsonObject qObj = new JsonObject();
            qObj.addProperty("text", qText);
            qObj.addProperty("multiple", q.multiMode);

            JsonArray optsArr = new JsonArray();
            opts.forEach(optsArr::add);
            qObj.add("options", optsArr);
            questionsJson.add(qObj);

            int correctIdx = opts.indexOf(correct.get(0));
            answersJson.addProperty(String.valueOf(i), correctIdx);
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

    static class QuestionRow {
        private final VBox node;
        private final TextField textField;
        private final VBox optionsBox;
        private final List<OptionRow> optionRows = new ArrayList<>();
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private Label numberLabel;
        private boolean multiMode = false;

        interface RemoveCallback { void remove(QuestionRow row); }

        QuestionRow(int number, RemoveCallback onRemove) {
            node = new VBox(8);
            node.setStyle("-fx-background-color:#161b22;-fx-border-color:#30363d;" +
                "-fx-border-radius:6;-fx-background-radius:6;-fx-border-width:1;-fx-padding:16;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            numberLabel = new Label("Питання " + number);

            numberLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            CheckBox multiCheck = new CheckBox("Декілька правильних");
            multiCheck.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12px;-fx-cursor:hand;");
            multiCheck.selectedProperty().addListener((obs, old, val) -> setMultiMode(val));

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color:#da3633;-fx-text-fill:white;" +
                "-fx-border-radius:4;-fx-background-radius:4;-fx-padding:2 8;-fx-cursor:hand;");

            removeBtn.setOnAction(e -> onRemove.remove(this));
            header.getChildren().addAll(numberLabel, spacer, multiCheck, removeBtn);

            textField = new TextField();
            textField.setPromptText("Текст питання...");
            textField.setStyle("-fx-background-color:#0d1117;-fx-border-color:#3d444d;" +
                "-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;" +
                "-fx-text-fill:#e6edf3;-fx-prompt-text-fill:#545d68;-fx-font-size:14px;-fx-padding:10 14;");

            optionsBox = new VBox(6);

            Button addOptBtn = new Button("+ Додати варіант");
            addOptBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#388bfd;" +
                "-fx-cursor:hand;-fx-font-size:13px;-fx-border-width:0;-fx-padding:4 0;");
            addOptBtn.setOnAction(e -> addOption());

            addOption();
            addOption();

            node.getChildren().addAll(header, textField, optionsBox, addOptBtn);
        }

        private void setMultiMode(boolean multi) {
            this.multiMode = multi;

            List<String> texts = new ArrayList<>();
            List<Boolean> selected = new ArrayList<>();

            for (OptionRow o : optionRows) {
                texts.add(o.getText());
                selected.add(o.isSelected());
            }

            optionRows.clear();
            optionsBox.getChildren().clear();

            for (int i = 0; i < texts.size(); i++) {
                OptionRow o = new OptionRow(multi, toggleGroup, this::removeOption);
                o.setText(texts.get(i));

                if (selected.get(i)) o.setSelected(true);
                optionRows.add(o);
                optionsBox.getChildren().add(o.getNode());
            }
        }

        private void addOption() {
            OptionRow opt = new OptionRow(multiMode, toggleGroup, this::removeOption);
            optionRows.add(opt);
            optionsBox.getChildren().add(opt.getNode());
        }

        private void removeOption(OptionRow opt) {
            if (optionRows.size() <= 2) return;
            optionRows.remove(opt);
            optionsBox.getChildren().remove(opt.getNode());
        }

        void setNumber(int n) { numberLabel.setText("Питання " + n); }
        VBox getNode() { return node; }
        String getText() { return textField.getText().trim(); }

        List<String> getOptions() {
            List<String> list = new ArrayList<>();
            optionRows.forEach(o -> { if (!o.getText().isBlank()) list.add(o.getText()); });
            return list;
        }

        List<String> getCorrectOptions() {
            List<String> list = new ArrayList<>();
            optionRows.forEach(o -> { if (o.isSelected() && !o.getText().isBlank()) list.add(o.getText()); });
            return list;
        }
    }

    static class OptionRow {
        private final HBox node;
        private final TextField textField;
        private RadioButton radio;
        private CheckBox checkbox;
        private final boolean multi;

        interface RemoveCallback { void remove(OptionRow row); }

        OptionRow(boolean multi, ToggleGroup group, RemoveCallback onRemove) {
            this.multi = multi;
            node = new HBox(8);
            node.setAlignment(Pos.CENTER_LEFT);

            String fieldStyle = "-fx-background-color:#0d1117;-fx-border-color:#3d444d;" +
                "-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;" +
                "-fx-text-fill:#e6edf3;-fx-prompt-text-fill:#545d68;-fx-font-size:13px;-fx-padding:8 12;";

            textField = new TextField();
            textField.setPromptText("Варіант відповіді...");
            textField.setStyle(fieldStyle);
            HBox.setHgrow(textField, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#8b949e;" +
                "-fx-cursor:hand;-fx-border-width:0;-fx-font-size:12px;-fx-padding:4 6;");
            removeBtn.setOnAction(e -> onRemove.remove(this));

            if (multi) {
                checkbox = new CheckBox();
                checkbox.setStyle("-fx-cursor:hand;");
                node.getChildren().addAll(checkbox, textField, removeBtn);
            } else {
                radio = new RadioButton();
                radio.setToggleGroup(group);
                radio.setStyle("-fx-cursor:hand;");
                node.getChildren().addAll(radio, textField, removeBtn);
            }
        }

        HBox getNode() { return node; }
        String getText() { return textField.getText().trim(); }
        void setText(String t) { textField.setText(t); }

        boolean isSelected() {
            return multi ? (checkbox != null && checkbox.isSelected())
                : (radio != null && radio.isSelected());
        }

        void setSelected(boolean v) {
            if (multi && checkbox != null) checkbox.setSelected(v);
            else if (!multi && radio != null) radio.setSelected(v);
        }
    }
}
