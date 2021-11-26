package ru.gb.storage.service;

import ru.gb.storage.model.User;

import java.util.List;

public interface UserService {
    User getUserById(long id);
    User getUserByLogin(String login);
    List<User> getAllUsers();
    boolean addNewUser(User user);
    void updateUser(User user);
}
