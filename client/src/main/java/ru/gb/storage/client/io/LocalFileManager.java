package ru.gb.storage.client.io;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import ru.gb.storage.commons.io.File;

import java.awt.*;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileManager {

    public static String FS_SEPARATOR = FileSystems.getDefault().getSeparator();
    @Getter
    private final Map<Path, List<File>> prevStateDir = new HashMap<>();

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
                            true, //TODO
                            null,
                            null
                    ))
                    .filter(file -> {
                        if (file.getIsDirectory()) {
                            return true;
                        }
                        final List<File> prevFiles = prevStateDir.get(Path.of(path));
                        if (prevFiles != null) {
                            if (!prevFiles.contains(file)) {
                                file.setIsReady(false);
                            } else {
                                prevFiles.stream()
                                        .filter(f -> f.equals(file))
                                        .findFirst()
                                        .ifPresent(prevFile -> {
                                            file.setIsReady(prevFile.getIsReady());
                                        });
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            files.addAll(fileList);
            prevStateDir.put(Path.of(path), fileList);
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
}
