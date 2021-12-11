package ru.gb.storage.client.ui.controller;

import com.dustinredmond.fxalert.FXAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.client.netty.ClientService;
import ru.gb.storage.client.ui.table.CustomNameCellCallback;
import ru.gb.storage.client.ui.table.CustomSizeCellCallback;
import ru.gb.storage.client.utils.ConfigProperties;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.FileRequestMessage;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Setter
public class ExplorerController implements Initializable, LostConnection {

    @FXML
    private TextField pathLocalTextField;
    @FXML
    private TableView<File> localTableView;
    @FXML
    private TableColumn<File, String> localNameTableColumn;
    @FXML
    private TableColumn<File, String> localSizeTableColumn;

    @FXML
    private TextField pathRemoteTextField;
    @FXML
    private TableView<File> remoteTableView;
    @FXML
    private TableColumn<File, String> remoteNameTableColumn;
    @FXML
    private TableColumn<File, String> remoteSizeTableColumn;
    @FXML
    private HBox downloadHBox;

    @FXML
    private Button buttonCopy;

    private ClientService clientService;
    private ObservableList<File> localFiles;
    private ObservableList<File> remoteFiles;
    private LocalFileManager localFileManager;
    private ScreenController screenController;
    private long storageId;
    private long rootDirId;
    private long currentRemoteDirId;
    private Stage stage;
    private LoginController loginController;
    private DownloadsController downloadsController;

    private boolean isActiveLocalTableView = false;
    private boolean isActiveRemoteTableView = false;

    private boolean downloadSpinnerExist = false;

    private final LinkedList<IdName> queueRemotePath = new LinkedList<>();

    private ProgressIndicator downloadSpinner;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        localTableView.setPlaceholder(new Label("This directory is empty..."));

        remoteTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        remoteTableView.setPlaceholder(new Label("This directory is empty..."));


        localNameTableColumn.setCellFactory(new CustomNameCellCallback());
        localSizeTableColumn.setCellFactory(new CustomSizeCellCallback());
        localNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));


        remoteNameTableColumn.setCellFactory(new CustomNameCellCallback());
        remoteSizeTableColumn.setCellFactory(new CustomSizeCellCallback());
        remoteNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        localFileManager = new LocalFileManager();

        localTableView.setRowFactory(tv -> {
            TableRow<File> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    File localFile = row.getItem();
                    if (localFile.getIsDirectory()) {
                        reloadLocalFiles(localFile.getPath());
                    }
                }
            });
            return row;
        });

        remoteTableView.setRowFactory(tv -> {
            TableRow<File> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    File remoteFile = row.getItem();
                    if (remoteFile.getIsDirectory()) {
                        String s = currentRemoteDirId == rootDirId ? "" : LocalFileManager.FS_SEPARATOR;
                        queueRemotePath.addLast(
                                new IdName(
                                        remoteFile.getId(),
                                        s + remoteFile.getName()
                                )
                        );
                        getRemoteFiles(storageId, remoteFile.getId());
                    }
                }
            });
            return row;
        });

        remoteFiles = FXCollections.observableArrayList();
        remoteTableView.setItems(remoteFiles);

        downloadSpinner = new ProgressIndicator();
        downloadSpinner.setPrefWidth(23);
        downloadSpinner.setPrefHeight(18);
    }

    private void reloadLocalFiles(String path) {
        Path p = Paths.get(path);
        if (Files.exists(p) && Files.isDirectory(p)) {
            pathLocalTextField.setText(path);
            localFiles.clear();
            localFiles.addAll(localFileManager.getLocalFiles(path));
        } else if (!Files.isRegularFile(p)) {
            FXAlert.showError("Path: [" + path + "] does not exist!");
        }
    }

    public void updateRemoteFiles(List<File> files) {
        remoteFiles.clear();
        remoteFiles.addAll(files);
        updatePathRemoteTextField();
    }

    public void pathLocalTextFieldOnAction() {
        final String pathLocal = pathLocalTextField.getText();
        reloadLocalFiles(pathLocal);
    }

    public void getRemoteFiles(long storageId, long parentDirId) {
        currentRemoteDirId = parentDirId;
        final FileRequestMessage fileRequestMessage = new FileRequestMessage();
        fileRequestMessage.setType(FileRequestMessage.Type.GET);
        fileRequestMessage.setParentDirId(parentDirId);
        fileRequestMessage.setStorageId(storageId);
        clientService.sendMessage(fileRequestMessage);
    }

    public void postInit(String login, long storageId, long rootDirId) {
        this.storageId = storageId;
        this.rootDirId = rootDirId;
        final String prevTitle = stage.getTitle();
        stage.setTitle(prevTitle + ": " + login);
        queueRemotePath.addFirst(new IdName(rootDirId, LocalFileManager.FS_SEPARATOR));
        getRemoteFiles(storageId, rootDirId);

        final String userHome = System.getProperty("user.home");
        localFiles = localFileManager.getLocalFiles(userHome);
        pathLocalTextField.setText(userHome);

        localTableView.setItems(localFiles);
    }

    public void copy() {
        if (isActiveLocalTableView) { // user wants upload this selected items to server
            final ObservableList<File> selectedLocalFiles = localTableView.getSelectionModel().getSelectedItems();
            if (selectedLocalFiles.isEmpty()) {
                return;
            }

            final Set<File> collisions = getCollisionsBeforeCopy(remoteFiles, selectedLocalFiles);

            // if we have collisions, do nothing for now
            if (!collisions.isEmpty()) {
                final boolean answerRewrite = showCollisionsConfirm(collisions);
                if (answerRewrite) {
                    //TODO move this files to bin on server
                    FXAlert.showError("This feature currently not available.");
                    return;
                } else {
                    return;
                }
            }

            for (File localFile : selectedLocalFiles) {
                final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                fileRequestMessage.setFile(localFile);
                fileRequestMessage.setType(FileRequestMessage.Type.UPLOAD);
                fileRequestMessage.setParentDirId(currentRemoteDirId);
                fileRequestMessage.setStorageId(storageId);
                if (LocalFileManager.FS_SEPARATOR.equals(pathRemoteTextField.getText())) {
                    fileRequestMessage.setDestPath(LocalFileManager.FS_SEPARATOR + localFile.getName());
                } else {
                    fileRequestMessage.setDestPath(Path.of(pathRemoteTextField.getText(), localFile.getName()).toString());
                }
                clientService.sendMessage(fileRequestMessage);
            }
            return;
        }
        if (isActiveRemoteTableView) { // user wants download this selected items from server
            final ObservableList<File> selectedRemoteFiles = remoteTableView.getSelectionModel().getSelectedItems();
            if (selectedRemoteFiles.isEmpty()) {
                return;
            }

            final Set<File> collisions = getCollisionsBeforeCopy(localFiles, selectedRemoteFiles);

            // if we have collisions, do nothing for now
            if (!collisions.isEmpty()) {
                final boolean answerRewrite = showCollisionsConfirm(collisions);
                if (answerRewrite) {
                    //TODO move this files to bin on client
                    FXAlert.showError("This feature currently not available.");
                    return;
                } else {
                    return;
                }
            }

            final String currentLocalPath = pathLocalTextField.getText();
            for (File remoteFile : selectedRemoteFiles) {
                final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                fileRequestMessage.setFile(remoteFile);
                fileRequestMessage.setType(FileRequestMessage.Type.DOWNLOAD);
                fileRequestMessage.setParentDirId(currentRemoteDirId);
                fileRequestMessage.setStorageId(storageId);
                String destPath = Paths.get(currentLocalPath, remoteFile.getName()).toString();
                fileRequestMessage.setDestPath(destPath);
                fileRequestMessage.setRealPath(remoteFile.getPath());
                clientService.sendMessage(fileRequestMessage);
            }
        }
    }

    public void lastFocusLocalTable() {
        isActiveLocalTableView = true;
        isActiveRemoteTableView = false;
        remoteTableView.getSelectionModel().clearSelection();
    }

    public void lastFocusRemoteTable() {
        isActiveRemoteTableView = true;
        isActiveLocalTableView = false;
        localTableView.getSelectionModel().clearSelection();
    }

    public void localBack() {
        final String parentPath = localFileManager.getParentPath(pathLocalTextField.getText());
        reloadLocalFiles(parentPath);
    }

    public void remoteBack() {
        if (queueRemotePath.size() > 1) {
            queueRemotePath.pollLast();
            getRemoteFiles(storageId, queueRemotePath.peekLast().getId());
        }
    }

    private void updatePathRemoteTextField() {
        StringBuilder sb = new StringBuilder();
        queueRemotePath.forEach(item -> sb.append(item.getName()));
        pathRemoteTextField.setText(sb.toString());
    }

    public void refreshLocal() {
        reloadLocalFiles(pathLocalTextField.getText());
    }

    public void refreshRemote() {
        getRemoteFiles(storageId, currentRemoteDirId);
    }

    private Set<File> getCollisionsBeforeCopy(List<File> selectedFiles, List<File> oppositeDirFiles) {
        return selectedFiles
                .stream()
                .distinct()
                .filter(oppositeDirFiles::contains)
                .collect(Collectors.toSet());
    }

    private boolean showCollisionsConfirm(Set<File> duplicates) {
        StringBuilder message = new StringBuilder();
        message.append("This files/folders already exists in opposite directory.\n");
        message.append("Do you want to replace them with the ones you are copying?\n\n");
        duplicates.forEach(file -> {
            message
                    .append(" - ")
                    .append(file.getName())
                    .append(file.getIsDirectory() ? LocalFileManager.FS_SEPARATOR : "")
                    .append("\n");
        });
        return FXAlert.showConfirmed(message.toString());
    }

    private boolean showDeleteConfirm(List<File> files) {
        StringBuilder message = new StringBuilder();
        message.append("Do you want to delete them all?\n\n");
        files.forEach(file -> {
            message
                    .append(" - ")
                    .append(file.getName())
                    .append(file.getIsDirectory() ? LocalFileManager.FS_SEPARATOR : "")
                    .append("\n");
        });
        return FXAlert.showConfirmed(message.toString());
    }

    public void deleteSelectedFiles() {
        if (isActiveLocalTableView) {
            final ObservableList<File> selectedLocalFiles = localTableView.getSelectionModel().getSelectedItems();
            if (selectedLocalFiles.isEmpty()) {
                FXAlert.showInfo("Please, choose files to delete.");
                return;
            } else {
                selectedLocalFiles.removeIf(file -> !file.getIsReady());
                if (selectedLocalFiles.isEmpty()) {
                    FXAlert.showInfo("Incompletely downloaded files cannot be deleted.");
                    return;
                }
            }

            final boolean answerDelete = showDeleteConfirm(selectedLocalFiles);
            if (!answerDelete) {
                return;
            }

            try {
                localFileManager.moveFilesToTheBin(selectedLocalFiles);
            } catch (Exception e) {
                FXAlert.showException(e, "Something went wrong...");
            } finally {
                refreshLocal();
            }
            return;
        }
        if (isActiveRemoteTableView) {
            final ObservableList<File> selectedRemoteFiles = remoteTableView.getSelectionModel().getSelectedItems();
            if (selectedRemoteFiles.isEmpty()) {
                FXAlert.showInfo("Please, choose files to delete.");
                return;
            } else {
                selectedRemoteFiles.removeIf(file -> !file.getIsReady());
                if (selectedRemoteFiles.isEmpty()) {
                    FXAlert.showInfo("Incompletely uploaded files cannot be deleted.");
                    return;
                }
            }

            final boolean answerDelete = showDeleteConfirm(selectedRemoteFiles);
            if (!answerDelete) {
                return;
            }

            for (File remoteFile : selectedRemoteFiles) {
                final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                fileRequestMessage.setFile(remoteFile);
                fileRequestMessage.setType(FileRequestMessage.Type.DELETE);
                clientService.sendMessage(fileRequestMessage);
            }
        }
    }

    public void open() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
        final java.io.File directory = directoryChooser.showDialog(stage);
        reloadLocalFiles(directory.getAbsolutePath());
    }

    public void logout() {
        pathLocalTextField.clear();
        pathRemoteTextField.clear();
        queueRemotePath.clear();
        localFiles.clear();
        remoteFiles.clear();
        final Optional<String> title = ConfigProperties.getPropertyValue("title");
        title.ifPresent(stage::setTitle);
        screenController.activate("login");
    }

    public void close() {
        stage.close();
    }

    public void openGithub() {
        final Optional<String> github = ConfigProperties.getPropertyValue("github");
        github.ifPresent(url -> {
            try {
                Desktop.getDesktop().browse(new URL(url).toURI());
            } catch (IOException | URISyntaxException e) {
                FXAlert.showInfo(url);
            }
        });
    }

    @Override
    public void lostConnection() {
        loginController.lostConnection();
        logout();
    }

    public void showDownloadsDetails() {
        screenController.activate("downloads");
    }

    public void showDownloadSpinner(boolean flag) {
        final boolean activeDownloadsExist = downloadsController.isActiveDownloadsExist();
        if (!activeDownloadsExist && flag && !downloadSpinnerExist) {
            downloadSpinnerExist = true;
            downloadHBox.getChildren().add(downloadSpinner);
        } else if (downloadSpinnerExist && !activeDownloadsExist) {
            downloadSpinnerExist = false;
            downloadHBox.getChildren().remove(downloadSpinner);
        }
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class IdName {
        private long id;
        private String name;
    }
}
