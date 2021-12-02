package ru.gb.storage.client.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Setter;
import ru.gb.storage.client.io.FileManager;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.FileRequestType;
import ru.gb.storage.commons.message.ListFilesMessage;
import ru.gb.storage.commons.message.Message;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;

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

    private Client client;
    private ObservableList<File> localFiles;
    private ObservableList<File> remoteFiles;
    private BlockingQueue<Message> messagesQueue;
    private FileManager fileManager;
    private ScreenController screenController;
    private long storageId;
    private long rootDirId;
    private Stage stage;

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
                    getLocalFiles(rowData.getPath());
                }
            });
            return row;
        });

        remoteFiles = FXCollections.observableArrayList();
        remoteTableView.setItems(remoteFiles);

    }

    private void getLocalFiles(String path) {
        Path p = Paths.get(path);
        if (Files.isDirectory(p)) {
            pathLocalTextField.setText(path);
            localFiles.clear();
            localFiles.addAll(fileManager.getLocalFiles(path));
        } else if (!Files.isRegularFile(p)) {
            System.err.println("Path: [" + path + "] does not exist!");
        }
    }

    private void updateRemoteFiles(List<File> files) {
        remoteFiles.clear();
        remoteFiles.addAll(files);
    }

    public void pathLocalTextFieldOnAction() {
        final String pathLocal = pathLocalTextField.getText();
        getLocalFiles(pathLocal);
    }

    private void sendFileRequestMessage(long storageId, long parentId) {
        final FileRequestMessage fileRequestMessage = new FileRequestMessage();
        fileRequestMessage.setType(FileRequestType.GET);
        fileRequestMessage.setFileId(parentId);
        fileRequestMessage.setStorageId(storageId);
        client.sendMessage(fileRequestMessage);
    }

    public void postInit(String login, long storageId, long rootDirId) {
        read();
        this.storageId = storageId;
        this.rootDirId = rootDirId;
        stage.setTitle("Netty-network-storage: " + login);
        sendFileRequestMessage(storageId, rootDirId);
    }

    private void read() {
        final ReaderService readerService = new ReaderService(messagesQueue);

        readerService.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue instanceof ListFilesMessage) {
                var message = (ListFilesMessage) newValue;
                final List<File> remoteFiles = message.getFiles();
                updateRemoteFiles(remoteFiles);
            }
        });

        readerService.start();
    }

    @AllArgsConstructor
    private static class ReaderService extends Service<Message> {

        private final BlockingQueue<Message> messagesQueue;

        @Override
        protected Task<Message> createTask() {
            return new Task<>() {
                @Override
                protected Message call() throws Exception {
                    while (true) {
                        final Message msg = messagesQueue.take(); // wait incoming message
                        updateValue(msg);
                    }
                }
            };
        }
    }

}
