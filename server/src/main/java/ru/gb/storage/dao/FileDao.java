package ru.gb.storage.dao;

import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;

import java.util.List;

public interface FileDao extends Dao<File> {
    List<File> getFilesByParent(File file);
    List<File> getFilesByStorage(Storage storage);
}
