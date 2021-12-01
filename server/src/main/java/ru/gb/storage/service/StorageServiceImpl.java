package ru.gb.storage.service;

import lombok.AllArgsConstructor;
import ru.gb.storage.dao.StorageDao;
import ru.gb.storage.io.FileManager;
import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;


@AllArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final StorageDao storageDao;
    private final static long CAPACITY = 5368709120L; // 5 GB

    @Override
    public Storage getStorageById(long id) {
        return storageDao.findById(id).orElse(null);
    }

    @Override
    public Storage getStorageByUser(User user) {
        return storageDao.findStorageByUser(user).orElse(null);
    }

    @Override
    public Storage addNewStorage(User newUser) {
        final Storage newStorage = new Storage();
        newStorage.setCapacity(CAPACITY);
        newStorage.setUserId(newUser.getId());
        final long id = storageDao.save(newStorage);
        newStorage.setId(id);
        return newStorage;
    }

    @Override
    public void update(Storage storage) {
        storageDao.update(storage);
    }
}
