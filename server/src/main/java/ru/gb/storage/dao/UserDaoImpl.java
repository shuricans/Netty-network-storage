package ru.gb.storage.dao;

import ru.gb.storage.model.User;
import ru.gb.storage.service.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final String TABLE_NAME = "usr";
    private static final String COL_ID = "id";
    private static final String COL_LOGIN = "login";
    private static final String COL_PASSWORD = "password";
    private static final String NEW_INDEX = "currval";

    private Connection conn;
    private PreparedStatement prs;

    @Override
    public Optional<User> findById(long id) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));

            prs.setLong(1, id);

            final ResultSet rs = prs.executeQuery();

            if (rs.next()) {
                return Optional.of(
                        new User(
                                rs.getLong(COL_ID),
                                rs.getString(COL_LOGIN),
                                rs.getString(COL_PASSWORD)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = null;
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s", TABLE_NAME));

            final ResultSet rs = prs.executeQuery();

            users = new ArrayList<>();
            while (rs.next()) {
                users.add(new User(
                        rs.getLong(COL_ID),
                        rs.getString(COL_LOGIN),
                        rs.getString(COL_PASSWORD)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return users;
    }

    @Override
    public long save(User user) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s " +
                                    "(%s, %s) " +
                                    "VALUES (?, ?)",
                            TABLE_NAME,
                            COL_LOGIN,
                            COL_PASSWORD
                    ));

            prs.setString(1, user.getLogin());
            prs.setString(2, user.getPassword());
            prs.executeUpdate();

            prs = conn.prepareStatement(
                    String.format(
                            "SELECT %s(pg_get_serial_sequence('%s', '%s'))",
                            NEW_INDEX, TABLE_NAME, COL_ID
                    ));
            final ResultSet rs = prs.executeQuery();
            if (rs.next()) {
                return rs.getLong(NEW_INDEX);
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            disconnect();
        }
    }

    @Override
    public void update(User user) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                            TABLE_NAME,
                            COL_LOGIN, COL_PASSWORD,
                            COL_ID
                    ));
            prs.setString(1, user.getLogin());
            prs.setString(2, user.getPassword());
            prs.setLong(3, user.getId());
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    @Override
    public void delete(User user) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));
            prs.setLong(1, user.getId());
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_LOGIN));
            prs.setString(1, login);
            ResultSet resultSet = prs.executeQuery();
            if (resultSet.next()) {
                return Optional.of(
                        new User(
                                resultSet.getLong(COL_ID),
                                resultSet.getString(COL_LOGIN),
                                resultSet.getString(COL_PASSWORD)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        return Optional.empty();
    }

    private void disconnect() {
        if (prs != null) {
            try {
                prs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
