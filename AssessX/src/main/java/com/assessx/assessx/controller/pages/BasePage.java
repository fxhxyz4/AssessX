package com.assessx.assessx.controller.pages;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BasePage {

    protected static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    protected void setSpinner(ProgressIndicator spinner, boolean visible) {
        if (spinner != null) {
            spinner.setVisible(visible);
            spinner.setManaged(visible);
        }
    }

    protected void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);

            errorLabel.setManaged(true);
        }
    }

    protected void hideError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    protected String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    protected long lng(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
    }

    protected int num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
    }

    protected String formatDate(JsonObject o, String key) {
        String raw = str(o, key);
        if (raw.isBlank()) return "—";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.length() > 19 ? raw.substring(0, 19) : raw,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            return dt.format(DATE_FMT);
        } catch (Exception e) {
            return raw;
        }
    }

    /** Row: left label + right label */
    protected HBox infoRow(String left, String right) {
        Label l = new Label(left);
        l.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        Label r = new Label(right);
        r.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(l, spacer, r);
        row.setStyle("-fx-padding: 4 0;");

        return row;
    }

    protected Label badge(String text, String colorClass) {
        Label b = new Label(text);
        b.getStyleClass().addAll("badge", colorClass);

        return b;
    }

    protected Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 12 0 4 0;");

        return l;
    }

    protected VBox card(Node... children) {
        VBox v = new VBox(8);
        v.getStyleClass().add("card");

        v.getChildren().addAll(children);
        return v;
    }

    protected void runAsync(Runnable task, ProgressIndicator spinner, Label errorLabel) {
        Platform.runLater(() -> setSpinner(spinner, true));
        Thread.ofVirtual().start(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setSpinner(spinner, false);
                    showError(errorLabel, "Помилка: " + e.getMessage());
                });
            }
        });
    }
}
