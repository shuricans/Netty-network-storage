package ru.gb.storage.client.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import lombok.Setter;
import ru.gb.storage.client.ui.components.DownloadHBox;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
        final DownloadHBox prevHBox = downloadHBoxMap.get(fileId);
        if (prevHBox != null) {
            rootVBox.getChildren().remove(prevHBox);
        }
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
                .filter(DownloadHBox::isDone)
                .forEach(rootVBox.getChildren()::remove);
        downloadHBoxMap.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    public boolean isActiveDownloadsExist() {
        return downloadHBoxMap.values()
                .stream()
                .anyMatch(downloadHBox -> !downloadHBox.isDone());
    }

    public List<String> getAllActiveDownloadsPaths() {
        return downloadHBoxMap.values()
                .stream()
                .filter(dhb -> "download".equals(dhb.getType()) && !dhb.isDone())
                .map(DownloadHBox::getPath)
                .collect(Collectors.toList());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @Override
    public void lostConnection() {
        explorerController.lostConnection();
    }
}
