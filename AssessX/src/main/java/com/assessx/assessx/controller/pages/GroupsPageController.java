package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupsPageController {

    @FXML private VBox  listContainer;
    @FXML private Label emptyLabel;
    @FXML private ProgressIndicator spinner;

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        spinner.setVisible(true);
        listContainer.getChildren().clear();

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> groups = ApiClient.get().getGroups();
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    spinner.setManaged(false);
                    if (groups.isEmpty()) {
                        emptyLabel.setVisible(true);
                        emptyLabel.setManaged(true);
                    } else {
                        groups.forEach(g -> listContainer.getChildren().add(buildCard(g)));
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

    private VBox buildCard(JsonObject g) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        long id = g.get("id").getAsLong();
        String name = g.get("name").getAsString();
        int studentCount = g.getAsJsonArray("studentIds").size();

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label("👥 " + name);
        nameLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f0f6fc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(" студентів:" + " " + studentCount);
        countLabel.getStyleClass().addAll("badge", "badge-blue");

        header.getChildren().addAll(nameLabel, spacer, countLabel);

        VBox studentsBox = new VBox(4);
        studentsBox.setVisible(false);
        studentsBox.setManaged(false);

        Button toggleBtn = new Button("▶ Показати студентів");
        toggleBtn.getStyleClass().add("btn-secondary");
        toggleBtn.setOnAction(e -> {
            if (studentsBox.isVisible()) {
                studentsBox.setVisible(false);

                studentsBox.setManaged(false);
                toggleBtn.setText("▶ Показати студентів");
            } else {
                loadStudents(id, studentsBox);
                studentsBox.setVisible(true);

                studentsBox.setManaged(true);
                toggleBtn.setText("▼ Сховати студентів");
            }
        });

        VBox addSection = new VBox(6);
        addSection.setStyle("-fx-padding: 8 0 0 0;");

        Label addLabel = new Label("Додати студента за GitHub логіном:");
        addLabel.getStyleClass().add("field-label");

        HBox addRow = new HBox(8);
        addRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField loginField = new TextField();
        loginField.setPromptText("github-логін");

        HBox.setHgrow(loginField, Priority.ALWAYS);
        loginField.getStyleClass().add("input-field");

        Button addBtn = new Button("Додати");
        addBtn.getStyleClass().add("btn-primary");

        Label addError = new Label();
        addError.getStyleClass().add("field-error");

        addError.setVisible(false);
        addError.setManaged(false);

        addBtn.setOnAction(e -> {
            String login = loginField.getText().trim();
            if (login.isBlank()) return;

            addError.setVisible(false);
            addError.setManaged(false);

            addBtn.setDisable(true);
            addBtn.setText("...");

            Thread.ofVirtual().start(() -> {
                try {
                    List<JsonObject> allUsers = ApiClient.get().getAllUsers();
                    Long foundId = null;

                    for (JsonObject u : allUsers) {
                        if (u.get("githubLogin").getAsString().equalsIgnoreCase(login)) {
                            foundId = u.get("id").getAsLong();
                            break;
                        }
                    }

                    if (foundId == null) {
                        Platform.runLater(() -> {
                            addError.setText("Користувача «" + login + "» не знайдено");
                            addError.setVisible(true);

                            addError.setManaged(true);
                            addBtn.setDisable(false);
                            addBtn.setText("Додати");
                        });

                        return;
                    }

                    final long uid = foundId;
                    ApiClient.get().addStudentToGroup(id, uid);

                    Platform.runLater(() -> {
                        loginField.clear();
                        addBtn.setDisable(false);

                        addBtn.setText("Додати");
                        if (studentsBox.isVisible()) loadStudents(id, studentsBox);
                    });
                } catch (ApiException ex) {
                    Platform.runLater(() -> {
                        addError.setText("Помилка: " + ex.getMessage());
                        addError.setVisible(true);

                        addError.setManaged(true);
                        addBtn.setDisable(false);
                        addBtn.setText("Додати");
                    });
                }
            });
        });

        addRow.getChildren().addAll(loginField, addBtn);
        addSection.getChildren().addAll(addLabel, addRow, addError);

        card.getChildren().addAll(header, toggleBtn, studentsBox, addSection);
        return card;
    }

    private void loadStudents(long groupId, VBox box) {
        box.getChildren().clear();
        ProgressIndicator pi = new ProgressIndicator();

        pi.setMaxSize(24, 24);
        box.getChildren().add(pi);

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> students = ApiClient.get().getGroupStudents(groupId);
                Platform.runLater(() -> {
                    box.getChildren().clear();
                    if (students.isEmpty()) {
                        Label empty = new Label("Студентів немає");
                        empty.getStyleClass().add("card-subtitle");
                        box.getChildren().add(empty);
                    } else {
                        students.forEach(s -> {
                            HBox row = new HBox(8);
                            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                            Label nameL = new Label(s.get("name").getAsString() +
                                    " (@" + s.get("githubLogin").getAsString() + ")");
                            nameL.getStyleClass().add("card-subtitle");

                            Region sp = new Region();
                            HBox.setHgrow(sp, Priority.ALWAYS);

                            Button rmBtn = new Button("✕");
                            rmBtn.getStyleClass().add("btn-danger");

                            rmBtn.setStyle("-fx-padding:2 8;-fx-font-size:11px;");
                            long uid = s.get("id").getAsLong();

                            rmBtn.setOnAction(e -> removeStudent(groupId, uid, box));
                            row.getChildren().addAll(nameL, sp, rmBtn);

                            box.getChildren().add(row);
                        });
                    }
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    box.getChildren().clear();
                    Label err = new Label("Помилка: " + e.getMessage());

                    err.getStyleClass().add("field-error");
                    box.getChildren().add(err);
                });
            }
        });
    }

    private void addStudent(long groupId, long userId, VBox studentsBox, VBox card, JsonObject g) {
        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().addStudentToGroup(groupId, userId);
                Platform.runLater(() -> {
                    if (studentsBox.isVisible()) loadStudents(groupId, studentsBox);
                    new Alert(Alert.AlertType.INFORMATION, "Студента додано!", ButtonType.OK).showAndWait();
                });
            } catch (ApiException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                        e.getMessage(), ButtonType.OK).showAndWait());
            }
        });
    }

    private void removeStudent(long groupId, long userId, VBox box) {
        Thread.ofVirtual().start(() -> {
            try {
                ApiClient.get().removeStudentFromGroup(groupId, userId);
                Platform.runLater(() -> loadStudents(groupId, box));
            } catch (ApiException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                        e.getMessage(), ButtonType.OK).showAndWait());
            }
        });
    }

    @FXML
    private void onCreate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Нова група");

        dialog.setHeaderText("Введіть назву групи:");
        dialog.setContentText("Назва:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            Map<String, Object> payload = new HashMap<>();

            payload.put("name", name.trim());
            Thread.ofVirtual().start(() -> {
                try {
                    ApiClient.get().createGroup(payload);
                    Platform.runLater(this::loadData);
                } catch (ApiException e) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                            e.getMessage(), ButtonType.OK).showAndWait());
                }
            });
        });
    }
}
