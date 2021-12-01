package ru.gb.storage.service;

import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;

public interface StorageService {
    Storage getStorageById(long id);
    Storage getStorageByUser(User user);
    Storage addNewStorage(User newUser);
    void update(Storage storage);
}
