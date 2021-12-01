package ru.gb.storage.io;

import org.apache.commons.codec.digest.DigestUtils;
import ru.gb.storage.service.ConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {

    public static final Path ROOT;
    public static final long DIRECTORY_SIZE = 4096;

    static {
        ROOT = initRootPackage();
    }

    private static Path initRootPackage() {

        final String basePackage = ConfigProperties
                .getPropertyValue("io.basePackage")
                .orElseThrow(NoSuchFieldError::new);

        final String rootPackage = ConfigProperties
                .getPropertyValue("io.rootPackage")
                .orElseThrow(NoSuchFieldError::new);

        Path path = Paths.get(basePackage, rootPackage);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static Path generatePath(String name) {
        String md5Hex = DigestUtils
                .md5Hex(name).toUpperCase();
        return Paths.get(
                ROOT.toString(),
                Character.toString(md5Hex.charAt(0)),
                Character.toString(md5Hex.charAt(1)),
                name
        );
    }
}
