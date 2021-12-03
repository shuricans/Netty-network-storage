package ru.gb.storage.client.ui.controller;

import javafx.application.Platform;
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
import ru.gb.storage.commons.message.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

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
    private BlockingQueue<Message> messagesQueue;
    private final BlockingQueue<File> uploadQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<File> downloadQueue = new LinkedBlockingQueue<>();
    private FileManager fileManager;
    private ScreenController screenController;
    private long storageId;
    private long rootDirId;
    private long currentRemoteDirId;
    private Stage stage;
    private ExecutorService executor;

    private boolean isActiveLocalTableView = false;
    private boolean isActiveRemoteTableView = false;


    private static final int BUFFER_SIZE = 64 * 1024;

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

    private void getRemoteFiles(long storageId, long parentDirId) {
        currentRemoteDirId = parentDirId;
        final FileRequestMessage fileRequestMessage = new FileRequestMessage();
        fileRequestMessage.setType(FileRequestType.GET);
        fileRequestMessage.setParentDirId(parentDirId);
        fileRequestMessage.setStorageId(storageId);
        client.sendMessage(fileRequestMessage);
    }

    public void postInit(String login, long storageId, long rootDirId) {
        read();
        this.storageId = storageId;
        this.rootDirId = rootDirId;
        stage.setTitle("Netty-network-storage: " + login);
        getRemoteFiles(storageId, rootDirId);
    }

    private void read() {
        final ReaderService readerService = new ReaderService(messagesQueue);

        readerService.valueProperty().addListener((observableValue, prevMessage, newMessage) -> {
            if (newMessage instanceof ListFilesMessage) {
                var message = (ListFilesMessage) newMessage;
                final List<File> remoteFiles = message.getFiles();
                updateRemoteFiles(remoteFiles);
            }
            if (newMessage instanceof FileRequestMessage) {
                var message = (FileRequestMessage) newMessage;
                switch (message.getType()) {
                    case UPLOAD:
                        executor.execute(new FileUploader(client, message));
                }
            }
            if (newMessage instanceof FileTransferMessage) {
                var message = (FileTransferMessage) newMessage;
                Platform.runLater(() -> {
                    try(RandomAccessFile raf = new RandomAccessFile(message.getPath(), "rw")) {
                        raf.seek(message.getStartPosition());
                        raf.write(message.getContent());
                        if (message.getIsDone()) {
                            System.out.println("File transfer is finished");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        readerService.start();
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

    @AllArgsConstructor
    private static class FileUploader implements Runnable {

        private final Client client;
        private final FileRequestMessage message;

        @Override
        public void run() {
            try (final RandomAccessFile raf = new RandomAccessFile(message.getFile().getPath(), "r")) {
                final long fileLength = raf.length();
                boolean isDone = false;
                do {
                    final long filePointer = raf.getFilePointer();
                    final long availableBytes = fileLength - filePointer;

                    byte[] buffer;

                    if (availableBytes >= BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                    } else {
                        buffer = new byte[(int) availableBytes];
                        isDone = true;
                    }

                    raf.read(buffer);

                    final FileTransferMessage fileTransferMessage = new FileTransferMessage();

                    fileTransferMessage.setContent(buffer);
                    fileTransferMessage.setStartPosition(filePointer);
                    fileTransferMessage.setIsDone(isDone);
                    fileTransferMessage.setPath(message.getPath());

                    client.sendMessage(fileTransferMessage).sync();
                } while (raf.getFilePointer() < fileLength);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
