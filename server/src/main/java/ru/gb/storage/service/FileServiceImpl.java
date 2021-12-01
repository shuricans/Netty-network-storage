package ru.gb.storage.service;

import lombok.AllArgsConstructor;
import ru.gb.storage.dao.FileDao;
import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;

import java.util.List;

@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileDao fileDao;

    @Override
    public File getFileById(long id) {
        return fileDao.findById(id).orElse(null);
    }

    @Override
    public List<File> getFilesByDir(File directory) {
        return fileDao.getFilesByParent(directory);
    }

    @Override
    public List<File> getFilesByStorage(Storage storage) {
        return fileDao.getFilesByStorage(storage);
    }

    @Override
    public long addNewFile(File file) {
        return fileDao.save(file);
    }

    @Override
    public void update(File file) {
        fileDao.update(file);
    }
}
