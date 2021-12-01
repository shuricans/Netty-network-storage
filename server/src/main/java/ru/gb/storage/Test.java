package ru.gb.storage;

import ru.gb.storage.io.FileManager;
import ru.gb.storage.model.File;
import ru.gb.storage.model.User;
import ru.gb.storage.service.ConfigProperties;
import ru.gb.storage.service.DataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Test {

    private static final String TABLE_NAME = "usr";
    private static final String COL_ID = "id";
    private static final String COL_LOGIN = "login";
    private static final String COL_PASSWORD = "password";
    private static final String NEW_INDEX = "currval";


    public static void main(String[] args) {

        final String basePackage = ConfigProperties
                .getPropertyValue("io.basePackage")
                .orElseThrow(NoSuchFieldError::new);

        final String rootPackage = ConfigProperties
                .getPropertyValue("io.rootPackage")
                .orElseThrow(NoSuchFieldError::new);

        Path path = Paths.get(basePackage, rootPackage);

        final Path test1 = Paths.get("main", "test", "1");
        System.out.println(test1);
        try {
            Files.createDirectories(test1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);


        final Path test = FileManager.generatePath("test");
        try {
            Files.createDirectory(test);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(test);
        System.exit(0);

        try {
            final Connection conn = DataSource.getConnection();
            PreparedStatement prs = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s " +
                                    "(%s, %s) " +
                                    "VALUES (?, ?)",
                            TABLE_NAME,
                            COL_LOGIN,
                            COL_PASSWORD
                    ));

            prs.setString(1, "login4");
            prs.setString(2, "pass4");
            prs.executeUpdate();

            prs = conn.prepareStatement(
                    String.format(
                            "SELECT %s(pg_get_serial_sequence('%s', '%s'))",
                            NEW_INDEX, TABLE_NAME, COL_ID
                    ));

            final ResultSet rs = prs.executeQuery();
            if(rs.next()) {
                System.out.println(rs.getLong(NEW_INDEX));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
