package ru.gb.storage.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.gb.storage.dao.UserDao;
import ru.gb.storage.model.User;

import java.util.List;

@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao dao;
    private final PasswordEncoder encoder;

    @Override
    public User getUserById(long id) {
        return dao.findById(id).orElse(null);
    }

    @Override
    public User getUserByLogin(String login) {
        return dao.findByLogin(login).orElse(null);
    }

    @Override
    public List<User> getAllUsers() {
        return dao.findAll();
    }

    @Override
    public boolean addNewUser(User user) {
        String rawPass = user.getPassword();
        user.setPassword(encoder.encode(rawPass));
        return dao.save(user);
    }

    @Override
    public void updateUser(User user) {
        dao.update(user);
    }
}
