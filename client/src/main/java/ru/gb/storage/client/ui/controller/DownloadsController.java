package ru.gb.storage.client.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import lombok.Setter;
import ru.gb.storage.client.ui.components.DownloadHBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Setter
public class DownloadsController implements Initializable, LostConnection {

    @FXML
    private VBox rootVBox;

    private ExplorerController explorerController;
    private ScreenController screenController;
    private Map<Long, DownloadHBox> downloadHBoxMap = new HashMap<>();

    public DownloadHBox getDownloadHBox(long fileId) {
        return downloadHBoxMap.get(fileId);
    }

    public void addDownloadHBox(long fileId, DownloadHBox downloadHBox) {
        explorerController.showDownloadSpinner(true);
        downloadHBoxMap.put(fileId, downloadHBox);
        rootVBox.getChildren().add(downloadHBox);
    }

    public void returnToExplorer() {
        screenController.activate("explorer");
    }

    public void clear() {
        downloadHBoxMap.values()
                .stream()
                .filter(downloadHBox -> Double.compare(1d, downloadHBox.getProgressValue()) == 0)
                .forEach(rootVBox.getChildren()::remove);
        downloadHBoxMap.entrySet().removeIf(entry -> Double.compare(1d, entry.getValue().getProgressValue()) == 0);
    }

    public boolean isActiveDownloadsExist() {
        return downloadHBoxMap.values()
                .stream()
                .anyMatch(downloadHBox -> Double.compare(1d, downloadHBox.getProgressValue()) != 0);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @Override
    public void lostConnection() {
        explorerController.lostConnection();
    }
}
