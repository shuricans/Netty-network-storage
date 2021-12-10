package ru.gb.storage.dao;


import ru.gb.storage.commons.io.File;
import ru.gb.storage.model.Storage;

import java.util.List;
import java.util.Optional;

public interface FileDao extends Dao<File> {
    Optional<File> findRootDirByStorage(Storage storage);
    List<File> getFilesByParent(File file);
    List<File> getFilesByStorage(Storage storage);
    void setReadyByFileId(long fileId);
}
