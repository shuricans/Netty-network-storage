package ru.gb.storage.client.ui.components;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DownloadHBox extends HBox {

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final String type;
    private final String path;
    private final ProgressIndicator progressIndicator = new ProgressIndicator(.0);
    private boolean isDone = false;

    public DownloadHBox(String type, String path) {
        this.type = type;
        this.path = path;
        init();
    }

    public final void setProgressValue(double value) {
        progress.set(value);
    }

    private void init() {
        setPrefHeight(50);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(5d));
        progressIndicator.setMinWidth(27d);
        progressIndicator.progressProperty().bind(progress);
        getChildren().add(progressIndicator);
        final Separator s1 = new Separator(Orientation.VERTICAL);
        s1.setPadding(new Insets(0d, 0d, 0d, 3d));
        getChildren().add(s1);
        final Label labelType = new Label(type);
        labelType.setMinWidth(52d);
        labelType.setFont(new Font(18d));
        getChildren().add(labelType);
        final Separator s2 = new Separator(Orientation.VERTICAL);
        s2.setPadding(new Insets(0d, 0d, 0d, 3d));
        getChildren().add(s2);
        final Label labelPath = new Label(path);
        labelPath.setFont(new Font(18d));
        getChildren().add(labelPath);
    }

}
