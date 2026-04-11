package com.assessx.assessx.controller.pages;

import com.assessx.assessx.api.ApiClient;
import com.assessx.assessx.api.ApiException;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class GroupsController extends BasePage {

    @FXML private VBox  groupsList;
    @FXML private VBox  groupDetail;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;

    private List<JsonObject> allGroups = new ArrayList<>();
    private JsonObject selectedGroup;

    @FXML
    public void initialize() {
        loadGroups();
    }

    private void loadGroups() {
        runAsync(() -> {
            try {
                allGroups = ApiClient.get().getGroups();
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    groupsList.getChildren().clear();

                    if (allGroups.isEmpty()) {
                        Label empty = new Label("Групп немає");
                        empty.setStyle("-fx-text-fill: #8b949e; -fx-padding: 8 0;");
                        groupsList.getChildren().add(empty);
                        return;
                    }

                    for (JsonObject g : allGroups) {
                        Button btn = new Button(str(g, "name"));
                        btn.getStyleClass().add("nav-btn");

                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setOnAction(e -> selectGroup(g));

                        groupsList.getChildren().add(btn);
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

    private void selectGroup(JsonObject g) {
        selectedGroup = g;
        long groupId = lng(g, "id");

        String groupName = str(g, "name");

        groupDetail.getChildren().clear();

        Label title = new Label(groupName);
        title.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 0 16 0;");

        ProgressIndicator localSpinner = new ProgressIndicator();
        localSpinner.setMaxSize(28, 28);
        groupDetail.getChildren().addAll(title, localSpinner);

        Thread.ofVirtual().start(() -> {
            try {
                List<JsonObject> students = ApiClient.get().getGroupStudents(groupId);
                Platform.runLater(() -> {
                    groupDetail.getChildren().remove(localSpinner);

                    Label studHeader = sectionHeader("СТУДЕНТИ (" + students.size() + ")");
                    groupDetail.getChildren().add(studHeader);

                    if (students.isEmpty()) {
                        Label noStudents = new Label("У групі немає студентів");
                        noStudents.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");
                        groupDetail.getChildren().add(noStudents);
                    } else {
                        for (JsonObject s : students) {
                            groupDetail.getChildren().add(buildStudentRow(s, groupId));
                        }
                    }

                    Label addHeader = sectionHeader("ДОДАТИ СТУДЕНТА");
                    groupDetail.getChildren().add(addHeader);

                    HBox addRow = buildAddStudentRow(groupId);
                    groupDetail.getChildren().add(addRow);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> showError(errorLabel, e.getMessage()));
            }
        });
    }

    private HBox buildStudentRow(JsonObject student, long groupId) {
        Label name = new Label(str(student, "name"));
        name.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 14px;");

        Label login = new Label("@" + str(student, "githubLogin"));
        login.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-padding: 0 0 0 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("Видалити");
        removeBtn.getStyleClass().add("btn-danger");

        removeBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
        removeBtn.setOnAction(e -> {
            long userId = lng(student, "id");
            Thread.ofVirtual().start(() -> {
                try {
                    ApiClient.get().removeStudentFromGroup(groupId, userId);
                    Platform.runLater(() -> selectGroup(selectedGroup));
                } catch (ApiException ex) {
                    Platform.runLater(() -> showError(errorLabel, ex.getMessage()));
                }
            });
        });

        HBox row = new HBox(8, name, login, spacer, removeBtn);
        row.getStyleClass().add("list-row");

        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildAddStudentRow(long groupId) {
        TextField idField = new TextField();
        idField.setPromptText("ID студента");

        idField.getStyleClass().add("input-field");
        idField.setPrefWidth(120);

        HBox.setHgrow(idField, Priority.ALWAYS);

        Button addBtn = new Button("Додати");
        addBtn.getStyleClass().add("btn-primary");

        addBtn.setOnAction(e -> {
            String text = idField.getText().trim();
            if (text.isBlank()) return;

            try {
                long userId = Long.parseLong(text);
                Thread.ofVirtual().start(() -> {
                    try {
                        ApiClient.get().addStudentToGroup(groupId, userId);
                        Platform.runLater(() -> {
                            idField.clear();
                            selectGroup(selectedGroup);
                        });
                    } catch (ApiException ex) {
                        Platform.runLater(() -> showError(errorLabel, ex.getMessage()));
                    }
                });
            } catch (NumberFormatException ex) {
                showError(errorLabel, "ID має бути числом");
            }
        });

        return new HBox(8, idField, addBtn);
    }

    @FXML
    protected void onCreateGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Нова група");

        dialog.setHeaderText("Введіть назву групи:");
        dialog.setContentText("Назва:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            Thread.ofVirtual().start(() -> {
                try {
                    ApiClient.get().createGroup(java.util.Map.of("name", name));
                    Platform.runLater(this::loadGroups);
                } catch (ApiException e) {
                    Platform.runLater(() -> showError(errorLabel, e.getMessage()));
                }
            });
        });
    }
}
