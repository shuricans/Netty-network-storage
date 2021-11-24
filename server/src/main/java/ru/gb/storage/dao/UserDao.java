package ru.gb.storage.dao;

import ru.gb.storage.model.User;

import java.util.Optional;

public interface UserDao extends Dao<User> {

    Optional<User> findByLogin(String login);

    Optional<User> findByLoginAndPassword(String login, String password);
}