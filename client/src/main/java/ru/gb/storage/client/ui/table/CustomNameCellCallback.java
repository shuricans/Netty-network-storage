package ru.gb.storage.client.ui.table;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.commons.io.File;


public class CustomNameCellCallback implements Callback<TableColumn<File, String>, TableCell<File, String>> {
    @Override
    public TableCell<File, String> call(TableColumn<File, String> param) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                if (!empty) {
                    int currentIndex = indexProperty()
                            .getValue() < 0 ? 0
                            : indexProperty().getValue();
                    boolean isDir = param
                            .getTableView().getItems()
                            .get(currentIndex).getIsDirectory();
                    boolean isReady = param
                            .getTableView().getItems()
                            .get(currentIndex).getIsReady();
                    String name = param
                            .getTableView().getItems()
                            .get(currentIndex).getName();
                    if (isDir) {
                        setText(name + LocalFileManager.FS_SEPARATOR);
                        setTextFill(Color.PURPLE);
                    } else {
                        setText(name);
                        if (isReady) {
                            setTextFill(Color.BLACK);
                        } else {
                            setTextFill(Color.RED);
                        }
                    }
                } else {
                    setText("");
                    setTextFill(Color.BLACK);
                }
            }
        };
    }
}
