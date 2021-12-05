package ru.gb.storage.client.ui.controller;

import com.dustinredmond.fxalert.FXAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.client.ui.table.CustomNameCellCallback;
import ru.gb.storage.client.ui.table.CustomSizeCellCallback;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.FileRequestType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
public class ExplorerController implements Initializable {

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
    private Button buttonCopy;

    private Client client;
    private ObservableList<File> localFiles;
    private ObservableList<File> remoteFiles;
    private LocalFileManager localFileManager;
    private ScreenController screenController;
    private long storageId;
    private long rootDirId;
    private long currentRemoteDirId;
    private Stage stage;

    private boolean isActiveLocalTableView = false;
    private boolean isActiveRemoteTableView = false;

    private final LinkedList<IdName> queueRemotePath = new LinkedList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        localTableView.setPlaceholder(new Label("This directory is empty..."));

        remoteTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        remoteTableView.setPlaceholder(new Label("This directory is empty..."));

        localNameTableColumn.setCellFactory(new CustomNameCellCallback());
        localSizeTableColumn.setCellFactory(new CustomSizeCellCallback());

        remoteNameTableColumn.setCellFactory(new CustomNameCellCallback());
        remoteSizeTableColumn.setCellFactory(new CustomSizeCellCallback());

        localFileManager = new LocalFileManager();
        final String userHome = System.getProperty("user.home");
        localFiles = localFileManager.getLocalFiles(userHome);
        pathLocalTextField.setText(userHome);

        localTableView.setItems(localFiles);

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
                        String s = currentRemoteDirId == rootDirId ? "" : "/";
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
        fileRequestMessage.setType(FileRequestType.GET);
        fileRequestMessage.setParentDirId(parentDirId);
        fileRequestMessage.setStorageId(storageId);
        client.sendMessage(fileRequestMessage);
    }

    public void postInit(String login, long storageId, long rootDirId) {
        this.storageId = storageId;
        this.rootDirId = rootDirId;
        stage.setTitle("Netty-network-storage: " + login);
        queueRemotePath.addFirst(new IdName(rootDirId, "/"));
        getRemoteFiles(storageId, rootDirId);
    }

    public void copy() {
        if (isActiveLocalTableView) {
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
                fileRequestMessage.setType(FileRequestType.UPLOAD);
                fileRequestMessage.setFile(localFile);
                fileRequestMessage.setStorageId(storageId);
                fileRequestMessage.setParentDirId(currentRemoteDirId);
                client.sendMessage(fileRequestMessage);
            }
            return;
        }
        if (isActiveRemoteTableView) {
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
                fileRequestMessage.setType(FileRequestType.DOWNLOAD);
                fileRequestMessage.setStorageId(storageId);
                fileRequestMessage.setFile(remoteFile);
                String path = Paths.get(currentLocalPath, remoteFile.getName()).toString();
                fileRequestMessage.setPath(path);
                client.sendMessage(fileRequestMessage);
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
        final String parentPath = getParentPath(pathLocalTextField.getText());
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

    private String getParentPath(String path) {
        try {
            return Paths.get(path + "/..").toRealPath().toString();
        } catch (IOException ignore) {
        }
        return path;
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
                    .append(file.getIsDirectory() ? "/" : "")
                    .append("\n");
        });
        return FXAlert.showConfirmed(message.toString());
    }

    private boolean showDeleteConfirm(List<File> files) {
        StringBuilder message = new StringBuilder();
        message.append("Do you want to move all to the bin?\n\n");
        files.forEach(file -> {
            message
                    .append(" - ")
                    .append(file.getName())
                    .append(file.getIsDirectory() ? "/" : "")
                    .append("\n");
        });
        return FXAlert.showConfirmed(message.toString());
    }

    public void deleteSelectedFiles() {
        if (isActiveLocalTableView) {
            final ObservableList<File> selectedLocalFiles = localTableView.getSelectionModel().getSelectedItems();
            if (selectedLocalFiles.isEmpty()) {
                return;
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
                return;
            }

            final boolean answerDelete = showDeleteConfirm(selectedRemoteFiles);
            if (!answerDelete) {
                return;
            }
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
