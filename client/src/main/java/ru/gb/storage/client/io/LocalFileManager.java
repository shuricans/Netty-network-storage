package ru.gb.storage.client.io;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import ru.gb.storage.client.ui.controller.DownloadsController;
import ru.gb.storage.commons.io.File;

import java.awt.*;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class LocalFileManager {

    public static String FS_SEPARATOR = FileSystems.getDefault().getSeparator();
    private final DownloadsController downloadsController;

    public ObservableList<File> getLocalFiles(String path) {
        final ObservableList<File> files = FXCollections.observableArrayList();
        final List<String> activeDownloadsPaths = downloadsController.getAllActiveDownloadsPaths();
        System.out.println();
        System.out.println();
        activeDownloadsPaths.forEach(System.out::println);
        System.out.println();
        System.out.println();
        try (final Stream<Path> paths = Files.list(Paths.get(path))) {
            final List<File> fileList = paths
                    .map(pth -> new File(
                            null,
                            pth.getFileName().toString(),
                            pth.toString(),
                            getFileSize(pth),
                            Files.isDirectory(pth),
                            true,
                            null,
                            null
                    ))
                    .filter(file -> {
                        if (file.getIsDirectory()) {
                            return true;
                        }
                        if (activeDownloadsPaths.contains(file.getPath())) {
                            file.setIsReady(false);
                        }
                        return true;
                    })
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

    public void moveFilesToTheBin(List<File> selectedLocalFiles) {
        for (File file : selectedLocalFiles) {
            Desktop.getDesktop().moveToTrash(Path.of(file.getPath()).toFile());
        }
    }

    public String getParentPath(String path) {
        try {
            return Paths.get(path + "/..").toRealPath().toString();
        } catch (IOException ignore) {
        }
        return path;
    }
}
