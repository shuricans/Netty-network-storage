package ru.gb.storage.client.ui.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.gb.storage.client.io.FileManager;
import ru.gb.storage.client.io.FileWrapper;

import java.net.URL;
import java.util.ResourceBundle;

public class ExplorerController implements Initializable {

    @FXML
    private TextField pathLocalTextField;
    @FXML
    private TableView<FileWrapper> localTableView;
    @FXML
    private TableColumn<FileWrapper, String> localNameTableColumn;
    @FXML
    private TableColumn<FileWrapper, String> localSizeTableColumn;

    @FXML
    private TextField pathRemoteTextField;
    @FXML
    private TableView<FileWrapper> remoteTableView;
    @FXML
    private TableColumn<FileWrapper, String> remoteNameTableColumn;
    @FXML
    private TableColumn<FileWrapper, String> remoteSizeTableColumn;

    private ObservableList<FileWrapper> files;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        localSizeTableColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        remoteNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        remoteNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        final FileManager fileManager = new FileManager();
        final String userHomeDir = System.getProperty("user.home");
        pathLocalTextField.setText(userHomeDir);
        files = fileManager.getFiles(userHomeDir);
        localTableView.setItems(files);
    }

    public void addItem() {
        final FileWrapper fileWrapper = new FileWrapper();
        fileWrapper.setName("------------------------------");
        fileWrapper.setSize(0L);
        files.add(fileWrapper);
    }
}
