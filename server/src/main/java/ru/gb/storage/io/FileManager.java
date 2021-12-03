package ru.gb.storage.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.parameters.P;
import ru.gb.storage.service.ConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileManager {

    public static final Path ROOT;

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

//    public static String generatePath(String name) {
//        String md5Hex = DigestUtils.md5Hex(name);
//        return Paths.get(
//                ROOT.toString(),
//                Character.toString(md5Hex.charAt(0)),
//                Character.toString(md5Hex.charAt(1)),
//                name
//        ).toString();
//    }

    public static Pathway generatePath(String name) {
        final String directories = Paths.get(
                ROOT.toString(),
                Character.toString(name.charAt(0)),
                Character.toString(name.charAt(1))
        ).toString();
        final String fullPath = Paths.get(
                directories,
                name
        ).toString();
        return new Pathway(directories, fullPath);
    }

    @AllArgsConstructor
    @Getter
    public static class Pathway {
        private String directories;
        private String fullPath;
    }
}
