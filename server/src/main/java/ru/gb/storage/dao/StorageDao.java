package ru.gb.storage.dao;

import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;

import java.util.Optional;

public interface StorageDao extends Dao<Storage> {
    Optional<Storage> findStorageByUser(User user);
}
