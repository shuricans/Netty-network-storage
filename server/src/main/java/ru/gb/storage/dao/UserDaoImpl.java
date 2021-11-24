package ru.gb.storage.dao;

import ru.gb.storage.model.User;

import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    @Override
    public Optional<User> findById(long id) {
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return null;
    }

    @Override
    public boolean save(User user) {
        return false;
    }

    @Override
    public void update(User user) {

    }

    @Override
    public void delete(User user) {

    }

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByLoginAndPassword(String login, String password) {
        return Optional.empty();
    }
}
