package ru.gb.storage.client.io;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {
    public ObservableList<FileWrapper> getFiles(String path) {
        final ObservableList<FileWrapper> files = FXCollections.observableArrayList();

        final Path p = Paths.get(path);
        try (final Stream<Path> list = Files.list(p)) {
            final Collection<Path> paths = list.collect(Collectors.toList());
            for (Path pth : paths) {
                final FileWrapper fileWrapper = new FileWrapper();

                fileWrapper.setName(pth.getFileName().toString());
                fileWrapper.setPath(pth.toAbsolutePath().toString());
                final boolean isDirectory = Files.isDirectory(pth);
                fileWrapper.setDirectory(isDirectory);
                if (!isDirectory) {
                    fileWrapper.setSize(Files.size(pth) / 1024); // kB
                }
                fileWrapper.setP(pth);
                files.add(fileWrapper);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }
}
