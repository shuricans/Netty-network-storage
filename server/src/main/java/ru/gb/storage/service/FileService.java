package ru.gb.storage.service;

import ru.gb.storage.commons.io.File;
import ru.gb.storage.model.Storage;

import java.util.List;

public interface FileService {
    File getFileById(long id);
    List<File> getFilesByDir(File directory);
    List<File> getFilesByStorage(Storage storage);
    File getRootDir(Storage storage);
    long addNewFile(File file);
    void update(File file);
    void setReadyByFileId(long fileId);
    void delete(File file);
}
