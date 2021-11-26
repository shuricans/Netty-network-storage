package ru.gb.storage.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.gb.storage.model.User;


@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder encoder;

    @Override
    public boolean auth(User user) {
        final User userFromDB = userService.getUserByLogin(user.getLogin());
        if (userFromDB == null) {
            return false;
        }

        return encoder.matches(user.getPassword(), userFromDB.getPassword());
    }
}
