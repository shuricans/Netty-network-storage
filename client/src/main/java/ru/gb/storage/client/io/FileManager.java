package ru.gb.storage.client.io;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.gb.storage.commons.io.File;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {
    public ObservableList<File> getLocalFiles(String path) {
        final ObservableList<File> files = FXCollections.observableArrayList();

        try (final Stream<Path> paths = Files.list(Paths.get(path))) {
            final List<File> fileList = paths
                    .map(pth -> new File(
                            null,
                            pth.getFileName().toString(),
                            pth.toString(),
                            getFileSize(pth),
                            Files.isDirectory(pth),
                            null,
                            null
                    ))
                    .collect(Collectors.toList());
            files.addAll(fileList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    private long getFileSize(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
