package ru.gb.storage.service;

import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;

import java.util.List;

public interface FileService {
    File getFileById(long id);
    List<File> getFilesByDir(File directory);
    List<File> getFilesByStorage(Storage storage);
    long addNewFile(File file);
    void update(File file);
}
