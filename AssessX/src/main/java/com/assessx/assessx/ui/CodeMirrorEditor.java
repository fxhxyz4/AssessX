package com.assessx.assessx.ui;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.Objects;

public class CodeMirrorEditor extends StackPane {

    private final WebView   webView;
    private final WebEngine engine;

    private String  pendingValue = "";
    private boolean ready        = false;

    public CodeMirrorEditor() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        String url = Objects.requireNonNull(
            getClass().getResource("/codemirror/editor.html")
        ).toExternalForm();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                ready = true;
                if (!pendingValue.isEmpty()) {
                    setValueInternal(pendingValue);
                }
            }
        });

        engine.load(url);
        getChildren().add(webView);

        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
    }

    public String getValue() {
        try {
            Object result = engine.executeScript("window.getValue()");
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public void setValue(String value) {
        if (value == null) value = "";
        pendingValue = value;
        try {
            engine.executeScript("window.setValue(`" +
                value.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
                + "`)");
        } catch (Exception ignored) {}
    }

    private void setValueInternal(String value) {
        String escaped = value
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$");
        try {
            engine.executeScript("window.setValue(`" + escaped + "`)");
        } catch (Exception e) {
            System.err.println("[AceEditor] setValue failed: " + e.getMessage());
        }
    }
}
