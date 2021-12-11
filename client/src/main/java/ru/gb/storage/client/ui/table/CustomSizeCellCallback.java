package ru.gb.storage.client.ui.table;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import ru.gb.storage.commons.io.File;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;


public class CustomSizeCellCallback implements Callback<TableColumn<File, String>, TableCell<File, String>> {
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
                    long size = param
                            .getTableView().getItems()
                            .get(currentIndex).getSize();
                    if (isDir) {
                        setText("");
                    } else {
                        setText(humanReadableByteCountSI(size));
                    }
                } else {
                    setText("");
                    setTextFill(Color.BLACK);
                }
            }
        };
    }

    public String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
