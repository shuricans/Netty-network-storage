package ru.gb.storage.client.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.Setter;
import ru.gb.storage.client.io.FileManager;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.FileRequestType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

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
    private FileManager fileManager;
    private ScreenController screenController;
    private long storageId;
    private long rootDirId;
    private long currentRemoteDirId;
    private Stage stage;

    private boolean isActiveLocalTableView = false;
    private boolean isActiveRemoteTableView = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localTableView.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

        remoteTableView.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

        localNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        localSizeTableColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        remoteNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        remoteSizeTableColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        fileManager = new FileManager();
        final String userHome = System.getProperty("user.home");
        localFiles = fileManager.getLocalFiles(userHome);
        pathLocalTextField.setText(userHome);

        localTableView.setItems(localFiles);

        localTableView.setRowFactory(tv -> {
            TableRow<File> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    File rowData = row.getItem();
                    reloadLocalFiles(rowData.getPath());
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
            localFiles.addAll(fileManager.getLocalFiles(path));
        } else if (!Files.isRegularFile(p)) {
            System.err.println("Path: [" + path + "] does not exist!");
        }
    }

    public void updateRemoteFiles(List<File> files) {
        remoteFiles.clear();
        remoteFiles.addAll(files);
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
        getRemoteFiles(storageId, rootDirId);
    }

    public void copy() {
        if (isActiveLocalTableView) {
            final ObservableList<File> selectedLocalFiles = localTableView.getSelectionModel().getSelectedItems();
//            for (File localFile : selectedLocalFiles) {
            final File localFile = selectedLocalFiles.stream().findFirst().get();
            final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                fileRequestMessage.setType(FileRequestType.UPLOAD);
                fileRequestMessage.setFile(localFile);
                fileRequestMessage.setStorageId(storageId);
                fileRequestMessage.setParentDirId(currentRemoteDirId);
                client.sendMessage(fileRequestMessage);
//            }
            return;
        }
        if (isActiveRemoteTableView) {
            final ObservableList<File> selectedRemoteFiles = remoteTableView.getSelectionModel().getSelectedItems();
            final File file = selectedRemoteFiles.stream().findFirst().get();

            final FileRequestMessage fileRequestMessage = new FileRequestMessage();
            fileRequestMessage.setType(FileRequestType.DOWNLOAD);
            fileRequestMessage.setFile(file);
            fileRequestMessage.setStorageId(storageId);
            String path = Paths.get(pathLocalTextField.getText(), file.getName()).toString();
            fileRequestMessage.setPath(path);
            client.sendMessage(fileRequestMessage);
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

    private String getParentPath(String path) {
        try {
            return Paths.get(path + "/..").toRealPath().toString();
        } catch (IOException ignore) {}
        return path;
    }
}
