package ru.gb.storage.dao;

import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;
import ru.gb.storage.service.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StorageDaoImpl implements StorageDao {

    private static final String TABLE_NAME = "storage";
    private static final String COL_ID = "id";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_CAPACITY = "capacity";
    private static final String NEW_INDEX = "currval";

    private Connection conn;
    private PreparedStatement prs;

    @Override
    public Optional<Storage> findById(long id) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));

            prs.setLong(1, id);

            final ResultSet rs = prs.executeQuery();

            if (rs.next()) {
                return Optional.of(
                        new Storage(
                                rs.getLong(COL_ID),
                                rs.getLong(COL_CAPACITY),
                                rs.getLong(COL_USER_ID)
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
    public List<Storage> findAll() {
        // not used right now
        return null;
    }

    @Override
    public long save(Storage storage) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s " +
                                    "(%s, %s) " +
                                    "VALUES (?, ?)",
                            TABLE_NAME,
                            COL_USER_ID,
                            COL_CAPACITY
                    ));

            prs.setLong(1, storage.getUserId());
            prs.setLong(2, storage.getCapacity());
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
    public void update(Storage storage) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                            TABLE_NAME,
                            COL_USER_ID, COL_CAPACITY,
                            COL_ID
                    ));

            prs.setLong(1, storage.getUserId());
            prs.setLong(2, storage.getCapacity());
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    @Override
    public void delete(Storage storage) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));
            prs.setLong(1, storage.getId());
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
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

    @Override
    public Optional<Storage> findStorageByUser(User user) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_USER_ID));

            prs.setLong(1, user.getId());

            final ResultSet rs = prs.executeQuery();

            if (rs.next()) {
                return Optional.of(
                        new Storage(
                                rs.getLong(COL_ID),
                                rs.getLong(COL_CAPACITY),
                                rs.getLong(COL_USER_ID)
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
}
