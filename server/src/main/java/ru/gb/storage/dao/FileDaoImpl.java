package ru.gb.storage.dao;

import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;
import ru.gb.storage.service.DataSource;

import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileDaoImpl implements FileDao {

    private static final String TABLE_NAME = "file";
    private static final String COL_ID = "id";
    private static final String COL_PATH = "path";
    private static final String COL_PARENT_ID = "parent_id";
    private static final String COL_STORAGE_ID = "storage_id";
    private static final String NEW_INDEX = "currval";

    private Connection conn;
    private PreparedStatement prs;

    @Override
    public Optional<File> findById(long id) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));

            prs.setLong(1, id);

            final ResultSet rs = prs.executeQuery();

            if (rs.next()) {
                return Optional.of(
                        new File(
                                rs.getLong(COL_ID),
                                Paths.get(rs.getString(COL_PATH)),
                                rs.getLong(COL_PARENT_ID),
                                rs.getLong(COL_STORAGE_ID)
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
    public List<File> findAll() {
        List<File> files = null;
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s", TABLE_NAME));

            final ResultSet rs = prs.executeQuery();

            if (!rs.next()) {
                return null;
            }

            files = new ArrayList<>();
            while (rs.next()) {
                files.add(
                        new File(
                                rs.getLong(COL_ID),
                                Paths.get(rs.getString(COL_PATH)),
                                rs.getLong(COL_PARENT_ID),
                                rs.getLong(COL_STORAGE_ID)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return files;
    }

    @Override
    public long save(File file) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s " +
                                    "(%s, %s, %s) " +
                                    "VALUES (?, ?, ?)",
                            TABLE_NAME,
                            COL_PATH,
                            COL_PARENT_ID,
                            COL_STORAGE_ID
                    ));

            prs.setString(1, file.getPath().toString());
            if (file.getParentId() == null) {
                prs.setNull(2, Types.BIGINT);
            } else {
                prs.setLong(2, file.getParentId());
            }
            prs.setLong(3, file.getStorageId());

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
    public void update(File file) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?",
                            TABLE_NAME,
                            COL_PATH, COL_PARENT_ID, COL_STORAGE_ID,
                            COL_ID
                    ));

            prs.setString(1, file.getPath().toString());
            prs.setLong(2, file.getParentId());
            prs.setLong(3, file.getStorageId());
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    @Override
    public void delete(File file) {
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, COL_ID));
            prs.setLong(1, file.getId());
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
    public List<File> getFilesByParent(File parentFile) {
        List<File> files = null;
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_PARENT_ID));

            prs.setLong(1, parentFile.getParentId());
            final ResultSet rs = prs.executeQuery();

            if (!rs.next()) {
                return null;
            }

            files = new ArrayList<>();
            while (rs.next()) {
                files.add(
                        new File(
                                rs.getLong(COL_ID),
                                Paths.get(rs.getString(COL_PATH)),
                                rs.getLong(COL_PARENT_ID),
                                rs.getLong(COL_STORAGE_ID)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return files;
    }

    @Override
    public List<File> getFilesByStorage(Storage storage) {
        List<File> files = null;
        try {
            conn = DataSource.getConnection();
            prs = conn.prepareStatement(
                    String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, COL_STORAGE_ID));

            prs.setLong(1, storage.getId());
            final ResultSet rs = prs.executeQuery();

            if (!rs.next()) {
                return null;
            }

            files = new ArrayList<>();
            while (rs.next()) {
                files.add(
                        new File(
                                rs.getLong(COL_ID),
                                Paths.get(rs.getString(COL_PATH)),
                                rs.getLong(COL_PARENT_ID),
                                rs.getLong(COL_STORAGE_ID)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return files;
    }
}
