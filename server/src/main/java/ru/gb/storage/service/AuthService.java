package ru.gb.storage.service;

import ru.gb.storage.model.User;

public interface AuthService {
    boolean auth(User user);
}
